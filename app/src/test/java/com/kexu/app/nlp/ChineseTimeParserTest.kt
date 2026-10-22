package com.kexu.app.nlp

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChineseTimeParserTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val parser = ChineseTimeParser(zone)
    private val now = LocalDateTime.of(2026, 7, 22, 10, 0)

    @Test
    fun parsesRelativeDayAndChineseHour() {
        val result = parser.parse("明天晚上八点交高数作业", now)
        assertEquals("交高数作业", result.cleanTitle)
        assertEquals(LocalDateTime.of(2026, 7, 23, 20, 0).atZone(zone).toInstant().toEpochMilli(), result.dueAtMillis)
        assertEquals(listOf("明天", "晚上八点"), result.recognizedTokens)
        assertTrue(result.hasExplicitDate)
        assertTrue(result.hasExplicitTime)
    }

    @Test
    fun parsesNextWeekAndAdvanceReminder() {
        val result = parser.parse("下周三下午三点实验报告 提前两小时", now)
        assertEquals("实验报告", result.cleanTitle)
        assertNotNull(result.reminderAtMillis)
        assertEquals(result.dueAtMillis!! - 2 * 60 * 60 * 1000L, result.reminderAtMillis)
    }

    @Test
    fun timeWithoutDateMovesToTomorrowWhenPast() {
        val result = parser.parse("上午九点打印资料", now)
        assertEquals(LocalDateTime.of(2026, 7, 23, 9, 0).atZone(zone).toInstant().toEpochMilli(), result.dueAtMillis)
        assertFalse(result.hasExplicitDate)
        assertTrue(result.hasExplicitTime)
    }

    @Test
    fun dateOnlyDoesNotCreateLateNightReminder() {
        val result = parser.parse("明天交课程报告", now)

        assertEquals("交课程报告", result.cleanTitle)
        assertEquals(LocalDateTime.of(2026, 7, 23, 23, 59).atZone(zone).toInstant().toEpochMilli(), result.dueAtMillis)
        assertNull(result.reminderAtMillis)
        assertTrue(result.hasExplicitDate)
        assertFalse(result.hasExplicitTime)
    }

    @Test
    fun parsesEnglishRelativeDateAndTwelveHourTime() {
        val result = parser.parse("Submit report tomorrow at 3pm", now)

        assertEquals("Submit report", result.cleanTitle)
        assertEquals(
            LocalDateTime.of(2026, 7, 23, 15, 0).atZone(zone).toInstant().toEpochMilli(),
            result.dueAtMillis,
        )
        assertEquals(listOf("tomorrow", "at 3pm"), result.recognizedTokens)
        assertTrue(result.hasExplicitDate)
        assertTrue(result.hasExplicitTime)
    }

    @Test
    fun standaloneChinesePeriodsUseFriendlyDefaultTimes() {
        val morning = parser.parse("明天早晨跑步", now)
        val noon = parser.parse("明天中午吃饭", now)
        val afternoon = parser.parse("明天下午写作业", now)
        val dusk = parser.parse("明天傍晚散步", now)

        assertEquals(
            LocalDateTime.of(2026, 7, 23, 8, 0).atZone(zone).toInstant().toEpochMilli(),
            morning.dueAtMillis,
        )
        assertEquals(
            LocalDateTime.of(2026, 7, 23, 12, 0).atZone(zone).toInstant().toEpochMilli(),
            noon.dueAtMillis,
        )
        assertEquals(
            LocalDateTime.of(2026, 7, 23, 18, 0).atZone(zone).toInstant().toEpochMilli(),
            afternoon.dueAtMillis,
        )
        assertEquals(
            LocalDateTime.of(2026, 7, 23, 18, 0).atZone(zone).toInstant().toEpochMilli(),
            dusk.dueAtMillis,
        )
        assertEquals("写作业", afternoon.cleanTitle)
        assertTrue(afternoon.hasExplicitTime)
    }

    @Test
    fun explicitClockTimeOverridesPeriodDefault() {
        val result = parser.parse("明天下午三点开会", now)

        assertEquals(
            LocalDateTime.of(2026, 7, 23, 15, 0).atZone(zone).toInstant().toEpochMilli(),
            result.dueAtMillis,
        )
        assertEquals(listOf("明天", "下午三点"), result.recognizedTokens)
    }

    @Test
    fun supportsEveningAndNightSynonyms() {
        val evening = parser.parse("明天黄昏看日落", now)
        val night = parser.parse("明天夜里整理资料", now)

        assertEquals(
            LocalDateTime.of(2026, 7, 23, 18, 0).atZone(zone).toInstant().toEpochMilli(),
            evening.dueAtMillis,
        )
        assertEquals(
            LocalDateTime.of(2026, 7, 23, 22, 0).atZone(zone).toInstant().toEpochMilli(),
            night.dueAtMillis,
        )
    }
}
