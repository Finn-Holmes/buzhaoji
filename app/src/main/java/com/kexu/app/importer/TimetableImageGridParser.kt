package com.kexu.app.importer

import com.kexu.app.data.model.WeekPattern
import java.time.DayOfWeek
import kotlin.math.abs

data class OcrTextBox(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = (bottom - top).coerceAtLeast(1)
}

data class TimetableImageGridResult(
    val courses: List<CourseDraft>,
    val warnings: List<String>,
    val detectedDayColumns: Int,
)

object TimetableImageGridParser {
    private data class DayHeader(val day: DayOfWeek, val box: OcrTextBox)
    private data class TimeRow(
        val number: Int,
        val centerY: Int,
        val startMinutes: Int,
        val endMinutes: Int,
    )

    fun parse(boxes: List<OcrTextBox>): TimetableImageGridResult {
        val cleanBoxes = boxes.filter { it.text.isNotBlank() && it.right > it.left && it.bottom > it.top }
        val headers = cleanBoxes.mapNotNull { box -> parseDay(box.text)?.let { DayHeader(it, box) } }
            .distinctBy(DayHeader::day)
            .sortedBy { it.box.centerX }
        if (headers.size < 5) {
            return TimetableImageGridResult(
                courses = emptyList(),
                warnings = listOf("没有检测到完整的星期表头"),
                detectedDayColumns = headers.size,
            )
        }
        val firstHeaderX = headers.minOf { it.box.centerX }
        val timeRows = cleanBoxes.mapNotNull(::parseTimeRow)
            .filter { it.first.centerX < firstHeaderX }
            .map { it.second }
            .sortedBy(TimeRow::centerY)
            .distinctBy { it.number to it.centerY }
        if (timeRows.size < 2) {
            return TimetableImageGridResult(
                courses = emptyList(),
                warnings = listOf("检测到了星期网格，但没有读到至少两个节次时间"),
                detectedDayColumns = headers.size,
            )
        }

        val medianHeight = cleanBoxes.map(OcrTextBox::height).sorted().let { it[it.size / 2] }
        val headerBottom = headers.maxOf { it.box.bottom }
        val warnings = mutableListOf<String>()
        val courses = mutableListOf<CourseDraft>()
        headers.forEachIndexed { index, header ->
            val leftBoundary = if (index == 0) {
                header.box.centerX - (headers[index + 1].box.centerX - header.box.centerX) / 2
            } else {
                (headers[index - 1].box.centerX + header.box.centerX) / 2
            }
            val rightBoundary = if (index == headers.lastIndex) {
                header.box.centerX + (header.box.centerX - headers[index - 1].box.centerX) / 2
            } else {
                (header.box.centerX + headers[index + 1].box.centerX) / 2
            }
            val columnBoxes = cleanBoxes.filter { box ->
                box.centerX in leftBoundary..rightBoundary &&
                    box.top > headerBottom &&
                    parseDay(box.text) == null &&
                    clockValues(box.text).size < 2
            }.sortedBy(OcrTextBox::top)
            cluster(columnBoxes, medianHeight).forEach { cluster ->
                parseCourseCluster(header.day, cluster, timeRows, warnings)?.let(courses::add)
            }
        }
        val unique = courses.distinctBy {
            listOf(it.name, it.day, it.startMinutes, it.endMinutes, it.weekMask, it.location, it.teacher)
        }
        if (unique.isEmpty() && warnings.isEmpty()) warnings += "检测到了课表网格，但没有形成可导入的课程块"
        return TimetableImageGridResult(unique, warnings.distinct(), headers.size)
    }

