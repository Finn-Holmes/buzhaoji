package com.kexu.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.kexu.app.ui.MotionIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

internal data class TaskDateSelection(
    val dueAtMillis: Long?,
    val endAtMillis: Long?,
    val reminderAtMillis: Long?,
    val recurrenceRule: String?,
    val hasExplicitTime: Boolean,
)

private enum class TaskTimeMode {
    DATE,
    RANGE,
}

private enum class TimePickerTarget {
    START,
    END,
}

private val pickerBlue = Color(0xFF4C73F8)
private val pickerMuted = Color(0xFF9A9A9F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DidaTaskDateTimeSheet(
    initialSelection: TaskDateSelection,
    onDismiss: () -> Unit,
    onConfirm: (TaskDateSelection) -> Unit,
    onClear: () -> Unit,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val now = remember { LocalDateTime.now(zoneId) }
    val initialStart = remember(initialSelection.dueAtMillis) {
        initialSelection.dueAtMillis
            ?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDateTime() }
            ?: now.withSecond(0).withNano(0)
    }
    val initialEnd = remember(initialSelection.endAtMillis, initialStart) {
        initialSelection.endAtMillis
            ?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDateTime() }
            ?: initialStart.plusHours(1)
    }
    var selectedDateEpochDay by rememberSaveable {
        mutableLongStateOf(initialStart.toLocalDate().toEpochDay())
    }
    var shownMonthOffset by rememberSaveable {
        val todayMonth = YearMonth.from(LocalDate.now())
        val selectedMonth = YearMonth.from(initialStart.toLocalDate())
        mutableIntStateOf(
            (selectedMonth.year - todayMonth.year) * 12 +
                selectedMonth.monthValue -
                todayMonth.monthValue,
        )
    }
    var modeName by rememberSaveable {
        mutableStateOf(
            if (initialSelection.endAtMillis != null) {
                TaskTimeMode.RANGE.name
            } else {
                TaskTimeMode.DATE.name
            },
        )
    }
    val mode = TaskTimeMode.valueOf(modeName)
    var hasTime by rememberSaveable {
        mutableStateOf(initialSelection.hasExplicitTime || initialSelection.endAtMillis != null)
    }
    var allDay by rememberSaveable {
        mutableStateOf(!hasTime && initialSelection.endAtMillis != null)
    }
    var startHour by rememberSaveable { mutableIntStateOf(initialStart.hour) }
    var startMinute by rememberSaveable { mutableIntStateOf(initialStart.minute) }
    var endHour by rememberSaveable { mutableIntStateOf(initialEnd.hour) }
    var endMinute by rememberSaveable { mutableIntStateOf(initialEnd.minute) }
    var reminderOffsetMinutes by rememberSaveable {
        mutableStateOf(
            initialSelection.dueAtMillis?.let { due ->
                initialSelection.reminderAtMillis?.let { reminder ->
                    ((due - reminder) / 60_000L).toInt()
                }
            },
        )
    }
    var recurrenceRule by rememberSaveable {
        mutableStateOf(initialSelection.recurrenceRule)
    }
    var timePickerTargetName by rememberSaveable { mutableStateOf<String?>(null) }
    var showReminderDialog by rememberSaveable { mutableStateOf(false) }
    var showRepeatDialog by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val todayMonth = remember { YearMonth.from(LocalDate.now()) }
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay)
    val shownMonth = todayMonth.plusMonths(shownMonthOffset.toLong())

    fun buildSelection(): TaskDateSelection {
        val start = if (hasTime && !allDay) {
            selectedDate.atTime(startHour, startMinute)
        } else {
            selectedDate.atTime(23, 59)
        }
        val dueAt = start.atZone(zoneId).toInstant().toEpochMilli()
        val endAt = if (mode == TaskTimeMode.RANGE) {
            if (allDay) {
                selectedDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            } else {
                var end = selectedDate.atTime(endHour, endMinute)
                if (!end.isAfter(start)) end = end.plusDays(1)
                end.atZone(zoneId).toInstant().toEpochMilli()
            }
        } else {
            null
        }
        val reminderAt = reminderOffsetMinutes
            ?.takeIf { hasTime && !allDay }
            ?.let { dueAt - it * 60_000L }
        return TaskDateSelection(
            dueAtMillis = dueAt,
            endAtMillis = endAt,
            reminderAtMillis = reminderAt,
            recurrenceRule = recurrenceRule,
            hasExplicitTime = hasTime && !allDay,
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.72f)) {
            Row(
                Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭时间设置")
                }
                PickerTab(
                    text = "日期",
                    selected = mode == TaskTimeMode.DATE,
                    onClick = {
                        modeName = TaskTimeMode.DATE.name
                        allDay = false
                    },
                )
                PickerTab(
                    text = "时间段",
                    selected = mode == TaskTimeMode.RANGE,
                    onClick = {
                        modeName = TaskTimeMode.RANGE.name
                        if (!hasTime) {
                            hasTime = true
                            reminderOffsetMinutes = 0
                        }
                    },
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onConfirm(buildSelection()) }) {
                    Icon(Icons.Default.Check, contentDescription = "确认时间设置")
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            ) {
                if (mode == TaskTimeMode.DATE) {
                    QuickMonthCalendar(
                        shownMonth = shownMonth,
                        selectedDate = selectedDate,
                        onPreviousMonth = { shownMonthOffset-- },
                        onNextMonth = { shownMonthOffset++ },
                        onSelectDate = { selectedDateEpochDay = it.toEpochDay() },
                    )
                    Spacer(Modifier.height(12.dp))
                    DateSettingsCard(
                        hasTime = hasTime,
                        timeLabel = timeText(startHour, startMinute),
                        reminderLabel = reminderLabel(reminderOffsetMinutes),
                        repeatLabel = recurrenceLabel(recurrenceRule, selectedDate),
                        onTimeClick = {
                            hasTime = true
                            if (reminderOffsetMinutes == null) reminderOffsetMinutes = 0
                            timePickerTargetName = TimePickerTarget.START.name
                        },
                        onClearTime = {
                            hasTime = false
                            reminderOffsetMinutes = null
                        },
                        onReminderClick = { showReminderDialog = true },
                        onClearReminder = { reminderOffsetMinutes = null },
                        onRepeatClick = { showRepeatDialog = true },
                    )
                } else {
                    RangeSettings(
                        selectedDate = selectedDate,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        allDay = allDay,
                        reminderLabel = reminderLabel(reminderOffsetMinutes),
                        repeatLabel = recurrenceLabel(recurrenceRule, selectedDate),
                        onDateClick = {
                            modeName = TaskTimeMode.DATE.name
                        },
                        onStartTimeClick = {
                            timePickerTargetName = TimePickerTarget.START.name
                        },
                        onEndTimeClick = {
                            timePickerTargetName = TimePickerTarget.END.name
                        },
                        onAllDayChange = {
                            allDay = it
                            hasTime = !it
                            if (it) reminderOffsetMinutes = null
                        },
                        onReminderClick = { showReminderDialog = true },
                        onClearReminder = { reminderOffsetMinutes = null },
                        onRepeatClick = { showRepeatDialog = true },
                    )
                }
                Text(
                    text = "清除",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onClear()
                        }
                        .padding(vertical = 34.dp),
                    color = Color(0xFFE53935),
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                )
            }
        }
    }

    timePickerTargetName?.let { targetName ->
        val target = TimePickerTarget.valueOf(targetName)
        val pickerState = rememberTimePickerState(
            initialHour = if (target == TimePickerTarget.START) startHour else endHour,
            initialMinute = if (target == TimePickerTarget.START) startMinute else endMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { timePickerTargetName = null },
            title = { Text(if (target == TimePickerTarget.START) "时间" else "结束时间") },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (target == TimePickerTarget.START) {
                            startHour = pickerState.hour
                            startMinute = pickerState.minute
                            if (mode == TaskTimeMode.RANGE) {
                                val startMinutes = startHour * 60 + startMinute
                                val endMinutes = endHour * 60 + endMinute
                                if (endMinutes <= startMinutes) {
                                    val adjusted = (startMinutes + 60) % (24 * 60)
                                    endHour = adjusted / 60
                                    endMinute = adjusted % 60
                                }
                            }
                        } else {
                            endHour = pickerState.hour
                            endMinute = pickerState.minute
                        }
                        hasTime = true
                        allDay = false
                        if (reminderOffsetMinutes == null) reminderOffsetMinutes = 0
                        timePickerTargetName = null
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { timePickerTargetName = null }) { Text("取消") }
            },
        )
    }

    if (showReminderDialog) {
        ReminderDialog(
            selectedOffset = reminderOffsetMinutes,
            onSelect = {
                reminderOffsetMinutes = it
                if (it != null) {
                    hasTime = true
                    allDay = false
                }
                showReminderDialog = false
            },
            onDismiss = { showReminderDialog = false },
        )
    }

    if (showRepeatDialog) {
        RepeatDialog(
            selectedRule = recurrenceRule,
            selectedDate = selectedDate,
            onSelect = {
                recurrenceRule = it
                showRepeatDialog = false
            },
            onDismiss = { showRepeatDialog = false },
        )
    }
}

