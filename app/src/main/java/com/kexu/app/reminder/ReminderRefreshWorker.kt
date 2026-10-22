package com.kexu.app.reminder

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kexu.app.KexuApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class ReminderRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val app = applicationContext as KexuApplication
        ReminderCoordinator(
            applicationContext,
            app.repository,
            app.settingsRepository,
        ).rescheduleAll()
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { if (it is CancellationException) throw it else Result.retry() },
    )

    companion object {
        private const val UNIQUE_WORK_NAME = "rolling-reminder-refresh"

        fun enqueueRestore(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork("restore-reminder-occurrences",
                ExistingWorkPolicy.APPEND_OR_REPLACE, OneTimeWorkRequestBuilder<ReminderRefreshWorker>().build())
        }

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderRefreshWorker>(
                7,
                TimeUnit.DAYS,
            )
                .setInitialDelay(6, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
