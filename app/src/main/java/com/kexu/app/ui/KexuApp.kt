package com.kexu.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Hexagon
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.NoteAlt
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import com.kexu.app.ui.MotionFloatingActionButton as FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.kexu.app.ui.MotionIconButton as IconButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CourseOverrideEntity
import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.ScheduleSlotEntity
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.data.model.WeekPattern
import com.kexu.app.importer.CourseDraft
import com.kexu.app.importer.OcrTextBox
import com.kexu.app.importer.TimetableImageGridParser
import com.kexu.app.importer.TimetableTextParser
import com.kexu.app.nlp.ChineseTimeParser
import com.kexu.app.reminder.ScheduleCalculator
import com.kexu.app.settings.FeatureModule
import com.kexu.app.settings.FocusTimerSettings
import com.kexu.app.settings.AppTheme
import com.kexu.app.settings.ThemeMode
import com.kexu.app.BuildConfig
import com.kexu.app.MainActivity
import com.kexu.app.R
import com.kexu.app.ui.theme.Coral
import com.kexu.app.ui.theme.Forest
import com.kexu.app.ui.theme.Mint
import com.kexu.app.ui.theme.paletteFor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class PendingCourseImport(
    val courses: List<CourseDraft>,
    val replaceExisting: Boolean,
)

private data class DisplayedCourseMeeting(
    val course: CourseEntity,
    val slot: ScheduleSlotEntity,
    val override: CourseOverrideEntity? = null,
    val isCancelled: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KexuApp(
    notificationTaskId: Long? = null,
    onNotificationTaskHandled: () -> Unit = {},
    widgetAction: String? = null,
    onWidgetActionHandled: () -> Unit = {},
    viewModel: KexuViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(MainTab.TODAY) }
    var taskFilter by rememberSaveable { mutableStateOf(TaskFilter.ALL) }
    var showTaskEditor by rememberSaveable { mutableStateOf(false) }
    var showCourseEditor by rememberSaveable { mutableStateOf(false) }
    var showImportHub by rememberSaveable { mutableStateOf(false) }
    var showAcademicImport by rememberSaveable { mutableStateOf(false) }
    var showLegacyImport by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showSemesterCalibration by rememberSaveable { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var editingCourseSlot by remember { mutableStateOf<Pair<CourseEntity, ScheduleSlotEntity>?>(null) }
    var taskCoursePreset by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskDefaultDateEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var taskDefaultPriority by rememberSaveable { mutableIntStateOf(0) }
    var pendingAcademicCourses by remember { mutableStateOf<List<CourseDraft>?>(null) }
    var pendingCourseImport by remember { mutableStateOf<PendingCourseImport?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var openFootprintsFromWidget by rememberSaveable { mutableStateOf(false) }
    if (openFootprintsFromWidget) FootprintsSheet(state, rememberRhythmDate(),
        { openFootprintsFromWidget = false }, viewModel::setCompanionMotion)

    LaunchedEffect(notificationTaskId, state.tasks, state.settings) {
        val taskId = notificationTaskId ?: return@LaunchedEffect
        val task = state.tasks.firstOrNull { it.id == taskId }
        if (task != null) {
            tab = MainTab.TODAY
            editingTask = task
            taskCoursePreset = task.courseId
            taskDefaultDateEpochDay = null
            taskDefaultPriority = task.priority
            showTaskEditor = true
            onNotificationTaskHandled()
        } else if (state.settings != null) {
            onNotificationTaskHandled()
        }
    }

    LaunchedEffect(widgetAction, state.settings) {
        val action = widgetAction ?: return@LaunchedEffect
        if (state.settings == null) return@LaunchedEffect
        when (action) {
            MainActivity.ACTION_NEW_TASK -> {
                tab = MainTab.TODAY
                editingTask = null
                taskCoursePreset = null
                taskDefaultDateEpochDay = LocalDate.now().toEpochDay()
                taskDefaultPriority = 0
                showTaskEditor = true
            }
            MainActivity.ACTION_OPEN_TODAY -> tab = MainTab.TODAY
            MainActivity.ACTION_OPEN_TASKS -> tab = MainTab.TASKS
            MainActivity.ACTION_OPEN_TIMETABLE -> tab = MainTab.TIMETABLE
            MainActivity.ACTION_OPEN_HABITS -> tab = MainTab.HABITS
            MainActivity.ACTION_OPEN_COUNTDOWN -> tab = MainTab.COUNTDOWN
            MainActivity.ACTION_OPEN_FOCUS -> tab = MainTab.FOCUS
            MainActivity.ACTION_OPEN_FOOTPRINTS -> { tab = MainTab.TODAY; openFootprintsFromWidget = true }
        }
        onWidgetActionHandled()
    }

    CompositionLocalProvider(
        LocalTaskTransitions provides state.taskTransitions,
        LocalHighlightedTaskId provides state.highlightedTaskId,
    ) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            KexuNavigationDrawer(
                state = state,
                selectedTab = tab,
                selectedTaskFilter = taskFilter,
                onToday = {
                    tab = MainTab.TODAY
                    scope.launch { drawerState.close() }
                },
                onTasks = { filter ->
                    taskFilter = filter
                    tab = MainTab.TASKS
                    scope.launch { drawerState.close() }
                },
                onTimetable = {
                    tab = MainTab.TIMETABLE
                    scope.launch { drawerState.close() }
                },
                onSettings = {
                    tab = MainTab.SETTINGS
                    scope.launch { drawerState.close() }
                },
                onSearch = {
                    showSearch = true
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                if (tab == MainTab.TASKS) {
                    KexuTopBar(
                        title = taskFilter.pageTitle,
                        onMenu = { scope.launch { drawerState.open() } },
                        onSearch = { showSearch = true },
                        showSearch = FeatureModule.SEARCH in state.settings?.enabledFeatures.orEmpty(),
                    )
                }
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(62.dp),
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                ) {
                    val taskSelected = tab == MainTab.TODAY || tab == MainTab.TASKS
                    NavigationBarItem(
                        selected = taskSelected,
                        onClick = { tab = MainTab.TODAY },
                        icon = { DidaTaskTabIcon(selected = taskSelected) },
                        colors = didaNavigationItemColors(),
                    )
                    NavigationBarItem(
                        selected = tab == MainTab.TIMETABLE,
                        onClick = { tab = MainTab.TIMETABLE },
                        icon = {
                            DidaCalendarTabIcon(
                                day = LocalDate.now().dayOfMonth,
                                selected = tab == MainTab.TIMETABLE,
                            )
                        },
                        colors = didaNavigationItemColors(),
                    )
                    val enabledFeatureTabs = listOf(
                        FeatureModule.QUADRANT to MainTab.QUADRANT,
                        FeatureModule.FOCUS to MainTab.FOCUS,
                        FeatureModule.HABITS to MainTab.HABITS,
                        FeatureModule.COUNTDOWN to MainTab.COUNTDOWN,
                    ).filter { it.first in state.settings?.enabledFeatures.orEmpty() }
                    val activeFeatureTab = enabledFeatureTabs.firstOrNull { it.second == tab }
                    val featureTabs =
                        if (activeFeatureTab != null && enabledFeatureTabs.indexOf(activeFeatureTab) >= 2) {
                            listOf(enabledFeatureTabs.first(), activeFeatureTab)
                        } else {
                            enabledFeatureTabs.take(2)
                        }
                    featureTabs.forEach { (feature, featureTab) ->
                        NavigationBarItem(
                            selected = tab == featureTab,
                            onClick = { tab = featureTab },
                            icon = {
                                Icon(
                                    when (feature) {
                                        FeatureModule.QUADRANT -> Icons.Outlined.GridView
                                        FeatureModule.FOCUS -> Icons.Outlined.Timer
                                        FeatureModule.HABITS -> Icons.Outlined.AccessTime
                                        FeatureModule.COUNTDOWN -> Icons.Outlined.HourglassBottom
                                        FeatureModule.SEARCH -> Icons.Outlined.Search
                                    },
                                    contentDescription = feature.moduleTitle,
                                )
                            },
                            colors = didaNavigationItemColors(),
                        )
                    }
                    NavigationBarItem(
                        selected = tab == MainTab.SETTINGS || tab == MainTab.FEATURES,
                        onClick = { tab = MainTab.SETTINGS },
                        icon = { DidaSettingsTabIcon(selected = tab == MainTab.SETTINGS || tab == MainTab.FEATURES) },
                        colors = didaNavigationItemColors(),
                    )
                }
            },
            floatingActionButton = {
                when (tab) {
                    MainTab.TODAY, MainTab.TASKS -> FloatingActionButton(
                        onClick = {
                            editingTask = null
                            taskCoursePreset = null
                            taskDefaultPriority = 0
                            taskDefaultDateEpochDay = if (tab == MainTab.TODAY) LocalDate.now().toEpochDay() else null
                            showTaskEditor = true
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新建任务")
                    }
                    MainTab.TIMETABLE -> Unit
                    MainTab.QUADRANT -> FloatingActionButton(
                        onClick = {
                            editingTask = null
                            taskCoursePreset = null
                            taskDefaultDateEpochDay = LocalDate.now().toEpochDay()
                            taskDefaultPriority = 3
                            showTaskEditor = true
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "添加四象限任务")
                    }
                    MainTab.FOCUS, MainTab.HABITS, MainTab.COUNTDOWN, MainTab.FEATURES, MainTab.SETTINGS -> Unit
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                },
                label = "main-tab-transition",
            ) { activeTab ->
            when (activeTab) {
                MainTab.TODAY -> TodayScreen(
                    state = state,
                    onSaveReflection = viewModel::saveReflection,
                    onCompanionMotion = viewModel::setCompanionMotion,
                    onToggle = viewModel::toggleTask,
                    onMenu = { scope.launch { drawerState.open() } },
                    onSearch = { showSearch = true },
                    onEdit = { task ->
                        editingTask = task
                        taskDefaultDateEpochDay = null
                        taskDefaultPriority = task.priority
                        showTaskEditor = true
                    },
                    modifier = Modifier.padding(padding),
                )
                MainTab.TIMETABLE -> CalendarHubScreen(
                    state = state,
                    onImport = { showImportHub = true },
                    onCalibrateSemester = { showSemesterCalibration = true },
                    onAddTask = { date ->
                        editingTask = null
                        taskCoursePreset = null
                        taskDefaultDateEpochDay = date.toEpochDay()
                        taskDefaultPriority = 0
                        showTaskEditor = true
                    },
                    onAddCourse = {
                        editingCourseSlot = null
                        showCourseEditor = true
                    },
                    onDeleteCourse = viewModel::deleteCourse,
                    onEditCourse = { course, slot ->
                        editingCourseSlot = course to slot
                        showCourseEditor = true
                    },
                    onSaveOverride = viewModel::saveCourseOverride,
                    onDeleteOverride = viewModel::deleteCourseOverride,
                    onAddTaskForCourse = { courseId ->
                        editingTask = null
                        taskCoursePreset = courseId
                        taskDefaultDateEpochDay = null
                        taskDefaultPriority = 0
                        showTaskEditor = true
                    },
                    onToggleTask = viewModel::toggleTask,
                    onEditTask = { task ->
                        editingTask = task
                        taskCoursePreset = task.courseId
                        taskDefaultDateEpochDay = null
                        taskDefaultPriority = task.priority
                        showTaskEditor = true
                    },
                    modifier = Modifier.padding(padding),
                )
                MainTab.TASKS -> TasksScreen(
                    state = state,
                    filter = taskFilter,
                    onFilterChange = { taskFilter = it },
                    onToggle = viewModel::toggleTask,
                    onDelete = viewModel::deleteTask,
                    onEdit = { task ->
                        editingTask = task
                        taskDefaultDateEpochDay = null
                        taskDefaultPriority = task.priority
                        showTaskEditor = true
                    },
                    modifier = Modifier.padding(padding),
                )
                MainTab.QUADRANT -> QuadrantScreen(
                    tasks = state.tasks,
                    onToggle = viewModel::toggleTask,
                    onEdit = { task ->
                        editingTask = task
                        taskDefaultDateEpochDay = null
                        taskDefaultPriority = task.priority
                        showTaskEditor = true
                    },
                    onAdd = { important, urgent ->
                        editingTask = null
                        taskCoursePreset = null
                        taskDefaultDateEpochDay = if (urgent) LocalDate.now().toEpochDay() else null
                        taskDefaultPriority = if (important) 3 else 0
                        showTaskEditor = true
                    },
                    modifier = Modifier.padding(padding),
                )
                MainTab.FOCUS -> FocusScreen(
                    state = state,
                    onRecordSession = viewModel::recordFocusSession,
                    onTimerChange = viewModel::setFocusTimer,
                    modifier = Modifier.padding(padding),
                )
                MainTab.HABITS -> HabitsScreen(
                    state = state,
                    onSaveHabit = viewModel::saveHabit,
                    onToggleHabit = viewModel::toggleHabit,
                    onDeleteHabit = viewModel::deleteHabit,
                    modifier = Modifier.padding(padding),
                )
                MainTab.COUNTDOWN -> CountdownsScreen(
                    state = state,
                    onSave = viewModel::saveCountdown,
                    onDelete = viewModel::deleteCountdown,
                    modifier = Modifier.padding(padding),
                )
                MainTab.FEATURES -> FeatureModulesScreen(
                    state = state,
                    onBack = { tab = MainTab.SETTINGS },
                    onToggle = viewModel::setFeatureEnabled,
                    onOpen = { feature ->
                        when (feature) {
                            FeatureModule.QUADRANT -> tab = MainTab.QUADRANT
                            FeatureModule.FOCUS -> tab = MainTab.FOCUS
                            FeatureModule.HABITS -> tab = MainTab.HABITS
                            FeatureModule.COUNTDOWN -> tab = MainTab.COUNTDOWN
                            FeatureModule.SEARCH -> showSearch = true
                        }
                    },
                    modifier = Modifier.padding(padding),
                )
                MainTab.SETTINGS -> SettingsScreen(
                    state = state,
                    onSemesterStartChange = viewModel::updateSemesterStart,
                    onDefaultCourseReminderChange = viewModel::updateDefaultCourseReminder,
                    onAppThemeChange = viewModel::setAppTheme,
                    onThemeModeChange = viewModel::setThemeMode,
                    onOpenImport = { showImportHub = true },
                    onOpenFeatureModules = { tab = MainTab.FEATURES },
                    modifier = Modifier.padding(padding),
                )
            }
            }
        }
    }

    }
    if (showSearch) {
        TaskSearchDialog(
            tasks = state.tasks,
            onDismiss = { showSearch = false },
            onTaskClick = { task ->
                editingTask = task
                taskDefaultDateEpochDay = null
                showSearch = false
                showTaskEditor = true
            },
        )
    }

    if (showTaskEditor) {
        TaskEditorSheet(
            courses = state.courses,
            existing = editingTask,
            initialCourseId = taskCoursePreset,
            initialDueDate = taskDefaultDateEpochDay?.let(LocalDate::ofEpochDay),
            initialPriority = taskDefaultPriority,
            onDismiss = {
                showTaskEditor = false
                taskCoursePreset = null
                taskDefaultDateEpochDay = null
                taskDefaultPriority = 0
            },
            onSave = { parsed, notes, courseId, priority ->
                viewModel.saveParsedTask(parsed, notes, courseId, priority, editingTask)
                showTaskEditor = false
                editingTask = null
                taskCoursePreset = null
                taskDefaultDateEpochDay = null
                taskDefaultPriority = 0
            },
        )
    }
    if (showCourseEditor) {
        val editing = editingCourseSlot
        CourseEditorSheet(
            existingCourse = editing?.first,
            existingSlot = editing?.second,
            onDismiss = {
                showCourseEditor = false
                editingCourseSlot = null
            },
            onSave = { name, teacher, location, day, start, end, startWeek, endWeek, pattern ->
                if (editing == null) {
                    viewModel.addCourse(name, teacher, location, day, start, end, startWeek, endWeek, pattern)
                } else {
                    viewModel.updateCourse(
                        editing.first.copy(
                            name = name.trim(),
                            teacher = teacher.trim(),
                            location = location.trim(),
                            startWeek = startWeek,
                            endWeek = endWeek,
                            weekPattern = pattern,
                            weekMask = 0L,
                        ),
                        editing.second.copy(
                            dayOfWeek = day.value,
                            startMinutes = start,
                            endMinutes = end,
                        ),
                    )
                }
                showCourseEditor = false
                editingCourseSlot = null
            },
        )
    }
    if (showImportHub) {
        TimetableImportHubSheet(
            onDismiss = { showImportHub = false },
            onAcademic = {
                showImportHub = false
                showAcademicImport = true
            },
            onOther = {
                showImportHub = false
                showLegacyImport = true
            },
        )
    }
    if (showAcademicImport) {
        AcademicWebImportDialog(
            onDismiss = { showAcademicImport = false },
            onImport = {
                if (state.courses.isEmpty()) {
                    pendingCourseImport = PendingCourseImport(it, replaceExisting = true)
                } else {
                    pendingAcademicCourses = it
                }
                showAcademicImport = false
            },
        )
    }
    if (showLegacyImport) {
        TimetableImportSheet(
            onDismiss = { showLegacyImport = false },
            onImport = {
                pendingCourseImport = PendingCourseImport(it, replaceExisting = false)
                showLegacyImport = false
            },
        )
    }
    pendingAcademicCourses?.let { courses ->
        AlertDialog(
            onDismissRequest = { pendingAcademicCourses = null },
            title = { Text("当前课表已有课程") },
            text = {
                Text(
                    "当前有 ${state.courses.size} 门课程，本次识别到 ${courses.size} 条课程安排。你可以清空后覆盖，或把识别结果合并到当前课表。",
                )
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingAcademicCourses = null }) { Text("取消") }
                    TextButton(
                        onClick = {
                            pendingCourseImport = PendingCourseImport(courses, replaceExisting = false)
                            pendingAcademicCourses = null
                        },
                    ) { Text("合并导入") }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingCourseImport = PendingCourseImport(courses, replaceExisting = true)
                        pendingAcademicCourses = null
                    },
                ) { Text("覆盖当前课表") }
            },
        )
    }
    pendingCourseImport?.let { pending ->
        SemesterCalibrationDialog(
            maxCourseWeek = maxImportedWeek(pending.courses),
            existingSemesterStartMillis = state.settings?.semesterStartMillis,
            requireExplicitSelection = true,
            onDismiss = { pendingCourseImport = null },
            onConfirm = { semesterStartMillis ->
                viewModel.importCourses(
                    drafts = pending.courses,
                    replaceExisting = pending.replaceExisting,
                    semesterStartMillis = semesterStartMillis,
                )
                pendingCourseImport = null
            },
        )
    }
    if (showSemesterCalibration && pendingCourseImport == null) {
        SemesterCalibrationDialog(
            maxCourseWeek = maxStoredWeek(state.courses),
            existingSemesterStartMillis = state.settings?.semesterStartMillis,
            onDismiss = { showSemesterCalibration = false },
            onConfirm = { semesterStartMillis ->
                viewModel.updateSemesterStart(semesterStartMillis)
                showSemesterCalibration = false
            },
        )
    }
}

