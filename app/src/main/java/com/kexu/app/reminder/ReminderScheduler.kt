package com.kexu.app.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.TaskEntity
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** One OS wakeup per occurrence. No resident service, polling, or held wake lock. */
class ReminderScheduler(context: Context) {
    private val context = context.applicationContext
    private val alarms = this.context.getSystemService(AlarmManager::class.java)
    private val store = ReminderStore(this.context)

    fun canScheduleExactReminders(): Boolean = ReminderRules.useExactAlarm(Build.VERSION.SDK_INT,
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms())

    fun scheduleTask(task: TaskEntity, overrideAt: Long? = null) {
        val record = ReminderRules.task(task, overrideAt) ?: return cancelTask(task.id)
        register(record, preserveSnooze = overrideAt == null)
    }

    fun cancelTask(taskId: Long) = cancelEntity(ACTION_TASK, taskId, "reminder-task-$taskId")

    fun scheduleHabit(habit: HabitEntity) {
        val record = ReminderRules.nextHabit(habit, System.currentTimeMillis(), ZoneId.systemDefault())
            ?: return cancelHabit(habit.id)
        register(record)
    }

    fun cancelHabit(habitId: Long) = cancelEntity(ACTION_HABIT, habitId, "reminder-habit-$habitId")

    fun scheduleCountdown(countdown: CountdownEntity) = register(
        ReminderRules.nextCountdown(countdown, System.currentTimeMillis(), ZoneId.systemDefault()))

    fun cancelCountdown(countdownId: Long) = cancelEntity(ACTION_COUNTDOWN, countdownId, "reminder-countdown-$countdownId")

    fun scheduleCourse(occurrence: CourseOccurrence) = register(ReminderRules.course(occurrence))

    private fun register(proposed: ReminderRecord, preserveSnooze: Boolean = true) = synchronized(ReminderStore.lock) {
        val records = store.records()
        val old = records.find { it.key == proposed.key }
        val record = ReminderRules.merge(old, proposed, preserveSnooze)
        val obsolete = if (proposed.action == ACTION_HABIT || proposed.action == ACTION_COUNTDOWN) records.filter {
            it.action == proposed.action && it.entityId == proposed.entityId && it.source != proposed.source
        } else emptyList()
        val updated = records.filterNot { it.key == record.key || it in obsolete }.map {
            if (it.action == proposed.action && it.entityId == proposed.entityId && it.source == proposed.source)
                it.copy(title = proposed.title, location = proposed.location) else it
        } + record
        if (updated != records) {
            store.replace(updated)
            obsolete.forEach(::cancelPlatform)
            if (old != null && record != old) cancelPlatform(old)
        }
        if (record.deliveredAt == null && record.expiresAt > System.currentTimeMillis()) arm(record)
    }

    private fun cancelEntity(action: String, id: Long, legacyName: String) = synchronized(ReminderStore.lock) {
        val records = store.records()
        val removed = records.filter { it.action == action && it.entityId == id }
        // A revision also invalidates reconciliation snapshots when this entry is already absent.
        store.replace(records - removed.toSet())
        removed.forEach(::cancelPlatform)
        WorkManager.getInstance(context).cancelUniqueWork(legacyName)
        Unit
    }

    @SuppressLint("ScheduleExactAlarm")
    internal fun arm(record: ReminderRecord) {
        val pending = pendingIntent(record, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val at = record.triggerAt.coerceAtLeast(System.currentTimeMillis() + 100L)
        try {
            if (canScheduleExactReminders()) {
                try {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
                    return
                } catch (_: SecurityException) {
                    // Access may be revoked between the check and the binder call.
                }
            }
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } catch (_: RuntimeException) {
            enqueueDelivery(context, record.key, record.generation, (at - System.currentTimeMillis()).coerceAtLeast(0L))
            ReminderDeliveryLog.record(context, record.triggerAt, record.title, "系统定时受限，已降级为后台补发")
        }
    }

    internal fun cancelPlatform(record: ReminderRecord) {
        pendingIntent(record, PendingIntent.FLAG_NO_CREATE)?.let { alarms.cancel(it); it.cancel() }
        WorkManager.getInstance(context).cancelUniqueWork(deliveryWorkName(record.key, record.generation))
    }

    private fun pendingIntent(record: ReminderRecord, flags: Int): PendingIntent? = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DISPATCH
            data = Uri.Builder().scheme("kexu").authority("reminder").appendPath(record.key).build()
            putExtra(EXTRA_RECORD_KEY, record.key)
            putExtra(EXTRA_GENERATION, record.generation)
        }, flags or PendingIntent.FLAG_IMMUTABLE)

    /** Migration only; current occurrences, including tests and snoozes, are never cleared. */
    internal fun migrateLegacy() {
        val preferences = context.getSharedPreferences("scheduled_alarms", Context.MODE_PRIVATE)
        if (preferences.getBoolean("migrated_to_occurrences_v2", false)) return
        WorkManager.getInstance(context).cancelAllWorkByTag("kexu-reminders")
        preferences.getStringSet("registered", emptySet()).orEmpty().forEach { encoded ->
            val parts = encoded.split("\u001F", limit = 3)
            val requestCode = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = parts.getOrNull(1).orEmpty()
                data = Uri.parse(parts.getOrNull(2).orEmpty())
            }
            PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let { alarms.cancel(it); it.cancel() }
        }
        preferences.edit().remove("registered").putBoolean("migrated_to_occurrences_v2", true).apply()
    }

    companion object {
        const val TEST_TASK_ID = -999L
        const val TEST_TASK_TITLE = "不着急测试提醒"
        const val ACTION_TASK = "com.kexu.app.action.TASK_REMINDER"
        const val ACTION_COURSE = "com.kexu.app.action.COURSE_REMINDER"
        const val ACTION_COUNTDOWN = "com.kexu.app.action.COUNTDOWN_REMINDER"
        const val ACTION_HABIT = "com.kexu.app.action.HABIT_REMINDER"
        internal const val ACTION_DISPATCH = "com.kexu.app.action.DISPATCH_REMINDER"
        internal const val EXTRA_RECORD_KEY = "record_key"
        internal const val EXTRA_GENERATION = "generation"
        internal const val EXTRA_TRIGGER_AT = "trigger_at"
        const val EXTRA_ACTION = "reminder_action"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_COUNTDOWN_ID = "countdown_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DUE_AT = "due_at"
        const val EXTRA_TARGET_EPOCH_DAY = "target_epoch_day"
        const val EXTRA_REPEAT_YEARLY = "repeat_yearly"
        const val EXTRA_REMINDER_DAYS = "reminder_days"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_START_AT = "start_at"
        const val EXTRA_HABIT_ID = "habit_id"
        const val EXTRA_MINUTES = "minutes"

        private fun deliveryWorkName(key: String, generation: String) = "reminder-delivery:$key:$generation"

        internal fun enqueueDelivery(context: Context, key: String, generation: String, delayMillis: Long = 0) {
            val request = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
                .setInputData(Data.Builder().putString(EXTRA_RECORD_KEY, key).putString(EXTRA_GENERATION, generation).build())
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork(deliveryWorkName(key, generation), ExistingWorkPolicy.KEEP, request)
        }

        fun taskRequestCode(taskId: Long): Int = "task:$taskId".hashCode()
        fun countdownRequestCode(countdownId: Long): Int = "countdown:$countdownId".hashCode()
        fun habitRequestCode(habitId: Long): Int = "habit:$habitId".hashCode()
    }
}
