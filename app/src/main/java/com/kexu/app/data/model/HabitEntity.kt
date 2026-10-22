package com.kexu.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val symbol: String = "✓",
    val reminderMinutes: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
