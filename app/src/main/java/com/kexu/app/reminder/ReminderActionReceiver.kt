package com.kexu.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.kexu.app.KexuApplication
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.ui.nextRecurringTask
import com.kexu.app.widget.updateAllHomeWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, 0L)
        if (taskId == 0L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val generation = intent.getStringExtra(ReminderScheduler.EXTRA_GENERATION)
                val key = intent.getStringExtra(ReminderScheduler.EXTRA_RECORD_KEY)
                val record = if (generation != null && key != null) {
                    ReminderStore(context).records().find { it.key == key && it.generation == generation }
                        ?: return@launch
                } else null
                if (taskId == ReminderScheduler.TEST_TASK_ID) {
                    if (intent.action == ACTION_SNOOZE) {
                        val snoozeAt = System.currentTimeMillis() + 10 * 60_000L
                        ReminderScheduler(context).scheduleTask(
                            TaskEntity(
                                id = ReminderScheduler.TEST_TASK_ID,
                                title = ReminderScheduler.TEST_TASK_TITLE,
                                remindAt = snoozeAt,
                            ),
                        )
                    }
                    NotificationManagerCompat.from(context)
                        .cancel(ReminderScheduler.taskRequestCode(taskId))
                    return@launch
                }
                val app = context.applicationContext as KexuApplication
                val task = app.repository.task(taskId) ?: return@launch
                if (task.isCompleted || (record != null && ReminderRules.taskSource(task) != record.source)) return@launch
                when (intent.action) {
                    ACTION_COMPLETE -> {
                        if (task.dueAt != null && !task.recurrenceRule.isNullOrBlank()) {
                            val next = nextRecurringTask(task)
                            if (!app.repository.updateTaskIfUnchanged(task, next)) return@launch
                            ReminderScheduler(context).cancelTask(taskId)
                            ReminderScheduler(context).scheduleTask(next)
                        } else {
                            if (!app.repository.updateTaskIfUnchanged(task,
                                task.copy(isCompleted = true, completedAt = System.currentTimeMillis()),
                            )) return@launch
                            ReminderScheduler(context).cancelTask(taskId)
                        }
                    }
                    ACTION_SNOOZE -> ReminderScheduler(context).scheduleTask(
                        task,
                        overrideAt = System.currentTimeMillis() + 10 * 60_000L,
                    )
                }
                NotificationManagerCompat.from(context).cancel(ReminderScheduler.taskRequestCode(taskId))
                updateAllHomeWidgets(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.kexu.app.action.COMPLETE_TASK"
        const val ACTION_SNOOZE = "com.kexu.app.action.SNOOZE_TASK"
    }
}
