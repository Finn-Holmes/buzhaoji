package com.kexu.app.reminder

import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import com.kexu.app.data.model.WeekPattern
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class CourseOccurrence(
    val course: CourseEntity,
    val slot: ScheduleSlotEntity,
    val startsAtMillis: Long,
    val reminderAtMillis: Long,
)

object ScheduleCalculator {
    fun startOfWeek(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value % 7).toLong())

    fun semesterStartForCurrentWeek(date: LocalDate, currentWeek: Int): LocalDate =
        startOfWeek(date).minusWeeks((currentWeek.coerceAtLeast(1) - 1).toLong())

    fun occurrences(
        course: CourseEntity,
        slot: ScheduleSlotEntity,
        semesterStartMillis: Long,
        fromMillis: Long,
        days: Int = 28,
        zoneId: ZoneId = ZoneId.systemDefault(),
        overrides: List<CourseOverrideEntity> = emptyList(),
    ): List<CourseOccurrence> {
        val semesterStart = startOfWeek(
            Instant.ofEpochMilli(semesterStartMillis).atZone(zoneId).toLocalDate(),
        )
        val fromDate = Instant.ofEpochMilli(fromMillis).atZone(zoneId).toLocalDate()
        val endDate = fromDate.plusDays(days.toLong())
        val overridesByWeek = overrides
            .filter { it.slotId == slot.id }
            .associateBy { it.weekNumber }
        return (course.startWeek..course.endWeek.coerceAtMost(63)).mapNotNull { week ->
            if (!course.isActiveInWeek(week)) return@mapNotNull null
            val override = overridesByWeek[week]
            if (override?.isCancelled == true) return@mapNotNull null

            val effectiveDay = override?.dayOfWeek ?: slot.dayOfWeek
            val effectiveStart = override?.startMinutes ?: slot.startMinutes
            val effectiveEnd = override?.endMinutes ?: slot.endMinutes
            val date = semesterStart
                .plusWeeks((week - 1).toLong())
                .plusDays((effectiveDay % 7).toLong())
            if (date.isBefore(fromDate) || date.isAfter(endDate)) return@mapNotNull null

            val effectiveCourse = override?.location
                ?.let { course.copy(location = it) }
                ?: course
            val effectiveSlot = slot.copy(
                dayOfWeek = effectiveDay,
                startMinutes = effectiveStart,
                endMinutes = effectiveEnd,
            )
            val startsAt = date
                .atStartOfDay()
                .plusMinutes(effectiveStart.toLong())
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
            val reminderAt = startsAt - course.reminderMinutes * 60_000L
            if (reminderAt <= fromMillis) return@mapNotNull null
            CourseOccurrence(effectiveCourse, effectiveSlot, startsAt, reminderAt)
        }
    }

    fun currentWeek(semesterStartMillis: Long, date: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val start = startOfWeek(
            Instant.ofEpochMilli(semesterStartMillis).atZone(zoneId).toLocalDate(),
        )
        return semesterWeek(start, date)
    }

    private fun semesterWeek(start: LocalDate, date: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(start, date)
        return Math.floorDiv(days, 7L).toInt() + 1
    }

    private fun CourseEntity.isActiveInWeek(week: Int): Boolean {
        if (week !in startWeek..endWeek) return false
        if (weekMask != 0L) return week in 1..63 && weekMask and (1L shl (week - 1)) != 0L
        return when (runCatching { WeekPattern.valueOf(weekPattern) }.getOrDefault(WeekPattern.ALL)) {
            WeekPattern.ALL -> true
            WeekPattern.ODD -> week % 2 == 1
            WeekPattern.EVEN -> week % 2 == 0
        }
    }
}
