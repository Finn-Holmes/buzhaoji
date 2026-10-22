package com.kexu.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY name")
    fun observeCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses")
    suspend fun getCourses(): List<CourseEntity>

    @Query("SELECT * FROM schedule_slots ORDER BY dayOfWeek, startMinutes")
    fun observeSlots(): Flow<List<ScheduleSlotEntity>>

    @Query("SELECT * FROM schedule_slots ORDER BY dayOfWeek, startMinutes")
    suspend fun getSlots(): List<ScheduleSlotEntity>

    @Query("SELECT * FROM course_overrides ORDER BY weekNumber, slotId")
    fun observeOverrides(): Flow<List<CourseOverrideEntity>>

    @Query("SELECT * FROM course_overrides ORDER BY weekNumber, slotId")
    suspend fun getOverrides(): List<CourseOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: ScheduleSlotEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Update
    suspend fun updateSlot(slot: ScheduleSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: CourseOverrideEntity): Long

    @Delete
    suspend fun deleteOverride(override: CourseOverrideEntity)

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()

    @Delete
    suspend fun deleteSlot(slot: ScheduleSlotEntity)
}
