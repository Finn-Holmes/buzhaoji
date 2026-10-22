package com.kexu.app.widget

import com.kexu.app.data.model.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TaskWidgetBehaviorTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 9, 6)

    @Test fun `today uses local deadline time and other days use dates`() {
        val task = TaskEntity(id = 1, title = "报告", dueAt = today.atTime(18, 30).atZone(zone).toInstant().toEpochMilli())
        assertEquals("18:30", compactTaskDeadline(task, today, zone))
        assertEquals("9/6", compactTaskDeadline(task, today.minusDays(1), zone))
        assertEquals("9/6", compactTaskDeadline(task, today.plusDays(1), zone))
        assertEquals("", compactTaskDeadline(task.copy(dueAt = null), today, zone))
    }

    @Test fun `stale completion and recurring occurrence actions have different versions`() {
        val task = TaskEntity(id = 2, title = "复习", dueAt = 1000L, recurrenceRule = "DAILY")
        assertNotEquals(task.widgetVersion(), task.copy(isCompleted = true, completedAt = 2000L).widgetVersion())
        assertNotEquals(task.widgetVersion(), task.copy(dueAt = 2000L).widgetVersion())
        assertEquals(task.widgetVersion(), task.copy().widgetVersion())
    }
}
