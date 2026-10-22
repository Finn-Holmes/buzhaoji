package com.kexu.app.reminder

import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.util.UUID

/** A durable occurrence, distinct from the editable task/habit which produced it. */
internal data class ReminderRecord(
    val key: String,
    val action: String,
    val entityId: Long,
    val triggerAt: Long,
    val expiresAt: Long,
    val source: String,
    val title: String,
    val generation: String = UUID.randomUUID().toString(),
    val dueAt: Long = 0L,
    val location: String = "",
    val occurrenceDay: Long = 0L,
    val snoozed: Boolean = false,
    val deliveredAt: Long? = null,
)

internal object ReminderRules {
    const val TASK_GRACE_MILLIS = 24 * 60 * 60_000L
    const val COURSE_GRACE_MILLIS = 5 * 60_000L

    fun useExactAlarm(sdk: Int, specialAccess: Boolean): Boolean = sdk < 31 || specialAccess

    fun mayDeliver(record: ReminderRecord, generation: String, now: Long): Boolean =
        record.generation == generation && record.deliveredAt == null &&
            now >= record.triggerAt && now < record.expiresAt

    /** Restoring a base reminder must neither clear snooze nor replay a delivered occurrence. */
    fun merge(existing: ReminderRecord?, proposed: ReminderRecord, preserveSnooze: Boolean = true): ReminderRecord {
        if (existing == null || existing.key != proposed.key || existing.source != proposed.source) return proposed
        val refreshed = existing.copy(title = proposed.title, dueAt = proposed.dueAt, location = proposed.location)
        if (preserveSnooze && existing.snoozed) return refreshed
        return if (existing.triggerAt == proposed.triggerAt) refreshed else proposed
    }

    fun taskSource(task: TaskEntity): String =
        listOf(task.remindAt, task.recurrenceRule).joinToString("\u001f")

    fun habitSource(habit: HabitEntity): String = "${habit.reminderMinutes}"

    fun countdownSource(event: CountdownEntity): String =
        "${event.targetDateEpochDay}\u001f${event.repeatYearly}\u001f${event.reminderDaysBefore}"

    fun courseSource(occurrence: CourseOccurrence): String =
        "${occurrence.course.id}\u001f${occurrence.reminderAtMillis}"

    fun task(task: TaskEntity, overrideAt: Long? = null): ReminderRecord? {
        if (task.isCompleted) return null
        val trigger = overrideAt ?: task.remindAt ?: return null
        return ReminderRecord("task:${task.id}", ReminderScheduler.ACTION_TASK, task.id,
            trigger, trigger + TASK_GRACE_MILLIS, taskSource(task), task.title,
            dueAt = task.dueAt ?: 0L, snoozed = overrideAt != null)
    }

    fun habit(habit: HabitEntity, date: LocalDate, zone: ZoneId): ReminderRecord? {
        val minutes = habit.reminderMinutes?.takeIf { it in 0..1439 } ?: return null
        val trigger = date.atTime(minutes / 60, minutes % 60).atZone(zone).toInstant().toEpochMilli()
        return ReminderRecord("habit:${habit.id}:${date.toEpochDay()}", ReminderScheduler.ACTION_HABIT,
            habit.id, trigger, date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            habitSource(habit), habit.title, occurrenceDay = date.toEpochDay())
    }

    fun nextHabit(habit: HabitEntity, now: Long, zone: ZoneId): ReminderRecord? {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val candidate = habit(habit, today, zone) ?: return null
        return if (candidate.triggerAt > now) candidate else habit(habit, today.plusDays(1), zone)
    }

    /** After downtime, recover only today's occurrence of an already registered reminder. */
    fun recoverHabit(habit: HabitEntity, before: List<ReminderRecord>, now: Long, zone: ZoneId): ReminderRecord? {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val candidate = habit(habit, today, zone) ?: return null
        return candidate.takeIf { recoverable(it, before, now) }
    }

    fun countdown(event: CountdownEntity, target: LocalDate, zone: ZoneId): ReminderRecord {
        val day = target.minusDays(event.reminderDaysBefore.toLong())
        val trigger = day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        return ReminderRecord("countdown:${event.id}:${target.toEpochDay()}", ReminderScheduler.ACTION_COUNTDOWN,
            event.id, trigger, day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            countdownSource(event), event.name,
            dueAt = target.atStartOfDay(zone).toInstant().toEpochMilli(), occurrenceDay = target.toEpochDay())
    }

    fun nextCountdown(event: CountdownEntity, now: Long, zone: ZoneId): ReminderRecord {
        var target = LocalDate.ofEpochDay(event.targetDateEpochDay)
        if (event.repeatYearly) {
            val monthDay = MonthDay.from(target)
            target = monthDay.atYear(Instant.ofEpochMilli(now).atZone(zone).year)
            while (countdown(event, target, zone).triggerAt <= now) target = monthDay.atYear(target.year + 1)
        }
        return countdown(event, target, zone)
    }

    fun recoverCountdown(event: CountdownEntity, before: List<ReminderRecord>, now: Long, zone: ZoneId): ReminderRecord? {
        if (!event.repeatYearly) return null // A single occurrence is already restored by nextCountdown.
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val targetYear = today.plusDays(event.reminderDaysBefore.toLong()).year
        val pattern = MonthDay.from(LocalDate.ofEpochDay(event.targetDateEpochDay))
        return (targetYear - 1..targetYear + 1).asSequence()
            .map { countdown(event, pattern.atYear(it), zone) }
            .firstOrNull { recoverable(it, before, now) }
    }

    private fun recoverable(candidate: ReminderRecord, before: List<ReminderRecord>, now: Long): Boolean =
        candidate.triggerAt <= now && now < candidate.expiresAt && before.any {
            it.action == candidate.action && it.entityId == candidate.entityId &&
                it.source == candidate.source && it.triggerAt <= candidate.triggerAt
        }

    fun course(occurrence: CourseOccurrence): ReminderRecord = ReminderRecord(
        "course:${occurrence.slot.id}:${occurrence.startsAtMillis}", ReminderScheduler.ACTION_COURSE,
        occurrence.slot.id, occurrence.reminderAtMillis, occurrence.startsAtMillis + COURSE_GRACE_MILLIS,
        courseSource(occurrence), occurrence.course.name, dueAt = occurrence.startsAtMillis,
        location = occurrence.course.location)
}
