package com.kexu.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "countdowns",
    indices = [Index("targetDateEpochDay")],
)
data class CountdownEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetDateEpochDay: Long,
    val type: String = "COUNTDOWN",
    val symbol: String = "⌛",
    val repeatYearly: Boolean = false,
    val reminderDaysBefore: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
