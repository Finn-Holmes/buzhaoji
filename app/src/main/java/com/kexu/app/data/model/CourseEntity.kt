package com.kexu.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val teacher: String = "",
    val location: String = "",
    val colorArgb: Long = 0xFF5D7C66,
    val startWeek: Int = 1,
    val endWeek: Int = 20,
    val weekPattern: String = WeekPattern.ALL.name,
    /** Bit 0 represents week 1. Zero keeps the legacy range/odd-even behavior. */
    val weekMask: Long = 0L,
    val reminderMinutes: Int = 15,
)

enum class WeekPattern {
    ALL,
    ODD,
    EVEN,
}
