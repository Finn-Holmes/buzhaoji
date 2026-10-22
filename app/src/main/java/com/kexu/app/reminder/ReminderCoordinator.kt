package com.kexu.app.reminder

import android.content.Context
import com.kexu.app.data.KexuRepository
import com.kexu.app.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.ZoneId

class ReminderCoordinator(
    context: Context,
    private val repository: KexuRepository,
    private val settings: SettingsRepository,
) {
    private val context = context.applicationContext
    private val scheduler = ReminderScheduler(this.context)
    private val store = ReminderStore(this.context)

    suspend fun rescheduleAll() = withContext(Dispatchers.IO) {
        reconcileMutex.withLock {
            scheduler.migrateLegacy()
            repeat(3) {
                val (revision, before) = synchronized(ReminderStore.lock) { store.revision() to store.records() }
                val now = System.currentTimeMillis()
                val zone = ZoneId.systemDefault()
                val snapshot = ReminderSnapshot.load(repository, settings)
                val desired = linkedMapOf<String, ReminderRecord>()
                fun add(proposed: ReminderRecord?) {
                    if (proposed == null) return
                    val record = ReminderRules.merge(before.find { it.key == proposed.key }, proposed)
                    if (record.expiresAt > now || record.deliveredAt != null) desired[record.key] = record
                }
                snapshot.tasks.values.forEach { add(ReminderRules.task(it)) }
                snapshot.habits.values.forEach {
                    add(ReminderRules.nextHabit(it, now, zone))
                    add(ReminderRules.recoverHabit(it, before, now, zone))
                }
                snapshot.countdowns.values.forEach {
                    add(ReminderRules.nextCountdown(it, now, zone))
                    add(ReminderRules.recoverCountdown(it, before, now, zone))
                }
                snapshot.slots.values.forEach { slot ->
                    val course = snapshot.courses[slot.courseId] ?: return@forEach
                    ScheduleCalculator.occurrences(course, slot, snapshot.semesterStart,
                        fromMillis = now - ReminderRules.TASK_GRACE_MILLIS, days = 43, zoneId = zone,
                        overrides = snapshot.overrides[slot.id].orEmpty())
                        .filter { it.startsAtMillis + ReminderRules.COURSE_GRACE_MILLIS > now }
                        .forEach { add(ReminderRules.course(it)) }
                }
                // Keep valid pending occurrences skipped by "next occurrence" calculation, including
                // snoozes, today's late habit, and the independent delayed-test event.
                before.forEach { record ->
                    val retained = record.expiresAt > now ||
                        (record.deliveredAt != null && record.triggerAt >= now - 7 * ReminderRules.TASK_GRACE_MILLIS)
                    if (retained && snapshot.valid(record, zone)) desired.putIfAbsent(record.key, record)
                }
                val applied = synchronized(ReminderStore.lock) {
                    if (store.revision() != revision) false else {
                        val records = desired.values.toList()
                        if (records != before) store.replace(records)
                        before.filter { old -> desired[old.key]?.generation != old.generation }.forEach(scheduler::cancelPlatform)
                        records.filter { it.deliveredAt == null && it.expiresAt > now }.forEach(scheduler::arm)
                        true
                    }
                }
                if (applied) return@withLock
            }
            // Edits raced every snapshot. A durable one-off repair retries without discarding plans.
            ReminderRefreshWorker.enqueueRestore(context)
        }
    }

    private companion object { val reconcileMutex = Mutex() }
}
