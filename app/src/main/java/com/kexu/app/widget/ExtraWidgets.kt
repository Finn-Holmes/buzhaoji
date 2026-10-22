package com.kexu.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.text.TextDecoration
import androidx.glance.semantics.semantics
import androidx.glance.semantics.contentDescription
import androidx.glance.unit.ColorProvider
import com.kexu.app.KexuApplication
import com.kexu.app.MainActivity
import com.kexu.app.R
import com.kexu.app.data.model.CourseEntity
import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.reminder.CourseOccurrence
import com.kexu.app.reminder.ReminderScheduler
import com.kexu.app.reminder.ScheduleCalculator
import com.kexu.app.ui.allocateTimetableColors
import com.kexu.app.ui.normalizedCourseName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private data class WidgetPalette(
    val primary: ColorProvider,
    val secondary: ColorProvider,
    val accent: ColorProvider,
    val divider: ColorProvider,
)

private fun palette(dark: Boolean) = if (dark) {
    WidgetPalette(
        primary = ColorProvider(Color.White),
        secondary = ColorProvider(Color(0xFF929399)),
        accent = ColorProvider(Color(0xFF4D73F6)),
        divider = ColorProvider(Color(0xFF3B3C3F)),
    )
} else {
    WidgetPalette(
        primary = ColorProvider(Color(0xFF202124)),
        secondary = ColorProvider(Color(0xFF74777F)),
        accent = ColorProvider(Color(0xFF496FF5)),
        divider = ColorProvider(Color(0xFFE1E2E6)),
    )
}

private fun openApp(context: Context, action: String? = null) =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply {
                action?.let { putExtra(MainActivity.EXTRA_WIDGET_ACTION, it) }
            },
    )

private fun openWidgetMenu(context: Context, id: GlanceId) =
    actionStartActivity(
        widgetOverlayIntent(
            context = context,
            mode = WidgetOverlayActivity.MODE_WIDGET_MENU,
            identity = WidgetPreferences.appWidgetId(context, id).toString(),
        ) {
            putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                WidgetPreferences.appWidgetId(context, id),
            )
        },
    )

private fun editWidgetTask(context: Context, taskId: Long) =
    actionStartActivity(
        Intent(context, MainActivity::class.java)
            .setData(Uri.parse("kexu://widget/task/$taskId/edit"))
            .putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    )

private fun taskTime(task: TaskEntity, zoneId: ZoneId): String =
    task.dueAt?.let {
        Instant.ofEpochMilli(it).atZone(zoneId).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.orEmpty()

private fun taskDate(task: TaskEntity, zoneId: ZoneId): String =
    task.dueAt?.let {
        val date = Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate()
        when (date) {
            LocalDate.now(zoneId) -> taskTime(task, zoneId).ifBlank { "今天" }
            LocalDate.now(zoneId).plusDays(1) -> "明天"
            else -> date.format(DateTimeFormatter.ofPattern("M月d日"))
        }
    }.orEmpty()

class CompactTaskWidget : KexuGlanceWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KexuApplication
        val initialTasks = app.repository.currentTasks()
        val initialCourses = app.repository.currentCourses()
        provideContent {
            val tasks by app.repository.tasks.collectAsState(initialTasks)
            val courses by app.repository.courses.collectAsState(initialCourses)
            // Settings may change while the Glance composition is still alive.
            currentState<Preferences>()
            val preferences = WidgetPreferences.settings(context, id)
            CompactTaskContent(
                context,
                id,
                tasks,
                courses,
                preferences.selectedItemId,
                preferences.showCompleted,
                widgetIsDark(context, id),
            )
        }
    }
}

