package com.kexu.app.data

import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.FocusSessionEntity
import com.kexu.app.data.model.HabitCheckEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import com.kexu.app.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

class KexuRepository(
    private val taskDao: TaskDao,
    private val courseDao: CourseDao,
    private val featureDao: FeatureDao,
) {
    val tasks: Flow<List<TaskEntity>> = taskDao.observeAll()
    val courses: Flow<List<CourseEntity>> = courseDao.observeCourses()
    val slots: Flow<List<ScheduleSlotEntity>> = courseDao.observeSlots()
    val overrides: Flow<List<CourseOverrideEntity>> = courseDao.observeOverrides()
    val habits: Flow<List<HabitEntity>> = featureDao.observeHabits()
    val habitChecks: Flow<List<HabitCheckEntity>> = featureDao.observeHabitChecks()
    val countdowns: Flow<List<CountdownEntity>> = featureDao.observeCountdowns()
    val focusSessions: Flow<List<FocusSessionEntity>> = featureDao.observeFocusSessions()
    val reflections = featureDao.observeReflections()
    val completions = taskDao.observeCompletions()
    suspend fun currentCompletions() = taskDao.getCompletions()
    suspend fun saveReflection(reflection: com.kexu.app.data.model.DayReflectionEntity) =
        featureDao.saveReflection(reflection)

    suspend fun saveTask(task: TaskEntity): Long =
        if (task.id == 0L) taskDao.insert(task) else {
            taskDao.update(task)
            task.id
        }

    suspend fun task(id: Long): TaskEntity? = taskDao.getById(id)
    suspend fun updateTaskIfUnchanged(expected: TaskEntity, updated: TaskEntity): Boolean =
        taskDao.updateIfUnchanged(expected, updated)
    suspend fun currentTasks() = taskDao.getAll()
    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)
    suspend fun futureTaskReminders(after: Long) = taskDao.getFutureReminders(after)
    suspend fun tasksDueBetween(start: Long, end: Long) = taskDao.getDueBetween(start, end)

    suspend fun addCourse(course: CourseEntity, slot: ScheduleSlotEntity): Long {
        val courseId = courseDao.insertCourse(course)
        courseDao.insertSlot(slot.copy(courseId = courseId))
        return courseId
    }

    suspend fun updateCourse(course: CourseEntity) = courseDao.updateCourse(course)
    suspend fun updateSlot(slot: ScheduleSlotEntity) = courseDao.updateSlot(slot)
    suspend fun deleteCourse(course: CourseEntity) = courseDao.deleteCourse(course)
    suspend fun deleteAllCourses() = courseDao.deleteAllCourses()
    suspend fun addSlot(slot: ScheduleSlotEntity) = courseDao.insertSlot(slot)
    suspend fun deleteSlot(slot: ScheduleSlotEntity) = courseDao.deleteSlot(slot)
    suspend fun currentCourses() = courseDao.getCourses()
    suspend fun currentSlots() = courseDao.getSlots()
    suspend fun currentOverrides() = courseDao.getOverrides()
    suspend fun saveOverride(override: CourseOverrideEntity) = courseDao.upsertOverride(override)
    suspend fun deleteOverride(override: CourseOverrideEntity) = courseDao.deleteOverride(override)

    suspend fun saveHabit(habit: HabitEntity) = featureDao.saveHabit(habit)
    suspend fun deleteHabit(habit: HabitEntity) = featureDao.deleteHabit(habit)
    suspend fun currentHabits() = featureDao.getHabits()
    suspend fun currentHabitChecks(dateEpochDay: Long) = featureDao.getHabitChecks(dateEpochDay)
    suspend fun currentHabitCheckDays(habitId: Long) = featureDao.getHabitCheckDays(habitId)
    suspend fun saveHabitCheck(check: HabitCheckEntity) = featureDao.saveHabitCheck(check)
    suspend fun deleteHabitCheck(habitId: Long, dateEpochDay: Long) =
        featureDao.deleteHabitCheck(habitId, dateEpochDay)

    suspend fun saveCountdown(countdown: CountdownEntity) = featureDao.saveCountdown(countdown)
    suspend fun deleteCountdown(countdown: CountdownEntity) = featureDao.deleteCountdown(countdown)
    suspend fun currentCountdowns() = featureDao.getCountdowns()
    suspend fun currentFocusSessions() = featureDao.getFocusSessions()
    suspend fun saveFocusSession(session: FocusSessionEntity) = featureDao.saveFocusSessionOnce(session)
}