private val MainTab.label: String
    get() = when (this) {
        MainTab.TODAY -> "今天"
        MainTab.TIMETABLE -> "课表"
        MainTab.TASKS -> "任务"
        MainTab.QUADRANT -> "四象限"
        MainTab.FOCUS -> "番茄专注"
        MainTab.HABITS -> "习惯"
        MainTab.COUNTDOWN -> "倒数纪念日"
        MainTab.FEATURES -> "功能模块"
        MainTab.SETTINGS -> "设置"
    }

private val FeatureModule.moduleTitle: String
    get() = when (this) {
        FeatureModule.QUADRANT -> "四象限"
        FeatureModule.FOCUS -> "番茄专注"
        FeatureModule.HABITS -> "习惯打卡"
        FeatureModule.COUNTDOWN -> "倒数纪念日"
        FeatureModule.SEARCH -> "搜索"
    }

@Composable
private fun didaNavigationItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor = Color.Transparent,
)

@Composable
private fun DidaTaskTabIcon(selected: Boolean) {
    Icon(
        Icons.Filled.CheckBox,
        contentDescription = "任务",
        modifier = Modifier.size(25.dp),
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DidaCalendarTabIcon(day: Int, selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier.size(23.dp),
        shape = RoundedCornerShape(5.dp),
        color = color,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                day.toString(),
                color = Color.White,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DidaSettingsTabIcon(selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
        Icon(
            Icons.Filled.Hexagon,
            contentDescription = "设置",
            modifier = Modifier.fillMaxSize(),
            tint = color,
        )
        Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.background, CircleShape))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KexuTopBar(
    title: String,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    showSearch: Boolean,
) {
    TopAppBar(
        title = {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenu) {
                Icon(Icons.Outlined.Menu, contentDescription = "打开侧边栏")
            }
        },
        actions = {
            if (showSearch) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Outlined.Search, contentDescription = "搜索")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun KexuNavigationDrawer(
    state: KexuUiState,
    selectedTab: MainTab,
    selectedTaskFilter: TaskFilter,
    onToday: () -> Unit,
    onTasks: (TaskFilter) -> Unit,
    onTimetable: () -> Unit,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val today = LocalDate.now()
    val todayCount = state.tasks.count { !it.isCompleted && it.dueAt?.toLocalDate() == today }
    val inboxCount = state.tasks.count { !it.isCompleted && it.dueAt == null }
    val nextSevenDays = today.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val nextSevenCount = state.tasks.count { !it.isCompleted && it.dueAt != null && it.dueAt in now..nextSevenDays }
    val allCount = state.tasks.count { !it.isCompleted }
    val completedCount = state.tasks.count { it.isCompleted }

    ModalDrawerSheet(
        modifier = Modifier.width(355.dp),
        drawerShape = RoundedCornerShape(0.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.app_icon_turtle),
                    contentDescription = "不着急",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Text("不着急", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSearch, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Outlined.Search, contentDescription = "搜索")
                }
                IconButton(onClick = {}, enabled = false, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = "通知")
                }
                IconButton(onClick = onSettings, modifier = Modifier.size(38.dp)) {
                    DidaSettingsTabIcon(selected = false)
                }
            }

            NavigationDrawerItem(
                selected = selectedTab == MainTab.TODAY,
                onClick = onToday,
                icon = { Icon(Icons.Outlined.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                label = { Text("今天") },
                badge = { if (todayCount > 0) DrawerCount(todayCount) },
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(20.dp),
                colors = didaDrawerItemColors(),
            )
            NavigationDrawerItem(
                selected = selectedTab == MainTab.TASKS && selectedTaskFilter == TaskFilter.NEXT_7_DAYS,
                onClick = { onTasks(TaskFilter.NEXT_7_DAYS) },
                icon = { Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null, tint = Color(0xFF8A62A8)) },
                label = { Text("最近 7 天") },
                badge = { if (nextSevenCount > 0) DrawerCount(nextSevenCount) },
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(20.dp),
                colors = didaDrawerItemColors(),
            )
            NavigationDrawerItem(
                selected = selectedTab == MainTab.TASKS && selectedTaskFilter == TaskFilter.ALL,
                onClick = { onTasks(TaskFilter.ALL) },
                icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF5D7C66)) },
                label = { Text("所有") },
                badge = { if (allCount > 0) DrawerCount(allCount) },
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(20.dp),
                colors = didaDrawerItemColors(),
            )
            NavigationDrawerItem(
                selected = selectedTab == MainTab.TASKS && selectedTaskFilter == TaskFilter.INBOX,
                onClick = { onTasks(TaskFilter.INBOX) },
                icon = { Icon(Icons.Outlined.Inbox, contentDescription = null, tint = Color(0xFF5A89D8)) },
                label = { Text("收集箱") },
                badge = { if (inboxCount > 0) DrawerCount(inboxCount) },
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(20.dp),
                colors = didaDrawerItemColors(),
            )
            NavigationDrawerItem(
                selected = selectedTab == MainTab.TASKS && selectedTaskFilter == TaskFilter.COMPLETED,
                onClick = { onTasks(TaskFilter.COMPLETED) },
                icon = { Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                label = { Text("已完成") },
                badge = { if (completedCount > 0) DrawerCount(completedCount) },
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(20.dp),
                colors = didaDrawerItemColors(),
            )

            NavigationDrawerItem(
                selected = selectedTab == MainTab.TIMETABLE,
                onClick = onTimetable,
                icon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color(0xFFF09B37)) },
                label = { Text("我的课表") },
                badge = { if (state.courses.isNotEmpty()) DrawerCount(state.courses.size) },
                modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(20.dp),
                colors = didaDrawerItemColors(),
            )

            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("添加", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettings) {
                    Icon(Icons.Outlined.Tune, contentDescription = "侧栏设置")
                }
            }
        }
    }
}

