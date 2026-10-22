package com.kexu.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "day_reflections")
data class DayReflectionEntity(
    @PrimaryKey val dateEpochDay: Long,
    val note: String = "",
    val closedAt: Long? = null,
)

/** An occurrence survives recurring-task advancement and deletion of the original task. */
@Entity(tableName = "task_completions", indices = [Index("completedAt"), Index("taskId")])
data class TaskCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val title: String,
    val completedAt: Long,
)
