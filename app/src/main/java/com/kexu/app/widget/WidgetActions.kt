package com.kexu.app.widget

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import com.kexu.app.KexuApplication
import com.kexu.app.data.model.HabitCheckEntity
import com.kexu.app.reminder.ReminderScheduler
import com.kexu.app.ui.nextRecurringTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.time.LocalDate

internal val TaskIdKey = ActionParameters.Key<Long>("task_id")
internal val TaskVersionKey = ActionParameters.Key<String>("task_version")
internal val TaskCompletedTargetKey = ActionParameters.Key<Boolean>("task_completed_target")
internal val HabitIdKey = ActionParameters.Key<Long>("habit_id")
internal val HabitDateKey = ActionParameters.Key<Long>("habit_date")
internal val HabitCheckedTargetKey = ActionParameters.Key<Boolean>("habit_checked_target")
internal val WeekDeltaKey = ActionParameters.Key<Int>("week_delta")
internal val WidgetIdKey = ActionParameters.Key<Int>("app_widget_id")

class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val app = context.applicationContext as KexuApplication
        val task = app.repository.task(taskId) ?: return
        val completed = parameters[TaskCompletedTargetKey]
        if (completed == null || parameters[TaskVersionKey] != task.widgetVersion() || completed == task.isCompleted) {
            updateAllHomeWidgets(context)
            return
        }
        val scheduler = ReminderScheduler(context)
        if (completed) {
            updateAppWidgetState(context, glanceId) { state ->
                state[CompletingTaskIdKey] = taskId
                state[CompletingTaskUntilKey] = System.currentTimeMillis() + 5_000L
            }
        }
        try {
        val effectiveTask =
        if (completed && task.dueAt != null && !task.recurrenceRule.isNullOrBlank()) {
            val next = nextRecurringTask(task)
            if (!app.repository.updateTaskIfUnchanged(task, next)) return
            scheduler.cancelTask(taskId)
            scheduler.scheduleTask(next)
            next
        } else {
            val updated = task.copy(
                isCompleted = completed,
                completedAt = if (completed) System.currentTimeMillis() else null,
            )
            if (!app.repository.updateTaskIfUnchanged(task, updated)) return
            if (completed) scheduler.cancelTask(taskId) else scheduler.scheduleTask(updated)
            updated
        }
        syncTaskWidgetState(context, effectiveTask)
        if (completed) {
            withContext(Dispatchers.Main) {
                val audio = context.getSystemService(AudioManager::class.java)
                if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
                    Settings.System.getInt(context.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 0) == 1
                ) {
                    audio.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN)
                }
                Toast.makeText(context,
                    if (effectiveTask.isCompleted) "已完成：${task.title}" else "已完成本次，下次任务已安排",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        updateAllHomeWidgets(context)
        if (completed) delay(500)
        } finally {
            if (completed) {
                updateAppWidgetState(context, glanceId) { state ->
                    if (state[CompletingTaskIdKey] == taskId) {
                        state.remove(CompletingTaskIdKey)
                        state.remove(CompletingTaskUntilKey)
                    }
                }
                val compactIds = GlanceAppWidgetManager(context).getGlanceIds(CompactTaskWidget::class.java)
                if (glanceId in compactIds) CompactTaskWidget().update(context, glanceId)
                else TodayWidget().update(context, glanceId)
            }
        }
    }
}

internal suspend fun syncTaskWidgetState(context: Context, task: com.kexu.app.data.model.TaskEntity) {
    val manager = GlanceAppWidgetManager(context)
    val ids = manager.getGlanceIds(CompactTaskWidget::class.java) +
        manager.getGlanceIds(TodayWidget::class.java)
    ids.forEach { id ->
        updateAppWidgetState(context, id) { state ->
            state[taskCompletedStateKey(task.id)] = task.isCompleted
            state[taskDueAtStateKey(task.id)] = task.dueAt ?: -1L
        }
    }
}

class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val habitId = parameters[HabitIdKey] ?: return
        val app = context.applicationContext as KexuApplication
        val today = LocalDate.now().toEpochDay()
        if (parameters[HabitDateKey] != today || app.repository.currentHabits().none { it.id == habitId }) {
            updateAllHomeWidgets(context)
            return
        }
        val checked = app.repository.currentHabitChecks(today).any { it.habitId == habitId }
        val target = parameters[HabitCheckedTargetKey] ?: !checked
        if (target == checked) return
        if (!target) {
            app.repository.deleteHabitCheck(habitId, today)
        } else {
            app.repository.saveHabitCheck(HabitCheckEntity(habitId, today))
        }
        updateAllHomeWidgets(context)
    }
}

class ShiftTimetableWeekAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val appWidgetId = parameters[WidgetIdKey]
            ?: WidgetPreferences.appWidgetId(context, glanceId).takeIf { it != 0 }
            ?: return
        val delta = parameters[WeekDeltaKey] ?: 0
        if (delta == 0) return
        WidgetPreferences.shiftTimetableWeek(context, appWidgetId, delta)
        TimetableWidget().update(context, glanceId)
    }
}

class RefreshWidgetsAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAllHomeWidgets(context)
    }
}

class ToggleCompactMenuAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { state ->
            state[CompactMenuVisibleStateKey] = !(state[CompactMenuVisibleStateKey] ?: false)
        }
        CompactTaskWidget().update(context, glanceId)
    }
}

class RefreshCompactWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, glanceId) { state ->
            state[CompactMenuVisibleStateKey] = false
        }
        CompactTaskWidget().update(context, glanceId)
    }
}
