package com.kexu.app.reminder

import android.content.Context
import android.content.Intent
import com.kexu.app.KexuApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZoneId

internal object ReminderDispatcher {
    private val deliveryMutex = Mutex()

    suspend fun deliver(context: Context, key: String, generation: String) = withContext(Dispatchers.IO) {
        deliveryMutex.withLock {
            val store = ReminderStore(context)
            val record = store.records().find { it.key == key } ?: return@withLock
            if (record.generation != generation || record.deliveredAt != null) return@withLock
            val scheduler = ReminderScheduler(context)
            if (System.currentTimeMillis() < record.triggerAt) {
                scheduler.arm(record) // A clock correction moved this occurrence back into the future.
                return@withLock
            }
            val app = context.applicationContext as KexuApplication
            val snapshot = ReminderSnapshot.load(app.repository, app.settingsRepository)
            val valid = snapshot.valid(record, ZoneId.systemDefault())
            val alreadyChecked = record.action == ReminderScheduler.ACTION_HABIT && valid &&
                app.repository.currentHabitChecks(record.occurrenceDay).any { it.habitId == record.entityId }
            synchronized(ReminderStore.lock) {
                val records = store.records()
                val current = records.find { it.key == key } ?: return@synchronized
                if (current.generation != generation || current.deliveredAt != null) return@synchronized
                val now = System.currentTimeMillis()
                if (now < current.triggerAt) {
                    scheduler.arm(current)
                    return@synchronized
                }
                if (!valid) {
                    store.replace(records.filterNot { it.key == key })
                    scheduler.cancelPlatform(current)
                    ReminderDeliveryLog.record(context, current.triggerAt, current.title, "已跳过：事项已删除或提醒已变更")
                    return@synchronized
                }
                val consumed = when {
                    now >= current.expiresAt -> {
                        ReminderDeliveryLog.record(context, current.triggerAt, current.title, "已跳过：超过补发时限")
                        true
                    }
                    alreadyChecked -> {
                        ReminderDeliveryLog.record(context, current.triggerAt, current.title, "已跳过：当天已打卡")
                        true
                    }
                    else -> ReminderReceiver.showReminder(context, current.toIntent())
                }
                if (consumed) store.replace(records.map { if (it.key == key) it.copy(deliveredAt = now) else it })
                // Each day/year has a different occurrence key; a running fallback worker is never REPLACEd.
                when (current.action) {
                    ReminderScheduler.ACTION_HABIT -> snapshot.habits[current.entityId]?.let(scheduler::scheduleHabit)
                    ReminderScheduler.ACTION_COUNTDOWN -> snapshot.countdowns[current.entityId]
                        ?.takeIf { it.repeatYearly }?.let(scheduler::scheduleCountdown)
                }
            }
        }
    }

    private fun ReminderRecord.toIntent(): Intent = Intent(action).apply {
        putExtra(ReminderScheduler.EXTRA_RECORD_KEY, key)
        putExtra(ReminderScheduler.EXTRA_GENERATION, generation)
        putExtra(ReminderScheduler.EXTRA_TRIGGER_AT, triggerAt)
        putExtra(ReminderScheduler.EXTRA_TITLE, title)
        putExtra(ReminderScheduler.EXTRA_TASK_ID, entityId)
        putExtra(ReminderScheduler.EXTRA_HABIT_ID, entityId)
        putExtra(ReminderScheduler.EXTRA_COUNTDOWN_ID, entityId)
        putExtra(ReminderScheduler.EXTRA_DUE_AT, dueAt)
        putExtra(ReminderScheduler.EXTRA_START_AT, dueAt)
        putExtra(ReminderScheduler.EXTRA_LOCATION, location)
    }
}
