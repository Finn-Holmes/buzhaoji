package com.kexu.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kexu.app.KexuApplication
import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.FocusSessionEntity
import com.kexu.app.data.model.HabitCheckEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.nlp.ParsedTaskInput
import com.kexu.app.importer.CourseDraft
import com.kexu.app.reminder.ReminderCoordinator
import com.kexu.app.reminder.ReminderScheduler
import com.kexu.app.settings.AppSettings
import com.kexu.app.settings.AppTheme
import com.kexu.app.settings.FeatureModule
import com.kexu.app.settings.FocusTimerSettings
import com.kexu.app.settings.ThemeMode
import com.kexu.app.widget.updateAllHomeWidgets
import com.kexu.app.widget.syncTaskWidgetState
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class MainTab {
    TODAY,
    TIMETABLE,
    TASKS,
    QUADRANT,
    FOCUS,
    HABITS,
    COUNTDOWN,
    FEATURES,
    SETTINGS,
}

data class KexuUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val taskTransitions: Map<Long, TaskCompletionTransition> = emptyMap(),
    val highlightedTaskId: Long? = null,
    val courses: List<CourseEntity> = emptyList(),
    val slots: List<ScheduleSlotEntity> = emptyList(),
    val overrides: List<CourseOverrideEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitChecks: List<HabitCheckEntity> = emptyList(),
    val countdowns: List<CountdownEntity> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList(),
    val settings: AppSettings? = null,
    val reflections: List<com.kexu.app.data.model.DayReflectionEntity> = emptyList(),
    val completions: List<com.kexu.app.data.model.TaskCompletionEntity> = emptyList(),
)

class KexuViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as KexuApplication
    private val repository = app.repository
    private val scheduler = ReminderScheduler(application)
    private val taskTransitions = MutableStateFlow<Map<Long, TaskCompletionTransition>>(emptyMap())
    private val highlightedTaskId = MutableStateFlow<Long?>(null)
    private val pendingHabits = mutableSetOf<Pair<Long, Long>>()

    private val coreState = combine(
        repository.tasks,
        repository.courses,
        repository.slots,
        repository.overrides,
        app.settingsRepository.settings,
    ) { tasks, courses, slots, overrides, settings ->
        KexuUiState(
            tasks = tasks,
            courses = courses,
            slots = slots,
            overrides = overrides,
            settings = settings,
        )
    }

    val uiState = combine(
        coreState,
        repository.habits,
        repository.habitChecks,
        repository.countdowns,
        repository.focusSessions,
    ) { core, habits, habitChecks, countdowns, focusSessions ->
        core.copy(
            habits = habits,
            habitChecks = habitChecks,
            countdowns = countdowns,
            focusSessions = focusSessions,
        )
    }.combine(repository.reflections) { state, reflections -> state.copy(reflections = reflections)
    }.combine(repository.completions) { state, completions -> state.copy(completions = completions)
    }.combine(taskTransitions) { state, transitions ->
        state.copy(tasks = applyTaskTransitions(state.tasks, transitions), taskTransitions = transitions)
    }.combine(highlightedTaskId) { state, id -> state.copy(highlightedTaskId = id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KexuUiState())

    fun saveParsedTask(
        parsed: ParsedTaskInput,
        notes: String = "",
        courseId: Long? = null,
        priority: Int = 0,
        existing: TaskEntity? = null,
    ) {
        if (parsed.cleanTitle.isBlank()) return
        viewModelScope.launch {
            // UI may be displaying a brief completion preview; never persist that preview as real state.
            val current = if (existing != null) repository.task(existing.id) ?: return@launch else null
            val task = (current ?: TaskEntity(title = parsed.cleanTitle)).copy(
                title = parsed.cleanTitle,
                notes = notes.trim(),
                dueAt = parsed.dueAtMillis,
                endAt = parsed.endAtMillis,
                remindAt = parsed.reminderAtMillis,
                recurrenceRule = parsed.recurrenceRule,
                courseId = courseId,
                priority = priority,
            )
            val id = repository.saveTask(task)
            if (current == null) {
                highlightedTaskId.value = id
                launch {
                    delay(2_000)
                    if (highlightedTaskId.value == id) highlightedTaskId.value = null
                }
            }
            scheduler.scheduleTask(task.copy(id = id))
            updateAllHomeWidgets(app)
        }
    }

    fun saveReflection(dateEpochDay: Long, note: String, closed: Boolean) {
        viewModelScope.launch {
            repository.saveReflection(com.kexu.app.data.model.DayReflectionEntity(
                dateEpochDay, note.trim().take(280), if (closed) System.currentTimeMillis() else null,
            ))
        }
    }

    fun setCompanionMotion(enabled: Boolean) {
        viewModelScope.launch { app.settingsRepository.setCompanionMotion(enabled) }
    }

    fun importCourses(
        drafts: List<CourseDraft>,
        replaceExisting: Boolean = false,
        semesterStartMillis: Long? = null,
    ) {
        viewModelScope.launch {
            semesterStartMillis?.let { app.settingsRepository.setSemesterStart(it) }
            val reminderMinutes = app.settingsRepository.snapshot().defaultCourseReminderMinutes
            if (replaceExisting) repository.deleteAllCourses()
            drafts.forEach { draft ->
                repository.addCourse(
                    CourseEntity(
                        name = draft.name,
                        teacher = draft.teacher,
                        location = draft.location,
                        startWeek = draft.startWeek,
                        endWeek = draft.endWeek,
                        weekPattern = draft.weekPattern.name,
                        weekMask = draft.weekMask,
                        reminderMinutes = reminderMinutes,
                    ),
                    ScheduleSlotEntity(
                        courseId = 0,
                        dayOfWeek = draft.day.value,
                        startMinutes = draft.startMinutes,
                        endMinutes = draft.endMinutes,
                    ),
                )
            }
            ReminderCoordinator(app, repository, app.settingsRepository).rescheduleAll()
            updateAllHomeWidgets(app)
        }
    }

    fun toggleTask(task: TaskEntity) {
        if (task.id in taskTransitions.value) return
        taskTransitions.value += task.id to TaskCompletionTransition(task, !task.isCompleted)
        viewModelScope.launch {
          try {
            val completed = !task.isCompleted
            if (completed && task.dueAt != null && !task.recurrenceRule.isNullOrBlank()) {
                val next = nextRecurringTask(task)
                if (!repository.updateTaskIfUnchanged(task, next)) return@launch
                scheduler.cancelTask(task.id)
                scheduler.scheduleTask(next)
                syncTaskWidgetState(app, next)
                delay(300)
                updateAllHomeWidgets(app)
                return@launch
            }
            val updated = task.copy(
                    isCompleted = completed,
                    completedAt = if (completed) System.currentTimeMillis() else null,
                )
            if (!repository.updateTaskIfUnchanged(task, updated)) return@launch
            if (completed) scheduler.cancelTask(task.id) else scheduler.scheduleTask(updated)
            syncTaskWidgetState(app, updated)
            delay(300)
            updateAllHomeWidgets(app)
          } finally {
              taskTransitions.value -= task.id
          }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            scheduler.cancelTask(task.id)
            repository.deleteTask(task)
            updateAllHomeWidgets(app)
        }
    }

    fun addCourse(
        name: String,
        teacher: String,
        location: String,
        day: DayOfWeek,
        startMinutes: Int,
        endMinutes: Int,
        startWeek: Int,
        endWeek: Int,
        weekPattern: String,
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val reminderMinutes = app.settingsRepository.snapshot().defaultCourseReminderMinutes
            repository.addCourse(
                CourseEntity(
                    name = name.trim(),
                    teacher = teacher.trim(),
                    location = location.trim(),
                    startWeek = startWeek,
                    endWeek = endWeek,
                    weekPattern = weekPattern,
                    reminderMinutes = reminderMinutes,
                ),
                ScheduleSlotEntity(
                    courseId = 0,
                    dayOfWeek = day.value,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                ),
            )
            ReminderCoordinator(app, repository, app.settingsRepository).rescheduleAll()
            updateAllHomeWidgets(app)
        }
    }

    fun deleteCourse(course: CourseEntity) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            ReminderCoordinator(app, repository, app.settingsRepository).rescheduleAll()
            updateAllHomeWidgets(app)
        }
    }

    fun updateCourse(course: CourseEntity, slot: ScheduleSlotEntity) {
        if (course.name.isBlank() || slot.endMinutes <= slot.startMinutes) return
        viewModelScope.launch {
            repository.updateCourse(course.copy(name = course.name.trim()))
            repository.updateSlot(slot)
            ReminderCoordinator(app, repository, app.settingsRepository).rescheduleAll()
            updateAllHomeWidgets(app)
        }
    }

    fun saveCourseOverride(override: CourseOverrideEntity) {
        if (override.weekNumber !in 1..63) return
        viewModelScope.launch {
            repository.saveOverride(override)
            ReminderCoordinator(app, repository, app.settingsRepository).rescheduleAll()
            updateAllHomeWidgets(app)
        }
    }

    fun deleteCourseOverride(override: CourseOverrideEntity) {
        viewModelScope.launch {
            repository.deleteOverride(override)
            ReminderCoordinator(app, repository, app.settingsRepository).rescheduleAll()
            updateAllHomeWidgets(app)
        }
    }

    fun updateSemesterStart(value: Long) {
        viewModelScope.launch {
            app.settingsRepository.setSemesterStart(value)
            ReminderCoordinator(app, repository, app.settingsRepository).rescheduleAll()
            updateAllHomeWidgets(app)
        }
    }

    fun updateDefaultCourseReminder(minutes: Int) {
        viewModelScope.launch {
            app.settingsRepository.setCourseReminder(minutes)
        }
    }

    fun setFeatureEnabled(feature: FeatureModule, enabled: Boolean) {
        viewModelScope.launch {
            app.settingsRepository.setFeatureEnabled(feature, enabled)
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            app.settingsRepository.setAppTheme(theme)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            app.settingsRepository.setThemeMode(mode)
        }
    }

    fun setFocusTimer(timer: FocusTimerSettings) {
        viewModelScope.launch {
            app.settingsRepository.setFocusTimer(timer)
            updateAllHomeWidgets(app)
        }
    }

    fun saveHabit(
        title: String,
        description: String,
        symbol: String,
        reminderMinutes: Int?,
        existing: HabitEntity? = null,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val habit = (existing ?: HabitEntity(title = title.trim())).copy(
                    title = title.trim(),
                    description = description.trim(),
                    symbol = symbol.ifBlank { "✓" },
                    reminderMinutes = reminderMinutes,
                )
            val id = repository.saveHabit(habit)
            scheduler.scheduleHabit(habit.copy(id = id))
            updateAllHomeWidgets(app)
        }
    }

    fun toggleHabit(habitId: Long, dateEpochDay: Long) {
        val key = habitId to dateEpochDay
        if (!pendingHabits.add(key)) return
        viewModelScope.launch {
          try {
            val completed = repository.currentHabitChecks(dateEpochDay).any { it.habitId == habitId }
            if (completed) {
                repository.deleteHabitCheck(habitId, dateEpochDay)
            } else {
                repository.saveHabitCheck(HabitCheckEntity(habitId, dateEpochDay))
            }
            updateAllHomeWidgets(app)
          } finally {
              pendingHabits.remove(key)
          }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            scheduler.cancelHabit(habit.id)
            repository.deleteHabit(habit)
            updateAllHomeWidgets(app)
        }
    }

    fun saveCountdown(countdown: CountdownEntity) {
        if (countdown.name.isBlank()) return
        viewModelScope.launch {
            val saved = countdown.copy(name = countdown.name.trim())
            val id = repository.saveCountdown(saved)
            scheduler.scheduleCountdown(saved.copy(id = id))
            updateAllHomeWidgets(app)
        }
    }

    fun deleteCountdown(countdown: CountdownEntity) {
        viewModelScope.launch {
            scheduler.cancelCountdown(countdown.id)
            repository.deleteCountdown(countdown)
            updateAllHomeWidgets(app)
        }
    }

    fun recordFocusSession(
        taskId: Long?,
        durationSeconds: Int,
        startedAt: Long,
        mode: String,
    ) {
        if (durationSeconds <= 0) return
        viewModelScope.launch {
            repository.saveFocusSession(
                FocusSessionEntity(
                    taskId = taskId,
                    durationSeconds = durationSeconds,
                    startedAt = startedAt,
                    mode = mode,
                ),
            )
            updateAllHomeWidgets(app)
        }
    }
}

internal fun nextRecurringTask(
    task: TaskEntity,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): TaskEntity {
    val dueAt = requireNotNull(task.dueAt)
    val rule = requireNotNull(task.recurrenceRule)
    var next = Instant.ofEpochMilli(dueAt).atZone(zoneId).toLocalDateTime()
    do {
        next = when {
            rule == "DAILY" -> next.plusDays(1)
            rule == "WEEKDAYS" -> {
                var candidate = next.plusDays(1)
                while (candidate.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                    candidate = candidate.plusDays(1)
                }
                candidate
            }
            rule.startsWith("WEEKLY:") -> next.plusWeeks(1)
            rule.startsWith("MONTHLY:") -> next.plusMonths(1)
            rule.startsWith("YEARLY:") -> next.plusYears(1)
            else -> next.plusDays(1)
        }
    } while (next.atZone(zoneId).toInstant().toEpochMilli() <= nowMillis)
    val nextDueAt = next.atZone(zoneId).toInstant().toEpochMilli()
    val shift = nextDueAt - dueAt
    return task.copy(
        dueAt = nextDueAt,
        endAt = task.endAt?.plus(shift),
        remindAt = task.remindAt?.plus(shift),
        isCompleted = false,
        completedAt = null,
    )
}
