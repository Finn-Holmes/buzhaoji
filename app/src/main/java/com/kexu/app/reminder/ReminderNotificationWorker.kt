package com.kexu.app.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

/** A recovery path, never the primary clock for a time-critical reminder. */
class ReminderNotificationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val key = inputData.getString(ReminderScheduler.EXTRA_RECORD_KEY)
        val generation = inputData.getString(ReminderScheduler.EXTRA_GENERATION)
        if (key == null || generation == null) {
            // Old APKs left title-only requests: rebuild from current DB rather than showing stale data.
            ReminderRefreshWorker.enqueueRestore(applicationContext)
            return Result.success()
        }
        return try {
            ReminderDispatcher.deliver(applicationContext, key, generation)
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }
}