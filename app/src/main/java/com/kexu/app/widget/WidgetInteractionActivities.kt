package com.kexu.app.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import com.kexu.app.KexuApplication
import com.kexu.app.MainActivity
import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.nlp.ChineseTimeParser
import com.kexu.app.reminder.CourseOccurrence
import com.kexu.app.reminder.ReminderScheduler
import com.kexu.app.reminder.ScheduleCalculator
import com.kexu.app.ui.allocateTimetableColors
import com.kexu.app.ui.normalizedCourseName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Translucent, task-focused surfaces launched from a home-screen widget. The
 * launcher remains visible behind the activity, matching the interaction used
 * by mature task widgets without opening the app's normal navigation shell.
 */
class WidgetOverlayActivity : ComponentActivity() {
    private val app by lazy { application as KexuApplication }
    private var savingTask by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        WindowCompat.setDecorFitsSystemWindows(window, false)

        showRequestedOverlay()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (savingTask) return
        setIntent(intent)
        showRequestedOverlay()
    }

    private fun showRequestedOverlay() {
        when (intent.getStringExtra(EXTRA_MODE)) {
            MODE_DAY_AGENDA -> showDayAgenda(
                intent.getLongExtra(EXTRA_EPOCH_DAY, LocalDate.now().toEpochDay()),
                intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
            )
            MODE_TIMETABLE_MENU -> showTimetableMenu(
                intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
            )
            MODE_WIDGET_MENU -> showWidgetMenu(
                intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
            )
            MODE_TASK_LIST_CHOOSER -> showTaskListChooser(
                intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
            )
            else -> showQuickAdd()
        }
    }

    private fun showQuickAdd() {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        var courses by mutableStateOf<List<CourseEntity>>(emptyList())
        val filter = WidgetPreferences.settings(this, appWidgetId).selectedItemId
        val today = LocalDate.now()
        val defaultDay = when (filter) {
            COMPACT_FILTER_TODAY, COMPACT_FILTER_NEXT_7_DAYS -> today
            COMPACT_FILTER_TOMORROW -> today.plusDays(1)
            else -> null
        }
        val suggestedDueAt = defaultDay?.atTime(23, 59)?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        lifecycleScope.launch {
            courses = withContext(Dispatchers.IO) { app.repository.currentCourses() }
        }
        setContent {
            WidgetMaterial(widgetDark(appWidgetId)) {
                QuickAddOverlay(
                    courses = courses,
                    initialCourseId = filter.takeIf { it > 0L },
                    suggestedDueAt = suggestedDueAt,
                    saving = savingTask,
                    onDismiss = { if (!savingTask) finish() },
                    onExpand = {
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                                )
                                .putExtra(MainActivity.EXTRA_WIDGET_ACTION, MainActivity.ACTION_NEW_TASK),
                        )
                        finish()
                    },
                    onSave = { text, notes, manualDueAt, courseId, priority ->
                        saveQuickTask(text, notes, manualDueAt, courseId, priority, suggestedDueAt)
                    },
                )
            }
        }
    }

    private fun saveQuickTask(
        text: String,
        notes: String,
        manualDueAt: Long?,
        courseId: Long?,
        priority: Int,
        suggestedDueAt: Long?,
    ) {
        if (savingTask) return
        val parsed = ChineseTimeParser().parse(text)
        val title = parsed.cleanTitle.ifBlank { text.trim() }
        if (title.isBlank()) return
        savingTask = true
        lifecycleScope.launch {
          try {
            val dueAt = manualDueAt ?: parsed.dueAtMillis ?: suggestedDueAt
            val reminderAt = if (manualDueAt != null) manualDueAt else parsed.reminderAtMillis
            val task = TaskEntity(
                title = title,
                notes = notes.trim(),
                dueAt = dueAt,
                remindAt = reminderAt,
                courseId = courseId,
                priority = priority,
                recurrenceRule = parsed.recurrenceRule,
                isCompleted = false,
                completedAt = null,
            )
            val id = withContext(Dispatchers.IO) { app.repository.saveTask(task) }
            // Once Room commits, a scheduling/refresh failure must not be presented as
            // a failed insertion (which would prompt the user to submit it again).
            runCatching { ReminderScheduler(this@WidgetOverlayActivity).scheduleTask(task.copy(id = id)) }
            runCatching { CompactTaskWidget().updateAll(this@WidgetOverlayActivity) }
            runCatching { TodayWidget().updateAll(this@WidgetOverlayActivity) }
            Toast.makeText(this@WidgetOverlayActivity, "任务已添加", Toast.LENGTH_SHORT).show()
            finish()
          } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
          } catch (_: Exception) {
            Toast.makeText(this@WidgetOverlayActivity, "保存失败，请重试", Toast.LENGTH_SHORT).show()
          } finally {
            savingTask = false
          }
        }
    }

    private fun showDayAgenda(epochDay: Long, appWidgetId: Int) {
        val date = LocalDate.ofEpochDay(epochDay)
        var items by mutableStateOf<List<CourseOccurrence>?>(null)
        lifecycleScope.launch {
            items = withContext(Dispatchers.IO) { occurrencesForDate(date) }
        }
        setContent {
            WidgetMaterial(widgetDark(appWidgetId)) {
                DayAgendaOverlay(
                    date = date,
                    occurrences = items,
                    onDismiss = ::finish,
                    onCourseClick = {
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                                )
                                .putExtra(MainActivity.EXTRA_WIDGET_ACTION, MainActivity.ACTION_OPEN_TIMETABLE),
                        )
                        finish()
                    },
                )
            }
        }
    }

    private suspend fun occurrencesForDate(date: LocalDate): List<CourseOccurrence> {
        val zone = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val settings = app.settingsRepository.snapshot()
        val courses = app.repository.currentCourses()
        val slots = app.repository.currentSlots()
        val overrides = app.repository.currentOverrides().groupBy { it.slotId }
        return courses.flatMap { course ->
            slots.filter { it.courseId == course.id }.flatMap { slot ->
                ScheduleCalculator.occurrences(
                    course = course,
                    slot = slot,
                    semesterStartMillis = settings.semesterStartMillis,
                    fromMillis = startMillis - 1,
                    days = 1,
                    zoneId = zone,
                    overrides = overrides[slot.id].orEmpty(),
                )
            }
        }.filter {
            Instant.ofEpochMilli(it.startsAtMillis).atZone(zone).toLocalDate() == date
        }.sortedBy { it.startsAtMillis }
    }

    private fun showTimetableMenu(appWidgetId: Int) {
        setContent {
            WidgetMaterial(widgetDark(appWidgetId)) {
                TimetableMenuOverlay(
                    onDismiss = ::finish,
                    onRefresh = {
                        lifecycleScope.launch {
                            TimetableCache.refresh(application as KexuApplication)
                            TimetableWidget().updateAll(this@WidgetOverlayActivity)
                            Toast.makeText(this@WidgetOverlayActivity, "课表已刷新", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    },
                    onBackToCurrentWeek = {
                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            val current = WidgetPreferences.settings(this, appWidgetId)
                            WidgetPreferences.save(
                                this,
                                appWidgetId,
                                current.copy(
                                    timetableWeekOffset = 0,
                                    timetableNavigationEpochDay = LocalDate.now().toEpochDay(),
                                ),
                            )
                        }
                        lifecycleScope.launch {
                            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                updateAppWidgetState(
                                    this@WidgetOverlayActivity,
                                    widgetGlanceId(this@WidgetOverlayActivity, appWidgetId),
                                ) { state -> state[TimetableWeekOffsetStateKey] = 0 }
                            }
                            TimetableWidget().updateAll(this@WidgetOverlayActivity)
                            finish()
                        }
                    },
                    onSettings = {
                        startActivity(
                            Intent(this, WidgetConfigActivity::class.java)
                                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                        )
                        finish()
                    },
                )
            }
        }
    }

    private fun showWidgetMenu(appWidgetId: Int) {
        setContent {
            WidgetMaterial(widgetDark(appWidgetId)) {
                WidgetMenuOverlay(
                    showCompleted = WidgetPreferences.settings(this, appWidgetId).showCompleted,
                    onToggleCompleted = {
                        val current = WidgetPreferences.settings(this, appWidgetId)
                        WidgetPreferences.save(this, appWidgetId, current.copy(showCompleted = !current.showCompleted))
                        lifecycleScope.launch {
                            updateAppWidgetState(this@WidgetOverlayActivity, widgetGlanceId(this@WidgetOverlayActivity, appWidgetId)) { state ->
                                state[CompactMenuVisibleStateKey] = false
                            }
                            updateAllHomeWidgets(this@WidgetOverlayActivity)
                            finish()
                        }
                    },
                    onDismiss = ::finish,
                    onRefresh = {
                        lifecycleScope.launch {
                            updateAllHomeWidgets(this@WidgetOverlayActivity)
                            Toast.makeText(this@WidgetOverlayActivity, "小组件已刷新", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    },
                    onSettings = {
                        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            startActivity(
                                Intent(this, WidgetConfigActivity::class.java)
                                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                            )
                        }
                        finish()
                    },
                )
            }
        }
    }

    private fun showTaskListChooser(appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        var courses by mutableStateOf<List<CourseEntity>>(emptyList())
        lifecycleScope.launch {
            courses = withContext(Dispatchers.IO) { app.repository.currentCourses() }
        }
        setContent {
            WidgetMaterial {
                TaskListChooserOverlay(
                    selectedId = WidgetPreferences.settings(this, appWidgetId).selectedItemId,
                    courses = courses,
                    onDismiss = ::finish,
                    onSelected = { selectedId ->
                        val current = WidgetPreferences.settings(this, appWidgetId)
                        WidgetPreferences.save(
                            this,
                            appWidgetId,
                            current.copy(selectedItemId = selectedId),
                        )
                        lifecycleScope.launch {
                            val glanceId = widgetGlanceId(this@WidgetOverlayActivity, appWidgetId)
                            updateAppWidgetState(this@WidgetOverlayActivity, glanceId) { state ->
                                state[CompactMenuVisibleStateKey] = false
                            }
                            CompactTaskWidget().update(this@WidgetOverlayActivity, glanceId)
                            finish()
                        }
                    },
                )
            }
        }
    }


    private fun widgetDark(appWidgetId: Int): Boolean =
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        } else {
            widgetIsDark(this, appWidgetId)
        }

    companion object {
        const val EXTRA_MODE = "com.kexu.app.widget.extra.MODE"
        const val EXTRA_EPOCH_DAY = "com.kexu.app.widget.extra.EPOCH_DAY"
        const val MODE_QUICK_ADD = "quick_add"
        const val MODE_DAY_AGENDA = "day_agenda"
        const val MODE_TIMETABLE_MENU = "timetable_menu"
        const val MODE_WIDGET_MENU = "widget_menu"
        const val MODE_TASK_LIST_CHOOSER = "task_list_chooser"
    }
}

