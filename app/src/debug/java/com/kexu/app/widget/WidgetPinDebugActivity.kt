package com.kexu.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity

/** Debug-only launcher pin request used by emulator verification. */
class WidgetPinDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = AppWidgetManager.getInstance(this)
        val receiver = when (intent.getStringExtra(EXTRA_WIDGET)) {
            "compact" -> CompactTaskWidgetReceiver::class.java
            "habit" -> HabitWidgetReceiver::class.java
            "timetable" -> TimetableWidgetReceiver::class.java
            "today" -> TodayWidgetReceiver::class.java
            "countdown" -> CountdownWidgetReceiver::class.java
            "countdown_list" -> CountdownListWidgetReceiver::class.java
            "stats" -> TaskStatsWidgetReceiver::class.java
            else -> PomodoroRemoteWidgetReceiver::class.java
        }
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(
                ComponentName(this, receiver),
                null,
                null,
            )
        }
        finish()
    }

    companion object {
        const val EXTRA_WIDGET = "widget"
    }
}