@Composable
private fun CompactTaskContent(
    context: Context,
    id: GlanceId,
    tasks: List<TaskEntity>,
    courses: List<CourseEntity>,
    selectedFilter: Long,
    showCompleted: Boolean,
    dark: Boolean,
) {
    val state = currentState<Preferences>()
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val appWidgetId = WidgetPreferences.appWidgetId(context, id)
    val completingId = state[CompletingTaskIdKey].takeIf {
        (state[CompletingTaskUntilKey] ?: 0L) > System.currentTimeMillis()
    }
    val size = LocalSize.current
    // Room is the single source of truth. Keeping a second completed-state cache here caused
    // the widget and the app task list to disagree after either side changed a task.
    val filteredTasks = tasks.filter { task ->
        val taskDay = task.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        when {
            selectedFilter == COMPACT_FILTER_TODAY -> taskDay == today
            selectedFilter == COMPACT_FILTER_TOMORROW -> taskDay == today.plusDays(1)
            selectedFilter == COMPACT_FILTER_NEXT_7_DAYS ->
                taskDay != null && !taskDay.isBefore(today) && taskDay <= today.plusDays(6)
            selectedFilter == COMPACT_FILTER_INBOX -> task.courseId == null
            selectedFilter > 0L -> {
                val selectedName = courses.firstOrNull { it.id == selectedFilter }?.name?.normalizedCourseName()
                task.courseId == selectedFilter || (selectedName != null && courses.any {
                    it.id == task.courseId && it.name.normalizedCourseName() == selectedName
                })
            }
            else -> true
        }
    }.filter { showCompleted || !it.isCompleted || it.id == completingId }
        .sortedWith(compareBy<TaskEntity> { it.isCompleted && it.id != completingId }
            .thenBy { it.dueAt == null }.thenBy { it.dueAt }.thenByDescending { it.createdAt })
    val visibleTasks = filteredTasks
    val filterTitle = when (selectedFilter) {
        COMPACT_FILTER_TODAY -> "今天"
        COMPACT_FILTER_TOMORROW -> "明天"
        COMPACT_FILTER_NEXT_7_DAYS -> "最近7天"
        COMPACT_FILTER_INBOX -> "收集箱"
        else -> courses.firstOrNull { it.id == selectedFilter }?.name ?: "所有"
    }
    val colors = palette(dark)
    val listChooser = actionStartActivity(
        widgetOverlayIntent(
            context,
            WidgetOverlayActivity.MODE_TASK_LIST_CHOOSER,
            appWidgetId.toString(),
        ) {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        },
    )
    val quickAdd = actionStartActivity(
        widgetOverlayIntent(
            context = context,
            mode = WidgetOverlayActivity.MODE_QUICK_ADD,
            identity = appWidgetId.toString(),
        ) {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        },
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(widgetSurface(context, id, darkColor = Color(0xFF232323)))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(
                GlanceModifier.fillMaxWidth().height(34.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight().clickable(listChooser),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "$filterTitle · ${filteredTasks.size}",
                        modifier = GlanceModifier.defaultWeight(),
                        maxLines = 1,
                        style = TextStyle(
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Image(
                        provider = ImageProvider(R.drawable.widget_ic_expand_more),
                        contentDescription = "切换清单",
                        modifier = GlanceModifier.width(12.dp).height(16.dp),
                        colorFilter = ColorFilter.tint(colors.secondary),
                    )
                }
                Image(
                    provider = ImageProvider(R.drawable.widget_ic_add_pressable),
                    contentDescription = "添加任务",
                    modifier = GlanceModifier.width(30.dp).fillMaxHeight().clickable(quickAdd).padding(6.dp),
                    colorFilter = ColorFilter.tint(colors.primary),
                )
                Image(
                    provider = ImageProvider(R.drawable.widget_ic_more_pressable),
                    contentDescription = "更多",
                    modifier = GlanceModifier.width(26.dp).fillMaxHeight().clickable(
                        openWidgetMenu(context, id),
                    ).padding(5.dp),
                    colorFilter = ColorFilter.tint(colors.primary),
                )
            }
            if (visibleTasks.isEmpty()) {
                Text(
                    "没有任务",
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 16.dp),
                    style = TextStyle(
                        color = colors.secondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            } else {
                LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
                  items(visibleTasks, itemId = { it.id }) { task ->
                    val label = compactTaskDeadline(task, today, zone)
                    val overdue = task.dueAt?.let { it < System.currentTimeMillis() } == true && !task.isCompleted
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CheckBox(
                            checked = task.isCompleted,
                            onCheckedChange = actionRunCallback<ToggleTaskAction>(
                                actionParametersOf(
                                    TaskIdKey to task.id,
                                    TaskVersionKey to task.widgetVersion(),
                                    TaskCompletedTargetKey to !task.isCompleted,
                                ),
                            ),
                            modifier = GlanceModifier.width(30.dp).height(32.dp).semantics {
                                contentDescription = if (task.isCompleted) "恢复任务：${task.title}" else "完成任务：${task.title}"
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.accent,
                                uncheckedColor = colors.secondary,
                            ),
                        )
                        // Keep editing separate from the checkbox's completion hit area.
                        Row(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                                .clickable(editWidgetTask(context, task.id))
                                .semantics { contentDescription = "编辑任务：${task.title}" },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Text(
                            task.title,
                            modifier = GlanceModifier.defaultWeight(),
                            maxLines = 1,
                            style = TextStyle(
                                color = if (task.isCompleted) colors.secondary else colors.primary,
                                fontSize = 12.sp,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            ),
                        )
                        if (label.isNotEmpty()) {
                            Spacer(GlanceModifier.width(4.dp))
                            Text(
                                label,
                                modifier = GlanceModifier.width(if (size.width.value < 160f) 38.dp else 46.dp),
                                maxLines = 1,
                                style = TextStyle(
                                    color = if (overdue) {
                                        ColorProvider(Color(0xFFFF5252))
                                    } else {
                                        colors.accent
                                    },
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.End,
                                ),
                            )
                        }
                        }
                    }
                  }
                }
            }
        }
    }
}

class CompactTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactTaskWidget()
}

class TimetableWidget : KexuGlanceWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KexuApplication
        val initial = TimetableCache.initial(app)
        provideContent {
            val snapshot by produceState(initial, app) {
                TimetableCache.observe(app).collect { value = it }
            }
            val preferencesFlow = remember(context, id) { WidgetPreferences.observe(context, id) }
            val widgetSettings by preferencesFlow.collectAsState(WidgetPreferences.settings(context, id))
            val zone = snapshot.zone
            val courseColors = remember(snapshot.data.courses) {
                allocateTimetableColors(snapshot.data.courses.map { it.name })
            }
            val todayEpochDay = LocalDate.now(zone).toEpochDay()
            val weekOffset = if (
                widgetSettings.timetableNavigationEpochDay == todayEpochDay
            ) {
                widgetSettings.timetableWeekOffset
            } else {
                0
            }
            val weekStart = ScheduleCalculator.startOfWeek(LocalDate.now(zone))
                .plusWeeks(weekOffset.toLong())
            val occurrences = remember(snapshot, weekStart,
                widgetSettings.timetableStartLesson, widgetSettings.timetableEndLesson) {
                snapshot.week(weekStart).filter { occurrence ->
                    occurrenceInLessonRange(occurrence, widgetSettings.timetableStartLesson,
                        widgetSettings.timetableEndLesson)
                }
            }
            LaunchedEffect(snapshot, weekStart) {
                TimetableCache.warm(app, snapshot, weekStart)
            }
            val week = ScheduleCalculator.currentWeek(
                snapshot.data.semesterStart,
                weekStart,
                zone,
            )
            TimetableContent(
                context = context,
                id = id,
                week = week,
                weekStart = weekStart,
                occurrences = occurrences,
                courseColors = courseColors,
                startLesson = widgetSettings.timetableStartLesson,
                endLesson = widgetSettings.timetableEndLesson,
                // TickTick's calendar widget is dark by default. FOLLOW_SYSTEM also uses the
                // high-contrast dark presentation; LIGHT remains available when chosen explicitly.
                dark = widgetSettings.theme != WidgetTheme.LIGHT,
            )
        }
    }
}

private val timetableLessonRanges = listOf(
    8 * 60 to 8 * 60 + 45,
    8 * 60 + 50 to 9 * 60 + 35,
    9 * 60 + 55 to 10 * 60 + 40,
    10 * 60 + 45 to 11 * 60 + 30,
    11 * 60 + 35 to 12 * 60 + 20,
    14 * 60 to 14 * 60 + 45,
    14 * 60 + 50 to 15 * 60 + 35,
    15 * 60 + 55 to 16 * 60 + 40,
    16 * 60 + 45 to 17 * 60 + 30,
    19 * 60 to 19 * 60 + 45,
    19 * 60 + 50 to 20 * 60 + 35,
    20 * 60 + 40 to 21 * 60 + 25,
)

