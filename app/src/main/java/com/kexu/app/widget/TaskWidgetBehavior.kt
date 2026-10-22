package com.kexu.app.widget

import com.kexu.app.data.model.TaskEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Captured in each rendered action: a delayed/double tap must not invert newer data
// or advance a recurring task twice after its deadline has moved to the next occurrence.
internal fun TaskEntity.widgetVersion(): String =
    "$id|$isCompleted|$dueAt|$completedAt|$createdAt"

internal fun compactTaskDeadline(task: TaskEntity, today: LocalDate, zone: ZoneId): String {
    val due = task.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone) } ?: return ""
    return due.format(DateTimeFormatter.ofPattern(if (due.toLocalDate() == today) "HH:mm" else "M/d"))
}
