package com.kexu.app.reminder

import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kexu.app.KexuApplication
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.time.Instant
import java.time.ZoneId

/** Uses actual Room, preferences and Android scheduling, then deletes only its own task ID. */
@RunWith(AndroidJUnit4::class)
class ReminderSchedulingIntegrationTest {
    @Test
    fun snoozeSurvivesCoordinatorRefreshAndRejectsOriginalDelivery() = runBlocking(Dispatchers.IO) {
        withOwnedTask { app, scheduler, task ->
            val original = ReminderStore(app).records().single { it.key == "task:${task.id}" }
            assertEquals(task.remindAt, original.triggerAt)
            val snoozeAt = System.currentTimeMillis() + 3_600_000L
            scheduler.scheduleTask(task, overrideAt = snoozeAt)
            val snoozed = ReminderStore(app).records().single { it.key == original.key }
            assertEquals(snoozeAt, snoozed.triggerAt)
            assertTrue(snoozed.snoozed)
            assertNotEquals(original.generation, snoozed.generation)

            // Saving unrelated task fields must refresh notification text without losing snooze.
            val renamed = task.copy(title = "${task.title} · renamed", priority = 2)
            app.repository.saveTask(renamed)
            scheduler.scheduleTask(renamed)
            val afterEdit = ReminderStore(app).records().single { it.key == original.key }
            assertEquals(snoozeAt, afterEdit.triggerAt)
            assertEquals(snoozed.generation, afterEdit.generation)
            assertEquals(renamed.title, afterEdit.title)
            assertTrue(afterEdit.snoozed)

            // Exercise the same restore entry point used after boot/permission changes.
            ReminderCoordinator(app, app.repository, app.settingsRepository).rescheduleAll()
            val restored = ReminderStore(app).records().single { it.key == original.key }
            assertEquals("Refresh must not revert a snooze to the task's original date", snoozeAt, restored.triggerAt)
            assertTrue(restored.snoozed)
            assertEquals("Snooze must not rewrite the task deadline or original reminder", renamed, app.repository.task(task.id))

            // A previously queued AlarmManager/WorkManager callback may arrive after snoozing.
            ReminderDispatcher.deliver(app, original.key, original.generation)
            val afterOldCallback = ReminderStore(app).records().single { it.key == original.key }
            assertNull("A stale callback must not consume the new reminder", afterOldCallback.deliveredAt)
            assertEquals(snoozeAt, afterOldCallback.triggerAt)
            assertFalse(app.getSystemService(android.app.NotificationManager::class.java).activeNotifications.any {
                it.id == ReminderScheduler.taskRequestCode(task.id)
            })
        }
    }

    @Test
    fun completingTaskCancelsItsPersistedPlanAndRefreshDoesNotRecreateIt() = runBlocking(Dispatchers.IO) {
        withOwnedTask { app, scheduler, task ->
            assertNotNull(ReminderStore(app).records().firstOrNull { it.key == "task:${task.id}" })
            val completed = task.copy(isCompleted = true, completedAt = System.currentTimeMillis())
            assertTrue(app.repository.updateTaskIfUnchanged(task, completed))
            scheduler.cancelTask(task.id)
            ReminderCoordinator(app, app.repository, app.settingsRepository).rescheduleAll()
            assertNull(ReminderStore(app).records().firstOrNull { it.key == "task:${task.id}" })
            assertEquals(completed, app.repository.task(task.id))
        }
    }

