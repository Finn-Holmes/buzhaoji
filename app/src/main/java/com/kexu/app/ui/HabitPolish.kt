package com.kexu.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kexu.app.data.model.HabitEntity
import com.kexu.app.reminder.ScheduleCalculator
import java.time.LocalDate

/** Colors animate without moving the day labels or changing the selected date's hit area. */
@Composable
internal fun HabitWeekStrip(
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekStart = ScheduleCalculator.startOfWeek(selectedDate)
    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
    val today = LocalDate.now()
    val colors = MaterialTheme.colorScheme

    Row(
        modifier.fillMaxWidth().selectableGroup().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        (0L..6L).forEach { offset ->
            val date = weekStart.plusDays(offset)
            val selected = date == selectedDate
            val dayName = weekDays[offset.toInt()]
            val background by animateColorAsState(
                if (selected) colors.primaryContainer else colors.background,
                animationSpec = tween(190),
                label = "habitDateBackground",
            )
            val foreground by animateColorAsState(
                if (selected) colors.onPrimaryContainer else colors.onSurface,
                animationSpec = tween(190),
                label = "habitDateForeground",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(background)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(date) })
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${date.monthValue}月${date.dayOfMonth}日，星期$dayName" +
                            if (date == today) "，今天" else ""
                    }
                    .padding(vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    dayName,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) foreground else colors.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    date.dayOfMonth.toString(),
                    modifier = Modifier.clearAndSetSemantics { },
                    fontSize = 16.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = foreground,
                    maxLines = 1,
                )
                // Reserve the indicator's space on every day to keep the week strip stable.
                Box(
                    Modifier.size(3.dp).background(
                        if (date == today) foreground else Color.Transparent,
                        RoundedCornerShape(50),
                    ),
                )
            }
        }
    }
}

/** A fixed text footprint prevents a check-in's new streak label from shifting nearby rows. */
@Composable
internal fun HabitPolishedRow(
    habit: HabitEntity,
    checked: Boolean,
    streak: Int,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        if (checked) lerp(colors.surface, colors.primary, .065f) else colors.surface,
        animationSpec = tween(200),
        label = "habitCompletionBackground",
    )
    val subtitleColor by animateColorAsState(
        if (checked) colors.primary else colors.onSurfaceVariant,
        animationSpec = tween(200),
        label = "habitCompletionSubtitle",
    )
    val shape = RoundedCornerShape(18.dp)

    Surface(
        modifier = modifier.fillMaxWidth().softClick(shape, onClick = onEdit),
        color = background,
        shape = shape,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 92.dp).padding(start = 14.dp, end = 5.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Text(habit.symbol, fontSize = 22.sp, modifier = Modifier.clearAndSetSemantics { }, maxLines = 1)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (streak > 0) "已连续 $streak 天" else habit.description.ifBlank { "从今天开始坚持" },
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(3.dp))
            CompletionToggle(
                checked = checked,
                label = if (checked) "取消打卡：${habit.title}" else "打卡：${habit.title}",
                onToggle = onToggle,
                visualSize = 28.dp,
            )
        }
    }
}

@Composable
internal fun HabitCompletionCount(done: Int, total: Int, modifier: Modifier = Modifier) {
    // Crossfade retains the outgoing number's width until the incoming value is ready.
    Crossfade(
        targetState = done to total,
        modifier = modifier.semantics { contentDescription = "已完成 $done 项，共 $total 项习惯" },
        animationSpec = tween(180),
        label = "habitCompletionCount",
    ) { (completed, count) ->
        Text(
            "$completed / $count",
            modifier = Modifier.clearAndSetSemantics { },
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
