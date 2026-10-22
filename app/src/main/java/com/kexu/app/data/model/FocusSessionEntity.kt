package com.kexu.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    indices = [Index("taskId"), Index("startedAt")],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val durationSeconds: Int,
    val startedAt: Long,
    val completedAt: Long = System.currentTimeMillis(),
    val mode: String = "POMODORO",
)
