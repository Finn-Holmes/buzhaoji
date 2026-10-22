package com.kexu.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import com.kexu.app.KexuApplication
import com.kexu.app.MainActivity
import com.kexu.app.R
import com.kexu.app.settings.FocusTimerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Traditional RemoteViews implementation for the Pomo widget.
 *
 * A native Chronometer keeps ticking in the launcher without waking the app every second,
 * which is both smoother and much more power-efficient than periodic Glance recomposition.
 */
class PomodoroRemoteWidgetReceiver : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager,
        appWidgetId: Int, newOptions: android.os.Bundle) {
        updateAsync(context, manager, intArrayOf(appWidgetId))
    }
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        updateAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> toggleAsync(context)
            ACTION_FINISH -> finishAsync(context)
        }
    }

    private fun updateAsync(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as KexuApplication
                val timer = app.settingsRepository.snapshot().focusTimer
                updateInstances(context, manager, ids, timer)
                syncFinishAlarm(context, timer)
            } finally {
                pending.finish()
            }
        }
    }

    private fun toggleAsync(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as KexuApplication
                val timer = app.settingsRepository.snapshot().focusTimer
                val updated = toggledTimer(timer, System.currentTimeMillis())
                app.settingsRepository.setFocusTimer(updated)
                updateAll(context, updated)
            } finally {
                pending.finish()
            }
        }
    }

    private fun finishAsync(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as KexuApplication
                val timer = app.settingsRepository.snapshot().focusTimer
                if (timer.running && !timer.positiveMode && displayedSeconds(timer) <= 1) {
                    app.repository.saveFocusSession(com.kexu.app.data.model.FocusSessionEntity(
                        taskId = timer.selectedTaskId, durationSeconds = timer.durationMinutes * 60,
                        startedAt = timer.startedAtMillis,
                        completedAt = timer.anchorAtMillis + timer.remainingSeconds * 1000L,
                    ))
                    val reset = timer.copy(
                        remainingSeconds = timer.durationMinutes * 60,
                        elapsedSeconds = 0,
                        running = false,
                        startedAtMillis = 0L,
                        anchorAtMillis = 0L,
                    )
                    app.settingsRepository.setFocusTimer(reset)
                    updateAll(context, reset)
                } else {
                    updateAll(context, timer)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_TOGGLE = "com.kexu.app.widget.POMODORO_TOGGLE"
        private const val ACTION_FINISH = "com.kexu.app.widget.POMODORO_FINISH"
        private const val FINISH_REQUEST_CODE = 8_431

        suspend fun updateAll(context: Context) {
            val app = context.applicationContext as KexuApplication
            updateAll(context, app.settingsRepository.snapshot().focusTimer)
        }

        private fun updateAll(context: Context, timer: FocusTimerSettings) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, PomodoroRemoteWidgetReceiver::class.java),
            )
            updateInstances(context, manager, ids, timer)
            syncFinishAlarm(context, timer)
        }

        private fun updateInstances(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            timer: FocusTimerSettings,
        ) {
            val seconds = displayedSeconds(timer)
            ids.forEach { appWidgetId ->
                val settings = WidgetPreferences.settings(context, appWidgetId)
                val dark = when (settings.theme) {
                    WidgetTheme.DARK -> true
                    WidgetTheme.LIGHT -> false
                    WidgetTheme.FOLLOW_SYSTEM ->
                        context.resources.configuration.uiMode and
                            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                }
                val alpha = (settings.opacity.coerceIn(20, 100) * 255 / 100)
                val background = if (dark) {
                    Color.argb(alpha, 36, 36, 38)
                } else {
                    Color.argb(alpha, 253, 253, 253)
                }
                val primary = if (dark) Color.WHITE else Color.rgb(35, 35, 38)
                val secondary = if (dark) Color.rgb(184, 184, 190) else Color.rgb(105, 105, 112)
                val views = RemoteViews(context.packageName, R.layout.widget_pomodoro)
                views.setInt(R.id.widget_pomodoro_background, "setColorFilter", background)
                views.setTextColor(R.id.widget_pomodoro_toggle, primary)
                views.setTextColor(R.id.widget_pomodoro_settings, secondary)
                views.setTextColor(R.id.widget_pomodoro_time, if (dark) Color.rgb(240, 195, 174) else Color.rgb(146, 89, 108))
                views.setContentDescription(R.id.widget_pomodoro_toggle, if (timer.running) "暂停专注" else "开始专注")
                views.setContentDescription(R.id.widget_pomodoro_settings, "专注小组件设置")
                views.setContentDescription(R.id.widget_pomodoro_time, "打开专注计时")
                views.setTextViewText(R.id.widget_pomodoro_toggle, if (timer.running) "Ⅱ" else "▶")

                val base = if (timer.positiveMode) {
                    SystemClock.elapsedRealtime() - seconds * 1_000L
                } else {
                    // Chronometer truncates fractional countdown seconds after IPC delivery.
                    SystemClock.elapsedRealtime() + seconds * 1_000L + 999L
                }
                views.setChronometerCountDown(R.id.widget_pomodoro_time, !timer.positiveMode)
                views.setChronometer(
                    R.id.widget_pomodoro_time,
                    base,
                    "%s",
                    timer.running,
                )
                views.setOnClickPendingIntent(
                    R.id.widget_pomodoro_time,
                    openFocusIntent(context, appWidgetId),
                )
                views.setOnClickPendingIntent(
                    R.id.widget_pomodoro_toggle,
                    toggleIntent(context, appWidgetId),
                )
                views.setOnClickPendingIntent(
                    R.id.widget_pomodoro_settings,
                    configIntent(context, appWidgetId),
                )
                manager.updateAppWidget(appWidgetId, views)
            }
        }

        internal fun displayedSeconds(
            timer: FocusTimerSettings,
            nowMillis: Long = System.currentTimeMillis(),
        ): Int {
            val wallSeconds = if (timer.running && timer.anchorAtMillis > 0L) {
                ((nowMillis - timer.anchorAtMillis).coerceAtLeast(0L) / 1_000L).toInt()
            } else {
                0
            }
            return if (timer.positiveMode) {
                timer.elapsedSeconds + wallSeconds
            } else {
                (timer.remainingSeconds - wallSeconds).coerceAtLeast(0)
            }
        }

        internal fun toggledTimer(timer: FocusTimerSettings, nowMillis: Long): FocusTimerSettings {
            val seconds = displayedSeconds(timer, nowMillis)
            return if (timer.running) {
                timer.copy(
                    remainingSeconds = if (timer.positiveMode) timer.remainingSeconds else seconds,
                    elapsedSeconds = if (timer.positiveMode) seconds else timer.elapsedSeconds,
                    running = false,
                    anchorAtMillis = 0L,
                )
            } else {
                timer.copy(
                    remainingSeconds = if (!timer.positiveMode && seconds == 0) {
                        timer.durationMinutes * 60
                    } else {
                        seconds
                    },
                    elapsedSeconds = if (timer.positiveMode) seconds else timer.elapsedSeconds,
                    running = true,
                    startedAtMillis = if (timer.startedAtMillis == 0L) nowMillis else timer.startedAtMillis,
                    anchorAtMillis = nowMillis,
                )
            }
        }

        private fun syncFinishAlarm(context: Context, timer: FocusTimerSettings) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val pendingIntent = finishIntent(context)
            if (!timer.running || timer.positiveMode) {
                alarmManager.cancel(pendingIntent)
                return
            }
            val seconds = displayedSeconds(timer)
            if (seconds <= 0) return
            val triggerAt = System.currentTimeMillis() + seconds * 1_000L
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }
        }

        private fun openFocusIntent(context: Context, appWidgetId: Int): PendingIntent =
            PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 1,
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_WIDGET_ACTION, MainActivity.ACTION_OPEN_FOCUS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun configIntent(context: Context, appWidgetId: Int): PendingIntent =
            PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 2,
                Intent(context, WidgetConfigActivity::class.java)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun toggleIntent(context: Context, appWidgetId: Int): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 3,
                Intent(context, PomodoroRemoteWidgetReceiver::class.java)
                    .setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun finishIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                FINISH_REQUEST_CODE,
                Intent(context, PomodoroRemoteWidgetReceiver::class.java)
                    .setAction(ACTION_FINISH),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
