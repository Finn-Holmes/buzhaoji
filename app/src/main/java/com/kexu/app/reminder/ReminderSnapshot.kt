package com.kexu.app.reminder

import com.kexu.app.data.KexuRepository
import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.settings.SettingsRepository
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId

internal data class ReminderSnapshot(
    val tasks: Map<Long, TaskEntity>,
    val habits: Map<Long, HabitEntity>,
    val countdowns: Map<Long, CountdownEntity>,
    val courses: Map<Long, CourseEntity>,
    val slots: Map<Long, ScheduleSlotEntity>,
    val overrides: Map<Long, List<CourseOverrideEntity>>,
    val semesterStart: Long,
) {
    fun valid(record: ReminderRecord, zone: ZoneId): Boolean = when (record.action) {
        ReminderScheduler.ACTION_TASK -> record.entityId == ReminderScheduler.TEST_TASK_ID ||
            tasks[record.entityId]?.let { !it.isCompleted && it.remindAt != null &&
                ReminderRules.taskSource(it) == record.source } == true
        ReminderScheduler.ACTION_HABIT -> habits[record.entityId]?.let {
            val current = ReminderRules.habit(it, LocalDate.ofEpochDay(record.occurrenceDay), zone)
            current != null && current.source == record.source && current.triggerAt == record.triggerAt
        } == true
        ReminderScheduler.ACTION_COUNTDOWN -> countdowns[record.entityId]?.let {
            val target = LocalDate.ofEpochDay(record.occurrenceDay)
            val original = LocalDate.ofEpochDay(it.targetDateEpochDay)
            val expected = if (it.repeatYearly) MonthDay.from(original).atYear(target.year) else original
            val current = ReminderRules.countdown(it, target, zone)
            expected == target && current.source == record.source && current.triggerAt == record.triggerAt
        } == true
        ReminderScheduler.ACTION_COURSE -> slots[record.entityId]?.let { slot ->
            courses[slot.courseId]?.let { course ->
                ScheduleCalculator.occurrences(course, slot, semesterStart,
                    fromMillis = record.triggerAt - 1, days = 366, zoneId = zone,
                    overrides = overrides[slot.id].orEmpty()).any {
                    it.startsAtMillis == record.dueAt && ReminderRules.courseSource(it) == record.source
                }
            }
        } == true
        else -> false
    }

    companion object {
        suspend fun load(repository: KexuRepository, settings: SettingsRepository): ReminderSnapshot = ReminderSnapshot(
            repository.currentTasks().associateBy { it.id }, repository.currentHabits().associateBy { it.id },
            repository.currentCountdowns().associateBy { it.id }, repository.currentCourses().associateBy { it.id },
            repository.currentSlots().associateBy { it.id }, repository.currentOverrides().groupBy { it.slotId },
            settings.snapshot().semesterStartMillis)
    }
}
