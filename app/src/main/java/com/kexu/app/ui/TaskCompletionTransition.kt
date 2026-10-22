package com.kexu.app.ui

import com.kexu.app.data.model.TaskEntity
import androidx.compose.runtime.compositionLocalOf

internal val LocalTaskTransitions = compositionLocalOf<Map<Long, TaskCompletionTransition>> { emptyMap() }
internal val LocalHighlightedTaskId = compositionLocalOf<Long?> { null }

data class TaskCompletionTransition(val original: TaskEntity, val completed: Boolean) {
    val display: TaskEntity get() = original.copy(isCompleted = completed)
}

internal fun KexuUiState.completedForPlacement(task: TaskEntity): Boolean =
    taskTransitions[task.id]?.original?.isCompleted ?: task.isCompleted

internal fun applyTaskTransitions(tasks: List<TaskEntity>, transitions: Map<Long, TaskCompletionTransition>): List<TaskEntity> {
    if (transitions.isEmpty()) return tasks
    // Room moves completed rows to the bottom immediately. Keep its original ordering during feedback.
    return tasks.map { transitions[it.id]?.display ?: it }.sortedWith(
        compareBy<TaskEntity> { transitions[it.id]?.original?.isCompleted ?: it.isCompleted }
            .thenBy { it.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.createdAt }.thenByDescending { it.id },
    )
}