private fun occurrenceInLessonRange(
    occurrence: CourseOccurrence,
    startLesson: Int,
    endLesson: Int,
): Boolean {
    val range = occurrenceLessonRange(occurrence)
    return range.last >= startLesson && range.first <= endLesson
}

private fun occurrenceLessonRange(occurrence: CourseOccurrence): IntRange {
    val startIndex = timetableLessonRanges.indices.minByOrNull { index ->
        abs(timetableLessonRanges[index].first - occurrence.slot.startMinutes)
    } ?: 0
    val endIndex = timetableLessonRanges.indices.minByOrNull { index ->
        abs(timetableLessonRanges[index].second - occurrence.slot.endMinutes)
    }?.coerceAtLeast(startIndex) ?: startIndex
    return (startIndex + 1)..(endIndex + 1)
}

private data class WidgetCourseBlock(
    val occurrence: CourseOccurrence,
    val startLesson: Int,
    val endLesson: Int,
) {
    val lessonSpan: Int get() = endLesson - startLesson + 1
}

private fun timetableBlocks(
    occurrences: List<CourseOccurrence>,
    startLesson: Int,
    endLesson: Int,
): List<WidgetCourseBlock> = occurrences.mapNotNull { occurrence ->
    val range = occurrenceLessonRange(occurrence)
    val clippedStart = maxOf(range.first, startLesson)
    val clippedEnd = minOf(range.last, endLesson)
    if (clippedStart > clippedEnd) null else {
        WidgetCourseBlock(occurrence, clippedStart, clippedEnd)
    }
}.sortedWith(compareBy<WidgetCourseBlock> { it.startLesson }.thenBy { it.occurrence.startsAtMillis })

private fun timetableMonthLabel(weekStart: LocalDate): String {
    val weekEnd = weekStart.plusDays(6)
    return if (weekStart.monthValue == weekEnd.monthValue) {
        "${weekStart.monthValue}月"
    } else {
        "${weekStart.monthValue}月 - ${weekEnd.monthValue}月"
    }
}