@Composable
private fun WidgetMaterial(darkOverride: Boolean? = null, content: @Composable () -> Unit) {
    val dark = darkOverride ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) {
            darkColorScheme(
                primary = Color(0xFF5B7CFF),
                surface = Color(0xFF242426),
                background = Color.Transparent,
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF4F6EEB),
                surface = Color(0xFFFCFCFF),
                background = Color.Transparent,
            )
        },
        content = content,
    )
}

@Composable
private fun QuickAddOverlay(
    courses: List<CourseEntity>,
    initialCourseId: Long?,
    suggestedDueAt: Long?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onExpand: () -> Unit,
    onSave: (String, String, Long?, Long?, Int) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    var input by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var manualDueAt by remember { mutableStateOf<Long?>(null) }
    var priority by remember { mutableStateOf(0) }
    var courseId by remember { mutableStateOf(initialCourseId) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showCourseMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val parser = remember { ChineseTimeParser() }
    val parsed = remember(input) { parser.parse(input) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf(String::isNotBlank)
            ?.let { recognized -> input = recognized }
    }

    LaunchedEffect(Unit) {
        delay(140)
        focusRequester.requestFocus()
        keyboard?.show()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .38f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 28.dp)
                .fillMaxWidth()
                .heightIn(min = 196.dp, max = 300.dp)
                .clickable(enabled = false) { },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        placeholder = { Text("准备做什么？") },
                        textStyle = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                    IconButton(onClick = onExpand) {
                        Icon(Icons.Outlined.ArrowOutward, contentDescription = "打开完整编辑")
                    }
                }
                TextField(
                    value = notes,
                    maxLines = 2,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("描述") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                val scheduleText = when {
                    manualDueAt != null -> formatQuickTime(manualDueAt!!)
                    parsed.dueAtMillis != null -> formatQuickTime(parsed.dueAtMillis)
                    suggestedDueAt != null -> formatQuickTime(suggestedDueAt)
                    else -> null
                }
                if (scheduleText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = .11f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            scheduleText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .10f))
                Row(
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = "日期",
                            tint = if (scheduleText != null) MaterialTheme.colorScheme.primary else Color(0xFF6E7078),
                        )
                    }
                    Box {
                        IconButton(onClick = { showPriorityMenu = true }) {
                            Icon(
                                Icons.Outlined.Flag,
                                contentDescription = "优先级",
                                tint = widgetPriorityColor(priority),
                            )
                        }
                        DropdownMenu(
                            expanded = showPriorityMenu,
                            onDismissRequest = { showPriorityMenu = false },
                        ) {
                            listOf(3 to "高优先级", 2 to "中优先级", 1 to "低优先级", 0 to "无优先级")
                                .forEach { (level, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        leadingIcon = {
                                            Icon(Icons.Outlined.Flag, contentDescription = null, tint = widgetPriorityColor(level))
                                        },
                                        trailingIcon = {
                                            if (priority == level) Icon(Icons.Filled.Check, contentDescription = null)
                                        },
                                        onClick = {
                                            priority = level
                                            showPriorityMenu = false
                                        },
                                    )
                                }
                        }
                    }
                    IconButton(onClick = { input += " #" }) {
                        Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = "标签")
                    }
                    Box {
                        IconButton(onClick = { showCourseMenu = true }) {
                            Icon(
                                Icons.Outlined.Inbox,
                                contentDescription = "清单",
                                tint = if (courseId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(
                            expanded = showCourseMenu,
                            onDismissRequest = { showCourseMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("收集箱") },
                                trailingIcon = { if (courseId == null) Icon(Icons.Filled.Check, contentDescription = null) },
                                onClick = {
                                    courseId = null
                                    showCourseMenu = false
                                },
                            )
                            courses.distinctBy { it.name.normalizedCourseName() }.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text(course.name) },
                                    trailingIcon = { if (courseId == course.id) Icon(Icons.Filled.Check, contentDescription = null) },
                                    onClick = {
                                        courseId = course.id
                                        showCourseMenu = false
                                    },
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Outlined.MoreHoriz, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("清空内容") },
                                onClick = {
                                    input = ""
                                    notes = ""
                                    manualDueAt = null
                                    priority = 0
                                    courseId = null
                                    showMoreMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("打开完整编辑") },
                                onClick = {
                                    showMoreMenu = false
                                    onExpand()
                                },
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (input.isBlank()) {
                        IconButton(
                            modifier = Modifier.size(44.dp),
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                }
                                runCatching { voiceLauncher.launch(intent) }
                                    .onFailure {
                                        Toast.makeText(context, "当前设备没有可用的语音输入", Toast.LENGTH_SHORT).show()
                                    }
                            },
                        ) {
                            Icon(Icons.Outlined.Mic, contentDescription = "语音输入", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ) {
                            IconButton(
                                enabled = !saving,
                                onClick = {
                                    keyboard?.hide()
                                    onSave(input, notes, manualDueAt, courseId, priority)
                                },
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = if (saving) "保存中" else "添加", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (manualDueAt ?: parsed.dueAtMillis ?: suggestedDueAt ?: System.currentTimeMillis()),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis ?: return@TextButton
                    val date = Instant.ofEpochMilli(selected).atZone(ZoneId.of("UTC")).toLocalDate()
                    val time = parsed.dueAtMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }
                        ?: java.time.LocalTime.of(18, 0)
                    manualDueAt = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun WidgetMenuOverlay(
    showCompleted: Boolean,
    onToggleCompleted: () -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .12f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            modifier = Modifier.statusBarsPadding().padding(top = 76.dp, end = 18.dp).width(190.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                MenuLine("刷新", onRefresh)
                MenuLine(if (showCompleted) "隐藏已完成任务" else "显示已完成任务", onToggleCompleted)
                MenuLine("设置", onSettings)
            }
        }
    }
}

private data class TaskListChoice(
    val id: Long,
    val title: String,
    val icon: ImageVector,
)

@Composable
private fun TaskListChooserOverlay(
    selectedId: Long,
    courses: List<CourseEntity>,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val choices = listOf(
        TaskListChoice(COMPACT_FILTER_ALL, "所有", Icons.Outlined.Checklist),
        TaskListChoice(COMPACT_FILTER_TODAY, "今天", Icons.Outlined.Today),
        TaskListChoice(COMPACT_FILTER_TOMORROW, "明天", Icons.Outlined.WbSunny),
        TaskListChoice(COMPACT_FILTER_NEXT_7_DAYS, "最近7天", Icons.Outlined.DateRange),
        TaskListChoice(COMPACT_FILTER_INBOX, "收集箱", Icons.Outlined.Inbox),
    ) + courses.distinctBy { it.name.normalizedCourseName() }.map { course ->
        TaskListChoice(course.id, course.name, Icons.Outlined.School)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .32f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(.90f)
                .heightIn(max = 640.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 20.dp,
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = 18.dp, bottom = 12.dp),
            ) {
                Text(
                    "选择清单",
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                choices.forEach { choice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(choice.id) }
                            .padding(horizontal = 22.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            choice.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .48f),
                            modifier = Modifier.size(25.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(choice.title, modifier = Modifier.weight(1f), fontSize = 18.sp)
                        if (choice.id == selectedId) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "当前清单",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("取消") }
                }
            }
        }
    }
}

