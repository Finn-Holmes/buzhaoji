package com.kexu.app.widget

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.currentState
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.unit.ColorProvider
import java.time.LocalDate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import com.kexu.app.KexuApplication

internal enum class WidgetTheme {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK,
}

internal data class HomeWidgetSettings(
    val opacity: Int = 90,
    val theme: WidgetTheme = WidgetTheme.FOLLOW_SYSTEM,
    val selectedItemId: Long = 0L,
    val timetableWeekOffset: Int = 0,
    val timetableNavigationEpochDay: Long = Long.MIN_VALUE,
    val timetableStartLesson: Int = 1,
    val timetableEndLesson: Int = 12,
    val showCompleted: Boolean = false,
)

internal val WidgetOpacityStateKey = intPreferencesKey("widget_opacity")
internal val WidgetThemeStateKey = stringPreferencesKey("widget_theme")
internal val TimetableWeekOffsetStateKey = intPreferencesKey("timetable_week_offset")
internal val CompactMenuVisibleStateKey = booleanPreferencesKey("compact_menu_visible")
internal val CompletingTaskIdKey = longPreferencesKey("completing_task_id")
internal val CompletingTaskUntilKey = longPreferencesKey("completing_task_until")
internal fun taskCompletedStateKey(taskId: Long) = booleanPreferencesKey("task_${taskId}_completed")
internal fun taskDueAtStateKey(taskId: Long) = longPreferencesKey("task_${taskId}_due_at")

internal const val COMPACT_FILTER_ALL = 0L
internal const val COMPACT_FILTER_TODAY = -1L
internal const val COMPACT_FILTER_TOMORROW = -2L
internal const val COMPACT_FILTER_NEXT_7_DAYS = -3L
internal const val COMPACT_FILTER_INBOX = -4L

abstract class KexuGlanceWidget : GlanceAppWidget() {
    final override val stateDefinition = PreferencesGlanceStateDefinition
}

internal object WidgetPreferences {
    private const val FILE_NAME = "home_widget_preferences"
    private const val DEFAULT_OPACITY = 90

    fun observe(context: Context, glanceId: GlanceId) = callbackFlow {
        val id = appWidgetId(context, glanceId)
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(settings(context, id))
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(settings(context, id))
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().distinctUntilChanged()

    fun appWidgetId(context: Context, glanceId: GlanceId): Int =
        runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrDefault(0)

    fun settings(context: Context, glanceId: GlanceId): HomeWidgetSettings =
        settings(context, appWidgetId(context, glanceId))

    fun settings(context: Context, appWidgetId: Int): HomeWidgetSettings {
        val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return HomeWidgetSettings(
            opacity = preferences.getInt(key(appWidgetId, "opacity"), DEFAULT_OPACITY)
                .coerceIn(20, 100),
            theme = preferences.getString(key(appWidgetId, "theme"), null)
                ?.let { stored -> WidgetTheme.entries.firstOrNull { it.name == stored } }
                ?: WidgetTheme.FOLLOW_SYSTEM,
            selectedItemId = preferences.getLong(key(appWidgetId, "selected_item"), 0L),
            timetableWeekOffset = preferences.getInt(key(appWidgetId, "week_offset"), 0)
                .coerceIn(-30, 30),
            timetableNavigationEpochDay = preferences.getLong(
                key(appWidgetId, "week_navigation_epoch_day"),
                Long.MIN_VALUE,
            ),
            timetableStartLesson = preferences.getInt(key(appWidgetId, "start_lesson"), 1)
                .coerceIn(1, 20),
            timetableEndLesson = preferences.getInt(key(appWidgetId, "end_lesson"), 12)
                .coerceIn(1, 20),
            showCompleted = preferences.getBoolean(key(appWidgetId, "show_completed"), false),
        )
    }

    fun save(context: Context, appWidgetId: Int, settings: HomeWidgetSettings) {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit()
            .putInt(key(appWidgetId, "opacity"), settings.opacity.coerceIn(20, 100))
            .putString(key(appWidgetId, "theme"), settings.theme.name)
            .putLong(key(appWidgetId, "selected_item"), settings.selectedItemId)
            .putInt(key(appWidgetId, "week_offset"), settings.timetableWeekOffset.coerceIn(-30, 30))
            .putLong(
                key(appWidgetId, "week_navigation_epoch_day"),
                settings.timetableNavigationEpochDay,
            )
            .putInt(key(appWidgetId, "start_lesson"), settings.timetableStartLesson.coerceIn(1, 20))
            .putInt(key(appWidgetId, "end_lesson"), settings.timetableEndLesson.coerceIn(1, 20))
            .putBoolean(key(appWidgetId, "show_completed"), settings.showCompleted)
            .commit()
    }

    fun shiftTimetableWeek(context: Context, glanceId: GlanceId, delta: Int) {
        val appWidgetId = appWidgetId(context, glanceId)
        if (appWidgetId == 0) return
        shiftTimetableWeek(context, appWidgetId, delta)
    }

    @Synchronized fun shiftTimetableWeek(context: Context, appWidgetId: Int, delta: Int) {
        if (appWidgetId == 0) return
        val current = settings(context, appWidgetId)
        val today = LocalDate.now().toEpochDay()
        val offset = if (current.timetableNavigationEpochDay == today) current.timetableWeekOffset else 0
        // Apply updates the in-memory pair atomically; disk persistence need not block a click.
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit()
            .putInt(key(appWidgetId, "week_offset"), (offset + delta).coerceIn(-30, 30))
            .putLong(key(appWidgetId, "week_navigation_epoch_day"), today)
            .apply()
    }

    private fun key(appWidgetId: Int, name: String) = "${name}_$appWidgetId"
}

internal suspend fun widgetGlanceId(context: Context, appWidgetId: Int): GlanceId =
    GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)

