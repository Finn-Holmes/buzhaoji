package com.kexu.app.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kexu.app.MainActivity
import com.kexu.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SuppressLint("MissingPermission") // showReminder checks runtime, app, and channel permission before posting.
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(ReminderScheduler.EXTRA_RECORD_KEY)
        val generation = intent.getStringExtra(ReminderScheduler.EXTRA_GENERATION)
        if (key == null || generation == null) {
            ReminderRefreshWorker.enqueueRestore(context)
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000L) { ReminderDispatcher.deliver(context, key, generation) }
            } catch (_: Exception) {
                ReminderScheduler.enqueueDelivery(context, key, generation)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        internal fun showReminder(context: Context, intent: Intent): Boolean {
            val channel = when (intent.action) {
                ReminderScheduler.ACTION_TASK -> NotificationChannels.TASKS
                ReminderScheduler.ACTION_COURSE -> NotificationChannels.COURSES
                ReminderScheduler.ACTION_COUNTDOWN -> NotificationChannels.COUNTDOWNS
                ReminderScheduler.ACTION_HABIT -> NotificationChannels.HABITS
                else -> return false
            }
            NotificationChannels.create(context)
            val manager = NotificationManagerCompat.from(context)
            val scheduledAt = intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER_AT, System.currentTimeMillis())
            val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
            if ((Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) ||
                !manager.areNotificationsEnabled() ||
                manager.getNotificationChannel(channel)?.importance == NotificationManager.IMPORTANCE_NONE) {
                ReminderDeliveryLog.record(context, scheduledAt, title, "未提交：通知权限或提醒渠道已关闭")
                return false
            }
            try {
            when (intent.action) {
                ReminderScheduler.ACTION_TASK -> showTask(context, intent)
                ReminderScheduler.ACTION_COURSE -> showCourse(context, intent)
                ReminderScheduler.ACTION_COUNTDOWN -> showCountdown(context, intent)
                ReminderScheduler.ACTION_HABIT -> showHabit(context, intent)
            }
            } catch (_: SecurityException) {
                ReminderDeliveryLog.record(context, scheduledAt, title, "未提交：通知权限发生变化")
                return false
            }
            ReminderDeliveryLog.record(context, scheduledAt, title, "已提交系统通知")
            return true
        }

        private fun showTask(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, 0L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val dueAt = intent.getLongExtra(ReminderScheduler.EXTRA_DUE_AT, 0L)
        val open = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            Intent(context, MainActivity::class.java).putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val complete = actionIntent(context, ReminderActionReceiver.ACTION_COMPLETE, taskId, intent)
        val snooze = actionIntent(context, ReminderActionReceiver.ACTION_SNOOZE, taskId, intent)
        val body = if (dueAt > 0) "截止 ${formatTime(dueAt)}" else "任务时间到了"
        val notification = NotificationCompat.Builder(context, NotificationChannels.TASKS)
            .setSmallIcon(R.drawable.ic_notification_turtle)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(isRepeatDelivery(context, intent, ReminderScheduler.taskRequestCode(taskId)))
            .addExtras(deliveryExtras(intent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .apply {
                if (taskId > 0L) {
                    addAction(0, "完成", complete)
                    addAction(0, "稍后 10 分钟", snooze)
                }
            }
            .build()
        NotificationManagerCompat.from(context).notify(ReminderScheduler.taskRequestCode(taskId), notification)
    }

    private fun showCourse(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val location = intent.getStringExtra(ReminderScheduler.EXTRA_LOCATION).orEmpty()
        val startsAt = intent.getLongExtra(ReminderScheduler.EXTRA_START_AT, 0L)
        val open = PendingIntent.getActivity(
            context,
            title.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = buildString {
            append(formatTime(startsAt))
            if (location.isNotBlank()) append(" · $location")
        }
        val notification = NotificationCompat.Builder(context, NotificationChannels.COURSES)
            .setSmallIcon(R.drawable.ic_notification_turtle)
            .setContentTitle(if (System.currentTimeMillis() >= startsAt) "$title 上课了" else "$title 即将开始")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(isRepeatDelivery(context, intent, "$title:$startsAt".hashCode()))
            .addExtras(deliveryExtras(intent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        NotificationManagerCompat.from(context).notify("$title:$startsAt".hashCode(), notification)
    }

    private fun showCountdown(context: Context, intent: Intent) {
        val countdownId = intent.getLongExtra(ReminderScheduler.EXTRA_COUNTDOWN_ID, 0L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val dueAt = intent.getLongExtra(ReminderScheduler.EXTRA_DUE_AT, 0L)
        val open = PendingIntent.getActivity(
            context,
            ReminderScheduler.countdownRequestCode(countdownId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.COUNTDOWNS)
            .setSmallIcon(R.drawable.ic_notification_turtle)
            .setContentTitle(title)
            .setContentText("重要日子将在 ${formatTime(dueAt)} 到来")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(isRepeatDelivery(context, intent, ReminderScheduler.countdownRequestCode(countdownId)))
            .addExtras(deliveryExtras(intent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context)
            .notify(ReminderScheduler.countdownRequestCode(countdownId), notification)
    }

    private fun showHabit(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(ReminderScheduler.EXTRA_HABIT_ID, 0L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty()
        val open = PendingIntent.getActivity(
            context,
            ReminderScheduler.habitRequestCode(habitId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.HABITS)
            .setSmallIcon(R.drawable.ic_notification_turtle)
            .setContentTitle("习惯打卡")
            .setContentText("该坚持「$title」了")
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(isRepeatDelivery(context, intent, ReminderScheduler.habitRequestCode(habitId)))
            .addExtras(deliveryExtras(intent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(ReminderScheduler.habitRequestCode(habitId), notification)
    }

    private fun deliveryExtras(intent: Intent) = Bundle().apply {
        putString(ReminderScheduler.EXTRA_GENERATION, intent.getStringExtra(ReminderScheduler.EXTRA_GENERATION))
    }

    private fun isRepeatDelivery(context: Context, intent: Intent, notificationId: Int): Boolean {
        val generation = intent.getStringExtra(ReminderScheduler.EXTRA_GENERATION) ?: return false
        return NotificationManagerCompat.from(context).activeNotifications.any {
            it.id == notificationId && it.notification.extras.getString(ReminderScheduler.EXTRA_GENERATION) == generation
        }
    }

    private fun actionIntent(context: Context, action: String, taskId: Long, source: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            "$action:$taskId".hashCode(),
            Intent(context, ReminderActionReceiver::class.java).apply {
                this.action = action
                putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
                putExtra(ReminderScheduler.EXTRA_RECORD_KEY, source.getStringExtra(ReminderScheduler.EXTRA_RECORD_KEY))
                putExtra(ReminderScheduler.EXTRA_GENERATION, source.getStringExtra(ReminderScheduler.EXTRA_GENERATION))
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun formatTime(value: Long): String = Instant.ofEpochMilli(value)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    }
}
