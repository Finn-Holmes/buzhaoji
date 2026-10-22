package com.kexu.app.reminder

import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import com.kexu.app.data.model.WeekPattern
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleCalculatorTest {
    @Test
    fun calculatesSemesterStartFromRealCurrentWeek() {
        val today = LocalDate.of(2026, 3, 25)

        val semesterStart = ScheduleCalculator.semesterStartForCurrentWeek(today, 5)

        assertEquals(LocalDate.of(2026, 2, 22), semesterStart)
        val millis = semesterStart.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
        assertEquals(5, ScheduleCalculator.currentWeek(millis, today, ZoneId.of("Asia/Shanghai")))
    }

    @Test
    fun semesterStartAlwaysFallsOnSunday() {
        val semesterStart = ScheduleCalculator.semesterStartForCurrentWeek(
            LocalDate.of(2026, 3, 29),
            1,
        )

        assertEquals(LocalDate.of(2026, 3, 29), semesterStart)
    }

    @Test
    fun dayBeforeSemesterIsWeekZeroInsteadOfWeekOne() {
        val semesterStart = LocalDate.of(2026, 9, 6)
            .atStartOfDay(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()

        assertEquals(
            0,
            ScheduleCalculator.currentWeek(
                semesterStart,
                LocalDate.of(2026, 9, 5),
                ZoneId.of("Asia/Shanghai"),
            ),
        )
    }

    @Test
    fun sevenDaysBeforeSemesterIsMinusOne() {
        val semesterStart = LocalDate.of(2026, 9, 6)
            .atStartOfDay(ZoneId.of("Asia/Shanghai"))
            .toInstant()
            .toEpochMilli()

        assertEquals(
            -1,
            ScheduleCalculator.currentWeek(
                semesterStart,
                LocalDate.of(2026, 8, 29),
                ZoneId.of("Asia/Shanghai"),
            ),
        )
    }

    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun respectsOddWeekPattern() {
        val semesterStart = LocalDate.of(2026, 9, 7).atStartOfDay(zone).toInstant().toEpochMilli()
        val from = LocalDate.of(2026, 9, 7).atStartOfDay(zone).toInstant().toEpochMilli()
        val course = CourseEntity(id = 1, name = "数据库", startWeek = 1, endWeek = 4, weekPattern = WeekPattern.ODD.name)
        val slot = ScheduleSlotEntity(id = 1, courseId = 1, dayOfWeek = 3, startMinutes = 8 * 60, endMinutes = 9 * 60 + 40)
        val result = ScheduleCalculator.occurrences(course, slot, semesterStart, from, days = 28, zoneId = zone)
        assertEquals(2, result.size)
        assertTrue(result[1].startsAtMillis - result[0].startsAtMillis >= 13 * 24 * 60 * 60 * 1000L)
    }

    @Test
    fun sundayCourseIsPlacedAtStartOfTeachingWeek() {
        val semesterStart = LocalDate.of(2026, 9, 6).atStartOfDay(zone).toInstant().toEpochMilli()
        val from = LocalDate.of(2026, 9, 5).atStartOfDay(zone).toInstant().toEpochMilli()
        val course = CourseEntity(id = 1, name = "形势与政策", startWeek = 1, endWeek = 1)
        val slot = ScheduleSlotEntity(
            id = 1,
            courseId = 1,
            dayOfWeek = DayOfWeek.SUNDAY.value,
            startMinutes = 8 * 60,
            endMinutes = 9 * 60,
        )

        val occurrence = ScheduleCalculator.occurrences(
            course,
            slot,
            semesterStart,
            from,
            days = 7,
            zoneId = zone,
        ).single()

        assertEquals(
            LocalDate.of(2026, 9, 6),
            Instant.ofEpochMilli(occurrence.startsAtMillis).atZone(zone).toLocalDate(),
        )
    }

    @Test
    fun respectsExactNonContinuousWeekMask() {
        val zone = ZoneId.of("Asia/Shanghai")
        val semesterStart = LocalDate.of(2026, 2, 22).atStartOfDay(zone).toInstant().toEpochMilli()
        val from = LocalDate.of(2026, 2, 22).atStartOfDay(zone).toInstant().toEpochMilli()
        val weeks = listOf(1, 2, 5, 8)
        val mask = weeks.fold(0L) { value, week -> value or (1L shl (week - 1)) }
        val course = CourseEntity(id = 1, name = "课程设计", startWeek = 1, endWeek = 8, weekMask = mask)
        val slot = ScheduleSlotEntity(id = 1, courseId = 1, dayOfWeek = DayOfWeek.MONDAY.value, startMinutes = 8 * 60, endMinutes = 9 * 60)

        val result = ScheduleCalculator.occurrences(course, slot, semesterStart, from, days = 60, zoneId = zone)

        val actualWeeks = result.map {
            java.time.temporal.ChronoUnit.WEEKS.between(
                LocalDate.of(2026, 2, 22),
                Instant.ofEpochMilli(it.startsAtMillis).atZone(zone).toLocalDate(),
            ).toInt() + 1
        }
        assertEquals(weeks, actualWeeks)
    }

    @Test
    fun cancelledOverrideSuppressesOnlyTheSelectedTeachingWeek() {
        val semesterDate = LocalDate.of(2026, 9, 7)
        val semesterStart = semesterDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val from = semesterDate.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val course = CourseEntity(id = 1, name = "数据库", startWeek = 1, endWeek = 2)
        val slot = ScheduleSlotEntity(
            id = 7,
            courseId = course.id,
            dayOfWeek = DayOfWeek.MONDAY.value,
            startMinutes = 8 * 60,
            endMinutes = 9 * 60 + 40,
        )
        val cancelledWeek = CourseOverrideEntity(
            slotId = slot.id,
            weekNumber = 1,
            isCancelled = true,
        )

        val result = ScheduleCalculator.occurrences(
            course = course,
            slot = slot,
            semesterStartMillis = semesterStart,
            fromMillis = from,
            days = 15,
            zoneId = zone,
            overrides = listOf(cancelledWeek),
        )

        assertEquals(1, result.size)
        assertEquals(
            LocalDate.of(2026, 9, 14),
            Instant.ofEpochMilli(result.single().startsAtMillis).atZone(zone).toLocalDate(),
        )
    }

    @Test
    fun rescheduledOverrideChangesOnlyThatWeeksDayTimeAndLocation() {
        val semesterDate = LocalDate.of(2026, 9, 7)
        val semesterStart = semesterDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val from = semesterDate.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val course = CourseEntity(
            id = 1,
            name = "数据库",
            location = "南教 101",
            startWeek = 1,
            endWeek = 2,
        )
        val slot = ScheduleSlotEntity(
            id = 7,
            courseId = course.id,
            dayOfWeek = DayOfWeek.MONDAY.value,
            startMinutes = 8 * 60,
            endMinutes = 9 * 60 + 40,
        )
        val rescheduledWeek = CourseOverrideEntity(
            slotId = slot.id,
            weekNumber = 1,
            dayOfWeek = DayOfWeek.FRIDAY.value,
            startMinutes = 10 * 60,
            endMinutes = 11 * 60 + 40,
            location = "东环 302",
        )

        val result = ScheduleCalculator.occurrences(
            course = course,
            slot = slot,
            semesterStartMillis = semesterStart,
            fromMillis = from,
            days = 15,
            zoneId = zone,
            overrides = listOf(rescheduledWeek),
        )

        assertEquals(2, result.size)
        val changed = result.first()
        val changedDateTime = Instant.ofEpochMilli(changed.startsAtMillis).atZone(zone)
        assertEquals(LocalDate.of(2026, 9, 11), changedDateTime.toLocalDate())
        assertEquals(10, changedDateTime.hour)
        assertEquals("东环 302", changed.course.location)
        assertEquals(DayOfWeek.FRIDAY.value, changed.slot.dayOfWeek)

        val unchanged = result.last()
        assertEquals(
            LocalDate.of(2026, 9, 14),
            Instant.ofEpochMilli(unchanged.startsAtMillis).atZone(zone).toLocalDate(),
        )
        assertEquals("南教 101", unchanged.course.location)
    }
}
