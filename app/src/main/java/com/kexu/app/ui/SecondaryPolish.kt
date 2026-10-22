package com.kexu.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kexu.app.data.model.CountdownEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Keep the event readable; the number is a quiet, consistently aligned anchor. */
@Composable
internal fun CountdownPolishedRow(
    countdown: CountdownEntity,
    target: LocalDate,
    days: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    val dateLabel = remember(target) {
        target.format(DateTimeFormatter.ofPattern("yyyy.M.d", Locale.CHINA))
    }
    Surface(
        modifier = modifier.fillMaxWidth().softClick(shape, onClick = onClick),
        color = if (days == 0L) colors.primaryContainer.copy(alpha = .38f) else colors.surface,
        shape = shape,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 19.dp)) {
            val contentWidth = maxWidth
            val stackValue = fontScale > 1.3f || maxWidth < 270.dp
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(countdown.symbol, fontSize = 21.sp, maxLines = 1, modifier = Modifier.widthIn(min = 28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            countdown.name,
                            fontSize = 16.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = colors.onSurface,
                        )
                        Text(
                            dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!stackValue) {
                        Spacer(Modifier.width(14.dp))
                        CountdownValue(days, Modifier.widthIn(min = 76.dp, max = 116.dp))
                    }
                }
                if (stackValue) {
                    CountdownValue(days, Modifier.align(Alignment.End).widthIn(min = 76.dp, max = contentWidth))
                }
            }
        }
    }
}

@Composable
private fun CountdownValue(days: Long, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier) {
        val availableWidth = maxWidth.value
        // Crossfade only when the value changes, never count up from zero on entry.
        Crossfade(
            targetState = days,
            animationSpec = tween(160),
            label = "countdownValue",
        ) { displayedDays ->
            val accent = if (displayedDays >= 0) colors.primary else colors.onSurfaceVariant
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (displayedDays == 0L) {
                    Text(
                        "就是今天",
                        color = accent,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        maxLines = 1,
                    )
                } else {
                    val dayText = abs(displayedDays).toString()
                    val numberSize = ((availableWidth - 19f * fontScale) / (dayText.length * .64f * fontScale))
                        .coerceAtMost(32f).coerceAtLeast(12f)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            dayText,
                            color = accent,
                            fontSize = numberSize.sp,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline(),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "天",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                    Text(
                        if (displayedDays > 0) "还有" else "已经过去",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

/** The dial interpolates between real readings; only the caller owns the clock. */
@Composable
internal fun FocusTimerDial(
    seconds: Int,
    totalSeconds: Int,
    positiveMode: Boolean,
    running: Boolean,
    modifier: Modifier = Modifier,
) {
    val safeSeconds = seconds.coerceAtLeast(0)
    val progress = if (totalSeconds > 0) {
        (1f - safeSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress = remember(totalSeconds, positiveMode) { Animatable(progress) }
    LaunchedEffect(progress, running, positiveMode) {
        if (running && !positiveMode) {
            // Compose animation APIs follow the platform animator-duration scale.
            animatedProgress.animateTo(progress, tween(800, easing = LinearEasing))
        } else {
            animatedProgress.snapTo(progress)
        }
    }
    val formattedTime = String.format(Locale.ROOT, "%02d:%02d", safeSeconds / 60, safeSeconds % 60)
    val status = when {
        !positiveMode && totalSeconds > 0 && safeSeconds == 0 -> "已完成"
        running -> "专注进行中"
        (positiveMode && safeSeconds == 0) || (!positiveMode && safeSeconds == totalSeconds) -> "准备开始"
        else -> "已暂停"
    }
    val colors = MaterialTheme.colorScheme
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier.widthIn(max = 256.dp).aspectRatio(1f).clearAndSetSemantics {
            contentDescription = "${if (positiveMode) "已专注" else "剩余"} ${safeSeconds / 60} 分 ${safeSeconds % 60} 秒"
            stateDescription = if (positiveMode) status else "$status，进度 ${(progress * 100).toInt()}%"
            if (!positiveMode) progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
        },
        contentAlignment = Alignment.Center,
    ) {
        val dialWidth = maxWidth.value
        Canvas(Modifier.matchParentSize().padding(8.dp)) {
            val stroke = 3.dp.toPx()
            val radius = (size.minDimension - stroke) / 2f
            if (positiveMode) {
                // An open-ended stopwatch has no meaningful completion percentage.
                for (index in 0 until 60) {
                    val angle = Math.toRadians(index * 6.0 - 90.0)
                    val tickLength = if (index % 5 == 0) 7.dp.toPx() else 3.dp.toPx()
                    val innerRadius = radius - tickLength
                    drawLine(
                        color = if (index % 5 == 0) colors.onSurfaceVariant.copy(alpha = .48f)
                            else colors.outlineVariant.copy(alpha = .7f),
                        start = center + Offset(cos(angle).toFloat() * innerRadius, sin(angle).toFloat() * innerRadius),
                        end = center + Offset(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            } else {
                drawCircle(colors.outlineVariant.copy(alpha = .55f), radius = radius, style = Stroke(stroke))
                val fraction = if (running) animatedProgress.value else progress
                if (fraction > 0f) {
                    drawArc(
                        color = colors.primary,
                        startAngle = -90f,
                        sweepAngle = fraction * 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            // Allow larger system text until the large display would exceed the dial.
            val timeFontSize = ((dialWidth - 40f) / (formattedTime.length * .64f * fontScale))
                .coerceAtMost(50f).coerceAtLeast(12f)
            Text(
                formattedTime,
                color = colors.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = timeFontSize.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                maxLines = 1,
            )
            Text(status, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
internal fun FocusPlaybackGlyph(running: Boolean) {
    Crossfade(targetState = running, animationSpec = tween(140), label = "focusPlayback", modifier = Modifier.size(24.dp)) { isRunning ->
        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
    }
}