@Composable
private fun TimetableContent(
    context: Context,
    id: GlanceId,
    week: Int,
    weekStart: LocalDate,
    occurrences: List<CourseOccurrence>,
    courseColors: Map<String, Color>,
    startLesson: Int,
    endLesson: Int,
    dark: Boolean,
) {
    val colors = palette(dark)
    val zone = ZoneId.systemDefault()
    val appWidgetId = WidgetPreferences.appWidgetId(context, id)
    val today = LocalDate.now(zone)
    val todayAgenda = actionStartActivity(
        widgetOverlayIntent(
            context,
            WidgetOverlayActivity.MODE_DAY_AGENDA,
            "$appWidgetId/${today.toEpochDay()}",
        ) {
            putExtra(WidgetOverlayActivity.EXTRA_EPOCH_DAY, today.toEpochDay())
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        },
    )
    val size = LocalSize.current
    val visibleStart = startLesson.coerceIn(1, timetableLessonRanges.size)
    val visibleEnd = endLesson.coerceIn(visibleStart, timetableLessonRanges.size)
    val blocksByDay = remember(occurrences, zone, visibleStart, visibleEnd) {
        occurrences.groupBy { Instant.ofEpochMilli(it.startsAtMillis).atZone(zone).toLocalDate() }
            .mapValues { (_, daily) -> timetableBlocks(daily, visibleStart, visibleEnd) }
    }
    val lessonCount = visibleEnd - visibleStart + 1
    // LocalSize includes the widget's outer padding. Seven direct children keep the
    // Glance Row below RemoteViews' child limit and make every weekday consume the
    // same amount of the available width.
    val dayWidth = ((size.width.value - 18f) / 7f).coerceAtLeast(1f).dp
    val timelineHeight = (size.height.value - 86f).coerceAtLeast(1f)
    val lessonHeight = (timelineHeight / lessonCount).dp
    val gridLine = ColorProvider(
        if (dark) Color.White.copy(alpha = .065f) else Color.Black.copy(alpha = .045f),
    )
    val isCurrentWeek = weekStart == ScheduleCalculator.startOfWeek(today)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(
                widgetSurface(
                    context,
                    id,
                    darkColor = Color(0xFF232323),
                    lightColor = Color(0xFFFBFBFC),
                    darkOverride = dark,
                ),
            )
            .padding(horizontal = 9.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.widget_ic_calendar_pressable),
                contentDescription = "今天的日程",
                modifier = GlanceModifier.width(34.dp).height(36.dp)
                    .clickable(todayAgenda).padding(8.dp),
                colorFilter = ColorFilter.tint(colors.primary),
            )
            Image(
                provider = ImageProvider(R.drawable.widget_ic_chevron_left_pressable),
                contentDescription = "上一周",
                modifier = GlanceModifier.width(38.dp).height(36.dp)
                    .clickable(
                        actionRunCallback<ShiftTimetableWeekAction>(
                            actionParametersOf(
                                WeekDeltaKey to -1,
                                WidgetIdKey to appWidgetId,
                            ),
                        ),
                    ).padding(8.dp),
                colorFilter = ColorFilter.tint(colors.primary),
            )
            Column(
                modifier = GlanceModifier.defaultWeight().height(36.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                Text(
                    timetableMonthLabel(weekStart),
                    style = TextStyle(
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                    ),
                )
                Text(
                    "第 ${week.coerceAtLeast(1)} 周${if (isCurrentWeek) " · 本周" else ""}",
                    style = TextStyle(
                        color = colors.secondary,
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp,
                    ),
                )
            }
            Image(
                provider = ImageProvider(R.drawable.widget_ic_chevron_right_pressable),
                contentDescription = "下一周",
                modifier = GlanceModifier.width(38.dp).height(36.dp)
                    .clickable(
                        actionRunCallback<ShiftTimetableWeekAction>(
                            actionParametersOf(
                                WeekDeltaKey to 1,
                                WidgetIdKey to appWidgetId,
                            ),
                        ),
                    ).padding(8.dp),
                colorFilter = ColorFilter.tint(colors.primary),
            )
            Image(
                provider = ImageProvider(R.drawable.widget_ic_add_pressable),
                contentDescription = "添加任务",
                modifier = GlanceModifier.width(36.dp).height(36.dp).clickable(
                    actionStartActivity(
                        widgetOverlayIntent(
                            context,
                            WidgetOverlayActivity.MODE_QUICK_ADD,
                            appWidgetId.toString(),
                        ) {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        },
                    ),
                ).padding(7.dp),
                colorFilter = ColorFilter.tint(colors.primary),
            )
            Image(
                provider = ImageProvider(R.drawable.widget_ic_more_pressable),
                contentDescription = "更多",
                modifier = GlanceModifier.width(30.dp).height(36.dp).clickable(
                    actionStartActivity(
                        widgetOverlayIntent(
                            context,
                            WidgetOverlayActivity.MODE_TIMETABLE_MENU,
                            appWidgetId.toString(),
                        ) {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        },
                    ),
                ).padding(6.dp),
                colorFilter = ColorFilter.tint(colors.primary),
            )
        }
        Row(GlanceModifier.fillMaxWidth().height(34.dp)) {
            (0L..6L).forEach { offset ->
                val day = weekStart.plusDays(offset)
                val isToday = day == today
                val dayAgenda = actionStartActivity(
                    widgetOverlayIntent(
                        context,
                        WidgetOverlayActivity.MODE_DAY_AGENDA,
                        "$appWidgetId/${day.toEpochDay()}",
                    ) {
                        putExtra(WidgetOverlayActivity.EXTRA_EPOCH_DAY, day.toEpochDay())
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    },
                )
                Column(
                    modifier = GlanceModifier.width(dayWidth).fillMaxHeight().clickable(dayAgenda),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Text(
                        "日一二三四五六"[offset.toInt()].toString(),
                        modifier = GlanceModifier.height(13.dp),
                        style = TextStyle(color = colors.secondary, fontSize = 10.sp),
                    )
                    Text(
                        day.dayOfMonth.toString(),
                        modifier = GlanceModifier
                            .width(24.dp)
                            .height(21.dp)
                            .cornerRadius(11.dp)
                            .background(if (isToday) colors.accent else ColorProvider(Color.Transparent))
                            .padding(top = 2.dp),
                        style = TextStyle(
                            color = if (isToday) ColorProvider(Color.White) else colors.primary,
                            fontSize = 11.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
        if (occurrences.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                    Spacer(
                        GlanceModifier
                            .width(30.dp)
                            .height(3.dp)
                            .cornerRadius(2.dp)
                            .background(colors.accent),
                    )
                    Spacer(GlanceModifier.height(13.dp))
                    Text(
                        "本周暂无课程",
                        style = TextStyle(
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        "可使用左右按钮切换其他周",
                        style = TextStyle(
                            color = colors.secondary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        } else {
            Box(GlanceModifier.fillMaxWidth().defaultWeight()) {
                // One Box per two lessons avoids both an overly dense grid and the
                // RemoteViews child-limit issue caused by separate spacer/line items.
                Column(GlanceModifier.fillMaxSize()) {
                    val lessonGroups = (lessonCount + 1) / 2
                    repeat(lessonGroups) { group ->
                        val lessonsInGroup = minOf(2, lessonCount - group * 2)
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(lessonHeight * lessonsInGroup),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (group < lessonGroups - 1) {
                                Spacer(
                                    GlanceModifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(gridLine),
                                )
                            }
                        }
                    }
                }
                Row(GlanceModifier.fillMaxSize()) {
                    (0L..6L).forEach { offset ->
                        val day = weekStart.plusDays(offset)
                        val blocks = blocksByDay[day].orEmpty()
                        val dayAgenda = actionStartActivity(
                            widgetOverlayIntent(
                                context,
                                WidgetOverlayActivity.MODE_DAY_AGENDA,
                                "$appWidgetId/${day.toEpochDay()}",
                            ) {
                                putExtra(WidgetOverlayActivity.EXTRA_EPOCH_DAY, day.toEpochDay())
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            },
                        )
                        Column(
                            modifier = GlanceModifier
                                .width(dayWidth)
                                .fillMaxHeight()
                                .padding(horizontal = 2.dp),
                        ) {
                            var cursor = visibleStart
                            blocks.forEach { block ->
                                if (block.endLesson >= cursor) {
                                    val drawStart = maxOf(block.startLesson, cursor)
                                    if (drawStart > cursor) {
                                        Spacer(GlanceModifier.height(lessonHeight * (drawStart - cursor)))
                                    }
                                    val drawEnd = maxOf(drawStart, block.endLesson)
                                    val span = drawEnd - drawStart + 1
                                    val occurrence = block.occurrence
                                    val courseColor = courseColors[
                                        occurrence.course.name.normalizedCourseName()
                                    ] ?: Color(occurrence.course.colorArgb)
                                    Column(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .height((lessonHeight.value * span - 3f).coerceAtLeast(14f).dp)
                                            .cornerRadius(7.dp)
                                            .background(ColorProvider(courseColor.copy(alpha = .94f)))
                                            .clickable(dayAgenda)
                                            .padding(horizontal = 4.dp, vertical = 3.dp),
                                    ) {
                                        Text(
                                            occurrence.course.name,
                                            maxLines = when {
                                                lessonHeight.value * span >= 55f -> 3
                                                span >= 2 -> 2
                                                else -> 1
                                            },
                                            style = TextStyle(
                                                color = ColorProvider(Color.White),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                            ),
                                        )
                                        if (span >= 2) {
                                            Text(
                                                occurrence.course.location.ifBlank {
                                                    "%02d:%02d".format(
                                                        occurrence.slot.startMinutes / 60,
                                                        occurrence.slot.startMinutes % 60,
                                                    )
                                                },
                                                maxLines = 1,
                                                style = TextStyle(
                                                    color = ColorProvider(Color.White.copy(alpha = .78f)),
                                                    fontSize = 7.sp,
                                                ),
                                            )
                                        }
                                    }
                                    Spacer(GlanceModifier.height(3.dp))
                                    cursor = drawEnd + 1
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}