@Composable
private fun didaDrawerItemColors() = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun DrawerCount(count: Int) {
    Text(
        count.toString(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun TaskSearchDialog(
    tasks: List<TaskEntity>,
    onDismiss: () -> Unit,
    onTaskClick: (TaskEntity) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query, tasks) {
        if (query.isBlank()) emptyList() else tasks.filter { it.title.contains(query.trim(), ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("搜索任务") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    if (query.isNotBlank() && results.isEmpty()) {
                        item {
                            Text(
                                "没有找到匹配任务",
                                modifier = Modifier.padding(vertical = 20.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(results, key = { it.id }) { task ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onTaskClick(task) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                task.dueAt?.let {
                                    Text(dateTimeText(it), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemesterCalibrationDialog(
    maxCourseWeek: Int,
    existingSemesterStartMillis: Long?,
    requireExplicitSelection: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val today = LocalDate.now()
    val existingStart = existingSemesterStartMillis
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        ?.let(ScheduleCalculator::startOfWeek)
    val maxSelectableWeek = maxOf(30, maxCourseWeek).coerceAtMost(63)
    val existingWeek = existingSemesterStartMillis
        ?.let { ScheduleCalculator.currentWeek(it, today) }
        ?.takeIf { it in 1..maxSelectableWeek }
        ?: 1
    var semesterStartEpochDay by rememberSaveable(maxCourseWeek, existingSemesterStartMillis) {
        mutableStateOf(
            (existingStart ?: ScheduleCalculator.semesterStartForCurrentWeek(today, existingWeek)).toEpochDay(),
        )
    }
    var showFirstWeekPicker by rememberSaveable { mutableStateOf(false) }
    var dateConfirmed by rememberSaveable(
        maxCourseWeek,
        existingSemesterStartMillis,
        requireExplicitSelection,
    ) {
        mutableStateOf(!requireExplicitSelection)
    }
    val semesterStart = LocalDate.ofEpochDay(semesterStartEpochDay)
    val semesterStartMillis = semesterStart
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val currentWeek = ScheduleCalculator.currentWeek(semesterStartMillis, today)
    val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)

    fun selectCurrentWeek(week: Int) {
        semesterStartEpochDay = ScheduleCalculator
            .semesterStartForCurrentWeek(today, week.coerceIn(1, maxSelectableWeek))
            .toEpochDay()
        dateConfirmed = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择课程第 1 周日期") },
        text = {
            Column {
                Text(
                    "导入数据通常只有周号，没有具体日期。请确认第 1 周的起始日，应用会据此计算每一节课的真实日期。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showFirstWeekPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.DateRange, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("选择第 1 周起始日期")
                }
                Spacer(Modifier.height(14.dp))
                Text("或者按当前教学周自动推算", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Text("今天是学校的", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { selectCurrentWeek((currentWeek - 1).coerceAtLeast(1)) },
                        enabled = currentWeek > 1,
                        modifier = Modifier.size(44.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("−", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        if (currentWeek < 1) "尚未开学" else "第 ${currentWeek.coerceAtMost(maxSelectableWeek)} 周",
                        modifier = Modifier.width(116.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedButton(
                        onClick = { selectCurrentWeek((currentWeek + 1).coerceAtLeast(1)) },
                        enabled = currentWeek < maxSelectableWeek,
                        modifier = Modifier.size(44.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text("+", style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("第 1 周起始日（周日）", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Text(semesterStart.format(formatter), fontWeight = FontWeight.Medium)
                        Text(
                            "第 1 周：${weekRangeText(semesterStart, semesterStart.plusDays(6))}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (maxCourseWeek > 1) {
                            val lastWeekStart = semesterStart.plusWeeks((maxCourseWeek - 1).toLong())
                            Text(
                                "第 $maxCourseWeek 周：${weekRangeText(lastWeekStart, lastWeekStart.plusDays(6))}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (dateConfirmed) {
                        "日期已确认。课程提醒和“回到本周”都会使用这个日期。"
                    } else {
                        "请点击上方日期，或调整“今天是第几周”后再导入。"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm(semesterStartMillis) },
                enabled = dateConfirmed && currentWeek in 1..maxSelectableWeek,
            ) {
                Text("保存并导入")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )

    if (showFirstWeekPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = semesterStart
                .atStartOfDay(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showFirstWeekPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selectedUtc ->
                            val selected = Instant.ofEpochMilli(selectedUtc).atZone(ZoneId.of("UTC")).toLocalDate()
                            semesterStartEpochDay = ScheduleCalculator.startOfWeek(selected).toEpochDay()
                            dateConfirmed = true
                        }
                        showFirstWeekPicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = { TextButton(onClick = { showFirstWeekPicker = false }) { Text("取消") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun maxImportedWeek(courses: List<CourseDraft>): Int =
    courses.maxOfOrNull { course ->
        if (course.weekMask == 0L) course.endWeek else 64 - java.lang.Long.numberOfLeadingZeros(course.weekMask)
    } ?: 20

private fun maxStoredWeek(courses: List<CourseEntity>): Int =
    courses.maxOfOrNull { course ->
        if (course.weekMask == 0L) course.endWeek else 64 - java.lang.Long.numberOfLeadingZeros(course.weekMask)
    } ?: 20

private fun effectiveMeetingsForWeek(
    courses: List<CourseEntity>,
    slots: List<ScheduleSlotEntity>,
    overrides: List<CourseOverrideEntity>,
    week: Int,
): List<DisplayedCourseMeeting> {
    if (week !in 1..63) return emptyList()
    val courseMap = courses.associateBy { it.id }
    val overrideMap = overrides
        .filter { it.weekNumber == week }
        .associateBy { it.slotId }
    return slots.mapNotNull { originalSlot ->
        val course = courseMap[originalSlot.courseId] ?: return@mapNotNull null
        if (!course.isActiveInWeek(week)) return@mapNotNull null
        val override = overrideMap[originalSlot.id]
        if (override?.isCancelled == true) {
            return@mapNotNull DisplayedCourseMeeting(
                course = course,
                slot = originalSlot,
                override = override,
                isCancelled = true,
            )
        }
        val effectiveCourse = override?.location
            ?.let { course.copy(location = it) }
            ?: course
        val effectiveSlot = originalSlot.copy(
            dayOfWeek = override?.dayOfWeek ?: originalSlot.dayOfWeek,
            startMinutes = override?.startMinutes ?: originalSlot.startMinutes,
            endMinutes = override?.endMinutes ?: originalSlot.endMinutes,
        )
        DisplayedCourseMeeting(effectiveCourse, effectiveSlot, override)
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayScreen(
    state: KexuUiState,
    onSaveReflection: (Long, String, Boolean) -> Unit,
    onCompanionMotion: (Boolean) -> Unit,
    onToggle: (TaskEntity) -> Unit,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onEdit: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }
    var completedExpanded by rememberSaveable { mutableStateOf(true) }
    var showClosing by rememberSaveable { mutableStateOf(false) }
    var showFootprints by rememberSaveable { mutableStateOf(false) }
    val rhythmDate = rememberRhythmDate()
    if (showClosing) DayClosingSheet(state, rhythmDate, { showClosing = false }, onSaveReflection, onEdit)
    if (showFootprints) FootprintsSheet(state, rhythmDate, { showFootprints = false }, onCompanionMotion)
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val courseMap = state.courses.associateBy { it.id }
    val courseColors = remember(state.courses) {
        allocateTimetableColors(state.courses.map(CourseEntity::name))
    }
    val currentWeek = state.settings?.let { ScheduleCalculator.currentWeek(it.semesterStartMillis, today) } ?: 1
    val todayMeetings = effectiveMeetingsForWeek(
        courses = state.courses,
        slots = state.slots,
        overrides = state.overrides,
        week = currentWeek,
    ).filter { !it.isCancelled && it.slot.dayOfWeek == today.dayOfWeek.value }
        .sortedBy { it.slot.startMinutes }
    val tasks = state.tasks
        .filter { !state.completedForPlacement(it) && it.dueAt?.toLocalDate()?.let { date -> !date.isAfter(today) } == true }
        .sortedBy { it.dueAt }
    val overdueTasks = tasks.filter { it.dueAt?.toLocalDate()?.isBefore(today) == true }
    val dueTodayTasks = tasks.filter { it.dueAt?.toLocalDate() == today }
    val habitTasks = dueTodayTasks.filter { !it.recurrenceRule.isNullOrBlank() }
    val regularTodayTasks = dueTodayTasks.filter { it.recurrenceRule.isNullOrBlank() }
    val completedTasks = state.tasks
        .filter { task ->
            state.completedForPlacement(task) &&
                (task.completedAt?.toLocalDate() == today || task.dueAt?.toLocalDate() == today)
        }
        .sortedByDescending { it.completedAt ?: it.dueAt ?: 0L }
    val nextMeeting = todayMeetings.firstOrNull { it.slot.endMinutes >= now.hour * 60 + now.minute }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().height(80.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenu, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Menu, contentDescription = "打开侧边栏")
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("今天", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(today.format(DateTimeFormatter.ofPattern("M月d日 EEEE", java.util.Locale.CHINA)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Search, contentDescription = "搜索任务")
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("一周足迹") },
                            leadingIcon = { Icon(Icons.Outlined.Lightbulb, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                showFootprints = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("今日回顾") },
                            leadingIcon = { Icon(Icons.Outlined.NoteAlt, contentDescription = null) },
                            onClick = {
                                showMoreMenu = false
                                showClosing = true
                            },
                        )
                    }
                }
            }
        }
        if (overdueTasks.isNotEmpty()) {
            item {
                Text(
                    "已过期",
                    modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 2.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            items(overdueTasks, key = { "overdue-${it.id}" }) { task ->
              Box(Modifier.animateItem(fadeOutSpec = tween(100), placementSpec = tween(220, delayMillis = 100))) {
                DidaTaskCard(
                    task = task,
                    courseName = courseMap[task.courseId]?.name,
                    onToggle = onToggle,
                    onEdit = onEdit,
                )
              }
            }
        }
        items(regularTodayTasks, key = { "task-${it.id}" }) { task ->
          Box(Modifier.animateItem(fadeOutSpec = tween(100), placementSpec = tween(220, delayMillis = 100))) {
            DidaTaskCard(
                task = task,
                courseName = courseMap[task.courseId]?.name,
                onToggle = onToggle,
                onEdit = onEdit,
            )
          }
        }
        if (habitTasks.isNotEmpty()) {
            item {
                DidaHabitGroup(
                    tasks = habitTasks,
                    onToggle = onToggle,
                    onEdit = onEdit,
                )
            }
        }
        if (nextMeeting != null) {
            item {
                NextCourseCard(nextMeeting.course, nextMeeting.slot, now.hour * 60 + now.minute)
            }
        }
        items(todayMeetings, key = { "slot-${it.slot.id}" }) { meeting ->
            if (meeting != nextMeeting) {
                CourseRow(
                    course = meeting.course,
                    slot = meeting.slot,
                    color = courseColors[meeting.course.name.normalizedCourseName()]
                        ?: timetablePalette.first(),
                )
            }
        }
        if (tasks.isEmpty() && todayMeetings.isEmpty()) {
            item {
                GentleEntrance {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 88.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.empty_today_plane),
                            contentDescription = null,
                            modifier = Modifier.size(205.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Spacer(Modifier.height(18.dp))
                        Text("今天没有任务", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "享受美好的一天吧",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
        if (completedTasks.isNotEmpty()) {
            item {
                DidaCompletedGroup(
                    tasks = completedTasks.take(5),
                    expanded = completedExpanded,
                    onExpandedChange = { completedExpanded = !completedExpanded },
                    onToggle = onToggle,
                )
            }
            if (completedTasks.size > 5) {
                item {
                    Text(
                        "还有更多",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DidaTaskCard(
    task: TaskEntity,
    courseName: String?,
    onToggle: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().softClick(enabled = task.id !in LocalTaskTransitions.current) { onEdit(task) },
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
    ) {
        DidaTaskContent(task, courseName, onToggle, completedStyle = task.isCompleted)
    }
}

@Composable
private fun DidaCompletedGroup(
    tasks: List<TaskEntity>,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onToggle: (TaskEntity) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.animateContentSize(tween(220))) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button,
                        onClickLabel = if (expanded) "收起已完成任务" else "展开已完成任务",
                        onClick = onExpandedChange)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("已完成", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Text(
                    "${tasks.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.width(8.dp))
                DisclosureChevron(expanded, Modifier.size(20.dp))
            }
            if (expanded) {
                tasks.forEach { task ->
                    androidx.compose.runtime.key(task.id) {
                        DidaTaskContent(task, null, onToggle, completedStyle = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun DidaHabitGroup(
    tasks: List<TaskEntity>,
    onToggle: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.animateContentSize(tween(220))) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(role = androidx.compose.ui.semantics.Role.Button,
                        onClickLabel = if (expanded) "收起习惯任务" else "展开习惯任务") { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("习惯", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Text(
                    "${tasks.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.width(8.dp))
                DisclosureChevron(expanded, Modifier.size(20.dp))
            }
            if (expanded) {
                tasks.forEach { task ->
                  androidx.compose.runtime.key(task.id) {
                    DidaTaskContent(task, null, onToggle, completedStyle = task.isCompleted,
                        modifier = Modifier.softClick(enabled = task.id !in LocalTaskTransitions.current) { onEdit(task) })
                  }
                }
            }
        }
    }
}

@Composable
private fun DidaTaskContent(
    task: TaskEntity,
    courseName: String?,
    onToggle: (TaskEntity) -> Unit,
    completedStyle: Boolean,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val transitions = LocalTaskTransitions.current
    val rowTint by androidx.compose.animation.animateColorAsState(
        if (task.id == LocalHighlightedTaskId.current) MaterialTheme.colorScheme.primary.copy(alpha = .08f)
        else Color.Transparent,
        tween(240), label = "newTaskHighlight")
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(rowTint)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompletionToggle(
            checked = task.isCompleted,
            label = if (task.isCompleted) "恢复任务：${task.title}" else "完成任务：${task.title}",
            onToggle = { if (task.id !in transitions) onToggle(task) },
            visualSize = 22.dp, circular = false,
            outline = if (task.priority > 0) priorityColor(task.priority)
                else MaterialTheme.colorScheme.outline.copy(alpha = .72f),
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            CompletionTitle(
                task.title, task.isCompleted,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
                maxLines = 2,
            )
            val metadata = listOfNotNull(task.dueAt?.let(::dateTimeText),
                courseName?.takeIf { it.isNotBlank() }).joinToString(" · ")
            if (metadata.isNotBlank() || !task.recurrenceRule.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!task.recurrenceRule.isNullOrBlank()) {
                        Icon(Icons.Outlined.Sync, contentDescription = "重复任务",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(metadata,
                        color = when {
                            completedStyle -> MaterialTheme.colorScheme.onSurfaceVariant
                            task.dueAt != null && task.dueAt < System.currentTimeMillis() -> MaterialTheme.colorScheme.error
                            task.dueAt?.toLocalDate() == LocalDate.now() -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}

@Composable
private fun NextCourseCard(course: CourseEntity, slot: ScheduleSlotEntity, nowMinutes: Int) {
    val started = nowMinutes >= slot.startMinutes
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .72f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(course.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val details = listOf(
                    "${minutesText(slot.startMinutes)}–${minutesText(slot.endMinutes)}",
                    course.location.takeIf { it.isNotBlank() },
                ).filterNotNull().joinToString(" · ")
                Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                if (started) "进行中" else "下一节",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CalmCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Mint),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = color)
        Spacer(Modifier.width(8.dp))
        Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyInline(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CourseRow(
    course: CourseEntity,
    slot: ScheduleSlotEntity,
    color: Color,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(54.dp)) {
            Text(minutesText(slot.startMinutes), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
            Text(minutesText(slot.endMinutes), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Box(Modifier.width(3.dp).height(42.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(course.name, fontWeight = FontWeight.Normal)
            val details = listOf(course.location, course.teacher).filter { it.isNotBlank() }.joinToString(" · ")
            if (details.isNotBlank()) Text(details, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除课程", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CalendarHubScreen(
    state: KexuUiState,
    onImport: () -> Unit,
    onCalibrateSemester: () -> Unit,
    onAddTask: (LocalDate) -> Unit,
    onAddCourse: () -> Unit,
    onDeleteCourse: (CourseEntity) -> Unit,
    onEditCourse: (CourseEntity, ScheduleSlotEntity) -> Unit,
    onSaveOverride: (CourseOverrideEntity) -> Unit,
    onDeleteOverride: (CourseOverrideEntity) -> Unit,
    onAddTaskForCourse: (Long) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMonth by rememberSaveable { mutableStateOf(true) }
    var selectedDateEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay)

    Box(modifier.fillMaxSize()) {
        if (showMonth) {
            DidaMonthCalendarScreen(
                state = state,
                selectedDate = selectedDate,
                onSelectedDateChange = { selectedDateEpochDay = it.toEpochDay() },
                onOpenTimetable = { showMonth = false },
                onImport = onImport,
                onCalibrateSemester = onCalibrateSemester,
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            TimetableScreen(
                state = state,
                onImport = onImport,
                onCalibrateSemester = onCalibrateSemester,
                onOpenMonthCalendar = { showMonth = true },
                onDeleteCourse = onDeleteCourse,
                onEditCourse = onEditCourse,
                onSaveOverride = onSaveOverride,
                onDeleteOverride = onDeleteOverride,
                onAddTaskForCourse = onAddTaskForCourse,
                onToggleTask = onToggleTask,
                onEditTask = onEditTask,
                modifier = Modifier.fillMaxSize(),
            )
        }
        FloatingActionButton(
            onClick = {
                if (showMonth) onAddTask(selectedDate) else onAddCourse()
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = if (showMonth) "新建当天任务" else "添加课程",
            )
        }
    }
}

@Composable
private fun DidaMonthCalendarScreen(
    state: KexuUiState,
    selectedDate: LocalDate,
    onSelectedDateChange: (LocalDate) -> Unit,
    onOpenTimetable: () -> Unit,
    onImport: () -> Unit,
    onCalibrateSemester: () -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val baseMonth = remember { YearMonth.from(today) }
    val initialOffset = remember {
        (selectedDate.year - baseMonth.year) * 12 + selectedDate.monthValue - baseMonth.monthValue
    }
    val monthPagerState = rememberPagerState(
        initialPage = (1200 + initialOffset).coerceIn(0, 2400),
        pageCount = { 2401 },
    )
    val scope = rememberCoroutineScope()
    val shownMonth = baseMonth.plusMonths((monthPagerState.currentPage - 1200).toLong())
    var showViewMenu by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }
    val selectedTasks = state.tasks
        .filter { it.dueAt?.toLocalDate() == selectedDate }
        .sortedWith(compareBy<TaskEntity> { state.completedForPlacement(it) }.thenBy { it.dueAt ?: Long.MAX_VALUE })
    val selectedWeek = state.settings?.let {
        ScheduleCalculator.currentWeek(it.semesterStartMillis, selectedDate)
    } ?: 0
    val selectedMeetings = effectiveMeetingsForWeek(
        courses = state.courses,
        slots = state.slots,
        overrides = state.overrides,
        week = selectedWeek,
    ).filter { it.slot.dayOfWeek == selectedDate.dayOfWeek.value }
        .sortedBy { it.slot.startMinutes }
    val courseMap = state.courses.associateBy { it.id }
    val courseColors = remember(state.courses) {
        allocateTimetableColors(state.courses.map(CourseEntity::name))
    }

    LaunchedEffect(monthPagerState.currentPage) {
        val month = baseMonth.plusMonths((monthPagerState.currentPage - 1200).toLong())
        if (YearMonth.from(selectedDate) != month) {
            onSelectedDateChange(month.atDay(minOf(selectedDate.dayOfMonth, month.lengthOfMonth())))
        }
    }

    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().height(66.dp).padding(start = 18.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    chineseMonthTitle(shownMonth.monthValue),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            scope.launch { monthPagerState.animateScrollToPage(1200) }
                            onSelectedDateChange(today)
                        }
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showViewMenu = true }) {
                        Icon(Icons.Outlined.Tune, contentDescription = "日历视图设置")
                    }
                    DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("月视图") },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                            onClick = { showViewMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text("回到今天") },
                            onClick = {
                                showViewMenu = false
                                scope.launch { monthPagerState.animateScrollToPage(1200) }
                                onSelectedDateChange(today)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("周课表") },
                            onClick = {
                                showViewMenu = false
                                onOpenTimetable()
                            },
                        )
                    }
                }
                IconButton(onClick = onOpenTimetable) {
                    Icon(Icons.Outlined.ViewWeek, contentDescription = "打开周课表")
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "日历更多操作")
                    }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("导入课表") },
                            onClick = {
                                showMoreMenu = false
                                onImport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("校准学期第一周") },
                            onClick = {
                                showMoreMenu = false
                                onCalibrateSemester()
                            },
                        )
                    }
                }
            }
        }
        item {
            HorizontalPager(
                state = monthPagerState,
                modifier = Modifier.fillMaxWidth().height(324.dp),
                key = { it },
            ) { page ->
                val pageMonth = baseMonth.plusMonths((page - 1200).toLong())
                MonthCalendarGrid(
                    month = pageMonth,
                    selectedDate = selectedDate,
                    today = today,
                    state = state,
                    onDateClick = { date ->
                        onSelectedDateChange(date)
                        if (date.month != pageMonth.month) {
                            scope.launch {
                                monthPagerState.animateScrollToPage(
                                    (page + if (date.isBefore(pageMonth.atDay(1))) -1 else 1).coerceIn(0, 2400),
                                )
                            }
                        }
                    },
                )
            }
        }
        if (selectedMeetings.isEmpty() && selectedTasks.isEmpty()) {
            item {
                GentleEntrance {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 52.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.empty_calendar),
                            contentDescription = null,
                            modifier = Modifier.size(150.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Text("你这一天没有任务", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Text("放松一下吧", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(selectedMeetings, key = { "meeting-${it.slot.id}-${it.override?.id ?: 0}" }) { meeting ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    CourseRow(
                        course = meeting.course,
                        slot = meeting.slot,
                        color = courseColors[meeting.course.name.normalizedCourseName()]
                            ?: timetablePalette.first(),
                    )
                }
            }
            items(selectedTasks, key = { "task-${it.id}" }) { task ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    DidaTaskCard(
                        task = task,
                        courseName = task.courseId?.let { courseMap[it]?.name },
                        onToggle = onToggleTask,
                        onEdit = onEditTask,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    state: KexuUiState,
    onDateClick: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value % 7
    val gridStart = first.minusDays(leading.toLong())
    val dates = (0L until 42L).map(gridStart::plusDays)
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        repeat(6) { weekRow ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { dayColumn ->
                    val date = dates[weekRow * 7 + dayColumn]
                    MonthDayCell(
                        date = date,
                        isCurrentMonth = date.month == month.month,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        hasTask = state.tasks.any { !it.isCompleted && it.dueAt?.toLocalDate() == date },
                        hasCourse = state.settings?.let { settings ->
                            val week = ScheduleCalculator.currentWeek(settings.semesterStartMillis, date)
                            effectiveMeetingsForWeek(state.courses, state.slots, state.overrides, week)
                                .any { !it.isCancelled && it.slot.dayOfWeek == date.dayOfWeek.value }
                        } == true,
                        onClick = { onDateClick(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    hasTask: Boolean,
    hasCourse: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(31.dp).clip(CircleShape).background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                    else -> Color.Transparent
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    isSelected -> Color.White
                    isToday -> MaterialTheme.colorScheme.primary
                    isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .35f)
                },
                fontSize = 14.sp,
                fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (hasTask) Box(Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            if (hasCourse) Box(Modifier.size(3.dp).clip(CircleShape).background(Color(0xFF2EBB9A)))
        }
    }
}

private fun chineseMonthTitle(month: Int): String =
    listOf("一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月")[month - 1]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableScreen(
    state: KexuUiState,
    onImport: () -> Unit,
    onCalibrateSemester: () -> Unit,
    onOpenMonthCalendar: () -> Unit,
    onDeleteCourse: (CourseEntity) -> Unit,
    onEditCourse: (CourseEntity, ScheduleSlotEntity) -> Unit,
    onSaveOverride: (CourseOverrideEntity) -> Unit,
    onDeleteOverride: (CourseOverrideEntity) -> Unit,
    onAddTaskForCourse: (Long) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val semesterStart = state.settings?.semesterStartMillis
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        ?: ScheduleCalculator.startOfWeek(today)
    val calculatedWeek = state.settings?.let { ScheduleCalculator.currentWeek(it.semesterStartMillis, today) } ?: 1
    val lastCourseWeek = state.courses.maxOfOrNull { course ->
        if (course.weekMask == 0L) {
            course.endWeek
        } else {
            64 - java.lang.Long.numberOfLeadingZeros(course.weekMask)
        }
    } ?: 20
    val lastOverrideWeek = state.overrides.maxOfOrNull { it.weekNumber } ?: 1
    val maxWeek = maxOf(20, lastCourseWeek, lastOverrideWeek, calculatedWeek.coerceAtLeast(1)).coerceAtMost(63)
    val currentWeek = calculatedWeek.takeIf { it in 1..maxWeek }
    val initialWeek = currentWeek ?: if (calculatedWeek < 1) 1 else maxWeek
    val pagerState = rememberPagerState(initialPage = initialWeek - 1) { maxWeek }
    val scope = rememberCoroutineScope()
    val selectedWeek = pagerState.currentPage + 1
    val selectedWeekStart = semesterStart.plusWeeks((selectedWeek - 1).toLong())
    val selectedWeekEnd = selectedWeekStart.plusDays(6)
    val timetableColors = remember(state.courses) {
        allocateTimetableColors(state.courses.map(CourseEntity::name))
    }
    var showWeekMenu by rememberSaveable { mutableStateOf(false) }
    var showActionsMenu by rememberSaveable { mutableStateOf(false) }
    var selectedMeeting by remember { mutableStateOf<DisplayedCourseMeeting?>(null) }
    var overrideEditorTarget by remember { mutableStateOf<DisplayedCourseMeeting?>(null) }

    LaunchedEffect(state.settings?.semesterStartMillis, calculatedWeek, maxWeek) {
        val targetPage = when {
            currentWeek != null -> currentWeek - 1
            calculatedWeek < 1 -> 0
            else -> maxWeek - 1
        }
        if (state.settings != null && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Column(
                    Modifier
                        .clickable { showWeekMenu = true }
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        "第 $selectedWeek 周",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            weekRangeText(selectedWeekStart, selectedWeekEnd),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        when {
                            selectedWeek == currentWeek -> {
                                Text(
                                    "  ·  本周",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            selectedWeek == 1 && calculatedWeek < 1 -> {
                                Text(
                                    "  ·  尚未开学",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            selectedWeek == maxWeek && calculatedWeek > maxWeek -> {
                                Text(
                                    "  ·  学期已结束",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                DropdownMenu(expanded = showWeekMenu, onDismissRequest = { showWeekMenu = false }) {
                    (1..maxWeek).forEach { week ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "第 $week 周${if (week == currentWeek) " · 本周" else ""}",
                                    fontWeight = if (week == selectedWeek) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                showWeekMenu = false
                                scope.launch { pagerState.animateScrollToPage(week - 1) }
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (currentWeek != null && selectedWeek != currentWeek) {
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(currentWeek - 1) } },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) {
                    Text("本周")
                }
            }
            IconButton(onClick = onOpenMonthCalendar) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "打开月历")
            }
            IconButton(onClick = { showWeekMenu = true }) {
                Icon(Icons.Outlined.ViewWeek, contentDescription = "选择周次")
            }
            Box {
                IconButton(onClick = { showActionsMenu = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "课表更多操作")
                }
                DropdownMenu(expanded = showActionsMenu, onDismissRequest = { showActionsMenu = false }) {
                    if (currentWeek != null && selectedWeek != currentWeek) {
                        DropdownMenuItem(
                            text = { Text("回到本周") },
                            onClick = {
                                showActionsMenu = false
                                scope.launch { pagerState.animateScrollToPage(currentWeek - 1) }
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("校准周次") },
                        onClick = {
                            showActionsMenu = false
                            onCalibrateSemester()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("导入课表") },
                        onClick = {
                            showActionsMenu = false
                            onImport()
                        },
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            key = { it },
        ) { page ->
            val pageWeek = page + 1
            val pageStart = semesterStart.plusWeeks(page.toLong())
            val pageMeetings = effectiveMeetingsForWeek(
                courses = state.courses,
                slots = state.slots,
                overrides = state.overrides,
                week = pageWeek,
            ).sortedWith(
                compareBy<DisplayedCourseMeeting> { it.slot.startMinutes }
                    .thenBy { it.slot.dayOfWeek % 7 },
            )
            WeeklyTimetableGrid(
                weekStart = pageStart,
                selectedWeek = pageWeek,
                today = today,
                meetings = pageMeetings,
                courseColors = timetableColors,
                onCourseClick = { selectedMeeting = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    selectedMeeting?.let { meeting ->
        val course = meeting.course
        val slot = meeting.slot
        val originalCourse = state.courses.firstOrNull { it.id == course.id } ?: course
        val originalSlot = state.slots.firstOrNull { it.id == slot.id } ?: slot
        val relatedTasks = state.tasks
            .filter { it.courseId == course.id && !state.completedForPlacement(it) }
            .sortedBy { it.dueAt ?: Long.MAX_VALUE }
        ModalBottomSheet(onDismissRequest = { selectedMeeting = null }) {
            LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 28.dp),
            ) {
                item {
                    Text(course.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (meeting.isCancelled) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                        ) {
                            Text(
                                "第 $selectedWeek 周已停课",
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    } else if (meeting.override != null) {
                        Text(
                            "第 $selectedWeek 周临时安排",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text("周${DayOfWeek.of(slot.dayOfWeek).shortChinese} · ${minutesText(slot.startMinutes)}–${minutesText(slot.endMinutes)}")
                    if (course.location.isNotBlank()) Text("地点：${course.location}")
                    if (course.teacher.isNotBlank()) Text("教师：${course.teacher}")
                    Text(
                        "常规周次：${originalCourse.activeWeeksText()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    meeting.override?.note?.takeIf { it.isNotBlank() }?.let {
                        Text("备注：$it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (relatedTasks.isNotEmpty()) {
                    item {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        Text("相关任务", fontWeight = FontWeight.SemiBold)
                    }
                    items(relatedTasks.take(4), key = { it.id }) { task ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedMeeting = null
                                    onEditTask(task)
                                }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CompletionToggle(task.isCompleted, "完成任务：${task.title}", { onToggleTask(task) })
                            Spacer(Modifier.width(6.dp))
                            Column(Modifier.weight(1f)) {
                                Text(task.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                task.dueAt?.let {
                                    Text(
                                        dateTimeText(it),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    CourseActionRow("编辑整学期课程") {
                        selectedMeeting = null
                        onEditCourse(originalCourse, originalSlot)
                    }
                    if (!meeting.isCancelled) {
                        CourseActionRow("仅调整第 $selectedWeek 周") {
                            overrideEditorTarget = meeting.copy(course = originalCourse, slot = originalSlot)
                            selectedMeeting = null
                        }
                    }
                    if (meeting.override == null) {
                        CourseActionRow("第 $selectedWeek 周停课") {
                            onSaveOverride(
                                CourseOverrideEntity(
                                    slotId = originalSlot.id,
                                    weekNumber = selectedWeek,
                                    isCancelled = true,
                                ),
                            )
                            selectedMeeting = null
                        }
                    } else {
                        CourseActionRow("恢复第 $selectedWeek 周原安排") {
                            onDeleteOverride(meeting.override)
                            selectedMeeting = null
                        }
                    }
                    CourseActionRow("添加相关任务") {
                        selectedMeeting = null
                        onAddTaskForCourse(course.id)
                    }
                    CourseActionRow("删除整门课程", isDestructive = true) {
                        onDeleteCourse(originalCourse)
                        selectedMeeting = null
                    }
                }
            }
        }
    }

    overrideEditorTarget?.let { meeting ->
        CourseOverrideSheet(
            course = meeting.course,
            originalSlot = meeting.slot,
            weekNumber = selectedWeek,
            existing = meeting.override,
            onDismiss = { overrideEditorTarget = null },
            onSave = {
                onSaveOverride(it)
                overrideEditorTarget = null
            },
        )
    }
}

@Composable
private fun CourseActionRow(
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            color = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified,
        )
    }
}

@Composable
private fun WeeklyTimetableGrid(
    weekStart: LocalDate,
    selectedWeek: Int,
    today: LocalDate,
    meetings: List<DisplayedCourseMeeting>,
    courseColors: Map<String, Color>,
    onCourseClick: (DisplayedCourseMeeting) -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayDays = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
    )
    val displayStart = weekStart

    BoxWithConstraints(modifier.fillMaxSize()) {
        val timeColumnWidth = 50.dp
        val availableWidth = maxWidth
        val dayWidth = (availableWidth - timeColumnWidth) / 7

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(68.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(timeColumnWidth).height(68.dp)) {
                    Text(
                        "${displayStart.monthValue}月",
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                displayDays.forEachIndexed { index, day ->
                    val date = displayStart.plusDays(index.toLong())
                    val isToday = date == today
                    Column(
                        Modifier.width(dayWidth).height(68.dp).padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            day.shortChinese,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        if (isToday) {
                            Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(Modifier.height(2.dp))
                        } else {
                            Spacer(Modifier.height(7.dp))
                        }
                        Text(
                            date.dayOfMonth.toString(),
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))

            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
                val gridWidth = maxWidth
                val gridAvailableHeight = maxHeight
                val periodHeight = gridAvailableHeight / didaTimetablePeriods.size

                Box(Modifier.fillMaxSize()) {
                    (0..didaTimetablePeriods.size).forEach { line ->
                        HorizontalDivider(
                            modifier = Modifier
                                .offset(y = periodHeight * line)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .48f),
                        )
                    }
                    (0..7).forEach { line ->
                        Box(
                            Modifier
                                .offset(x = timeColumnWidth + dayWidth * line)
                                .width(1.dp)
                                .height(gridAvailableHeight)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = .30f)),
                        )
                    }

                    didaTimetablePeriods.forEachIndexed { index, period ->
                        Column(
                            Modifier
                                .offset(y = periodHeight * index)
                                .width(timeColumnWidth)
                                .height(periodHeight)
                                .padding(top = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                period.number.toString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                lineHeight = 11.sp,
                            )
                            Text(
                                minutesText(period.startMinutes),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp,
                                lineHeight = 9.sp,
                            )
                            Text(
                                minutesText(period.endMinutes),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp,
                                lineHeight = 9.sp,
                            )
                        }
                    }

                    meetings.forEach { meeting ->
                        val slot = meeting.slot
                        val course = meeting.course
                        val startIndex = didaTimetablePeriods.indices.minByOrNull {
                            kotlin.math.abs(didaTimetablePeriods[it].startMinutes - slot.startMinutes)
                        } ?: return@forEach
                        val endIndex = didaTimetablePeriods.indices.minByOrNull {
                            kotlin.math.abs(didaTimetablePeriods[it].endMinutes - slot.endMinutes)
                        }?.coerceAtLeast(startIndex) ?: startIndex
                        val dayIndex = slot.dayOfWeek % 7
                        val y = periodHeight * startIndex
                        val height = periodHeight * (endIndex - startIndex + 1)
                        val color = if (meeting.isCancelled) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            courseColors[course.name.normalizedCourseName()]
                                ?: timetablePalette.first()
                        }
                        Card(
                            modifier = Modifier
                                .offset(
                                    x = timeColumnWidth + dayWidth * dayIndex + 2.dp,
                                    y = y + 2.dp,
                                )
                                .width(dayWidth - 4.dp)
                                .height(height - 4.dp)
                                .clickable { onCourseClick(meeting) },
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = color),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 3.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    if (meeting.isCancelled) "${course.name}\n已停课" else course.name,
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!meeting.isCancelled && course.location.isNotBlank()) {
                                    Text(
                                        course.location,
                                        fontSize = 7.sp,
                                        lineHeight = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (meetings.isEmpty()) {
                        Column(
                            Modifier
                                .offset(x = timeColumnWidth)
                                .width(gridWidth - timeColumnWidth)
                                .padding(top = 72.dp, start = 24.dp, end = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(34.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("第 $selectedWeek 周暂无课程", fontWeight = FontWeight.SemiBold)
                            Text(
                                "左右滑动查看其他周",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TimetablePeriod(
    val number: Int,
    val startMinutes: Int,
    val endMinutes: Int,
)

private val didaTimetablePeriods = listOf(
    TimetablePeriod(1, 8 * 60, 8 * 60 + 45),
    TimetablePeriod(2, 8 * 60 + 50, 9 * 60 + 35),
    TimetablePeriod(3, 9 * 60 + 55, 10 * 60 + 40),
    TimetablePeriod(4, 10 * 60 + 45, 11 * 60 + 30),
    TimetablePeriod(5, 11 * 60 + 35, 12 * 60 + 20),
    TimetablePeriod(6, 14 * 60, 14 * 60 + 45),
    TimetablePeriod(7, 14 * 60 + 50, 15 * 60 + 35),
    TimetablePeriod(8, 15 * 60 + 55, 16 * 60 + 40),
    TimetablePeriod(9, 16 * 60 + 45, 17 * 60 + 30),
    TimetablePeriod(10, 19 * 60, 19 * 60 + 45),
    TimetablePeriod(11, 19 * 60 + 50, 20 * 60 + 35),
    TimetablePeriod(12, 20 * 60 + 40, 21 * 60 + 25),
)

private fun CourseEntity.activeWeeksText(): String {
    val weeks = if (weekMask != 0L) {
        (1..63).filter { week -> weekMask and (1L shl (week - 1)) != 0L }
    } else {
        (startWeek..endWeek).filter(::isActiveInWeek)
    }
    if (weeks.isEmpty()) return "未设置"
    if (weekMask == 0L) {
        val pattern = runCatching { WeekPattern.valueOf(weekPattern) }.getOrDefault(WeekPattern.ALL)
        return "${startWeek}–${endWeek}周${if (pattern == WeekPattern.ALL) "" else " · ${pattern.chinese}"}"
    }
    return weeks.joinToString(separator = "、", postfix = "周")
}

internal val timetablePalette = listOf(
    Color(0xFFF7D77B),
    Color(0xFF80B9E7),
    Color(0xFFF0A4A1),
    Color(0xFFCDE0C1),
    Color(0xFFB9A7E8),
    Color(0xFFF2B7D0),
    Color(0xFF91CCDB),
    Color(0xFFC2CF7A),
    Color(0xFF91DDDC),
    Color(0xFFF5C28B),
    Color(0xFFA6C8FF),
    Color(0xFFD6B7F0),
    Color(0xFFA7D8B8),
    Color(0xFFF4B3A2),
    Color(0xFFC8B6A6),
    Color(0xFFB6D3F2),
    Color(0xFFE8C57E),
    Color(0xFFC6B4D9),
    Color(0xFFA7DAD0),
    Color(0xFFE7B9A9),
)

/**
 * Gives each distinct course name a different pastel while the palette has room.
 * The sorted names and hash-based starting point keep the result deterministic
 * across week changes, recomposition and application restarts.
 */
internal fun allocateTimetableColors(courseNames: List<String>): Map<String, Color> {
    val normalizedNames = courseNames
        .map(String::normalizedCourseName)
        .filter(String::isNotBlank)
        .distinct()
        .sorted()
    val occupied = BooleanArray(timetablePalette.size)
    return buildMap {
        normalizedNames.forEach { name ->
            val preferred = (name.hashCode() and Int.MAX_VALUE) % timetablePalette.size
            val paletteIndex = if (size < timetablePalette.size) {
                (0 until timetablePalette.size)
                    .map { offset -> (preferred + offset) % timetablePalette.size }
                    .first { index -> !occupied[index] }
            } else {
                preferred
            }
            occupied[paletteIndex] = true
            put(name, timetablePalette[paletteIndex])
        }
    }
}

internal fun String.normalizedCourseName(): String =
    trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

private enum class TaskFilter(val label: String) {
    ALL("所有"),
    TODAY("今天"),
    NEXT_7_DAYS("最近 7 天"),
    INBOX("收集箱"),
    COMPLETED("已完成"),
}

private val TaskFilter.pageTitle: String
    get() = label

@Composable
private fun TasksScreen(
    state: KexuUiState,
    filter: TaskFilter,
    onFilterChange: (TaskFilter) -> Unit,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFilterMenu by rememberSaveable { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val today = LocalDate.now()
    val endOfNextSevenDays = today.plusDays(8).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
    val tasks = state.tasks
        .filter { task ->
            when (filter) {
                TaskFilter.ALL -> !state.completedForPlacement(task)
                TaskFilter.TODAY -> !state.completedForPlacement(task) && task.dueAt?.toLocalDate() == today
                TaskFilter.NEXT_7_DAYS -> !state.completedForPlacement(task) && task.dueAt != null && task.dueAt in now..endOfNextSevenDays
                TaskFilter.INBOX -> !state.completedForPlacement(task) && task.dueAt == null
                TaskFilter.COMPLETED -> state.completedForPlacement(task)
            }
        }
        .sortedWith(
            if (filter == TaskFilter.COMPLETED) {
                compareByDescending { it.completedAt ?: 0L }
            } else {
                compareByDescending<TaskEntity> { it.priority }.thenBy { it.dueAt ?: Long.MAX_VALUE }
            },
        )
    val courseMap = state.courses.associateBy { it.id }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${tasks.size} 个任务",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Box {
                TextButton(onClick = { showFilterMenu = true }) { Text("切换清单") }
                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                    TaskFilter.entries.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.label, fontWeight = if (item == filter) FontWeight.SemiBold else FontWeight.Normal) },
                            onClick = {
                                onFilterChange(item)
                                showFilterMenu = false
                            },
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        AnimatedContent(tasks.isEmpty(), label = "taskListEmpty",
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }) { empty ->
        if (empty) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.TaskAlt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = .55f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(if (filter == TaskFilter.COMPLETED) "还没有已完成任务" else "这里还没有任务")
                    Text(
                        if (filter == TaskFilter.INBOX) "没有日期的任务会显示在这里" else "点击右下角添加任务",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 104.dp)) {
                items(tasks, key = { it.id }) { task ->
                  Column(Modifier.animateItem(fadeOutSpec = tween(100), placementSpec = tween(220, delayMillis = 100))) {
                    TaskRow(task, courseMap[task.courseId]?.name, onToggle, onDelete, onEdit)
                    HorizontalDivider(
                        Modifier.padding(start = 64.dp, end = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f),
                    )
                  }
                }
            }
        }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    courseName: String?,
    onToggle: (TaskEntity) -> Unit,
    onDelete: ((TaskEntity) -> Unit)?,
    onEdit: ((TaskEntity) -> Unit)?,
) {
    var showMenu by rememberSaveable(task.id) { mutableStateOf(false) }
    val transitioning = task.id in LocalTaskTransitions.current
    DidaTaskContent(
        task = task,
        courseName = courseName,
        onToggle = onToggle,
        completedStyle = task.isCompleted,
        modifier = Modifier
            .fillMaxWidth()
            .softClick(enabled = onEdit != null && !transitioning) { onEdit?.invoke(task) },
    ) {
        if (onDelete != null || onEdit != null) {
            Box {
                IconButton(onClick = { showMenu = true }, enabled = !transitioning,
                    modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "任务菜单", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            enabled = !transitioning,
                            onClick = {
                                showMenu = false
                                onEdit(task)
                            },
                        )
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                            enabled = !transitioning,
                            onClick = {
                                showMenu = false
                                onDelete(task)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureModulesScreen(
    state: KexuUiState,
    onBack: () -> Unit,
    onToggle: (FeatureModule, Boolean) -> Unit,
    onOpen: (FeatureModule) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = state.settings?.enabledFeatures.orEmpty()
    val features = FeatureModule.entries
    LazyColumn(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().height(72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Text("‹", fontSize = 40.sp, fontWeight = FontWeight.Light)
                }
                Text("功能模块", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Text("基础模块", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.height(8.dp))
            DidaSettingsGroup(
                rows = listOf(
                    SettingsRowSpec(Icons.Outlined.CheckBox, "任务", "用清单管理工作和生活"),
                    SettingsRowSpec(Icons.Outlined.CalendarMonth, "日历", "在月历和课表中规划时间"),
                    SettingsRowSpec(Icons.Outlined.Settings, "设置", "查看和修改本地设置"),
                ),
            )
        }
        item {
            Text("可选模块", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
        }
        items(features, key = { it.name }) { feature ->
            val isEnabled = feature in enabled
            Surface(
                modifier = Modifier.fillMaxWidth().softClick(RoundedCornerShape(14.dp)) {
                    if (isEnabled) onOpen(feature) else onToggle(feature, true)
                },
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        feature.moduleIcon,
                        contentDescription = null,
                        tint = feature.moduleColor,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(feature.moduleTitle, fontSize = 17.sp)
                        Text(
                            feature.moduleDescription,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { onToggle(feature, it) },
                    )
                }
            }
        }
        item {
            Text(
                "底部导航最多显示两个可选模块；从这里打开其他模块时，会临时显示当前模块。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

private val FeatureModule.moduleDescription: String
    get() = when (this) {
        FeatureModule.QUADRANT -> "关注重要且紧急的事情"
        FeatureModule.FOCUS -> "使用番茄计时，保持专注"
        FeatureModule.HABITS -> "养成习惯，让自律成为日常"
        FeatureModule.COUNTDOWN -> "记录每一个重要日子"
        FeatureModule.SEARCH -> "快速搜索任务"
    }

private val FeatureModule.moduleIcon: androidx.compose.ui.graphics.vector.ImageVector
    get() = when (this) {
        FeatureModule.QUADRANT -> Icons.Outlined.GridView
        FeatureModule.FOCUS -> Icons.Outlined.Timer
        FeatureModule.HABITS -> Icons.Outlined.AccessTime
        FeatureModule.COUNTDOWN -> Icons.Outlined.HourglassBottom
        FeatureModule.SEARCH -> Icons.Outlined.Search
    }

private val FeatureModule.moduleColor: Color
    get() = when (this) {
        FeatureModule.QUADRANT -> Color(0xFF8D8E92)
        FeatureModule.FOCUS -> Color(0xFF4A72FA)
        FeatureModule.HABITS -> Color(0xFF20C59B)
        FeatureModule.COUNTDOWN -> Color(0xFFFFA62B)
        FeatureModule.SEARCH -> Color(0xFF65738E)
    }

@Composable
private fun QuadrantScreen(
    tasks: List<TaskEntity>,
    onToggle: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onAdd: (important: Boolean, urgent: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val transitions = LocalTaskTransitions.current
    val active = tasks.filterNot { transitions[it.id]?.original?.isCompleted ?: it.isCompleted }
    fun urgent(task: TaskEntity): Boolean =
        task.dueAt?.toLocalDate()?.let { !it.isAfter(today) } == true
    fun important(task: TaskEntity): Boolean = task.priority >= 2
    val groups = listOf(
        QuadrantSpec("Ⅰ", "重要且紧急", Color(0xFFFF5864), true, true),
        QuadrantSpec("Ⅱ", "重要不紧急", Color(0xFFFFA600), true, false),
        QuadrantSpec("Ⅲ", "不重要但紧急", Color(0xFF4772FA), false, true),
        QuadrantSpec("Ⅳ", "不重要不紧急", Color(0xFF18BF92), false, false),
    )
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("四象限", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${active.size} 项", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 4.dp)) {
            repeat(2) { row ->
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(2) { column ->
                        val spec = groups[row * 2 + column]
                        val quadrantTasks = active.filter {
                            important(it) == spec.important && urgent(it) == spec.urgent
                        }
                        QuadrantCard(
                            spec = spec,
                            tasks = quadrantTasks,
                            onToggle = onToggle,
                            onEdit = onEdit,
                            onAdd = { onAdd(spec.important, spec.urgent) },
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
                if (row == 0) Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private data class QuadrantSpec(
    val numeral: String,
    val title: String,
    val color: Color,
    val important: Boolean,
    val urgent: Boolean,
)

@Composable
private fun QuadrantCard(
    spec: QuadrantSpec,
    tasks: List<TaskEntity>,
    onToggle: (TaskEntity) -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = spec.color, shape = CircleShape) {
                    Text(
                        spec.numeral,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(7.dp))
                Text(spec.title, color = spec.color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onAdd, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "添加${spec.title}任务", tint = spec.color)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("没有任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                tasks.take(7).forEach { task ->
                  androidx.compose.runtime.key(task.id) {
                    Row(
                        Modifier.fillMaxWidth().softClick(enabled = task.id !in LocalTaskTransitions.current) { onEdit(task) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CompletionToggle(task.isCompleted, "完成任务：${task.title}", { onToggle(task) },
                            visualSize = 19.dp, circular = false, accent = spec.color, outline = spec.color)
                        Spacer(Modifier.width(7.dp))
                        CompletionTitle(task.title, task.isCompleted, maxLines = 2,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp))
                    }
                  }
                }
                if (tasks.size > 7) {
                    Text("+${tasks.size - 7}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FocusScreen(
    state: KexuUiState,
    onRecordSession: (Long?, Int, Long, String) -> Unit,
    onTimerChange: (FocusTimerSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timer = state.settings?.focusTimer ?: FocusTimerSettings()
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showTaskMenu by rememberSaveable { mutableStateOf(false) }
    var showCustomDuration by rememberSaveable { mutableStateOf(false) }
    var customDurationText by rememberSaveable { mutableStateOf(timer.durationMinutes.toString()) }
    val wallSeconds = if (timer.running && timer.anchorAtMillis > 0L) {
        ((nowMillis - timer.anchorAtMillis).coerceAtLeast(0L) / 1_000L).toInt()
    } else {
        0
    }
    val seconds = if (timer.positiveMode) {
        timer.elapsedSeconds + wallSeconds
    } else {
        (timer.remainingSeconds - wallSeconds).coerceAtLeast(0)
    }

    LaunchedEffect(timer.running, timer.anchorAtMillis) {
        nowMillis = System.currentTimeMillis()
        while (timer.running) {
            delay(1_000)
            nowMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(timer.running, timer.positiveMode, seconds) {
        if (timer.running && !timer.positiveMode && seconds == 0) {
            onRecordSession(
                timer.selectedTaskId,
                timer.durationMinutes * 60,
                timer.startedAtMillis,
                "POMODORO",
            )
            onTimerChange(
                timer.copy(
                    remainingSeconds = timer.durationMinutes * 60,
                    elapsedSeconds = 0,
                    running = false,
                    startedAtMillis = 0L,
                    anchorAtMillis = 0L,
                ),
            )
        }
    }

    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (timer.positiveMode) "正计时" else "番茄计时",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = {
                    val nextPositiveMode = !timer.positiveMode
                    onTimerChange(
                        timer.copy(
                            positiveMode = nextPositiveMode,
                            remainingSeconds = timer.durationMinutes * 60,
                            elapsedSeconds = 0,
                            running = false,
                            startedAtMillis = 0L,
                            anchorAtMillis = 0L,
                        ),
                    )
                },
            ) {
                Text(if (timer.positiveMode) "番茄" else "正计时")
            }
        }
        Box {
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showTaskMenu = true }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.tasks.firstOrNull { it.id == timer.selectedTaskId }?.title ?: "专注")
                Text("  ›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = showTaskMenu, onDismissRequest = { showTaskMenu = false }) {
                DropdownMenuItem(
                    text = { Text("不关联任务") },
                    onClick = {
                        onTimerChange(timer.copy(selectedTaskId = null))
                        showTaskMenu = false
                    },
                )
                state.tasks.filterNot { it.isCompleted }.take(12).forEach { task ->
                    DropdownMenuItem(
                        text = { Text(task.title, maxLines = 1) },
                        onClick = {
                            onTimerChange(timer.copy(selectedTaskId = task.id))
                            showTaskMenu = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        FocusTimerDial(seconds, timer.durationMinutes * 60, timer.positiveMode, timer.running)
        if (!timer.positiveMode && !timer.running) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(15, 25, 45).forEach { minutes ->
                    FilterChip(
                        selected = timer.durationMinutes == minutes,
                        onClick = {
                            onTimerChange(
                                timer.copy(
                                    durationMinutes = minutes,
                                    remainingSeconds = minutes * 60,
                                    elapsedSeconds = 0,
                                ),
                            )
                        },
                        label = { Text("$minutes 分") },
                    )
                }
                val customDuration = timer.durationMinutes !in setOf(15, 25, 45)
                FilterChip(
                    selected = customDuration,
                    onClick = {
                        customDurationText = timer.durationMinutes.toString()
                        showCustomDuration = true
                    },
                    label = {
                        Text(if (customDuration) "${timer.durationMinutes} 分" else "自定义")
                    },
                )
            }
        } else if (timer.positiveMode && !timer.running && seconds > 0) {
            TextButton(
                onClick = {
                    onRecordSession(timer.selectedTaskId, seconds, timer.startedAtMillis, "STOPWATCH")
                    onTimerChange(
                        timer.copy(
                            elapsedSeconds = 0,
                            running = false,
                            startedAtMillis = 0L,
                            anchorAtMillis = 0L,
                        ),
                    )
                },
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("完成并记录")
            }
        } else {
            Spacer(Modifier.height(66.dp))
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                val now = System.currentTimeMillis()
                if (!timer.running) {
                    onTimerChange(
                        timer.copy(
                            remainingSeconds = if (!timer.positiveMode && seconds == 0) {
                                timer.durationMinutes * 60
                            } else {
                                seconds
                            },
                            elapsedSeconds = if (timer.positiveMode) seconds else timer.elapsedSeconds,
                            running = true,
                            startedAtMillis = if (timer.startedAtMillis == 0L) now else timer.startedAtMillis,
                            anchorAtMillis = now,
                        ),
                    )
                } else {
                    onTimerChange(
                        timer.copy(
                            remainingSeconds = if (timer.positiveMode) timer.remainingSeconds else seconds,
                            elapsedSeconds = if (timer.positiveMode) seconds else timer.elapsedSeconds,
                            running = false,
                            startedAtMillis = timer.startedAtMillis,
                            anchorAtMillis = 0L,
                        ),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(.52f).height(58.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            FocusPlaybackGlyph(timer.running)
            Spacer(Modifier.width(8.dp))
            Text(if (timer.running) "暂停" else "开始", fontSize = 18.sp)
        }
        val todaySessions = state.focusSessions.filter {
            Instant.ofEpochMilli(it.completedAt).atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
        }
        Text(
            "今日专注 ${todaySessions.sumOf { it.durationSeconds } / 60} 分钟 · ${todaySessions.size} 次",
            modifier = Modifier.padding(vertical = 18.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (showCustomDuration) {
        val customMinutes = customDurationText.toIntOrNull()
        AlertDialog(
            onDismissRequest = { showCustomDuration = false },
            title = { Text("自定义专注时长") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "设置 1–180 分钟。设置后会保留为下次默认时长。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = customDurationText,
                        onValueChange = { value ->
                            customDurationText = value.filter(Char::isDigit).take(3)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("分钟") },
                        suffix = { Text("分") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = customMinutes == null || customMinutes !in 1..180,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = customMinutes in 1..180,
                    onClick = {
                        val minutes = requireNotNull(customMinutes)
                        onTimerChange(
                            timer.copy(
                                durationMinutes = minutes,
                                remainingSeconds = minutes * 60,
                                elapsedSeconds = 0,
                                running = false,
                                startedAtMillis = 0L,
                                anchorAtMillis = 0L,
                            ),
                        )
                        showCustomDuration = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDuration = false }) { Text("取消") }
            },
        )
    }
}

private data class HabitTemplate(
    val title: String,
    val description: String,
    val symbol: String,
    val category: String,
    val reminderMinutes: Int? = null,
)

private val habitLibraryTemplates = listOf(
    HabitTemplate("早起", "规律作息，从清醒的一天开始", "🌅", "自律", 7 * 60),
    HabitTemplate("早睡", "睡前放下手机，保证充足睡眠", "🌙", "自律", 22 * 60 + 30),
    HabitTemplate("今日复盘", "记录完成、收获和明日计划", "✓", "自律", 21 * 60 + 30),
    HabitTemplate("整理待办", "梳理今天最重要的三件事", "📋", "自律", 8 * 60),
    HabitTemplate("喝水", "及时补充水分", "💧", "健康", 9 * 60),
    HabitTemplate("吃早餐", "认真吃一顿早餐", "🥣", "健康", 7 * 60 + 30),
    HabitTemplate("运动", "完成至少 30 分钟运动", "🏃", "健康", 18 * 60),
    HabitTemplate("散步", "到户外走一走，舒展身体", "🚶", "健康", 19 * 60),
    HabitTemplate("拉伸", "缓解久坐带来的紧张", "🧘", "健康", 21 * 60),
    HabitTemplate("按时吃药", "按医嘱完成今日用药", "💊", "健康", 8 * 60),
    HabitTemplate("阅读", "每天留一段不被打扰的阅读时间", "📖", "学习", 20 * 60),
    HabitTemplate("背单词", "积累词汇并及时复习", "🔤", "学习", 7 * 60 + 30),
    HabitTemplate("课前预习", "浏览下一节课的重点内容", "📝", "学习", 20 * 60),
    HabitTemplate("课后复习", "整理课堂笔记和疑问", "🎓", "学习", 20 * 60 + 30),
    HabitTemplate("专注学习", "完成一段无干扰的深度学习", "⏱️", "学习", 19 * 60 + 30),
    HabitTemplate("练习编程", "写代码并总结一个知识点", "💻", "学习", 20 * 60),
    HabitTemplate("整理房间", "让生活空间保持整洁", "🧹", "生活", 19 * 60),
    HabitTemplate("记账", "记录今天的收入和支出", "💰", "生活", 21 * 60),
    HabitTemplate("护肤", "完成每日基础护理", "🧴", "生活", 22 * 60),
    HabitTemplate("联系家人", "给家人留一段交流时间", "☎️", "生活", 20 * 60 + 30),
    HabitTemplate("记录心情", "写下此刻感受和发生的事情", "🙂", "心情", 21 * 60),
    HabitTemplate("感恩一件事", "记录今天值得感谢的人或事", "💛", "心情", 22 * 60),
    HabitTemplate("冥想", "安静呼吸，觉察当下", "🧘", "心情", 7 * 60),
    HabitTemplate("晒太阳", "到户外感受自然光", "☀️", "心情", 10 * 60),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitsScreen(
    state: KexuUiState,
    onSaveHabit: (String, String, String, Int?, HabitEntity?) -> Unit,
    onToggleHabit: (Long, Long) -> Unit,
    onDeleteHabit: (HabitEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<HabitEntity?>(null) }
    var draftTemplate by remember { mutableStateOf<HabitTemplate?>(null) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)
    val checks = state.habitChecks.filter { it.dateEpochDay == selectedEpochDay }.map { it.habitId }.toSet()

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("习惯", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                HabitCompletionCount(checks.size, state.habits.size)
            }
            HabitWeekStrip(selectedDate, { selectedEpochDay = it.toEpochDay() })
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 20.dp, 16.dp, 98.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.habits.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(top = 100.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Outlined.AccessTime, contentDescription = null, modifier = Modifier.size(86.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(18.dp))
                            Text("你这一天没有习惯", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("点击 + 创建第一个习惯", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.habits, key = { it.id }) { habit ->
                        val done = habit.id in checks
                        val streak = habitStreak(habit.id, state.habitChecks.map { it.habitId to it.dateEpochDay }.toSet())
                        androidx.compose.runtime.key(habit.id, selectedEpochDay) {
                            HabitPolishedRow(
                                habit, done, streak,
                                onEdit = { editing = habit; draftTemplate = null; showEditor = true },
                                onToggle = { onToggleHabit(habit.id, selectedEpochDay) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = {
                editing = null
                draftTemplate = null
                showLibrary = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "创建习惯")
        }
    }
    if (showLibrary) {
        HabitLibrarySheet(
            existingTitles = state.habits.map { it.title.trim() }.toSet(),
            onDismiss = { showLibrary = false },
            onCreateCustom = {
                showLibrary = false
                editing = null
                draftTemplate = null
                showEditor = true
            },
            onSelectTemplate = { template ->
                showLibrary = false
                editing = null
                draftTemplate = template
                showEditor = true
            },
        )
    }
    if (showEditor) {
        HabitEditorSheet(
            existing = editing,
            template = draftTemplate,
            onDismiss = {
                showEditor = false
                draftTemplate = null
            },
            onDelete = editing?.let { habit -> { onDeleteHabit(habit); showEditor = false } },
            onSave = { title, description, symbol, reminder ->
                onSaveHabit(title, description, symbol, reminder, editing)
                showEditor = false
                draftTemplate = null
            },
        )
    }
}

private fun habitStreak(habitId: Long, checks: Set<Pair<Long, Long>>): Int {
    var date = LocalDate.now()
    if ((habitId to date.toEpochDay()) !in checks) date = date.minusDays(1)
    var streak = 0
    while ((habitId to date.toEpochDay()) in checks) {
        streak += 1
        date = date.minusDays(1)
    }
    return streak
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitLibrarySheet(
    existingTitles: Set<String>,
    onDismiss: () -> Unit,
    onCreateCustom: () -> Unit,
    onSelectTemplate: (HabitTemplate) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("全部") }
    val categories = listOf("全部", "自律", "健康", "学习", "生活", "心情")
    val filtered = habitLibraryTemplates.filter { template ->
        (category == "全部" || template.category == category) &&
            (query.isBlank() || template.title.contains(query, ignoreCase = true) ||
                template.description.contains(query, ignoreCase = true))
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 720.dp).padding(horizontal = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("习惯库", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "从模板开始，也可以完全自定义",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text("${habitLibraryTemplates.size} 个模板", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("搜索习惯") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { value ->
                    FilterChip(
                        selected = category == value,
                        onClick = { category = value },
                        label = { Text(value) },
                    )
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().softClick(RoundedCornerShape(14.dp), onClick = onCreateCustom),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(9.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("创建自定义习惯", fontWeight = FontWeight.SemiBold)
                        Text(
                            "自定义名称、图标、说明和提醒时间",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                }
            }
            Text(
                if (category == "全部") "推荐习惯" else "$category · ${filtered.size}",
                modifier = Modifier.padding(top = 16.dp, bottom = 7.dp),
                fontWeight = FontWeight.SemiBold,
            )
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.title }) { template ->
                    val alreadyAdded = template.title in existingTitles
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .softClick(RoundedCornerShape(14.dp), enabled = !alreadyAdded) { onSelectTemplate(template) },
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(template.symbol, fontSize = 21.sp)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(template.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    template.description,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                template.reminderMinutes?.let {
                                    Text(
                                        "建议提醒 ${minutesText(it)}",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                            if (alreadyAdded) {
                                Text("已添加", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "使用${template.title}模板",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                if (filtered.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 42.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("没有找到相关习惯", fontWeight = FontWeight.SemiBold)
                            Text("换个关键词，或创建自定义习惯", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitEditorSheet(
    existing: HabitEntity?,
    template: HabitTemplate?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (String, String, String, Int?) -> Unit,
) {
    val editorKey = existing?.id?.toString() ?: template?.title.orEmpty()
    var title by rememberSaveable(editorKey) { mutableStateOf(existing?.title ?: template?.title.orEmpty()) }
    var description by rememberSaveable(editorKey) {
        mutableStateOf(existing?.description ?: template?.description.orEmpty())
    }
    var symbol by rememberSaveable(editorKey) { mutableStateOf(existing?.symbol ?: template?.symbol ?: "✓") }
    var reminder by rememberSaveable(editorKey) {
        mutableStateOf(existing?.reminderMinutes ?: template?.reminderMinutes)
    }
    var showCustomReminder by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                when {
                    existing != null -> "编辑习惯"
                    template != null -> "添加「${template.title}」"
                    else -> "自定义习惯"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("习惯名称") }, singleLine = true)
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("描述") }, maxLines = 2)
            Text("图标", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("✓", "💧", "📖", "🏃", "🌙") + symbol).distinct().forEach { value ->
                    FilterChip(selected = symbol == value, onClick = { symbol = value }, label = { Text(value) })
                }
            }
            Text("每日提醒", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(null, 8 * 60, 12 * 60, 21 * 60).forEach { minutes ->
                    FilterChip(
                        selected = reminder == minutes,
                        onClick = { reminder = minutes },
                        label = { Text(minutes?.let(::minutesText) ?: "关闭") },
                    )
                }
                val custom = reminder != null && reminder !in setOf(8 * 60, 12 * 60, 21 * 60)
                FilterChip(
                    selected = custom,
                    onClick = { showCustomReminder = true },
                    label = { Text(if (custom) minutesText(requireNotNull(reminder)) else "自定义") },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("删除") }
                }
                OutlinedButton(
                    onClick = { onSave(title, description, symbol, reminder) },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("保存") }
            }
        }
    }
    if (showCustomReminder) {
        val initial = reminder ?: 9 * 60
        val picker = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showCustomReminder = false },
            title = { Text("自定义提醒时间") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = picker)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        reminder = picker.hour * 60 + picker.minute
                        showCustomReminder = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomReminder = false }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownsScreen(
    state: KexuUiState,
    onSave: (CountdownEntity) -> Unit,
    onDelete: (CountdownEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CountdownEntity?>(null) }
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("倒数纪念日", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${state.countdowns.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.countdowns.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Outlined.HourglassBottom, contentDescription = null, modifier = Modifier.size(90.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(18.dp))
                        Text("记录每一个重要日子", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("支持倒数日、纪念日、生日和节日", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(state.countdowns, key = { it.id }) { countdown ->
                    val target = nextCountdownDate(countdown, LocalDate.now())
                    val days = target.toEpochDay() - LocalDate.now().toEpochDay()
                    CountdownPolishedRow(
                        countdown, target, days,
                        onClick = { editing = countdown; showEditor = true },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = {
                editing = null
                showEditor = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加重要日子")
        }
    }
    if (showEditor) {
        CountdownEditorSheet(
            existing = editing,
            onDismiss = { showEditor = false },
            onDelete = editing?.let { value -> { onDelete(value); showEditor = false } },
            onSave = {
                onSave(it)
                showEditor = false
            },
        )
    }
}

private fun nextCountdownDate(countdown: CountdownEntity, today: LocalDate): LocalDate {
    var target = LocalDate.ofEpochDay(countdown.targetDateEpochDay)
    if (countdown.repeatYearly) {
        val monthDay = MonthDay.from(target)
        target = monthDay.atYear(today.year)
        if (target.isBefore(today)) target = monthDay.atYear(today.year + 1)
    }
    return target
}

private fun countdownTypeColor(type: String): Color = when (type) {
    "BIRTHDAY" -> Color(0xFFFF6B72)
    "ANNIVERSARY" -> Color(0xFFFF5DA2)
    "HOLIDAY" -> Color(0xFF16BE91)
    else -> Color(0xFF4772FA)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownEditorSheet(
    existing: CountdownEntity?,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (CountdownEntity) -> Unit,
) {
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var targetEpochDay by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.targetDateEpochDay ?: LocalDate.now().plusDays(1).toEpochDay())
    }
    var type by rememberSaveable(existing?.id) { mutableStateOf(existing?.type ?: "COUNTDOWN") }
    var repeatYearly by rememberSaveable(existing?.id) { mutableStateOf(existing?.repeatYearly ?: false) }
    var reminderDays by rememberSaveable(existing?.id) { mutableIntStateOf(existing?.reminderDaysBefore ?: 0) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val symbol = when (type) {
        "HOLIDAY" -> "🎈"
        "BIRTHDAY" -> "🎂"
        "ANNIVERSARY" -> "💕"
        else -> "⌛"
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (existing == null) "添加重要日子" else "编辑重要日子", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("名称") }, singleLine = true)
            Text("类型", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("COUNTDOWN" to "倒数日", "ANNIVERSARY" to "纪念日", "BIRTHDAY" to "生日", "HOLIDAY" to "节日").forEach { (value, label) ->
                    FilterChip(selected = type == value, onClick = { type = value }, label = { Text(label) })
                }
            }
            SettingsValueRow(
                "日期",
                LocalDate.ofEpochDay(targetEpochDay).format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                onClick = { showDatePicker = true },
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("每年重复", Modifier.weight(1f))
                Switch(checked = repeatYearly, onCheckedChange = { repeatYearly = it })
            }
            Text("提前提醒", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 1, 3, 7).forEach { days ->
                    FilterChip(
                        selected = reminderDays == days,
                        onClick = { reminderDays = days },
                        label = { Text(if (days == 0) "当天" else "$days 天") },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onDelete != null) {
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("删除") }
                }
                OutlinedButton(
                    onClick = {
                        onSave(
                            (existing ?: CountdownEntity(name = name, targetDateEpochDay = targetEpochDay)).copy(
                                name = name,
                                targetDateEpochDay = targetEpochDay,
                                type = type,
                                symbol = symbol,
                                repeatYearly = repeatYearly,
                                reminderDaysBefore = reminderDays,
                            ),
                        )
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) { Text("保存") }
            }
        }
    }
    if (showDatePicker) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.ofEpochDay(targetEpochDay)
                .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        targetEpochDay = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) {
            DatePicker(picker)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsScreen(
    selectedTheme: AppTheme,
    themeMode: ThemeMode,
    onThemeChange: (AppTheme) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPage by rememberSaveable { mutableIntStateOf(0) }
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
            }
            Text(
                "外观",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            listOf("主题", "显示").forEachIndexed { index, label ->
                Column(
                    Modifier
                        .width(112.dp)
                        .fillMaxSize()
                        .clickable { selectedPage = index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        label,
                        color = if (selectedPage == index) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedPage == index) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(11.dp))
                    Box(
                        Modifier
                            .height(2.dp)
                            .width(42.dp)
                            .background(
                                if (selectedPage == index) MaterialTheme.colorScheme.primary
                                else Color.Transparent,
                                CircleShape,
                            ),
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))

        if (selectedPage == 0) {
            Text(
                "颜色系列",
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                gridItems(AppTheme.entries, key = { it.name }) { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        selected = theme == selectedTheme,
                        onClick = { onThemeChange(theme) },
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                Text(
                    "外观模式",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onThemeModeChange(mode) }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(mode.displayName, fontWeight = FontWeight.Medium)
                                    if (mode == ThemeMode.FOLLOW_SYSTEM) {
                                        Text(
                                            "自动跟随手机的深色外观设置",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                if (mode == themeMode) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "已选择",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            if (index != ThemeMode.entries.lastIndex) {
                                HorizontalDivider(
                                    Modifier.padding(start = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f),
                                )
                            }
                        }
                    }
                }
                Text(
                    "选择“暗夜”主题时会始终使用深色背景；其他主题可单独选择浅色、深色或跟随系统。",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = paletteFor(theme)
    Column(
        Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(.82f)
                .clip(RoundedCornerShape(11.dp))
                .then(
                    if (theme == AppTheme.PRISM) {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFDDD6FA),
                                    Color(0xFFD9F3EC),
                                    Color(0xFFFFE5CE),
                                ),
                            ),
                        )
                    } else {
                        Modifier.background(palette.background)
                    },
                )
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) palette.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(11.dp),
                ),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .align(Alignment.TopCenter),
                color = palette.primary,
            ) {}
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 27.dp, start = 9.dp, end = 9.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier
                            .fillMaxWidth(if (index == 1) .72f else 1f)
                            .height(5.dp)
                            .background(
                                if (theme == AppTheme.NIGHT) Color(0xFF454A55)
                                else palette.primary.copy(alpha = .2f),
                                CircleShape,
                            ),
                    )
                }
            }
            if (selected) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(22.dp),
                    color = palette.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "已选择",
                        modifier = Modifier.padding(4.dp),
                    )
                }
            }
        }
        Text(
            theme.displayName,
            modifier = Modifier.padding(top = 7.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: KexuUiState,
    onSemesterStartChange: (Long) -> Unit,
    onDefaultCourseReminderChange: (Int) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenImport: () -> Unit,
    onOpenFeatureModules: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRefresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val reminderHealth = remember(permissionRefresh) { readReminderHealth(context) }
    var showSemesterPicker by rememberSaveable { mutableStateOf(false) }
    var showReminderSettings by rememberSaveable { mutableStateOf(false) }
    var showAppearanceSettings by rememberSaveable { mutableStateOf(false) }
    var showWidgetHelp by rememberSaveable { mutableStateOf(false) }
    var showMoreSettings by rememberSaveable { mutableStateOf(false) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    val start = state.settings?.semesterStartMillis
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

    if (showAppearanceSettings) {
        AppearanceSettingsScreen(
            selectedTheme = state.settings?.appTheme ?: AppTheme.DEFAULT,
            themeMode = state.settings?.themeMode ?: ThemeMode.FOLLOW_SYSTEM,
            onThemeChange = onAppThemeChange,
            onThemeModeChange = onThemeModeChange,
            onBack = { showAppearanceSettings = false },
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item {
            Text(
                "设置",
                modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 14.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.app_icon_turtle),
                    contentDescription = "不着急",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("不着急", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsBadge("✓  仅本地存储", Color(0xFF63CDA3))
                        SettingsBadge("★  准时提醒", Color(0xFF8C99B3))
                    }
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
        }
        item {
            DidaSettingsGroup(
                rows = listOf(
                    SettingsRowSpec(
                        Icons.Outlined.Widgets,
                        "功能模块",
                        subtitle = "${state.settings?.enabledFeatures?.size ?: 0} 个已启用",
                        onClick = onOpenFeatureModules,
                    ),
                ),
            )
            Spacer(Modifier.height(14.dp))
            DidaSettingsGroup(
                rows = listOf(
                    SettingsRowSpec(
                        Icons.Outlined.Palette,
                        "外观",
                        subtitle = "${state.settings?.appTheme?.displayName ?: AppTheme.DEFAULT.displayName} · " +
                            (state.settings?.themeMode?.displayName ?: ThemeMode.FOLLOW_SYSTEM.displayName),
                        onClick = { showAppearanceSettings = true },
                    ),
                    SettingsRowSpec(
                        Icons.Outlined.DateRange,
                        "日期与时间",
                        subtitle = start?.format(dateFormatter),
                        onClick = { showSemesterPicker = true },
                    ),
                    SettingsRowSpec(
                        Icons.Outlined.MusicNote,
                        "声音、提醒与通知",
                        subtitle = reminderHealth.summary,
                        onClick = { showReminderSettings = true },
                    ),
                    SettingsRowSpec(
                        Icons.Outlined.Widgets,
                        "桌面小组件",
                        onClick = { showWidgetHelp = true },
                    ),
                    SettingsRowSpec(
                        Icons.Outlined.Tune,
                        "更多设置",
                        onClick = { showMoreSettings = true },
                    ),
                ),
            )
            Spacer(Modifier.height(14.dp))
            DidaSettingsGroup(
                rows = listOf(
                    SettingsRowSpec(
                        Icons.Outlined.Sync,
                        "关联与导入",
                        subtitle = "教务网、图片",
                        iconColor = Color(0xFF20C8A0),
                        onClick = onOpenImport,
                    ),
                ),
            )
            Spacer(Modifier.height(14.dp))
            DidaSettingsGroup(
                rows = listOf(
                    SettingsRowSpec(
                        Icons.Outlined.Home,
                        "本地数据",
                        subtitle = "${state.tasks.size} 个任务 · ${state.courses.size} 门课程",
                        iconColor = Color(0xFFFFB516),
                    ),
                    SettingsRowSpec(
                        Icons.Outlined.School,
                        "使用指南",
                        iconColor = Color(0xFFFFB516),
                        onClick = { showGuide = true },
                    ),
                    SettingsRowSpec(
                        Icons.Outlined.Info,
                        "帮助与反馈",
                        iconColor = Color(0xFFFFB516),
                        onClick = { showHelp = true },
                    ),
                    SettingsRowSpec(
                        Icons.Outlined.Info,
                        "关于",
                        subtitle = BuildConfig.VERSION_NAME,
                        iconColor = Color(0xFFFFB516),
                        onClick = { showAbout = true },
                    ),
                ),
            )
        }
    }
    if (showSemesterPicker) {
        val initialDateMillis = state.settings?.semesterStartMillis
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
            ?.atStartOfDay(ZoneId.of("UTC"))
            ?.toInstant()
            ?.toEpochMilli()
            ?: System.currentTimeMillis()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
        DatePickerDialog(
            onDismissRequest = { showSemesterPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selectedUtc ->
                            val date = Instant.ofEpochMilli(selectedUtc).atZone(ZoneId.of("UTC")).toLocalDate()
                            onSemesterStartChange(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                        }
                        showSemesterPicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showSemesterPicker = false }) { Text("取消") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
    if (showReminderSettings) {
        ModalBottomSheet(onDismissRequest = { showReminderSettings = false }) {
            ReminderSettingsContent(
                defaultCourseReminderMinutes = state.settings?.defaultCourseReminderMinutes,
                onDefaultCourseReminderChange = onDefaultCourseReminderChange,
                onPermissionStateChanged = { permissionRefresh++ },
            )
        }
    }
    if (showWidgetHelp) {
        LocalInfoSettingsSheet(
            title = "桌面小组件",
            lines = listOf(
                "不着急提供“下一节课”和“今日任务”桌面小组件。",
                "请长按系统桌面空白处，选择“小组件”，再找到不着急。",
            ),
            onDismiss = { showWidgetHelp = false },
        )
    }
    if (showMoreSettings) {
        LocalInfoSettingsSheet(
            title = "更多设置",
            lines = listOf(
                "所有任务、课表和设置只保存在这台设备。",
                "不着急没有账号、会员或云同步，也不会上传教务网登录信息。",
                "网页与图片课表解析均在本机完成。",
            ),
            onDismiss = { showMoreSettings = false },
        )
    }
    if (showGuide) {
        LocalInfoSettingsSheet(
            title = "使用指南",
            lines = listOf(
                "在任务页点击“+”，可直接输入“明晚八点交作业，提前两小时提醒”。",
                "在日历页左右滑动切换月份，点击周视图按钮进入可左右滑动的课表。",
                "导入课表时请在学校网页自行登录，打开学期理论课表后再确认导入。",
            ),
            onDismiss = { showGuide = false },
        )
    }
    if (showHelp) {
        LocalInfoSettingsSheet(
            title = "帮助与反馈",
            lines = listOf(
                "提醒未出现时，请在“声音、提醒与通知”中检查通知总开关、通知类别、精确定时和后台限制，并查看最近派发记录。",
                "教务网页无法打开时，可在导入页复制网页诊断信息。",
                "强行停止应用会阻止系统派发，应用无法绕过；重新打开不着急后会恢复登记。无需关闭全局省电功能。",
            ),
            onDismiss = { showHelp = false },
        )
    }
    if (showAbout) {
        LocalInfoSettingsSheet(
            title = "关于不着急",
            lines = listOf(
                "版本 ${BuildConfig.VERSION_NAME}",
                "本地优先的学生课表与任务清单。",
                "界面交互参考滴答清单，课表导入与课程提醒针对校园场景设计。",
            ),
            onDismiss = { showAbout = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalInfoSettingsSheet(
    title: String,
    lines: List<String>,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            lines.forEach { line ->
                Text(line, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            }
            if (actionLabel != null && onAction != null) {
                OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private data class SettingsRowSpec(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String? = null,
    val iconColor: Color = Color(0xFFE66D9A),
    val onClick: (() -> Unit)? = null,
)

@Composable
private fun DidaSettingsGroup(rows: List<SettingsRowSpec>) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column {
            rows.forEachIndexed { index, row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = row.onClick != null) { row.onClick?.invoke() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(row.icon, contentDescription = null, tint = row.iconColor, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(row.title, Modifier.weight(1f), fontSize = 16.sp)
                    row.subtitle?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(4.dp))
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFFB9BCC3),
                        modifier = Modifier.size(18.dp),
                    )
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(Modifier.padding(start = 50.dp), color = Color(0xFFF0F1F3))
                }
            }
        }
    }
}

@Composable
private fun SettingsBadge(text: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(5.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SettingsValueRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
    ) {
        Text(label, Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorSheet(
    courses: List<CourseEntity>,
    existing: TaskEntity?,
    initialCourseId: Long?,
    initialDueDate: LocalDate?,
    initialPriority: Int,
    onDismiss: () -> Unit,
    onSave: (com.kexu.app.nlp.ParsedTaskInput, String, Long?, Int) -> Unit,
) {
    val initialInput = remember(existing?.id) {
        existing?.let { task ->
            task.title + (task.dueAt?.let { " " + Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M月d日 HH:mm")) } ?: "")
        }.orEmpty()
    }
    var input by rememberSaveable(existing?.id) { mutableStateOf(initialInput) }
    var notes by rememberSaveable(existing?.id) { mutableStateOf(existing?.notes.orEmpty()) }
    var courseId by rememberSaveable(existing?.id, initialCourseId) {
        mutableStateOf(existing?.courseId ?: initialCourseId)
    }
    var priority by rememberSaveable(existing?.id, initialPriority) {
        mutableIntStateOf(existing?.priority ?: initialPriority)
    }
    var showPriorityMenu by rememberSaveable { mutableStateOf(false) }
    var showCourseMenu by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var scheduleOverridden by rememberSaveable(existing?.id) { mutableStateOf(existing != null) }
    var manualDueAtMillis by rememberSaveable(existing?.id) { mutableStateOf(existing?.dueAt) }
    var manualEndAtMillis by rememberSaveable(existing?.id) { mutableStateOf(existing?.endAt) }
    var manualReminderAtMillis by rememberSaveable(existing?.id) { mutableStateOf(existing?.remindAt) }
    var manualRecurrenceRule by rememberSaveable(existing?.id) { mutableStateOf(existing?.recurrenceRule) }
    var manualHasExplicitTime by rememberSaveable(existing?.id) {
        mutableStateOf(
            existing?.dueAt
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() != LocalTime.of(23, 59) }
                ?: false,
        )
    }
    val parser = remember { ChineseTimeParser() }
    val parsed = remember(input) { parser.parse(input) }
    val defaultDateMillis = remember(initialDueDate) {
        initialDueDate
                ?.atTime(23, 59)
                ?.atZone(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli()
    }
    val effectiveParsed = remember(
        parsed,
        defaultDateMillis,
        scheduleOverridden,
        manualDueAtMillis,
        manualEndAtMillis,
        manualReminderAtMillis,
        manualRecurrenceRule,
        manualHasExplicitTime,
    ) {
        if (scheduleOverridden) {
            parsed.copy(
                dueAtMillis = manualDueAtMillis,
                endAtMillis = manualEndAtMillis,
                reminderAtMillis = manualReminderAtMillis,
                recurrenceRule = manualRecurrenceRule,
                hasExplicitDate = manualDueAtMillis != null,
                hasExplicitTime = manualHasExplicitTime,
            )
        } else if (parsed.dueAtMillis == null && defaultDateMillis != null) {
            parsed.copy(dueAtMillis = defaultDateMillis, reminderAtMillis = null)
        } else {
            parsed
        }
    }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val timeAccentColor = MaterialTheme.colorScheme.primary
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val currentOnSave by rememberUpdatedState(onSave)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    var submitting by remember(existing?.id) { mutableStateOf(false) }
    var finished by remember(existing?.id) { mutableStateOf(false) }
    val canSubmit = parsed.cleanTitle.isNotBlank() && !submitting && !finished

    fun appendToken(token: String) {
        input = listOf(input.trim(), token).filter(String::isNotBlank).joinToString(" ")
    }

    fun submit() {
        if (!canSubmit || submitting || finished || sheetState.targetValue == SheetValue.Hidden) return
        // Capture the complete draft before closing; both send and IME Done use
        // this path, and a second tap cannot enqueue another save.
        submitting = true
        val submittedTask = effectiveParsed
        val submittedNotes = notes
        val submittedCourseId = courseId
        val submittedPriority = priority
        scope.launch {
            try {
                keyboard?.hide()
                sheetState.hide()
            } finally {
                // A dismissal interrupting the closing animation must not lose
                // a save that the user has already requested.
                if (!finished) {
                    finished = true
                    currentOnSave(submittedTask, submittedNotes, submittedCourseId, submittedPriority)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!submitting && !finished) {
                finished = true
                keyboard?.hide()
                currentOnDismiss()
            }
        },
        sheetState = sheetState,
        sheetGesturesEnabled = !submitting,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        // These locals belong to the sheet's dialog window. The Activity window
        // remains behind it and cannot tell us when a nested picker releases focus.
        val sheetWindowInfo = LocalWindowInfo.current
        val sheetKeyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(
            existing?.id,
            showDatePicker,
            sheetState.currentValue,
            sheetState.targetValue,
            sheetWindowInfo.isWindowFocused,
            submitting,
            finished,
        ) {
            if (!showDatePicker && !submitting && !finished && sheetWindowInfo.isWindowFocused &&
                sheetState.currentValue == SheetValue.Expanded && sheetState.targetValue == SheetValue.Expanded
            ) {
                withFrameNanos { }
                focusRequester.requestFocus()
                // Give the focused text field a frame to reconnect its input
                // session before asking this window's keyboard controller to show.
                withFrameNanos { }
                sheetKeyboard?.show()
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 16.dp, end = 12.dp, top = 0.dp, bottom = 8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp, max = 84.dp)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (input.isBlank()) {
                    Text("准备做什么？", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .42f), fontSize = 20.sp)
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    minLines = 1,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                    ),
                    visualTransformation = remember(
                        parsed.recognizedTokens,
                        timeAccentColor,
                    ) {
                        RecognizedTimeVisualTransformation(
                            recognizedTokens = parsed.recognizedTokens,
                            accentColor = timeAccentColor,
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 32.dp, max = 72.dp)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (notes.isBlank()) {
                    Text("描述", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .42f), fontSize = 16.sp)
                }
                BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { submit() },
                    ),
                )
            }
            Row(
                Modifier.fillMaxWidth().height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !submitting) {
                            keyboard?.hide()
                            showDatePicker = true
                        }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val dateLabel = taskEditorDateLabel(
                        dueAtMillis = effectiveParsed.dueAtMillis,
                        hasExplicitTime = effectiveParsed.hasExplicitTime,
                    )
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "设置日期",
                        tint = if (effectiveParsed.dueAtMillis != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Spacer(Modifier.width(4.dp))
                    Crossfade(
                        targetState = dateLabel,
                        modifier = Modifier.weight(1f),
                        animationSpec = tween(180),
                        label = "taskEditorDate",
                    ) { label ->
                        if (label != null) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { showPriorityMenu = true }, enabled = !submitting) {
                        Icon(
                            Icons.Outlined.Flag,
                            contentDescription = "设置优先级",
                            tint = priorityColor(priority),
                        )
                    }
                    DropdownMenu(
                        expanded = showPriorityMenu,
                        onDismissRequest = { showPriorityMenu = false },
                        properties = PopupProperties(focusable = false),
                    ) {
                        listOf(
                            3 to "高优先级",
                            2 to "中优先级",
                            1 to "低优先级",
                            0 to "无优先级",
                        ).forEach { (level, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Flag, contentDescription = null, tint = priorityColor(level))
                                },
                                trailingIcon = {
                                    if (priority == level) Icon(Icons.Default.Check, contentDescription = null)
                                },
                                onClick = {
                                    priority = level
                                    showPriorityMenu = false
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = { appendToken("#") }, enabled = !submitting) {
                    Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = "添加标签")
                }
                Box {
                    IconButton(onClick = { showCourseMenu = true }, enabled = !submitting) {
                        Icon(
                            Icons.Outlined.Inbox,
                            contentDescription = "选择清单或课程",
                            tint = if (courseId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showCourseMenu,
                        onDismissRequest = { showCourseMenu = false },
                        properties = PopupProperties(focusable = false),
                    ) {
                        DropdownMenuItem(
                            text = { Text("收集箱") },
                            leadingIcon = { Icon(Icons.Outlined.Inbox, contentDescription = null) },
                            trailingIcon = {
                                if (courseId == null) Icon(Icons.Default.Check, contentDescription = null)
                            },
                            onClick = {
                                courseId = null
                                showCourseMenu = false
                            },
                        )
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.name) },
                                leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null) },
                                trailingIcon = {
                                    if (courseId == course.id) Icon(Icons.Default.Check, contentDescription = null)
                                },
                                onClick = {
                                    courseId = course.id
                                    showCourseMenu = false
                                },
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }, enabled = !submitting) {
                        Icon(Icons.Outlined.MoreHoriz, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        properties = PopupProperties(focusable = false),
                    ) {
                        DropdownMenuItem(
                            text = { Text("图片") },
                            leadingIcon = { Icon(Icons.Outlined.Image, contentDescription = null) },
                            onClick = { showMoreMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text("转为笔记") },
                            leadingIcon = { Icon(Icons.Outlined.NoteAlt, contentDescription = null) },
                            onClick = { showMoreMenu = false },
                        )
                        DropdownMenuItem(
                            text = { Text("全屏编辑") },
                            leadingIcon = { Icon(Icons.Outlined.Fullscreen, contentDescription = null) },
                            onClick = { showMoreMenu = false },
                        )
                    }
                }
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = if (canSubmit) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        IconButton(
                            onClick = { submit() },
                            enabled = canSubmit,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = if (existing == null) "添加任务" else "保存修改",
                                tint = if (canSubmit) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DidaTaskDateTimeSheet(
            initialSelection = TaskDateSelection(
                dueAtMillis = effectiveParsed.dueAtMillis,
                endAtMillis = effectiveParsed.endAtMillis,
                reminderAtMillis = effectiveParsed.reminderAtMillis,
                recurrenceRule = effectiveParsed.recurrenceRule,
                hasExplicitTime = effectiveParsed.hasExplicitTime,
            ),
            onDismiss = {
                showDatePicker = false
            },
            onConfirm = { selection ->
                scheduleOverridden = true
                manualDueAtMillis = selection.dueAtMillis
                manualEndAtMillis = selection.endAtMillis
                manualReminderAtMillis = selection.reminderAtMillis
                manualRecurrenceRule = selection.recurrenceRule
                manualHasExplicitTime = selection.hasExplicitTime
                showDatePicker = false
            },
            onClear = {
                input = removeRecognizedTimeTokens(input, parsed.recognizedTokens)
                scheduleOverridden = true
                manualDueAtMillis = null
                manualEndAtMillis = null
                manualReminderAtMillis = null
                manualRecurrenceRule = null
                manualHasExplicitTime = false
                showDatePicker = false
            },
        )
    }
}

private fun taskEditorDateLabel(dueAtMillis: Long?, hasExplicitTime: Boolean): String? {
    val due = dueAtMillis
        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
        ?: return null
    val today = LocalDate.now()
    val dateLabel = when (due.toLocalDate()) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        today.plusDays(2) -> "后天"
        else -> due.format(DateTimeFormatter.ofPattern("M月d日"))
    }
    return if (hasExplicitTime) "$dateLabel, ${due.format(DateTimeFormatter.ofPattern("HH:mm"))}" else dateLabel
}

private fun removeRecognizedTimeTokens(input: String, tokens: List<String>): String =
    tokens
        .sortedByDescending(String::length)
        .fold(input) { current, token -> current.replace(token, " ") }
        .replace(Regex("\\s+"), " ")
        .trim()

private fun priorityColor(priority: Int): Color = when (priority) {
    3 -> Color(0xFFE53935)
    2 -> Color(0xFFFF9800)
    1 -> Color(0xFF4285F4)
    else -> Color(0xFF9E9E9E)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseEditorSheet(
    existingCourse: CourseEntity?,
    existingSlot: ScheduleSlotEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, DayOfWeek, Int, Int, Int, Int, String) -> Unit,
) {
    val editorKey = existingCourse?.id ?: 0L
    var name by rememberSaveable(editorKey) { mutableStateOf(existingCourse?.name.orEmpty()) }
    var teacher by rememberSaveable(editorKey) { mutableStateOf(existingCourse?.teacher.orEmpty()) }
    var location by rememberSaveable(editorKey) { mutableStateOf(existingCourse?.location.orEmpty()) }
    var day by rememberSaveable(editorKey) {
        mutableStateOf(existingSlot?.dayOfWeek?.let(DayOfWeek::of) ?: DayOfWeek.MONDAY)
    }
    var start by rememberSaveable(editorKey) {
        mutableStateOf(existingSlot?.startMinutes?.let(::minutesText) ?: "08:00")
    }
    var end by rememberSaveable(editorKey) {
        mutableStateOf(existingSlot?.endMinutes?.let(::minutesText) ?: "09:40")
    }
    var startWeek by rememberSaveable(editorKey) { mutableStateOf((existingCourse?.startWeek ?: 1).toString()) }
    var endWeek by rememberSaveable(editorKey) { mutableStateOf((existingCourse?.endWeek ?: 20).toString()) }
    var pattern by rememberSaveable(editorKey) {
        mutableStateOf(
            existingCourse?.weekPattern
                ?.let { runCatching { WeekPattern.valueOf(it) }.getOrNull() }
                ?: WeekPattern.ALL,
        )
    }
    val startMinutes = parseMinutes(start)
    val endMinutes = parseMinutes(end)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Text(
                    if (existingCourse == null) "添加课程" else "编辑课程",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item { Spacer(Modifier.height(14.dp)) }
            item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("课程名称") }) }
            item { Spacer(Modifier.height(10.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(teacher, { teacher = it }, Modifier.weight(1f), label = { Text("教师") })
                    OutlinedTextField(location, { location = it }, Modifier.weight(1f), label = { Text("教室") })
                }
            }
            item { Spacer(Modifier.height(16.dp)); Text("星期", fontWeight = FontWeight.SemiBold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    sundayFirstDays.forEach { value ->
                        FilterChip(selected = day == value, onClick = { day = value }, label = { Text(value.shortChinese) })
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(start, { start = it }, Modifier.weight(1f), label = { Text("开始时间") })
                    OutlinedTextField(end, { end = it }, Modifier.weight(1f), label = { Text("结束时间") })
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(startWeek, { startWeek = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("开始周") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(endWeek, { endWeek = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("结束周") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
            item { Spacer(Modifier.height(14.dp)); Text("周次规则", fontWeight = FontWeight.SemiBold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WeekPattern.entries.forEach { value ->
                        FilterChip(selected = pattern == value, onClick = { pattern = value }, label = { Text(value.chinese) })
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    androidx.compose.material3.Button(
                        onClick = { onSave(name, teacher, location, day, startMinutes!!, endMinutes!!, startWeek.toIntOrNull() ?: 1, endWeek.toIntOrNull() ?: 20, pattern.name) },
                        enabled = name.isNotBlank() && startMinutes != null && endMinutes != null && endMinutes > startMinutes,
                    ) { Text(if (existingCourse == null) "保存课程" else "保存修改") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseOverrideSheet(
    course: CourseEntity,
    originalSlot: ScheduleSlotEntity,
    weekNumber: Int,
    existing: CourseOverrideEntity?,
    onDismiss: () -> Unit,
    onSave: (CourseOverrideEntity) -> Unit,
) {
    val editorKey = "${originalSlot.id}-$weekNumber-${existing?.id ?: 0L}"
    var day by rememberSaveable(editorKey) {
        mutableStateOf(DayOfWeek.of(existing?.dayOfWeek ?: originalSlot.dayOfWeek))
    }
    var start by rememberSaveable(editorKey) {
        mutableStateOf(minutesText(existing?.startMinutes ?: originalSlot.startMinutes))
    }
    var end by rememberSaveable(editorKey) {
        mutableStateOf(minutesText(existing?.endMinutes ?: originalSlot.endMinutes))
    }
    var location by rememberSaveable(editorKey) {
        mutableStateOf(existing?.location ?: course.location)
    }
    var note by rememberSaveable(editorKey) { mutableStateOf(existing?.note.orEmpty()) }
    val startMinutes = parseMinutes(start)
    val endMinutes = parseMinutes(end)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Text(
                    "第 $weekNumber 周临时调课",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "只改变这一周，不影响其他周的课程安排。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text("星期", fontWeight = FontWeight.SemiBold)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    sundayFirstDays.forEach { value ->
                        FilterChip(
                            selected = day == value,
                            onClick = { day = value },
                            label = { Text(value.shortChinese) },
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("开始时间") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("结束时间") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("临时教室") },
                )
            }
            item {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注（可选）") },
                    minLines = 2,
                )
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    androidx.compose.material3.Button(
                        onClick = {
                            onSave(
                                CourseOverrideEntity(
                                    id = existing?.id ?: 0L,
                                    slotId = originalSlot.id,
                                    weekNumber = weekNumber,
                                    isCancelled = false,
                                    dayOfWeek = day.value,
                                    startMinutes = startMinutes,
                                    endMinutes = endMinutes,
                                    location = location.trim(),
                                    note = note.trim(),
                                ),
                            )
                        },
                        enabled = startMinutes != null && endMinutes != null && endMinutes > startMinutes,
                    ) {
                        Text("保存本周调整")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableImportHubSheet(
    onDismiss: () -> Unit,
    onAcademic: () -> Unit,
    onOther: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("导入课表", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("推荐从教务网直接导入，图片识别作为备用方式。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Card(
                onClick = onAcademic,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Mint),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("1  教务网导入", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Forest)
                    Text("选择学校，在校方网页自行登录并打开课表，然后确认导入。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("推荐", color = Forest, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(onClick = onOther, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("2  图片导入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("选择课表截图，在本机离线识别后校对。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onOther, modifier = Modifier.fillMaxWidth()) { Text("其他方式：表格文本 / CSV") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableImportSheet(
    onDismiss: () -> Unit,
    onImport: (List<com.kexu.app.importer.CourseDraft>) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    var ocrStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var rawOcrText by rememberSaveable { mutableStateOf("") }
    var imageCourses by remember { mutableStateOf<List<CourseDraft>?>(null) }
    var imageWarnings by remember { mutableStateOf(emptyList<String>()) }
    val context = LocalContext.current
    val recognizer = remember {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }
    DisposableEffect(recognizer) {
        onDispose { recognizer.close() }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            ocrStatus = "正在本机识别图片…"
            runCatching { InputImage.fromFilePath(context, uri) }
                .onSuccess { image ->
                    recognizer.process(image)
                        .addOnSuccessListener { text ->
                            rawOcrText = text.text
                            val boxes = text.textBlocks.flatMap { block -> block.lines }.mapNotNull { line ->
                                line.boundingBox?.let { bounds ->
                                    OcrTextBox(
                                        text = line.text,
                                        left = bounds.left,
                                        top = bounds.top,
                                        right = bounds.right,
                                        bottom = bounds.bottom,
                                    )
                                }
                            }
                            val grid = TimetableImageGridParser.parse(boxes)
                            imageWarnings = grid.warnings
                            if (grid.courses.isNotEmpty()) {
                                imageCourses = grid.courses
                                input = ""
                            } else {
                                imageCourses = null
                                input = text.text
                            }
                            ocrStatus = if (text.text.isBlank()) {
                                "没有识别到文字，请换一张更清晰的截图"
                            } else if (grid.courses.isNotEmpty()) {
                                "检测到 ${grid.detectedDayColumns} 个星期列，生成 ${grid.courses.size} 门课程候选"
                            } else {
                                "未能可靠还原课表网格，已回退为 OCR 文字，请按模板检查并补齐列"
                            }
                        }
                        .addOnFailureListener { error ->
                            ocrStatus = "识别失败：${error.localizedMessage ?: "无法读取图片"}"
                        }
                }
                .onFailure { error ->
                    ocrStatus = "图片读取失败：${error.localizedMessage ?: "未知错误"}"
                }
        }
    }
    val result = remember(input) { TimetableTextParser.parse(input) }
    val importCourses = imageCourses ?: result.courses
    val importIssues = remember(importCourses) { analyzeImportIssues(importCourses) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("图片与表格导入", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "课表截图会先在本机尝试识别星期网格、节次、周次与课程块；无法可靠还原时再回退为文字校对。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("选择课表截图 · 离线识别") }
            ocrStatus?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            imageCourses?.let { courses ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("网格识别候选", fontWeight = FontWeight.Bold)
                        courses.take(8).forEach { course ->
                            Text(
                                "${course.name} · 周${course.day.shortChinese} " +
                                    "${minutesText(course.startMinutes)}-${minutesText(course.endMinutes)} · " +
                                    "${course.startWeek}-${course.endWeek}周",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (courses.size > 8) Text("还有 ${courses.size - 8} 门课程…", style = MaterialTheme.typography.bodySmall)
                        imageWarnings.take(3).forEach { warning ->
                            Text(warning, color = Coral, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "图片网格识别仍是试验功能，导入后可以在课程详情继续调整。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                TextButton(
                    onClick = {
                        imageCourses = null
                        input = rawOcrText
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("改用 OCR 文字手动校对") }
            } ?: OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    imageWarnings = emptyList()
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                placeholder = { Text("数据库,周一,08:00,09:40,A302,王老师,1,16,单周") },
                label = { Text("课表数据") },
                supportingText = {
                    Text("每行：课程、星期、开始、结束、教室、教师、起始周、结束周、周次规则")
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    imageCourses != null -> "识别到 ${imageCourses.orEmpty().size} 门课程 · ${imageWarnings.size} 条校对提示"
                    input.isBlank() -> "等待选择图片或粘贴表格"
                    else -> "识别到 ${result.courses.size} 门课程 · ${result.errors.size} 个问题"
                },
                color = if (imageCourses != null || result.errors.isEmpty()) Forest else Coral,
            )
            if (importIssues.duplicatePairs > 0 || importIssues.conflictPairs > 0) {
                Text(
                    "另有 ${importIssues.duplicatePairs} 组疑似重复、${importIssues.conflictPairs} 处同周时间冲突，请导入后检查。",
                    color = Coral,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (imageCourses == null) {
                result.errors.take(2).forEach {
                    Text(it, color = Coral, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                androidx.compose.material3.Button(
                    onClick = { onImport(importCourses) },
                    enabled = importCourses.isNotEmpty() && (imageCourses != null || result.errors.isEmpty()),
                ) { Text("确认导入") }
            }
        }
    }
}

private fun CourseEntity.isActiveInWeek(week: Int): Boolean {
    if (week !in startWeek..endWeek) return false
    if (weekMask != 0L) return week in 1..63 && weekMask and (1L shl (week - 1)) != 0L
    return when (runCatching { WeekPattern.valueOf(weekPattern) }.getOrDefault(WeekPattern.ALL)) {
        WeekPattern.ALL -> true
        WeekPattern.ODD -> week % 2 == 1
        WeekPattern.EVEN -> week % 2 == 0
    }
}

private val DayOfWeek.shortChinese: String
    get() = listOf("一", "二", "三", "四", "五", "六", "日")[value - 1]

private val sundayFirstDays = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
)

private val WeekPattern.chinese: String
    get() = when (this) {
        WeekPattern.ALL -> "每周"
        WeekPattern.ODD -> "单周"
        WeekPattern.EVEN -> "双周"
    }

private fun parseMinutes(value: String): Int? {
    val parts = value.split(":", "：")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun minutesText(value: Int): String = "%02d:%02d".format(value / 60, value % 60)

private fun weekRangeText(start: LocalDate, end: LocalDate): String =
    if (start.month == end.month) {
        "${start.monthValue}月${start.dayOfMonth}日–${end.dayOfMonth}日"
    } else {
        "${start.monthValue}月${start.dayOfMonth}日–${end.monthValue}月${end.dayOfMonth}日"
    }

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

private fun dateTimeText(value: Long): String = Instant.ofEpochMilli(value)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