    @Test
    fun restoringAfterTwoDaysSchedulesTodaysMissedHabitAndTomorrowWithoutDuplicatingIt() = runBlocking(Dispatchers.IO) {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as KexuApplication
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val localNow = Instant.ofEpochMilli(now).atZone(zone)
        val minutes = localNow.hour * 60 + localNow.minute
        assumeTrue("Today's reminder must already be due for this scenario", minutes > 0)
        val originalHabits = app.repository.currentHabits().sortedBy { it.id }
        val token = UUID.randomUUID().toString()
        val draft = HabitEntity(title = "[Reminder QA] restore ${token.take(8)}",
            description = "reminder-integration-owner:$token", reminderMinutes = minutes - 1)
        val habit = draft.copy(id = app.repository.saveHabit(draft))
        val scheduler = ReminderScheduler(app)
        try {
            // Persist the last known occurrence, simulating downtime across two dates.
            val old = ReminderRules.habit(habit, localNow.toLocalDate().minusDays(2), zone)!!
            val store = ReminderStore(app)
            synchronized(ReminderStore.lock) { store.replace(store.records() + old) }
            val coordinator = ReminderCoordinator(app, app.repository, app.settingsRepository)
            coordinator.rescheduleAll()
            val today = localNow.toLocalDate().toEpochDay()
            val plans = store.records().filter { it.action == ReminderScheduler.ACTION_HABIT && it.entityId == habit.id }
            val recovered = plans.single { it.occurrenceDay == today }
            assertTrue(recovered.triggerAt <= now)
            assertTrue(recovered.expiresAt > now)
            assertEquals(1, plans.count { it.occurrenceDay == today + 1 })
            assertFalse(plans.any { it.key == old.key })
            coordinator.rescheduleAll()
            assertEquals(recovered.generation, store.records().single { it.key == recovered.key }.generation)
        } finally {
            withContext(NonCancellable) {
                val current = app.repository.currentHabits().firstOrNull { it.id == habit.id }
                check(current == null || current.description == draft.description) { "QA habit ownership changed" }
                scheduler.cancelHabit(habit.id)
                NotificationManagerCompat.from(app).cancel(ReminderScheduler.habitRequestCode(habit.id))
                current?.let { app.repository.deleteHabit(it) }
            }
        }
        assertEquals(originalHabits, app.repository.currentHabits().sortedBy { it.id })
    }

    private suspend fun withOwnedTask(block: suspend (KexuApplication, ReminderScheduler, TaskEntity) -> Unit) {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as KexuApplication
        val originalTasks = app.repository.currentTasks().sortedBy { it.id }
        val originalCompletions = app.repository.currentCompletions().sortedBy { it.id }
        val token = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val draft = TaskEntity(
            title = "[Reminder QA] integration ${token.take(8)}",
            notes = "reminder-integration-owner:$token",
            remindAt = now + 1_800_000L,
            dueAt = now + 7_200_000L,
        )
        val task = draft.copy(id = app.repository.saveTask(draft))
        val scheduler = ReminderScheduler(app)
        try {
            scheduler.scheduleTask(task)
            block(app, scheduler, task)
            assertEquals("The integration scenario must retain all pre-existing task data", originalTasks,
                app.repository.currentTasks().filterNot { it.id == task.id }.sortedBy { it.id })
            assertEquals("The integration scenario must retain all pre-existing completion history", originalCompletions,
                app.repository.currentCompletions().filterNot { it.taskId == task.id }.sortedBy { it.id })
        } finally {
            withContext(NonCancellable) {
                val current = app.repository.task(task.id)
                check(current == null || current.notes == draft.notes) {
                    "QA ownership changed; refusing to alter task ${task.id}"
                }
                scheduler.cancelTask(task.id)
                NotificationManagerCompat.from(app).cancel(ReminderScheduler.taskRequestCode(task.id))
                current?.let { app.repository.deleteTask(it) }
                app.database.openHelper.writableDatabase.execSQL(
                    "DELETE FROM task_completions WHERE taskId = ?", arrayOf(task.id),
                )
            }
        }
        assertEquals("Cleanup must leave the original task data intact", originalTasks,
            app.repository.currentTasks().sortedBy { it.id })
        assertEquals("Cleanup must leave the original completion history intact", originalCompletions,
            app.repository.currentCompletions().sortedBy { it.id })
    }
}
