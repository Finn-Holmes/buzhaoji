package com.kexu.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.FocusSessionEntity
import com.kexu.app.data.model.HabitCheckEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.DayReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeatureDao {
    @Query("SELECT * FROM day_reflections ORDER BY dateEpochDay DESC")
    fun observeReflections(): Flow<List<DayReflectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReflection(reflection: DayReflectionEntity)

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    suspend fun getHabits(): List<HabitEntity>

    @Query("SELECT * FROM habit_checks ORDER BY dateEpochDay DESC")
    fun observeHabitChecks(): Flow<List<HabitCheckEntity>>

    @Query("SELECT * FROM habit_checks WHERE dateEpochDay = :dateEpochDay")
    suspend fun getHabitChecks(dateEpochDay: Long): List<HabitCheckEntity>

    @Query("SELECT dateEpochDay FROM habit_checks WHERE habitId = :habitId ORDER BY dateEpochDay DESC")
    suspend fun getHabitCheckDays(habitId: Long): List<Long>

    @Query("SELECT * FROM countdowns ORDER BY targetDateEpochDay ASC")
    fun observeCountdowns(): Flow<List<CountdownEntity>>

    @Query("SELECT * FROM countdowns ORDER BY targetDateEpochDay ASC")
    suspend fun getCountdowns(): List<CountdownEntity>

    @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC")
    fun observeFocusSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY completedAt DESC")
    suspend fun getFocusSessions(): List<FocusSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHabit(habit: HabitEntity): Long

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHabitCheck(check: HabitCheckEntity)

    @Query("DELETE FROM habit_checks WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay")
    suspend fun deleteHabitCheck(habitId: Long, dateEpochDay: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCountdown(countdown: CountdownEntity): Long

    @Delete
    suspend fun deleteCountdown(countdown: CountdownEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFocusSession(session: FocusSessionEntity): Long

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE startedAt = :startedAt AND mode = :mode")
    suspend fun countFocusSession(startedAt: Long, mode: String): Int

    @androidx.room.Transaction
    suspend fun saveFocusSessionOnce(session: FocusSessionEntity) {
        if (session.startedAt > 0 && session.durationSeconds > 0 &&
            countFocusSession(session.startedAt, session.mode) == 0) saveFocusSession(session)
    }
}
