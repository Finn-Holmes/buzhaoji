package com.kexu.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.lifecycleScope
import com.kexu.app.reminder.ReminderCoordinator
import com.kexu.app.reminder.ReminderScheduler
import com.kexu.app.settings.AppTheme
import com.kexu.app.settings.AppSettings
import com.kexu.app.settings.ThemeMode
import com.kexu.app.ui.KexuApp
import com.kexu.app.ui.ReminderPermissionGuide
import com.kexu.app.ui.appDetailsIntent
import com.kexu.app.ui.markNotificationPermissionRequested
import com.kexu.app.ui.notificationSettingsIntent
import com.kexu.app.ui.openReminderSystemSettings
import com.kexu.app.ui.readReminderHealth
import com.kexu.app.ui.shouldPromptForNotifications
import com.kexu.app.ui.theme.KexuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var requestedTaskId by mutableStateOf<Long?>(null)
    private var requestedWidgetAction by mutableStateOf<String?>(null)
    private var permissionStateRevision by mutableIntStateOf(0)
    private var lastReminderPermissionSignature: String? = null
    private val permissionPreferences by lazy {
        getSharedPreferences("permission_setup", MODE_PRIVATE)
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refreshReminderPermissions()
    }

    private val exactAlarmPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        refreshReminderPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashView ->
                splashView.iconView?.animate()
                    ?.scaleX(1.02f)
                    ?.scaleY(1.02f)
                    ?.alpha(0f)
                    ?.setDuration(240L)
                    ?.setInterpolator(DecelerateInterpolator())
                    ?.start()
                splashView.animate()
                    .alpha(0f)
                    .setDuration(280L)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction { splashView.remove() }
                    .start()
            }
        }
        requestedTaskId = intent.reminderTaskId()
        requestedWidgetAction = intent.widgetAction()
        rescheduleReminders()

        val app = application as KexuApplication
        setContent {
            val settings by app.settingsRepository.settings.collectAsState(
                initial = AppSettings(
                    semesterStartMillis = 0L,
                    defaultCourseReminderMinutes = 15,
                    defaultTaskReminderMinutes = 0,
                    enabledFeatures = emptySet(),
                    appTheme = AppTheme.DEFAULT,
                    themeMode = ThemeMode.FOLLOW_SYSTEM,
                ),
            )
            KexuTheme(
                appTheme = settings.appTheme,
                themeMode = settings.themeMode,
            ) {
                val permissionRevision = permissionStateRevision
                val reminderHealth = remember(permissionRevision) { readReminderHealth(this@MainActivity) }
                val notificationsGranted = reminderHealth.notificationDisplayAllowed
                val exactRemindersAllowed = reminderHealth.exactAllowed
                var showPermissionGuide by rememberSaveable {
                    mutableStateOf(
                        !permissionPreferences.getBoolean(PERMISSION_GUIDE_SEEN, false),
                    )
                }
                LaunchedEffect(
                    showPermissionGuide,
                    notificationsGranted,
                    exactRemindersAllowed,
                ) {
                    if (
                        showPermissionGuide &&
                        notificationsGranted &&
                        exactRemindersAllowed
                    ) {
                        permissionPreferences.edit()
                            .putBoolean(PERMISSION_GUIDE_SEEN, true)
                            .apply()
                        showPermissionGuide = false
                    }
                }
                Box {
                    KexuApp(
                        notificationTaskId = requestedTaskId,
                        onNotificationTaskHandled = { requestedTaskId = null },
                        widgetAction = requestedWidgetAction,
                        onWidgetActionHandled = { requestedWidgetAction = null },
                    )
                    if (
                        showPermissionGuide &&
                        (!notificationsGranted || !exactRemindersAllowed)
                    ) {
                        ReminderPermissionGuide(
                            notificationsGranted = notificationsGranted,
                            exactRemindersAllowed = exactRemindersAllowed,
                            onRequestNotifications = {
                                if (shouldPromptForNotifications(this@MainActivity)) {
                                    markNotificationPermissionRequested(this@MainActivity)
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    openReminderSystemSettings(this@MainActivity, notificationSettingsIntent(this@MainActivity))
                                }
                            },
                            onRequestExactReminders = ::requestExactReminders,
                            onDismiss = {
                                permissionPreferences.edit()
                                    .putBoolean(PERMISSION_GUIDE_SEEN, true)
                                    .apply()
                                showPermissionGuide = false
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshReminderPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTaskId = intent.reminderTaskId()
        requestedWidgetAction = intent.widgetAction()
    }

    private fun Intent.reminderTaskId(): Long? =
        getLongExtra(ReminderScheduler.EXTRA_TASK_ID, 0L).takeIf { it > 0L }

    private fun Intent.widgetAction(): String? =
        getStringExtra(EXTRA_WIDGET_ACTION)

    private fun requestExactReminders() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val request = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:$packageName"),
        )
        runCatching { exactAlarmPermission.launch(request) }
            .onFailure {
                openReminderSystemSettings(this, appDetailsIntent(this))
            }
    }

    internal fun refreshReminderPermissions() {
        val health = readReminderHealth(this)
        val signature = "${health.notificationsAllowed}:${health.exactAllowed}:" +
            health.channels.joinToString { "${it.id}=${it.enabled}" }
        val previous = lastReminderPermissionSignature
        lastReminderPermissionSignature = signature
        permissionStateRevision++
        // Covers both the first-run guide and permissions opened from Settings.
        // Re-register only after a meaningful permission change, not every resume.
        if (previous != null && previous != signature) rescheduleReminders()
    }

    private fun rescheduleReminders() {
        val app = application as KexuApplication
        lifecycleScope.launch(Dispatchers.IO) {
            ReminderCoordinator(
                this@MainActivity,
                app.repository,
                app.settingsRepository,
            ).rescheduleAll()
        }
    }

    companion object {
        const val EXTRA_WIDGET_ACTION = "com.kexu.app.extra.WIDGET_ACTION"
        const val ACTION_NEW_TASK = "new_task"
        const val ACTION_OPEN_TODAY = "open_today"
        const val ACTION_OPEN_TASKS = "open_tasks"
        const val ACTION_OPEN_TIMETABLE = "open_timetable"
        const val ACTION_OPEN_HABITS = "open_habits"
        const val ACTION_OPEN_COUNTDOWN = "open_countdown"
        const val ACTION_OPEN_FOCUS = "open_focus"
        const val ACTION_OPEN_FOOTPRINTS = "open_footprints"

        const val PERMISSION_GUIDE_SEEN = "reminder_permission_guide_seen_v2"
    }
}
