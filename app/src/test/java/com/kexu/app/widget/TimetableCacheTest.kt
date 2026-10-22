package com.kexu.app.widget

import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class TimetableCacheTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val start = LocalDate.of(2026, 9, 6)
    private val course = CourseEntity(id = 1, name = "测试课程", location = "A101")
    private val slot = ScheduleSlotEntity(id = 2, courseId = 1, dayOfWeek = 1, startMinutes = 480, endMinutes = 570)
    private val data = TimetableData(start.atStartOfDay(zone).toInstant().toEpochMilli(), listOf(course), listOf(slot), emptyList())

    @Test fun `nearby weeks reuse results and survive disk round trip`() {
        val snapshot = TimetableSnapshot(data, zone)
        snapshot.warmAround(start)
        for (offset in -2L..2L) {
            val day = start.plusWeeks(offset)
            assertSame(snapshot.week(day), snapshot.week(day))
        }
        val json = snapshot.encode()
        assertEquals(5, JSONObject(json).getJSONArray("weeks").length())
        val restored = TimetableSnapshot.decode(json, zone)
        assertEquals(data, restored.data)
        for (offset in -2L..2L) assertEquals(snapshot.week(start.plusWeeks(offset)), restored.week(start.plusWeeks(offset)))
    }

    @Test fun `deleted moved cancelled and renamed courses replace cached contents`() {
        val original = TimetableSnapshot(data, zone)
        original.warmAround(start)
        assertEquals("A101", original.week(start).single().course.location)
        assertTrue(TimetableSnapshot(data.copy(courses = emptyList(), slots = emptyList()), zone).week(start).isEmpty())
        val moved = data.copy(courses = listOf(course.copy(name = "已修改")), overrides = listOf(
            CourseOverrideEntity(id = 3, slotId = 2, weekNumber = 1, dayOfWeek = 3,
                startMinutes = 600, endMinutes = 690, location = "B202")))
        val changed = TimetableSnapshot(moved, zone)
        val occurrence = changed.week(start).single()
        assertEquals("已修改", occurrence.course.name)
        assertEquals("B202", occurrence.course.location)
        assertEquals(3, occurrence.slot.dayOfWeek)
        assertEquals(600, occurrence.slot.startMinutes)
        assertEquals(changed.week(start), TimetableSnapshot.decode(changed.encode(), zone).week(start))
        val cancelled = moved.copy(overrides = listOf(moved.overrides.single().copy(isCancelled = true)))
        assertTrue(TimetableSnapshot(cancelled, zone).week(start).isEmpty())
    }

    @Test fun `semester changes invalidate week contents`() {
        val oneWeek = data.copy(courses = listOf(course.copy(endWeek = 1)))
        assertEquals(1, TimetableSnapshot(oneWeek, zone).week(start).size)
        val changed = oneWeek.copy(semesterStart = start.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli())
        val cache = TimetableSnapshot(changed, zone)
        assertTrue(cache.week(start).isEmpty())
        assertEquals(1, cache.week(start.plusWeeks(1)).size)
    }

    @Test fun `cache stays bounded when navigating many weeks`() {
        val cache = TimetableSnapshot(data, zone)
        for (offset in -30L..30L) cache.warmAround(start.plusWeeks(offset))
        assertEquals(11, JSONObject(cache.encode()).getJSONArray("weeks").length())
    }

    @Test fun `incompatible zone version and damaged cache are rejected`() {
        val text = TimetableSnapshot(data, zone).encode()
        for (invalid in listOf(text to ZoneId.of("UTC"), "{}" to zone,
            JSONObject(text).put("version", 2).toString() to zone)) {
            assertTrue(runCatching { TimetableSnapshot.decode(invalid.first, invalid.second) }.isFailure)
        }
    }
}
