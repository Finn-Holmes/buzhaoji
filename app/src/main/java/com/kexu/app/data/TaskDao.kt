package com.kexu.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.data.model.TaskCompletionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, createdAt DESC")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND remindAt IS NOT NULL AND remindAt > :after")
    suspend fun getFutureReminders(after: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND dueAt >= :start AND dueAt < :end ORDER BY dueAt ASC")
    suspend fun getDueBetween(start: Long, end: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Transaction
    suspend fun updateIfUnchanged(expected: TaskEntity, updated: TaskEntity): Boolean {
        if (getById(expected.id) != expected) return false
        update(updated)
        if (!expected.isCompleted && (updated.isCompleted ||
                (!expected.recurrenceRule.isNullOrBlank() && expected.dueAt != updated.dueAt))) {
            insertCompletion(TaskCompletionEntity(taskId = expected.id, title = expected.title,
                completedAt = updated.completedAt ?: System.currentTimeMillis()))
        } else if (expected.isCompleted && !updated.isCompleted) {
            removeCompletion(expected.id, expected.completedAt ?: -1L)
        }
        return true
    }

    @Insert
    suspend fun insertCompletion(completion: TaskCompletionEntity)

    @Query("DELETE FROM task_completions WHERE taskId = :taskId AND completedAt = :completedAt")
    suspend fun removeCompletion(taskId: Long, completedAt: Long)

    @Query("SELECT * FROM task_completions ORDER BY completedAt DESC")
    fun observeCompletions(): Flow<List<TaskCompletionEntity>>

    @Query("SELECT * FROM task_completions ORDER BY completedAt DESC")
    suspend fun getCompletions(): List<TaskCompletionEntity>

    @Delete
    suspend fun delete(task: TaskEntity)
}