@Composable
internal fun widgetSurface(
    context: Context,
    glanceId: GlanceId,
    darkColor: Color = Color(0xFF242426),
    lightColor: Color = Color(0xFFFDFDFD),
    darkOverride: Boolean? = null,
): ColorProvider {
    val stored = WidgetPreferences.settings(context, glanceId)
    val state = currentState<Preferences>()
    val opacity = state[WidgetOpacityStateKey] ?: stored.opacity
    val theme = state[WidgetThemeStateKey]
        ?.let { saved -> WidgetTheme.entries.firstOrNull { it.name == saved } }
        ?: stored.theme
    val systemDark =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    val dark = darkOverride ?: when (theme) {
        WidgetTheme.FOLLOW_SYSTEM -> systemDark
        WidgetTheme.LIGHT -> false
        WidgetTheme.DARK -> true
    }
    val color = if (dark) darkColor else lightColor
    return ColorProvider(color.copy(alpha = opacity.coerceIn(20, 100) / 100f))
}

@Composable
internal fun widgetIsDark(context: Context, glanceId: GlanceId): Boolean {
    val stored = WidgetPreferences.settings(context, WidgetPreferences.appWidgetId(context, glanceId))
    val savedTheme = currentState<Preferences>()[WidgetThemeStateKey]
        ?.let { saved -> WidgetTheme.entries.firstOrNull { it.name == saved } }
        ?: stored.theme
    val systemDark =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    return when (savedTheme) {
        WidgetTheme.FOLLOW_SYSTEM -> systemDark
        WidgetTheme.LIGHT -> false
        WidgetTheme.DARK -> true
    }
}

internal fun widgetIsDark(context: Context, appWidgetId: Int): Boolean {
    val settings = WidgetPreferences.settings(context, appWidgetId)
    val systemDark =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    return when (settings.theme) {
        WidgetTheme.FOLLOW_SYSTEM -> systemDark
        WidgetTheme.LIGHT -> false
        WidgetTheme.DARK -> true
    }
}

suspend fun updateAllHomeWidgets(context: Context) {
    // Data-changing call sites publish fresh timetable data before requesting any new render.
    TimetableCache.refresh(context.applicationContext as KexuApplication)
    TodayWidget().updateAll(context)
    CompactTaskWidget().updateAll(context)
    TimetableWidget().updateAll(context)
    HabitWidget().updateAll(context)
    CountdownWidget().updateAll(context)
    CountdownListWidget().updateAll(context)
    PomodoroRemoteWidgetReceiver.updateAll(context)
    TaskStatsWidget().updateAll(context)
}
