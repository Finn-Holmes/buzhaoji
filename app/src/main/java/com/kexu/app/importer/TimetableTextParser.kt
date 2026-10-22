package com.kexu.app.importer

import com.kexu.app.data.model.WeekPattern
import java.time.DayOfWeek

data class CourseDraft(
    val name: String,
    val day: DayOfWeek,
    val startMinutes: Int,
    val endMinutes: Int,
    val location: String = "",
    val teacher: String = "",
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val weekPattern: WeekPattern = WeekPattern.ALL,
    val weekMask: Long = 0L,
)

data class TimetableImportResult(
    val courses: List<CourseDraft>,
    val errors: List<String>,
)

object TimetableTextParser {
    fun parse(text: String): TimetableImportResult {
        val courses = mutableListOf<CourseDraft>()
        val errors = mutableListOf<String>()
        text.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .forEachIndexed { index, line ->
                val fields = line.split(if ('\t' in line) '\t' else ',').map(String::trim)
                if (fields.firstOrNull()?.contains("课程") == true && fields.getOrNull(1)?.contains("星期") == true) {
                    return@forEachIndexed
                }
                val name = fields.getOrNull(0).orEmpty()
                val day = parseDay(fields.getOrNull(1).orEmpty())
                val start = parseTime(fields.getOrNull(2).orEmpty())
                val end = parseTime(fields.getOrNull(3).orEmpty())
                if (name.isBlank() || day == null || start == null || end == null || end <= start) {
                    errors += "第 ${index + 1} 行格式不正确：$line"
                    return@forEachIndexed
                }
                courses += CourseDraft(
                    name = name,
                    day = day,
                    startMinutes = start,
                    endMinutes = end,
                    location = fields.getOrNull(4).orEmpty(),
                    teacher = fields.getOrNull(5).orEmpty(),
                    startWeek = fields.getOrNull(6)?.toIntOrNull() ?: 1,
                    endWeek = fields.getOrNull(7)?.toIntOrNull() ?: 20,
                    weekPattern = parsePattern(fields.getOrNull(8).orEmpty()),
                )
            }
        return TimetableImportResult(courses, errors)
    }

    private fun parseDay(value: String): DayOfWeek? = when (value.replace("星期", "").replace("周", "")) {
        "1", "一" -> DayOfWeek.MONDAY
        "2", "二" -> DayOfWeek.TUESDAY
        "3", "三" -> DayOfWeek.WEDNESDAY
        "4", "四" -> DayOfWeek.THURSDAY
        "5", "五" -> DayOfWeek.FRIDAY
        "6", "六" -> DayOfWeek.SATURDAY
        "7", "日", "天" -> DayOfWeek.SUNDAY
        else -> null
    }

    private fun parseTime(value: String): Int? {
        val parts = value.split(":", "：")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun parsePattern(value: String): WeekPattern = when (value.lowercase()) {
        "单周", "odd" -> WeekPattern.ODD
        "双周", "even" -> WeekPattern.EVEN
        else -> WeekPattern.ALL
    }
}
