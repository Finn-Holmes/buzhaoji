package com.kexu.app.ui

import com.kexu.app.data.model.WeekPattern
import com.kexu.app.importer.CourseDraft
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicImportIssueTest {
    @Test
    fun `same course at same time and weeks is marked duplicate`() {
        val course = draft("高等数学", 8 * 60, 9 * 60 + 35, weeks = listOf(1, 2, 3))

        val issues = analyzeImportIssues(listOf(course, course.copy(teacher = "另一条解析结果")))

        assertEquals(1, issues.duplicatePairs)
        assertEquals(0, issues.conflictPairs)
        assertTrue(issues.messages.getValue(0).single().contains("重复"))
        assertTrue(issues.messages.getValue(1).single().contains("重复"))
    }

    @Test
    fun `overlapping time only conflicts when teaching weeks overlap`() {
        val left = draft("数据结构", 9 * 60, 10 * 60 + 30, weeks = listOf(1, 3, 5))
        val differentWeeks = draft("大学英语", 10 * 60, 11 * 60, weeks = listOf(2, 4, 6))
        val sharedWeek = draft("操作系统", 10 * 60, 11 * 60, weeks = listOf(3, 7))

        val issues = analyzeImportIssues(listOf(left, differentWeeks, sharedWeek))

        assertEquals(0, issues.duplicatePairs)
        assertEquals(1, issues.conflictPairs)
        assertTrue(issues.messages.getValue(0).single().contains("操作系统"))
        assertTrue(issues.messages.getValue(2).single().contains("数据结构"))
    }

    @Test
    fun `fallback week ranges respect odd and even patterns`() {
        val odd = draft("单周课程", 8 * 60, 9 * 60, weeks = emptyList()).copy(
            startWeek = 1,
            endWeek = 8,
            weekPattern = WeekPattern.ODD,
            weekMask = 0L,
        )
        val even = odd.copy(name = "双周课程", weekPattern = WeekPattern.EVEN)

        val issues = analyzeImportIssues(listOf(odd, even))

        assertEquals(0, issues.duplicatePairs)
        assertEquals(0, issues.conflictPairs)
    }

    private fun draft(name: String, start: Int, end: Int, weeks: List<Int>): CourseDraft = CourseDraft(
        name = name,
        day = DayOfWeek.MONDAY,
        startMinutes = start,
        endMinutes = end,
        startWeek = weeks.minOrNull() ?: 1,
        endWeek = weeks.maxOrNull() ?: 20,
        weekMask = weeks.fold(0L) { mask, week -> mask or (1L shl (week - 1)) },
    )
}
