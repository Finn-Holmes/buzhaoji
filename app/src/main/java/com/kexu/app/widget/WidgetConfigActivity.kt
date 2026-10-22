package com.kexu.app.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.state.updateAppWidgetState
import com.kexu.app.KexuApplication
import com.kexu.app.data.model.CountdownEntity
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.settings.AppTheme
import com.kexu.app.settings.FocusTimerSettings
import com.kexu.app.settings.ThemeMode
import com.kexu.app.ui.theme.KexuTheme
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val manager = AppWidgetManager.getInstance(this)
        val providerName = manager.getAppWidgetInfo(appWidgetId)?.provider?.className.orEmpty()
        val kind = WidgetKind.fromProvider(providerName)
        val stored = WidgetPreferences.settings(this, appWidgetId)
        val app = application as KexuApplication
        if (kind == WidgetKind.COMPACT) {
            lifecycleScope.launch {
                val glanceId = widgetGlanceId(this@WidgetConfigActivity, appWidgetId)
                updateAppWidgetState(this@WidgetConfigActivity, glanceId) { state ->
                    state[CompactMenuVisibleStateKey] = false
                }
                CompactTaskWidget().update(this@WidgetConfigActivity, glanceId)
            }
        }

        setContent {
            KexuTheme(appTheme = AppTheme.DEFAULT, themeMode = ThemeMode.FOLLOW_SYSTEM) {
                var opacity by remember { mutableFloatStateOf(stored.opacity.toFloat()) }
                var theme by remember { mutableStateOf(stored.theme) }
                var selectedItemId by remember { mutableLongStateOf(stored.selectedItemId) }
                var showCompleted by remember { mutableStateOf(stored.showCompleted) }
                var startLesson by remember {
                    mutableFloatStateOf(stored.timetableStartLesson.toFloat())
                }
                var endLesson by remember {
                    mutableFloatStateOf(stored.timetableEndLesson.toFloat())
                }
                val focusTimer by produceState(initialValue = FocusTimerSettings()) {
                    value = app.settingsRepository.snapshot().focusTimer
                }
                var pomoMinutes by remember(focusTimer.durationMinutes) {
                    mutableFloatStateOf(focusTimer.durationMinutes.toFloat())
                }
                val habits by produceState<List<HabitEntity>>(initialValue = emptyList()) {
                    value = app.repository.currentHabits()
                }
                val countdowns by produceState<List<CountdownEntity>>(initialValue = emptyList()) {
                    value = app.repository.currentCountdowns()
                }
                val darkPreview = theme == WidgetTheme.DARK ||
                    (theme == WidgetTheme.FOLLOW_SYSTEM && (kind == WidgetKind.TIMETABLE || androidx.compose.foundation.isSystemInDarkTheme()))
                val previewBackground = when(kind) {
                    WidgetKind.HABIT -> if(darkPreview) Color(0xFF2E3540) else Color(0xFFF0F3E9)
                    WidgetKind.COUNTDOWN, WidgetKind.COUNTDOWN_LIST -> if(darkPreview) Color(0xFF383040) else Color(0xFFF9EEE7)
                    WidgetKind.STATS -> if(darkPreview) Color(0xFF302C40) else Color(0xFFF1EBF5)
                    else -> if(darkPreview) Color(0xFF29292B) else Color.White
                }
                val previewForeground = if (darkPreview) Color(0xFFF7EDF6) else Color(0xFF3E3649)

                Scaffold(
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = ::finish) {
                                Text("×", style = MaterialTheme.typography.headlineMedium)
                            }
                            Text(
                                "配置桌面小部件",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            TextButton(
                                onClick = {
                                    saveAndFinish(
                                        settings = stored.copy(
                                            opacity = opacity.toInt(),
                                            theme = theme,
                                            selectedItemId = selectedItemId,
                                            timetableStartLesson = startLesson.toInt(),
                                            timetableEndLesson = endLesson.toInt()
                                                .coerceAtLeast(startLesson.toInt()),
                                            showCompleted = showCompleted,
                                        ),
                                        kind = kind,
                                        pomoMinutes = pomoMinutes.toInt(),
                                    )
                                },
                            ) {
                                Text("✓", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    },
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(
                                if (kind == WidgetKind.TIMETABLE) 210.dp else 150.dp,
                            ),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Surface(
                                modifier = Modifier.padding(
                                    horizontal = if (kind == WidgetKind.TIMETABLE) 24.dp else 54.dp,
                                    vertical = 18.dp,
                                ),
                                shape = RoundedCornerShape(24.dp),
                                color = previewBackground.copy(alpha = opacity / 100f),
                            ) {
                                WidgetPreview(
                                    kind = kind,
                                    foreground = previewForeground,
                                    accent = if(darkPreview) Color(0xFFF0C3AE) else Color(0xFF92596C),
                                )
                            }
                        }

                        ConfigCard {
                            Text("主题", style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                WidgetTheme.entries.forEach { option ->
                                    FilterChip(
                                        selected = theme == option,
                                        onClick = { theme = option },
                                        label = {
                                            Text(
                                                when (option) {
                                                    WidgetTheme.FOLLOW_SYSTEM -> "跟随系统"
                                                    WidgetTheme.LIGHT -> "浅色"
                                                    WidgetTheme.DARK -> "深色"
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                            Text("不透明度  ${opacity.toInt()}%")
                            Slider(
                                value = opacity,
                                onValueChange = { opacity = it },
                                valueRange = 20f..100f,
                                steps = 7,
                            )
                        }

                        when (kind) {
                            WidgetKind.TODAY, WidgetKind.COMPACT -> ConfigCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("显示已完成任务")
                                        Text(
                                            "开启后可在小组件中撤销完成",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Switch(
                                        checked = showCompleted,
                                        onCheckedChange = { showCompleted = it },
                                    )
                                }
                            }
                            WidgetKind.HABIT -> EntitySelectorCard(
                                title = "选择习惯",
                                emptyText = "还没有习惯，请先到习惯库中添加",
                                items = habits.map { it.id to "${it.symbol}  ${it.title}" },
                                selectedId = selectedItemId,
                                onSelected = { selectedItemId = it },
                            )
                            WidgetKind.COUNTDOWN -> EntitySelectorCard(
                                title = "事件",
                                emptyText = "还没有倒数纪念日",
                                items = listOf(0L to "自动显示临近的日子") +
                                    countdowns.map { it.id to "${it.symbol}  ${it.name}" },
                                selectedId = selectedItemId,
                                onSelected = { selectedItemId = it },
                            )
                            WidgetKind.TIMETABLE -> ConfigCard {
                                Text("显示节次", style = MaterialTheme.typography.titleMedium)
                                Text("开始于第 ${startLesson.toInt()} 节")
                                Slider(
                                    value = startLesson,
                                    onValueChange = {
                                        startLesson = it
                                        if (endLesson < it) endLesson = it
                                    },
                                    valueRange = 1f..12f,
                                    steps = 10,
                                )
                                Text("结束于第 ${endLesson.toInt()} 节")
                                Slider(
                                    value = endLesson,
                                    onValueChange = { endLesson = it.coerceAtLeast(startLesson) },
                                    valueRange = 1f..12f,
                                    steps = 10,
                                )
                            }
                            WidgetKind.POMODORO -> ConfigCard {
                                Text("番茄时长", style = MaterialTheme.typography.titleMedium)
                                Text("${pomoMinutes.toInt()} 分钟")
                                Slider(
                                    value = pomoMinutes,
                                    onValueChange = { pomoMinutes = it },
                                    valueRange = 1f..180f,
                                    steps = 178,
                                )
                                Text(
                                    "桌面上的播放键会直接开始或暂停，不必打开应用。",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            WidgetKind.COUNTDOWN_LIST, WidgetKind.STATS -> Unit
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                }
            }
        }
    }

    private fun saveAndFinish(
        settings: HomeWidgetSettings,
        kind: WidgetKind,
        pomoMinutes: Int,
    ) {
        WidgetPreferences.save(this, appWidgetId, settings)
        lifecycleScope.launch {
            if (kind != WidgetKind.POMODORO) {
                updateAppWidgetState(this@WidgetConfigActivity, widgetGlanceId(this@WidgetConfigActivity, appWidgetId)) { state ->
                    state[WidgetOpacityStateKey] = settings.opacity
                    state[WidgetThemeStateKey] = settings.theme.name
                    if (kind == WidgetKind.TIMETABLE) {
                        state[TimetableWeekOffsetStateKey] = settings.timetableWeekOffset
                    }
                }
            }
            if (kind == WidgetKind.POMODORO) {
                val repository = (application as KexuApplication).settingsRepository
                val timer = repository.snapshot().focusTimer
                if (timer.durationMinutes != pomoMinutes.coerceIn(1, 180)) repository.setFocusTimer(
                    timer.copy(
                        durationMinutes = pomoMinutes.coerceIn(1, 180),
                        remainingSeconds = pomoMinutes.coerceIn(1, 180) * 60,
                        elapsedSeconds = 0,
                        running = false,
                        startedAtMillis = 0L,
                        anchorAtMillis = 0L,
                    ),
                )
            }
            val glanceId = widgetGlanceId(this@WidgetConfigActivity, appWidgetId)
            when (kind) {
                WidgetKind.TODAY -> TodayWidget().update(this@WidgetConfigActivity, glanceId)
                WidgetKind.COMPACT -> CompactTaskWidget().update(this@WidgetConfigActivity, glanceId)
                WidgetKind.TIMETABLE -> TimetableWidget().update(this@WidgetConfigActivity, glanceId)
                WidgetKind.HABIT -> HabitWidget().update(this@WidgetConfigActivity, glanceId)
                WidgetKind.COUNTDOWN -> CountdownWidget().update(this@WidgetConfigActivity, glanceId)
                WidgetKind.COUNTDOWN_LIST -> CountdownListWidget().update(this@WidgetConfigActivity, glanceId)
                WidgetKind.STATS -> TaskStatsWidget().update(this@WidgetConfigActivity, glanceId)
                WidgetKind.POMODORO -> PomodoroRemoteWidgetReceiver.updateAll(this@WidgetConfigActivity)
            }
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@androidx.compose.runtime.Composable
private fun ConfigCard(content: @androidx.compose.runtime.Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@androidx.compose.runtime.Composable
private fun EntitySelectorCard(
    title: String,
    emptyText: String,
    items: List<Pair<Long, String>>,
    selectedId: Long,
    onSelected: (Long) -> Unit,
) {
    ConfigCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (items.isEmpty()) {
            Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            items.forEach { (id, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelected(id) }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, modifier = Modifier.weight(1f))
                    Text(
                        if (selectedId == id) "●" else "○",
                        color = if (selectedId == id) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetPreview(kind: WidgetKind, foreground: Color, accent: Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (kind) {
            WidgetKind.TODAY -> {
                Text("不着急 · 今天", color = accent, fontWeight = FontWeight.Bold)
                Text("○  今天要做的事情", color = foreground)
                Text("○  下一项安排", color = foreground)
            }
            WidgetKind.COMPACT -> {
                Text("所有      ＋  ⋮", color = foreground, fontWeight = FontWeight.Bold)
                Text("○  完成报告        今天", color = foreground)
                Text("○  整理笔记        明天", color = foreground)
            }
            WidgetKind.TIMETABLE -> {
                Text("▦   ‹      7月 - 8月      ›   ＋   ⋮", color = foreground, fontWeight = FontWeight.Bold)
                Text("日    一    二    三    四    五    六", color = foreground)
                Text("27   28   29   30   31    1     2", color = foreground)
                Text("      高数          英语    物理", color = accent)
            }
            WidgetKind.HABIT -> {
                Text("日常一小步", color = accent)
                Text("读几页喜欢的书", color = foreground, fontWeight = FontWeight.Bold)
                Text("○  轻点，完成今天", color = accent)
            }
            WidgetKind.COUNTDOWN -> {
                Text("值得期待 · 周末", color = foreground, fontWeight = FontWeight.Bold)
                Text("7", color = accent, style = MaterialTheme.typography.headlineLarge)
                Text("还有这么多天", color = foreground)
            }
            WidgetKind.COUNTDOWN_LIST -> {
                Text("日子里的期待", color = foreground, fontWeight = FontWeight.Bold)
                Text("🎂 生日                     35 天", color = foreground)
                Text("🧧 春节                   269 天", color = foreground)
            }
            WidgetKind.POMODORO -> Text(
                "25:00                         ▶  ⋮",
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            WidgetKind.STATS -> {
                Text("一周足迹", color = foreground)
                Text("今日 8 件", color = foreground, style = MaterialTheme.typography.headlineSmall)
                Text("▃ ▆ ▂ ▅ ▇ ▄ ▆", color = accent)
            }
        }
    }
}

private enum class WidgetKind {
    TODAY,
    COMPACT,
    TIMETABLE,
    HABIT,
    COUNTDOWN,
    COUNTDOWN_LIST,
    POMODORO,
    STATS;

    companion object {
        fun fromProvider(provider: String): WidgetKind = when {
            provider.endsWith("TodayWidgetReceiver") -> TODAY
            provider.endsWith("CompactTaskWidgetReceiver") -> COMPACT
            provider.endsWith("TimetableWidgetReceiver") -> TIMETABLE
            provider.endsWith("HabitWidgetReceiver") -> HABIT
            provider.endsWith("CountdownListWidgetReceiver") -> COUNTDOWN_LIST
            provider.endsWith("CountdownWidgetReceiver") -> COUNTDOWN
            provider.endsWith("PomodoroWidgetReceiver") ||
                provider.endsWith("PomodoroRemoteWidgetReceiver") -> POMODORO
            provider.endsWith("TaskStatsWidgetReceiver") -> STATS
            else -> TODAY
        }
    }
}