    private fun parseCourseCluster(
        day: DayOfWeek,
        boxes: List<OcrTextBox>,
        timeRows: List<TimeRow>,
        warnings: MutableList<String>,
    ): CourseDraft? {
        if (boxes.isEmpty()) return null
        val lines = boxes.map { it.text.trim() }.filter(String::isNotBlank)
        val joined = lines.joinToString("\n")
        val weeks = parseWeeks(joined)
        val name = lines.firstOrNull { line ->
            parseDay(line) == null &&
                clockValues(line).isEmpty() &&
                !line.contains("周") &&
                !Regex("""第?\s*\d{1,2}\s*(?:[-~至—–]\s*\d{1,2})?\s*节""").containsMatchIn(line) &&
                !Regex("""^(教师|老师|教室|地点|上课地点)\s*[:：]""").containsMatchIn(line)
        }?.trim().orEmpty()
        if (name.length < 2) return null

        val sectionMatch = Regex("""第?\s*(\d{1,2})\s*(?:[-~至—–]\s*(\d{1,2}))?\s*节""").find(joined)
        val closestRow = timeRows.minByOrNull { abs(it.centerY - boxes.map(OcrTextBox::centerY).average()) } ?: return null
        val startNumber = sectionMatch?.groupValues?.get(1)?.toIntOrNull() ?: closestRow.number
        val endNumber = sectionMatch?.groupValues?.get(2)?.toIntOrNull()?.takeIf { it > 0 } ?: startNumber
        val startRow = timeRows.firstOrNull { it.number == startNumber } ?: closestRow
        val endRow = timeRows.firstOrNull { it.number == endNumber } ?: startRow
        val effectiveWeeks = weeks.ifEmpty {
            warnings += "“$name”没有识别到周次，暂按第1-20周，请导入前检查"
            (1..20).toList()
        }
        val teacher = labeledField(joined, listOf("教师", "老师"))
        val location = labeledField(joined, listOf("上课地点", "教室", "地点"))
        return CourseDraft(
            name = name,
            day = day,
            startMinutes = startRow.startMinutes,
            endMinutes = endRow.endMinutes,
            location = location,
            teacher = teacher,
            startWeek = effectiveWeeks.first(),
            endWeek = effectiveWeeks.last(),
            weekPattern = inferPattern(effectiveWeeks),
            weekMask = effectiveWeeks.fold(0L) { mask, week -> mask or (1L shl (week - 1)) },
        )
    }

    private fun parseTimeRow(box: OcrTextBox): Pair<OcrTextBox, TimeRow>? {
        val clocks = clockValues(box.text)
        if (clocks.size < 2 || clocks[1] <= clocks[0]) return null
        val number = Regex("""(?:第\s*)?(\d{1,2})\s*节""").find(box.text)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""^\s*(\d{1,2})(?:\s|[.、])""").find(box.text)?.groupValues?.get(1)?.toIntOrNull()
            ?: return null
        return box to TimeRow(number, box.centerY, clocks[0], clocks[1])
    }

    private fun clockValues(text: String): List<Int> = Regex("""(\d{1,2})[:：](\d{2})""")
        .findAll(text)
        .mapNotNull { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val minute = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
        }
        .toList()

    private fun parseWeeks(text: String): List<Int> {
        val result = mutableSetOf<Int>()
        val odd = text.contains("单周")
        val even = text.contains("双周")
        Regex("""(\d{1,2})(?:\s*[-~至—–]\s*(\d{1,2}))?\s*周""").findAll(text).forEach { match ->
            val start = match.groupValues[1].toIntOrNull() ?: return@forEach
            val end = match.groupValues[2].toIntOrNull() ?: start
            for (week in start..end.coerceAtMost(63)) {
                if ((!odd || week % 2 == 1) && (!even || week % 2 == 0)) result += week
            }
        }
        return result.sorted()
    }

    private fun inferPattern(weeks: List<Int>): WeekPattern = when {
        weeks.all { it % 2 == 1 } -> WeekPattern.ODD
        weeks.all { it % 2 == 0 } -> WeekPattern.EVEN
        else -> WeekPattern.ALL
    }

    private fun labeledField(text: String, labels: List<String>): String {
        labels.forEach { label ->
            Regex("""$label\s*[:：]\s*([^\n]+)""").find(text)?.groupValues?.get(1)?.trim()?.let { return it }
        }
        return ""
    }

    private fun parseDay(text: String): DayOfWeek? {
        val normalized = text.replace("\\s".toRegex(), "")
        return when {
            normalized.contains("星期一") || normalized.contains("周一") -> DayOfWeek.MONDAY
            normalized.contains("星期二") || normalized.contains("周二") -> DayOfWeek.TUESDAY
            normalized.contains("星期三") || normalized.contains("周三") -> DayOfWeek.WEDNESDAY
            normalized.contains("星期四") || normalized.contains("周四") -> DayOfWeek.THURSDAY
            normalized.contains("星期五") || normalized.contains("周五") -> DayOfWeek.FRIDAY
            normalized.contains("星期六") || normalized.contains("周六") -> DayOfWeek.SATURDAY
            normalized.contains("星期日") || normalized.contains("星期天") ||
                normalized.contains("周日") || normalized.contains("周天") -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun cluster(boxes: List<OcrTextBox>, medianHeight: Int): List<List<OcrTextBox>> {
        if (boxes.isEmpty()) return emptyList()
        val maximumGap = (medianHeight * 1.6).toInt().coerceAtLeast(8)
        val result = mutableListOf<MutableList<OcrTextBox>>()
        boxes.forEach { box ->
            val current = result.lastOrNull()
            if (current == null || box.top - current.maxOf(OcrTextBox::bottom) > maximumGap) {
                result += mutableListOf(box)
            } else {
                current += box
            }
        }
        return result
    }
}
