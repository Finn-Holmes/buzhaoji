package com.kexu.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "habit_checks",
    primaryKeys = ["habitId", "dateEpochDay"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId"), Index("dateEpochDay")],
)
data class HabitCheckEntity(
    val habitId: Long,
    val dateEpochDay: Long,
    val completedAt: Long = System.currentTimeMillis(),
)
