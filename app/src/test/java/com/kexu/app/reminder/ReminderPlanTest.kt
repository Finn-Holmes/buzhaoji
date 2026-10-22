package com.kexu.app.reminder

import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import com.kexu.app.data.model.TaskEntity
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderPlanTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val at = LocalDate.of(2026, 10, 8).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
    private val task = TaskEntity(id = 91, title = "交作业", remindAt = at, dueAt = at + 60_000)

    @Test fun exactAccessIsRequiredOnlyFromAndroid12() {
        assertTrue(ReminderRules.useExactAlarm(30, false))
        assertFalse(ReminderRules.useExactAlarm(31, false))
        assertFalse(ReminderRules.useExactAlarm(35, false))
        assertTrue(ReminderRules.useExactAlarm(35, true))
    }

    @Test fun restoredSnoozeKeepsItsTimeAndGeneration() {
        val snoozed = ReminderRules.task(task, at + 10 * 60_000)!!
        val restored = ReminderRules.merge(snoozed, ReminderRules.task(task)!!)
        assertEquals(snoozed, restored)
        assertTrue(restored.snoozed)
        assertFalse(ReminderRules.mayDeliver(restored, restored.generation, at + 5 * 60_000))
        assertTrue(ReminderRules.mayDeliver(restored, restored.generation, at + 10 * 60_000))
    }

    @Test fun editedTaskInvalidatesOldGenerationAndSnooze() {
        val old = ReminderRules.task(task, at + 10 * 60_000)!!
        val updated = ReminderRules.merge(old, ReminderRules.task(task.copy(remindAt = at + 20 * 60_000))!!)
        assertFalse(updated.snoozed)
        assertNotEquals(old.generation, updated.generation)
        assertFalse(ReminderRules.mayDeliver(updated, old.generation, updated.triggerAt))
    }

    @Test fun deliveredOccurrenceCannotReplayOnReconciliation() {
        val delivered = ReminderRules.task(task)!!.copy(deliveredAt = at)
        val restored = ReminderRules.merge(delivered, ReminderRules.task(task)!!)
        assertEquals(delivered, restored)
        assertFalse(ReminderRules.mayDeliver(restored, restored.generation, at + 1))
    }

    @Test fun explicitSnoozeCreatesNewOccurrenceAfterDelivery() {
        val delivered = ReminderRules.task(task)!!.copy(deliveredAt = at)
        val snoozed = ReminderRules.merge(delivered, ReminderRules.task(task, at + 600_000)!!, preserveSnooze = false)
        assertNull(snoozed.deliveredAt)
        assertNotEquals(delivered.generation, snoozed.generation)
        assertTrue(ReminderRules.mayDeliver(snoozed, snoozed.generation, at + 600_000))
    }

    @Test fun titleAndDeadlineEditRefreshPayloadWithoutLosingSnoozeOrReplayingDelivery() {
        val changed = ReminderRules.task(task.copy(title = "改标题", dueAt = at + 120_000))!!
        val snoozed = ReminderRules.task(task, at + 600_000)!!
        val merged = ReminderRules.merge(snoozed, changed)
        assertEquals(snoozed.triggerAt, merged.triggerAt)
        assertEquals(snoozed.generation, merged.generation)
        assertEquals("改标题", merged.title)
        assertEquals(at + 120_000, merged.dueAt)
        val delivered = ReminderRules.task(task)!!.copy(deliveredAt = at)
        assertEquals(at, ReminderRules.merge(delivered, changed).deliveredAt)
    }

    @Test fun taskCatchupIsBoundedAndNeverEarly() {
        val record = ReminderRules.task(task)!!
        assertFalse(ReminderRules.mayDeliver(record, record.generation, at - 1))
        assertTrue(ReminderRules.mayDeliver(record, record.generation, at))
        assertTrue(ReminderRules.mayDeliver(record, record.generation, at + ReminderRules.TASK_GRACE_MILLIS - 1))
        assertFalse(ReminderRules.mayDeliver(record, record.generation, at + ReminderRules.TASK_GRACE_MILLIS))
        assertNull(ReminderRules.task(task.copy(isCompleted = true)))
    }

    @Test fun onTimeCourseReminderHasARealDeliveryWindow() {
        val course = CourseEntity(id = 2, name = "数学", location = "A101", reminderMinutes = 0)
        val slot = ScheduleSlotEntity(id = 3, courseId = 2, dayOfWeek = 4, startMinutes = 600, endMinutes = 650)
        val record = ReminderRules.course(CourseOccurrence(course, slot, at, at))
        assertTrue(ReminderRules.mayDeliver(record, record.generation, at))
        assertTrue(ReminderRules.mayDeliver(record, record.generation, at + 60_000))
        assertFalse(ReminderRules.mayDeliver(record, record.generation, at + ReminderRules.COURSE_GRACE_MILLIS))
    }

    @Test fun dailyReminderExpiresAtLocalMidnightAcrossDaylightSaving() {
        val dstZone = ZoneId.of("America/New_York")
        val day = LocalDate.of(2026, 3, 8)
        val record = ReminderRules.habit(HabitEntity(id = 3, title = "读书", reminderMinutes = 90), day, dstZone)!!
        assertEquals(day.plusDays(1).atStartOfDay(dstZone).toInstant().toEpochMilli(), record.expiresAt)
        assertTrue(ReminderRules.mayDeliver(record, record.generation, record.expiresAt - 1))
        assertFalse(ReminderRules.mayDeliver(record, record.generation, record.expiresAt))
        assertNotEquals(24 * 60 * 60_000L, record.expiresAt - record.triggerAt)
    }

    @Test fun nextDailyOccurrenceUsesDifferentKeyAndDoesNotReplayToday() {
        val habit = HabitEntity(id = 3, title = "读书", reminderMinutes = 600)
        val today = ReminderRules.habit(habit, LocalDate.of(2026, 10, 8), zone)!!
        val next = ReminderRules.nextHabit(habit, at, zone)!!
        assertNotEquals(today.key, next.key)
        assertEquals(LocalDate.of(2026, 10, 9).toEpochDay(), next.occurrenceDay)
        assertEquals(at + 24 * 60 * 60_000L, next.triggerAt)
    }

    @Test fun restoreAfterMultipleDaysRecoversTodayAndKeepsTheNextReminder() {
        val habit = HabitEntity(id = 3, title = "读书", reminderMinutes = 600)
        val today = LocalDate.of(2026, 10, 8)
        val oldPlan = ReminderRules.habit(habit, today.minusDays(2), zone)!!
        val now = at + 60_000L
        val recovered = ReminderRules.recoverHabit(habit, listOf(oldPlan), now, zone)!!
        assertEquals(today.toEpochDay(), recovered.occurrenceDay)
        assertEquals(at, recovered.triggerAt)
        assertTrue(ReminderRules.mayDeliver(recovered, recovered.generation, now))
        assertEquals(today.plusDays(1).toEpochDay(), ReminderRules.nextHabit(habit, now, zone)!!.occurrenceDay)
        val delivered = recovered.copy(deliveredAt = now)
        val restored = ReminderRules.merge(delivered, ReminderRules.recoverHabit(habit, listOf(delivered), now, zone)!!)
        assertFalse(ReminderRules.mayDeliver(restored, restored.generation, now + 1))
    }

    @Test fun recoveryDoesNotInventPastRemindersForNewOrRetimedHabits() {
        val habit = HabitEntity(id = 3, title = "读书", reminderMinutes = 600)
        val now = at + 60_000L
        assertNull(ReminderRules.recoverHabit(habit, emptyList(), now, zone))
        val tomorrow = ReminderRules.nextHabit(habit, now, zone)!!
        assertNull(ReminderRules.recoverHabit(habit, listOf(tomorrow), now, zone))
        val old = ReminderRules.habit(habit.copy(reminderMinutes = 590), LocalDate.of(2026, 10, 6), zone)!!
        assertNull(ReminderRules.recoverHabit(habit, listOf(old), now, zone))
        // At the next midnight, the expired day's reminder must never be recovered.
        val midnight = LocalDate.of(2026, 10, 9).atStartOfDay(zone).toInstant().toEpochMilli()
        assertNull(ReminderRules.recoverHabit(habit, listOf(tomorrow), midnight, zone))
    }

    @Test fun annualRecoveryHandlesReminderBeforeNewYearWithoutReplayingPreviousYears() {
        val target = LocalDate.of(2024, 1, 2)
        val event = CountdownEntity(id = 4, name = "纪念日", targetDateEpochDay = target.toEpochDay(),
            repeatYearly = true, reminderDaysBefore = 3)
        val old = ReminderRules.countdown(event, target, zone)
        val reminderDay = LocalDate.of(2026, 12, 30)
        val now = reminderDay.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val recovered = ReminderRules.recoverCountdown(event, listOf(old), now, zone)!!
        assertEquals(LocalDate.of(2027, 1, 2).toEpochDay(), recovered.occurrenceDay)
        assertEquals(reminderDay.atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), recovered.triggerAt)
        assertTrue(ReminderRules.mayDeliver(recovered, recovered.generation, now))
        assertNull(ReminderRules.recoverCountdown(event, emptyList(), now, zone))
        val expired = reminderDay.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        assertNull(ReminderRules.recoverCountdown(event, listOf(old), expired, zone))
    }

    @Test fun renamingHabitCountdownOrCourseDoesNotReplaySameOccurrence() {
        val day = LocalDate.of(2026, 10, 8)
        val habit = HabitEntity(id = 3, title = "旧标题", reminderMinutes = 600)
        val oldHabit = ReminderRules.habit(habit, day, zone)!!.copy(deliveredAt = at)
        val newHabit = ReminderRules.merge(oldHabit, ReminderRules.habit(habit.copy(title = "新标题"), day, zone)!!)
        assertEquals(at, newHabit.deliveredAt)
        assertEquals("新标题", newHabit.title)
        val event = CountdownEntity(id = 4, name = "旧事件", targetDateEpochDay = day.toEpochDay())
        val oldEvent = ReminderRules.countdown(event, day, zone).copy(deliveredAt = at)
        assertEquals(at, ReminderRules.merge(oldEvent, ReminderRules.countdown(event.copy(name = "新事件"), day, zone)).deliveredAt)
        val course = CourseEntity(id = 2, name = "旧课程", reminderMinutes = 0)
        val slot = ScheduleSlotEntity(id = 3, courseId = 2, dayOfWeek = 4, startMinutes = 600, endMinutes = 650)
        val oldCourse = ReminderRules.course(CourseOccurrence(course, slot, at, at)).copy(deliveredAt = at)
        val newCourse = ReminderRules.merge(oldCourse, ReminderRules.course(CourseOccurrence(
            course.copy(name = "新课程", location = "B102"), slot, at, at)))
        assertEquals(at, newCourse.deliveredAt)
        assertEquals("B102", newCourse.location)
    }

    @Test fun yearlyLeapDayKeepsOriginalPatternAfterNonLeapYear() {
        val event = CountdownEntity(id = 2, name = "纪念日", targetDateEpochDay = LocalDate.of(2024, 2, 29).toEpochDay(),
            repeatYearly = true, reminderDaysBefore = 0)
        val march2027 = LocalDate.of(2027, 3, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val next = ReminderRules.nextCountdown(event, march2027, zone)
        assertEquals(LocalDate.of(2028, 2, 29).toEpochDay(), next.occurrenceDay)
    }

    @Test fun persistenceRoundTripRetainsSnoozeAndDeliveryTombstone() {
        val pending = ReminderRules.task(task, at + 600_000)!!
        val done = ReminderRules.task(task.copy(id = 92))!!.copy(deliveredAt = at + 2_000)
        assertEquals(listOf(pending, done), ReminderStore.decode(ReminderStore.encode(listOf(pending, done))))
        assertEquals(emptyList<ReminderRecord>(), ReminderStore.decode("corrupt"))
    }

    @Test fun snapshotRejectsCompletedDeletedOrRetimedTasks() {
        val record = ReminderRules.task(task)!!
        fun snapshot(tasks: Map<Long, TaskEntity>) = ReminderSnapshot(tasks, emptyMap(), emptyMap(), emptyMap(),
            emptyMap(), emptyMap(), at)
        assertTrue(snapshot(mapOf(task.id to task)).valid(record, zone))
        assertFalse(snapshot(emptyMap()).valid(record, zone))
        assertFalse(snapshot(mapOf(task.id to task.copy(isCompleted = true))).valid(record, zone))
        assertFalse(snapshot(mapOf(task.id to task.copy(remindAt = at + 1))).valid(record, zone))
        val test = ReminderRules.task(TaskEntity(id = ReminderScheduler.TEST_TASK_ID, title = "test", remindAt = at))!!
        assertTrue(snapshot(emptyMap()).valid(test, zone))
    }
}