@Composable
private fun PickerTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .height(58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text,
            color = if (selected) pickerBlue else pickerMuted,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .height(3.dp)
                .width(58.dp)
                .clip(CircleShape)
                .background(if (selected) pickerBlue else Color.Transparent),
        )
    }
}

@Composable
private fun QuickMonthCalendar(
    shownMonth: YearMonth,
    selectedDate: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    val firstDayOffset = shownMonth.atDay(1).dayOfWeek.value % 7
    val dates = remember(shownMonth) {
        List(42) { index ->
            val day = index - firstDayOffset + 1
            if (day in 1..shownMonth.lengthOfMonth()) shownMonth.atDay(day) else null
        }.chunked(7)
            .dropLastWhile { week -> week.all { it == null } }
            .flatten()
    }
    val today = LocalDate.now()

    Column {
        Row(
            Modifier.fillMaxWidth().height(54.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${shownMonth.monthValue}月",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onPreviousMonth) {
                Text("‹", color = pickerMuted, fontSize = 32.sp)
            }
            TextButton(onClick = onNextMonth) {
                Text("›", color = pickerMuted, fontSize = 32.sp)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    color = pickerMuted,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                )
            }
        }
        dates.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1.25f)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            val selected = date == selectedDate
                            val isToday = date == today
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .then(
                                        if (isToday && !selected) {
                                            Modifier.border(1.dp, pickerBlue.copy(alpha = .35f), CircleShape)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .clip(CircleShape)
                                    .background(if (selected) pickerBlue else Color.Transparent)
                                    .clickable { onSelectDate(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = when {
                                        selected -> Color.White
                                        isToday -> pickerBlue
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontSize = 17.sp,
                                    fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSettingsCard(
    hasTime: Boolean,
    timeLabel: String,
    reminderLabel: String,
    repeatLabel: String,
    onTimeClick: () -> Unit,
    onClearTime: () -> Unit,
    onReminderClick: () -> Unit,
    onClearReminder: () -> Unit,
    onRepeatClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column {
            PickerSettingRow(
                icon = Icons.Outlined.AccessTime,
                title = "时间",
                value = if (hasTime) timeLabel else "无",
                active = hasTime,
                onClick = onTimeClick,
                onClear = onClearTime.takeIf { hasTime },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PickerSettingRow(
                icon = Icons.Outlined.Alarm,
                title = "提醒",
                value = reminderLabel,
                active = reminderLabel != "无",
                onClick = onReminderClick,
                onClear = onClearReminder.takeIf { reminderLabel != "无" },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PickerSettingRow(
                icon = Icons.Outlined.Sync,
                title = "重复",
                value = repeatLabel,
                active = false,
                onClick = onRepeatClick,
            )
        }
    }
}

@Composable
private fun RangeSettings(
    selectedDate: LocalDate,
    startHour: Int,
    startMinute: Int,
    endHour: Int,
    endMinute: Int,
    allDay: Boolean,
    reminderLabel: String,
    repeatLabel: String,
    onDateClick: () -> Unit,
    onStartTimeClick: () -> Unit,
    onEndTimeClick: () -> Unit,
    onAllDayChange: (Boolean) -> Unit,
    onReminderClick: () -> Unit,
    onClearReminder: () -> Unit,
    onRepeatClick: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.weight(1f).softClick(RoundedCornerShape(16.dp), onClick = onDateClick),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("日期", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(20.dp))
                Text(
                    "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日，${weekdayText(selectedDate)}",
                    color = pickerBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                )
                Text(relativeDateLabel(selectedDate), color = pickerMuted)
            }
        }
        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("时间", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        timeText(startHour, startMinute),
                        modifier = Modifier.clickable(onClick = onStartTimeClick),
                        color = pickerBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Text(" - ", color = pickerBlue, fontSize = 18.sp)
                    Text(
                        timeText(endHour, endMinute),
                        modifier = Modifier.clickable(onClick = onEndTimeClick),
                        color = pickerBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
                val start = LocalTime.of(startHour, startMinute)
                var end = LocalTime.of(endHour, endMinute)
                var durationMinutes = Duration.between(start, end).toMinutes()
                if (durationMinutes <= 0) durationMinutes += 24 * 60
                Text("持续时间：${durationLabel(durationMinutes)}", color = pickerMuted)
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("全天", modifier = Modifier.weight(1f), fontSize = 18.sp)
            Switch(checked = allDay, onCheckedChange = onAllDayChange)
        }
    }
    Spacer(Modifier.height(12.dp))
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column {
            PickerSettingRow(
                icon = Icons.Outlined.Alarm,
                title = "提醒",
                value = reminderLabel,
                active = reminderLabel != "无",
                onClick = onReminderClick,
                onClear = onClearReminder.takeIf { reminderLabel != "无" },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PickerSettingRow(
                icon = Icons.Outlined.Sync,
                title = "重复",
                value = repeatLabel,
                active = false,
                onClick = onRepeatClick,
            )
        }
    }
}

@Composable
private fun PickerSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    active: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(62.dp).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) pickerBlue else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = if (active) pickerBlue else MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
        )
        Text(value, color = if (active) pickerBlue else pickerMuted, fontSize = 17.sp)
        if (onClear != null) {
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Outlined.Close,
                contentDescription = "清除$title",
                modifier = Modifier.size(28.dp).softClick(CircleShape, onClick = onClear),
                tint = pickerBlue,
            )
        } else if (title == "重复") {
            Spacer(Modifier.width(8.dp))
            Text("›", color = pickerMuted, fontSize = 26.sp)
        }
    }
}

