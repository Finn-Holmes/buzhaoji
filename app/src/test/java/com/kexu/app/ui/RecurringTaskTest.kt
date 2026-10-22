package com.kexu.app.ui

import com.kexu.app.data.model.TaskEntity
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringTaskTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun weekdayRuleSkipsWeekendAndKeepsReminderOffset() {
        val friday = LocalDateTime.of(2026, 7, 24, 9, 0).atZone(zone).toInstant().toEpochMilli()
        val task = TaskEntity(
            title = "工作日任务",
            dueAt = friday,
            remindAt = friday - 30 * 60_000L,
            recurrenceRule = "WEEKDAYS",
        )

        val next = nextRecurringTask(task, nowMillis = friday, zoneId = zone)

        val monday = LocalDateTime.of(2026, 7, 27, 9, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(monday, next.dueAt)
        assertEquals(monday - 30 * 60_000L, next.remindAt)
    }
}
