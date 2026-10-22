package com.kexu.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Alignment
import androidx.glance.text.TextDecoration
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kexu.app.KexuApplication
import com.kexu.app.MainActivity
import com.kexu.app.R
import com.kexu.app.data.model.TaskEntity
import com.kexu.app.reminder.CourseOccurrence
import com.kexu.app.reminder.ScheduleCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodayWidget : KexuGlanceWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as KexuApplication
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val widgetSettings = WidgetPreferences.settings(context, id)
        val tasks = app.repository.tasksDueBetween(start, end)
        val now = System.currentTimeMillis()
        val settings = app.settingsRepository.snapshot()
        val courses = app.repository.currentCourses()
        val slots = app.repository.currentSlots()
        val overrides = app.repository.currentOverrides().groupBy { it.slotId }
        provideContent {
            val liveCourses by app.repository.courses.collectAsState(courses)
            val liveSlots by app.repository.slots.collectAsState(slots)
            val liveOverrides by app.repository.overrides.collectAsState(overrides.values.flatten())
            val liveSettings by app.settingsRepository.settings.collectAsState(settings)
            val preferences by WidgetPreferences.observe(context, id).collectAsState(widgetSettings)
            val day = widgetToday()
            var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) { while (true) { currentTime = System.currentTimeMillis(); kotlinx.coroutines.delay(30_000) } }
            val nextCourse = liveCourses.flatMap { course ->
            liveSlots.filter { it.courseId == course.id }.flatMap { slot ->
                ScheduleCalculator.occurrences(
                    course = course,
                    slot = slot,
                    semesterStartMillis = liveSettings.semesterStartMillis,
                    fromMillis = currentTime - 24 * 60 * 60_000L,
                    days = 8,
                    zoneId = zone,
                    overrides = liveOverrides.filter { it.slotId == slot.id },
                ).filter { it.startsAtMillis > currentTime }
            }
        }.minByOrNull { it.startsAtMillis }
            val allTasks by app.repository.tasks.collectAsState(tasks)
            TodayWidgetContent(
                context,
                id,
                allTasks.filter { it.dueAt?.let { due -> Instant.ofEpochMilli(due).atZone(zone).toLocalDate() == day } == true }.sortedBy { it.dueAt },
                nextCourse,
                zone,
                preferences.showCompleted,
                widgetIsDark(context, id),
            )
        }
    }
}

