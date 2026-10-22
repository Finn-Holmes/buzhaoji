package com.kexu.app.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kexu.app.KexuApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class RestoreRemindersReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        // Persist recovery first; a reboot/time-change broadcast has only a short execution window.
        ReminderRefreshWorker.enqueueRestore(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withTimeout(8_000L) {
                    val app = context.applicationContext as KexuApplication
                    ReminderCoordinator(context, app.repository, app.settingsRepository).rescheduleAll()
                }
            } catch (_: Exception) {
                // The durable repair worker remains queued if the broadcast budget expires.
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    }
}