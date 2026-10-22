package com.kexu.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A one-week change for one recurring schedule slot.
 *
 * Null replacement fields keep the original slot value. A cancelled override
 * suppresses the occurrence entirely. The unique index guarantees that saving
 * another change for the same slot/week replaces the previous decision.
 */
@Entity(
    tableName = "course_overrides",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("slotId"),
        Index(value = ["slotId", "weekNumber"], unique = true),
    ],
)
data class CourseOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotId: Long,
    val weekNumber: Int,
    val isCancelled: Boolean = false,
    val dayOfWeek: Int? = null,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val location: String? = null,
    val note: String = "",
)