@Composable
private fun TodayWidgetContent(
    context: Context,
    id: GlanceId,
    tasks: List<TaskEntity>,
    nextCourse: CourseOccurrence?,
    zoneId: ZoneId,
    showCompleted: Boolean,
    dark: Boolean,
) {
    val visibleTasks = tasks.filter { showCompleted || !it.isCompleted }
    val colors = rhythmPalette(dark)
    val primary = colors.text
    val secondary = colors.muted
    val accent = colors.accent
    val appWidgetId = WidgetPreferences.appWidgetId(context, id)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(24.dp)
            .background(widgetSurface(context, id, darkColor = Color(0xFF302C40), lightColor = Color(0xFFFAF5F1)))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "今天，慢慢来",
                modifier = GlanceModifier.clickable(widgetOpen(context, MainActivity.ACTION_OPEN_TODAY)),
                style = TextStyle(color = primary, fontWeight = FontWeight.Bold, fontSize = 18.sp),
            )
            Spacer(GlanceModifier.width(12.dp))
            Text("${visibleTasks.count { !it.isCompleted }} 项", style = TextStyle(color = secondary, fontSize = 11.sp))
            Spacer(GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.widget_ic_add_pressable),
                contentDescription = "添加任务",
                modifier = GlanceModifier.width(46.dp).height(44.dp).clickable(
                    actionStartActivity(
                        widgetOverlayIntent(
                            context,
                            WidgetOverlayActivity.MODE_QUICK_ADD,
                            appWidgetId.toString(),
                        ) {
                            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        },
                    ),
                ).padding(8.dp),
                colorFilter = ColorFilter.tint(primary),
            )
            Image(
                provider = ImageProvider(R.drawable.widget_ic_more_pressable),
                contentDescription = "更多",
                modifier = GlanceModifier.width(36.dp).height(44.dp).clickable(
                    actionStartActivity(
                        widgetOverlayIntent(
                            context,
                            WidgetOverlayActivity.MODE_WIDGET_MENU,
                            appWidgetId.toString(),
                        ) {
                            putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        },
                    ),
                ).padding(7.dp),
                colorFilter = ColorFilter.tint(primary),
            )
        }
        Spacer(GlanceModifier.height(2.dp))
        nextCourse?.takeIf { androidx.glance.LocalSize.current.height >= 150.dp || visibleTasks.isEmpty() }?.let { occurrence ->
            val time = Instant.ofEpochMilli(occurrence.startsAtMillis)
                .atZone(zoneId)
                .format(DateTimeFormatter.ofPattern("E HH:mm", Locale.SIMPLIFIED_CHINESE))
            Text(
                "下一节 · $time  ${occurrence.course.name}",
                maxLines = 1,
                modifier = GlanceModifier.clickable(
                    openWidgetDestination(context, MainActivity.ACTION_OPEN_TIMETABLE),
                ),
                style = TextStyle(color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp),
            )
            Spacer(GlanceModifier.height(6.dp))
        }
        if (visibleTasks.isEmpty()) {
            Text("给今天留一点空白", modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                .clickable(widgetOpen(context, MainActivity.ACTION_OPEN_TODAY)), style = TextStyle(color = secondary))
        } else {
          LazyColumn(GlanceModifier.fillMaxWidth().defaultWeight()) {
            items(visibleTasks, itemId = { it.id }) { task ->
                Row(GlanceModifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        provider = ImageProvider(
                            if (task.isCompleted) {
                                R.drawable.widget_ic_checkbox_checked_pressable
                            } else {
                                R.drawable.widget_ic_checkbox_outline_pressable
                            },
                        ),
                        contentDescription = if (task.isCompleted) "标记为未完成" else "标记为完成",
                        modifier = GlanceModifier.width(44.dp).height(44.dp).clickable(
                            actionRunCallback<ToggleTaskAction>(
                                actionParametersOf(
                                    TaskIdKey to task.id,
                                    TaskVersionKey to task.widgetVersion(),
                                    TaskCompletedTargetKey to !task.isCompleted,
                                ),
                            ),
                        ).padding(10.dp),
                        colorFilter = ColorFilter.tint(
                            if (task.isCompleted) accent else secondary,
                        ),
                    )
                    Spacer(GlanceModifier.width(3.dp))
                    Text(
                        task.title,
                        modifier = GlanceModifier.defaultWeight().clickable(
                            actionStartActivity(android.content.Intent(context, MainActivity::class.java)
                                .setData(android.net.Uri.parse("kexu://widget/task/${task.id}/edit"))
                                .putExtra(com.kexu.app.reminder.ReminderScheduler.EXTRA_TASK_ID, task.id)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)),
                        ),
                        maxLines = 1,
                        style = TextStyle(color = if (task.isCompleted) secondary else primary,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None),
                    )
                    task.dueAt?.let {
                        Text(
                            Instant.ofEpochMilli(it).atZone(zoneId)
                                .format(DateTimeFormatter.ofPattern("HH:mm")),
                            style = TextStyle(color = secondary, fontSize = 11.sp),
                        )
                    }
                }
            }
          }
        }
    }
}

private fun openWidgetDestination(context: Context, action: String) =
    androidx.glance.appwidget.action.actionStartActivity(
        android.content.Intent(context, MainActivity::class.java)
            .addFlags(
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
            .putExtra(MainActivity.EXTRA_WIDGET_ACTION, action),
    )

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