@Composable
private fun DayAgendaOverlay(
    date: LocalDate,
    occurrences: List<CourseOccurrence>?,
    onDismiss: () -> Unit,
    onCourseClick: (CourseOccurrence) -> Unit,
) {
    val courseColors = remember(occurrences) {
        allocateTimetableColors(occurrences.orEmpty().map { it.course.name })
    }
    BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(enabled = false) { },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp,
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 18.dp).verticalScroll(rememberScrollState()),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "关闭") }
                }
                when {
                    occurrences == null -> Text("正在读取课表…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    occurrences.isEmpty() -> {
                        Text("这一天没有课程", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
                    }
                    else -> occurrences.forEach { occurrence ->
                        val courseColor = courseColors[
                            occurrence.course.name.normalizedCourseName()
                        ] ?: Color(occurrence.course.colorArgb)
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                                .clickable { onCourseClick(occurrence) },
                            shape = RoundedCornerShape(16.dp),
                            color = courseColor.copy(alpha = .18f),
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.width(5.dp).height(52.dp)
                                        .background(courseColor, RoundedCornerShape(4.dp)),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(occurrence.course.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    val detail = listOf(
                                        occurrence.course.location,
                                        occurrence.course.teacher,
                                    ).filter { it.isNotBlank() }.joinToString(" · ")
                                    if (detail.isNotBlank()) Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                                Text(
                                    "%02d:%02d".format(
                                        occurrence.slot.startMinutes / 60,
                                        occurrence.slot.startMinutes % 60,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TimetableMenuOverlay(
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onBackToCurrentWeek: () -> Unit,
    onSettings: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .18f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            modifier = Modifier.padding(top = 72.dp, end = 18.dp).width(230.dp).clickable(enabled = false) { },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 18.dp,
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                MenuLine("刷新课表", onRefresh)
                MenuLine("回到本周", onBackToCurrentWeek)
                MenuLine("显示范围与透明度", onSettings)
            }
        }
    }
}

@Composable
private fun MenuLine(title: String, onClick: () -> Unit) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        fontSize = 16.sp,
    )
}

private fun formatQuickTime(epochMillis: Long): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val day = when (time.toLocalDate()) {
        today -> "今天"
        today.plusDays(1) -> "明天"
        else -> time.format(DateTimeFormatter.ofPattern("M月d日"))
    }
    return "$day ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

private fun widgetPriorityColor(priority: Int): Color = when (priority) {
    3 -> Color(0xFFE53935)
    2 -> Color(0xFFFF9800)
    1 -> Color(0xFF4285F4)
    else -> Color(0xFF8B8D94)
}

internal fun widgetOverlayIntent(
    context: android.content.Context,
    mode: String,
    identity: String,
    configure: Intent.() -> Unit = {},
): Intent = Intent(context, WidgetOverlayActivity::class.java).apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    putExtra(WidgetOverlayActivity.EXTRA_MODE, mode)
    data = Uri.parse("kexu://widget-overlay/$mode/$identity")
    configure()
}
