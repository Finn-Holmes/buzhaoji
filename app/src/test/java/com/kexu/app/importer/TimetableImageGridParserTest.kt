package com.kexu.app.importer

import com.kexu.app.data.model.WeekPattern
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableImageGridParserTest {
    @Test
    fun `parses weekday columns section times and odd weeks`() {
        val boxes = headers() + listOf(
            box("第1节 08:00-08:45", 0, 180, 90, 210),
            box("第2节 08:50-09:35", 0, 280, 90, 310),
            box("第3节 09:55-10:40", 0, 380, 90, 410),
            box("高等数学", 120, 185, 195, 207),
            box("1-16周 单周", 120, 211, 205, 233),
            box("第1-2节", 120, 237, 185, 259),
            box("教师：张老师", 120, 263, 220, 285),
            box("教室：南教201", 120, 289, 230, 311),
            box("大学英语", 220, 285, 295, 307),
            box("2-4周", 220, 311, 285, 333),
            box("第2节", 220, 337, 275, 359),
        )

        val result = TimetableImageGridParser.parse(boxes)

        assertEquals(7, result.detectedDayColumns)
        assertEquals(2, result.courses.size)
        val math = result.courses.first { it.name == "高等数学" }
        assertEquals(DayOfWeek.MONDAY, math.day)
        assertEquals(8 * 60, math.startMinutes)
        assertEquals(9 * 60 + 35, math.endMinutes)
        assertEquals(WeekPattern.ODD, math.weekPattern)
        assertEquals(listOf(1, 3, 5, 7, 9, 11, 13, 15), activeWeeks(math))
        assertEquals("张老师", math.teacher)
        assertEquals("南教201", math.location)
        val english = result.courses.first { it.name == "大学英语" }
        assertEquals(DayOfWeek.TUESDAY, english.day)
        assertEquals(8 * 60 + 50, english.startMinutes)
        assertEquals(9 * 60 + 35, english.endMinutes)
        assertEquals(listOf(2, 3, 4), activeWeeks(english))
    }

    @Test
    fun `requires at least five weekday headers before creating candidates`() {
        val result = TimetableImageGridParser.parse(headers().take(4))

        assertTrue(result.courses.isEmpty())
        assertEquals(4, result.detectedDayColumns)
        assertTrue(result.warnings.single().contains("星期表头"))
    }

    @Test
    fun `missing week text uses explicit review warning`() {
        val boxes = headers() + listOf(
            box("第1节 08:00-08:45", 0, 180, 90, 210),
            box("第2节 08:50-09:35", 0, 280, 90, 310),
            box("线性代数", 120, 185, 195, 207),
            box("第1节", 120, 211, 180, 233),
        )

        val result = TimetableImageGridParser.parse(boxes)

        assertEquals(1, result.courses.size)
        assertEquals(1, result.courses.single().startWeek)
        assertEquals(20, result.courses.single().endWeek)
        assertTrue(result.warnings.single().contains("第1-20周"))
    }

    private fun headers(): List<OcrTextBox> = listOf("一", "二", "三", "四", "五", "六", "日").mapIndexed { index, day ->
        box("星期$day", 110 + index * 100, 80, 190 + index * 100, 120)
    }

    private fun box(text: String, left: Int, top: Int, right: Int, bottom: Int) =
        OcrTextBox(text, left, top, right, bottom)

    private fun activeWeeks(course: CourseDraft): List<Int> =
        (1..63).filter { week -> course.weekMask and (1L shl (week - 1)) != 0L }
}
