package com.kexu.app.ui

import com.kexu.app.data.model.TaskEntity
import org.junit.Assert.*
import org.junit.Test

class TaskCompletionTransitionTest {
    @Test fun `database reordering does not make completing rows cross their neighbors`() {
        val older = TaskEntity(id = 1, title = "较早", createdAt = 1000)
        val newer = TaskEntity(id = 2, title = "较新", createdAt = 2000)
        val databaseOrder = listOf(older, newer.copy(isCompleted = true))
        val displayed = applyTaskTransitions(databaseOrder, mapOf(2L to TaskCompletionTransition(newer, true)))
        assertEquals(listOf(2L, 1L), displayed.map { it.id })
    }

    @Test fun `completion animates in original list without changing persisted data`() {
        val original = TaskEntity(id = 1, title = "测试", dueAt = 1000)
        val persisted = original.copy(isCompleted = true, completedAt = 2000)
        val transitions = mapOf(1L to TaskCompletionTransition(original, true))
        val displayed = applyTaskTransitions(listOf(persisted), transitions).single()
        val state = KexuUiState(tasks = listOf(displayed), taskTransitions = transitions)
        assertTrue(displayed.isCompleted)
        assertFalse(state.completedForPlacement(displayed))
        assertEquals(persisted, applyTaskTransitions(listOf(persisted), emptyMap()).single())
        assertFalse(original.isCompleted)
    }

    @Test fun `restoring and recurring tasks stay in place until feedback ends`() {
        val completed = TaskEntity(id = 2, title = "已完成", isCompleted = true)
        val restored = completed.copy(isCompleted = false)
        val transition = mapOf(2L to TaskCompletionTransition(completed, false))
        assertTrue(KexuUiState(taskTransitions = transition).completedForPlacement(restored))
        val recurring = TaskEntity(id = 3, title = "每日复习", dueAt = 1000, recurrenceRule = "DAILY")
        val next = recurring.copy(dueAt = 2000)
        val feedback = mapOf(3L to TaskCompletionTransition(recurring, true))
        val display = applyTaskTransitions(listOf(next), feedback).single()
        assertTrue(display.isCompleted)
        assertEquals(1000L, display.dueAt)
        assertEquals(next, applyTaskTransitions(listOf(next), emptyMap()).single())
    }

    @Test fun `feedback cannot resurrect deleted tasks or affect other rows`() {
        val task = TaskEntity(id = 4, title = "测试")
        val other = TaskEntity(id = 5, title = "保留")
        val transitions = mapOf(4L to TaskCompletionTransition(task, true))
        assertEquals(listOf(other), applyTaskTransitions(listOf(other), transitions))
    }
}
