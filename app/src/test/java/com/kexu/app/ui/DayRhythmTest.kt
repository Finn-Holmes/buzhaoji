package com.kexu.app.ui

import com.kexu.app.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DayRhythmTest {
    private val zone=ZoneId.of("Asia/Shanghai")
    private val day=LocalDate.of(2026,9,10)
    private fun at(date: LocalDate)=date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test fun `day boundaries and recurring occurrences are counted independently`() {
        val state=KexuUiState(completions=listOf(
            TaskCompletionEntity(taskId=1,title="每日阅读",completedAt=at(day)-1),
            TaskCompletionEntity(taskId=1,title="每日阅读",completedAt=at(day)),
            TaskCompletionEntity(taskId=2,title="实验",completedAt=at(day.plusDays(1))-1),
            TaskCompletionEntity(taskId=3,title="明天",completedAt=at(day.plusDays(1))),
        ))
        assertEquals(2,rhythmDay(state,day,zone).completed)
    }
    @Test fun `focus seconds are summed before rounding and habits use calendar date`() {
        val state=KexuUiState(focusSessions=listOf(
            FocusSessionEntity(durationSeconds=40,startedAt=1,completedAt=at(day)+1),
            FocusSessionEntity(durationSeconds=40,startedAt=2,completedAt=at(day)+2),
        ),habitChecks=listOf(HabitCheckEntity(1,day.toEpochDay()),HabitCheckEntity(2,day.minusDays(1).toEpochDay())))
        val summary=rhythmDay(state,day,zone)
        assertEquals(1,summary.focusSeconds/60);assertEquals(1,summary.habits)
    }
}
