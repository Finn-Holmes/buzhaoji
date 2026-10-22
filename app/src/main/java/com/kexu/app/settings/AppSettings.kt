package com.kexu.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("kexu_settings")

data class AppSettings(
    val semesterStartMillis: Long,
    val defaultCourseReminderMinutes: Int,
    val defaultTaskReminderMinutes: Int,
    val enabledFeatures: Set<FeatureModule>,
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val focusTimer: FocusTimerSettings = FocusTimerSettings(),
    val companionMotion: Boolean = true,
)

data class FocusTimerSettings(
    val positiveMode: Boolean = false,
    val durationMinutes: Int = 25,
    val remainingSeconds: Int = 25 * 60,
    val elapsedSeconds: Int = 0,
    val running: Boolean = false,
    val startedAtMillis: Long = 0L,
    val anchorAtMillis: Long = 0L,
    val selectedTaskId: Long? = null,
)

enum class AppTheme(val displayName: String) {
    DEFAULT("默认"),
    SECRET_CYAN("秘青"),
    TURQUOISE("松石"),
    REED("蒹葭"),
    APRICOT("杏黄"),
    PEACH("桃夭"),
    PURPLE("暮山紫"),
    FROST("霜白"),
    ROCK("岩灰"),
    NIGHT("暗夜"),
    PRISM("百变"),
}

enum class ThemeMode(val displayName: String) {
    FOLLOW_SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

enum class FeatureModule {
    QUADRANT,
    FOCUS,
    HABITS,
    COUNTDOWN,
    SEARCH,
}

class SettingsRepository(private val context: Context) {
    private val companionMotion = booleanPreferencesKey("companion_motion")
    private val semesterStart = longPreferencesKey("semester_start")
    private val courseReminder = intPreferencesKey("course_reminder")
    private val taskReminder = intPreferencesKey("task_reminder")
    private val enabledFeatures = stringSetPreferencesKey("enabled_features")
    private val appTheme = stringPreferencesKey("app_theme")
    private val themeMode = stringPreferencesKey("theme_mode")
    private val focusPositiveMode = booleanPreferencesKey("focus_positive_mode")
    private val focusDurationMinutes = intPreferencesKey("focus_duration_minutes")
    private val focusRemainingSeconds = intPreferencesKey("focus_remaining_seconds")
    private val focusElapsedSeconds = intPreferencesKey("focus_elapsed_seconds")
    private val focusRunning = booleanPreferencesKey("focus_running")
    private val focusStartedAt = longPreferencesKey("focus_started_at")
    private val focusAnchorAt = longPreferencesKey("focus_anchor_at")
    private val focusTaskId = longPreferencesKey("focus_task_id")

    val settings: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            companionMotion = preferences[companionMotion] ?: true,
            semesterStartMillis = normalizeSemesterStart(
                preferences[semesterStart] ?: defaultSemesterStart(),
            ),
            defaultCourseReminderMinutes = preferences[courseReminder] ?: 15,
            defaultTaskReminderMinutes = preferences[taskReminder] ?: 0,
            enabledFeatures = preferences[enabledFeatures]
                .orEmpty()
                .mapNotNull { value -> FeatureModule.entries.firstOrNull { it.name == value } }
                .toSet(),
            appTheme = preferences[appTheme]
                ?.let { value -> AppTheme.entries.firstOrNull { it.name == value } }
                ?: AppTheme.DEFAULT,
            themeMode = preferences[themeMode]
                ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
                ?: ThemeMode.FOLLOW_SYSTEM,
            focusTimer = FocusTimerSettings(
                positiveMode = preferences[focusPositiveMode] ?: false,
                durationMinutes = preferences[focusDurationMinutes] ?: 25,
                remainingSeconds = preferences[focusRemainingSeconds] ?: 25 * 60,
                elapsedSeconds = preferences[focusElapsedSeconds] ?: 0,
                running = preferences[focusRunning] ?: false,
                startedAtMillis = preferences[focusStartedAt] ?: 0L,
                anchorAtMillis = preferences[focusAnchorAt] ?: 0L,
                selectedTaskId = preferences[focusTaskId]?.takeIf { it >= 0L },
            ),
        )
    }

    suspend fun snapshot(): AppSettings = settings.first()

    suspend fun setCompanionMotion(enabled: Boolean) {
        context.dataStore.edit { it[companionMotion] = enabled }
    }

    suspend fun setSemesterStart(value: Long) {
        context.dataStore.edit { it[semesterStart] = normalizeSemesterStart(value) }
    }

    suspend fun setCourseReminder(minutes: Int) {
        context.dataStore.edit { it[courseReminder] = minutes.coerceIn(0, 120) }
    }

    suspend fun setFeatureEnabled(feature: FeatureModule, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val values = preferences[enabledFeatures].orEmpty().toMutableSet()
            if (enabled) values += feature.name else values -= feature.name
            preferences[enabledFeatures] = values
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[appTheme] = theme.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeMode] = mode.name }
    }

    suspend fun setFocusTimer(timer: FocusTimerSettings) {
        context.dataStore.edit { preferences ->
            preferences[focusPositiveMode] = timer.positiveMode
            preferences[focusDurationMinutes] = timer.durationMinutes.coerceIn(1, 180)
            preferences[focusRemainingSeconds] = timer.remainingSeconds.coerceAtLeast(0)
            preferences[focusElapsedSeconds] = timer.elapsedSeconds.coerceAtLeast(0)
            preferences[focusRunning] = timer.running
            preferences[focusStartedAt] = timer.startedAtMillis
            preferences[focusAnchorAt] = timer.anchorAtMillis
            preferences[focusTaskId] = timer.selectedTaskId ?: -1L
        }
    }

    private fun defaultSemesterStart(): Long {
        val today = LocalDate.now()
        val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
        return sunday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun normalizeSemesterStart(value: Long): Long {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(value).atZone(zone).toLocalDate()
        val sunday = date.minusDays((date.dayOfWeek.value % 7).toLong())
        return sunday.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
