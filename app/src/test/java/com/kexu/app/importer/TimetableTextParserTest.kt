package com.kexu.app.importer

import com.kexu.app.data.model.WeekPattern
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableTextParserTest {
    @Test
    fun parsesCsvAndTabSeparatedRows() {
        val text = listOf(
            "数据库,周一,08:00,09:40,A302,王老师,1,16,单周",
            "高等数学\t星期三\t10:00\t11:40\tB201\t李老师\t1\t18\t每周",
        ).joinToString("\n")
        val result = TimetableTextParser.parse(text)
        assertTrue(result.errors.isEmpty())
        assertEquals(2, result.courses.size)
        assertEquals(DayOfWeek.MONDAY, result.courses[0].day)
        assertEquals(WeekPattern.ODD, result.courses[0].weekPattern)
    }
}
