package com.kexu.app.importer

import com.kexu.app.data.model.WeekPattern
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicWebAdapterTest {
    private val automaticSchool = SchoolSpec(
        id = "TEST",
        name = "测试大学",
        aliases = emptyList(),
        loginUrl = "https://example.edu.cn/",
        allowedDomainSuffixes = listOf("example.edu.cn"),
        system = EducationSystem.AUTO,
    )

    @Test
    fun `normalized page result keeps exact weeks and page section times`() {
        val result = AcademicWebAdapter.parseEvaluationResult(
            """
            {
              "adapter":"fixture",
              "timeSlots":[
                {"number":3,"startTime":"09:55","endTime":"10:40"},
                {"number":4,"startTime":"10:45","endTime":"11:30"}
              ],
              "courses":[{
                "name":"数据结构",
                "teacher":"王老师",
                "position":"南教201",
                "day":7,
                "weeks":[1,3,5,8],
                "startSection":3,
                "endSection":4
              }]
            }
            """.trimIndent(),
            automaticSchool,
        )

        assertTrue(result.errors.toString(), result.errors.isEmpty())
        val course = result.courses.single()
        assertEquals(DayOfWeek.SUNDAY, course.day)
        assertEquals(9 * 60 + 55, course.startMinutes)
        assertEquals(11 * 60 + 30, course.endMinutes)
        assertEquals(WeekPattern.ALL, course.weekPattern)
        assertEquals(listOf(1, 3, 5, 8), (1..63).filter { course.weekMask and (1L shl (it - 1)) != 0L })
    }

    @Test
    fun `course supplied clock times override section defaults`() {
        val result = AcademicWebAdapter.parseEvaluationResult(
            """
            {
              "courses":[{
                "name":"大学英语",
                "day":2,
                "weeks":[2,4,6],
                "startSection":1,
                "endSection":2,
                "startTime":"13:20",
                "endTime":"14:55"
              }]
            }
            """.trimIndent(),
            automaticSchool.copy(
                sectionTimes = mapOf(
                    1 to SectionTime(8 * 60, 8 * 60 + 45),
                    2 to SectionTime(8 * 60 + 50, 9 * 60 + 35),
                ),
            ),
        )

        assertTrue(result.errors.toString(), result.errors.isEmpty())
        val course = result.courses.single()
        assertEquals(13 * 60 + 20, course.startMinutes)
        assertEquals(14 * 60 + 55, course.endMinutes)
        assertEquals(WeekPattern.EVEN, course.weekPattern)
    }

    @Test
    fun `structure diagnostic excludes page content and credentials`() {
        val script = AcademicWebAdapter.structureDiagnosticScript

        assertTrue(script.contains("structure-diagnostic-v1"))
        assertTrue(script.contains("weekdayHeaders"))
        assertFalse(script.contains("innerHTML"))
        assertFalse(script.contains("outerHTML"))
        assertFalse(script.contains("document.cookie"))
        assertFalse(script.contains("location.search"))
    }

    @Test
    fun `structure diagnostic summary stays compact and useful`() {
        val summary = AcademicWebAdapter.structureDiagnosticSummary(
            """
            {
              "adapter":"structure-diagnostic-v1",
              "documentCount":1,
              "documents":[{
                "index":0,
                "path":"/jw/xskb",
                "iframeCount":2,
                "selectors":{
                  "timetable":0,
                  "kbcontent":0,
                  "zhengfangGrid":1,
                  "timetableCon":12,
                  "urpClass":0
                },
                "tables":[{
                  "index":0,
                  "id":"kbgrid_table_0",
                  "rows":14,
                  "maxColumns":8,
                  "weekdayHeaders":7,
                  "rowSpans":4,
                  "colSpans":7
                }]
              }]
            }
            """.trimIndent(),
        )

        assertTrue(summary.contains("adapter=structure-diagnostic-v1"))
        assertTrue(summary.contains("path=/jw/xskb"))
        assertTrue(summary.contains("z1"))
        assertTrue(summary.contains("kbgrid_table_0(14x8,weekdays=7,span=4/7)"))
    }
}