@Composable
private fun ReminderDialog(
    selectedOffset: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        null to "无",
        0 to "准时",
        5 to "提前5分钟",
        30 to "提前30分钟",
        60 to "提前1小时",
        24 * 60 to "提前1天",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提醒") },
        text = {
            Column {
                options.forEach { (offset, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(offset) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            color = if (selectedOffset == offset) pickerBlue else MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                        )
                        if (selectedOffset == offset) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = pickerBlue)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun RepeatDialog(
    selectedRule: String?,
    selectedDate: LocalDate,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val weekly = "WEEKLY:${selectedDate.dayOfWeek.value}"
    val monthly = "MONTHLY:${selectedDate.dayOfMonth}"
    val yearly = "YEARLY:${selectedDate.monthValue}-${selectedDate.dayOfMonth}"
    val options = listOf(
        null to "无",
        "DAILY" to "每天",
        weekly to "每周（${weekdayText(selectedDate)}）",
        monthly to "每月（${selectedDate.dayOfMonth}日）",
        yearly to "每年（${selectedDate.monthValue}月${selectedDate.dayOfMonth}日）",
        "WEEKDAYS" to "工作日",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重复") },
        text = {
            Column {
                options.forEach { (rule, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(rule) }.padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            color = if (selectedRule == rule) pickerBlue else MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                        )
                        if (selectedRule == rule) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = pickerBlue)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun timeText(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

private fun reminderLabel(offset: Int?): String = when (offset) {
    null -> "无"
    0 -> "准时"
    5 -> "提前5分钟"
    30 -> "提前30分钟"
    60 -> "提前1小时"
    24 * 60 -> "提前1天"
    else -> "提前${offset}分钟"
}

private fun recurrenceLabel(rule: String?, date: LocalDate): String = when {
    rule == null -> "无"
    rule == "DAILY" -> "每天"
    rule == "WEEKDAYS" -> "工作日"
    rule.startsWith("WEEKLY:") -> "每周（${weekdayText(date)}）"
    rule.startsWith("MONTHLY:") -> "每月（${date.dayOfMonth}日）"
    rule.startsWith("YEARLY:") -> "每年"
    else -> "自定义"
}

private fun relativeDateLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "今天"
    LocalDate.now().plusDays(1) -> "明天"
    LocalDate.now().plusDays(2) -> "后天"
    else -> date.year.toString()
}

private fun weekdayText(date: LocalDate): String =
    listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[date.dayOfWeek.value - 1]

private fun durationLabel(minutes: Long): String = when {
    minutes % 60L == 0L -> "${minutes / 60}小时"
    minutes >= 60L -> "${minutes / 60}小时${minutes % 60}分钟"
    else -> "${minutes}分钟"
}
