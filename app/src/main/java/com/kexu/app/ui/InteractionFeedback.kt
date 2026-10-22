package com.kexu.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton as MaterialIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton as MaterialFloatingActionButton
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

@Composable
private fun pressScale(source: MutableInteractionSource, amount: Float): Float {
    val pressed by source.collectIsPressedAsState()
    // Compose's animation clock respects the system duration scale, including zero.
    val scale by animateFloatAsState(if (pressed) amount else 1f,
        spring(dampingRatio = 1f, stiffness = 900f), label = "pressScale")
    return scale
}

/** Retain Material accessibility/ripple behaviour, but always clip to the visible circle. */
@Composable
fun MotionIconButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, .96f)
    MaterialIconButton(onClick = onClick, enabled = enabled, interactionSource = source,
        modifier = modifier.clip(CircleShape)) {
        // Animate the glyph, leaving the Material touch target at its full size.
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
fun MotionFloatingActionButton(onClick: () -> Unit, modifier: Modifier = Modifier,
    shape: Shape = CircleShape, containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = contentColorFor(containerColor), content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, .97f)
    MaterialFloatingActionButton(onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale }, shape = shape,
        containerColor = containerColor, contentColor = contentColor,
        interactionSource = source, content = content)
}

/** Custom cards/chips use an in-shape tint instead of the old rectangular ripple. */
fun Modifier.softClick(shape: Shape = RoundedCornerShape(12.dp), enabled: Boolean = true,
    onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale = pressScale(source, .993f)
    val tint = MaterialTheme.colorScheme.primary
    val alpha by animateFloatAsState(if (pressed) .06f else 0f, tween(120), label = "pressTint")
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clip(shape)
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
        .drawWithContent { drawContent(); drawRect(tint.copy(alpha = alpha)) }
}

/** Shared task/habit toggle: a generous hit area, animated fill and a drawn check stroke. */
@Composable
fun CompletionToggle(checked: Boolean, label: String, onToggle: () -> Unit,
    modifier: Modifier = Modifier, visualSize: Dp = 22.dp, circular: Boolean = true,
    accent: Color = MaterialTheme.colorScheme.primary,
    outline: Color = MaterialTheme.colorScheme.outline) {
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source, .96f)
    val fill by animateFloatAsState(if (checked) 1f else 0f, tween(140), label = "completionFill")
    val progress by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "completionStroke")
    val haptic = LocalHapticFeedback.current
    Box(modifier.size(48.dp)
        .clip(if (circular) CircleShape else RoundedCornerShape(12.dp))
        .toggleable(checked, role = Role.Checkbox,
            interactionSource = source, indication = null) {
            // Async deduplication belongs to the task/habit state owner, not a timed UI lock.
            haptic.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
            onToggle()
        }.semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        val checkColor = MaterialTheme.colorScheme.onPrimary
        Canvas(Modifier.size(visualSize).graphicsLayer { scaleX = scale; scaleY = scale }) {
            val radius = CornerRadius(if (circular) size.minDimension / 2 else 4.dp.toPx())
            drawRoundRect(accent.copy(alpha = fill), cornerRadius = radius)
            drawRoundRect(lerp(outline, accent, fill), cornerRadius = radius,
                style = Stroke(1.5.dp.toPx()))
            val full = Path().apply {
                moveTo(size.width * .23f, size.height * .51f)
                lineTo(size.width * .43f, size.height * .7f)
                lineTo(size.width * .78f, size.height * .31f)
            }
            val measure = PathMeasure().apply { setPath(full, false) }
            val stroke = Path()
            measure.getSegment(0f, measure.length * progress, stroke, true)
            drawPath(stroke, checkColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
fun CompletionTitle(text: String, completed: Boolean, modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge, maxLines: Int = Int.MAX_VALUE) {
    val progress by animateFloatAsState(if (completed) 1f else 0f, tween(220), label = "titleStrike")
    val color by animateColorAsState(if (completed) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurface, tween(180), label = "titleColor")
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(text, style = style, color = color, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout = it }, modifier = modifier.drawWithContent {
            drawContent()
            layout?.let { result ->
                for (line in 0 until result.lineCount) {
                    val left = result.getLineLeft(line)
                    val right = result.getLineRight(line)
                    val y = (result.getLineTop(line) + result.getLineBottom(line)) * .5f
                    if (progress > 0f) drawLine(color.copy(alpha = .7f), Offset(left, y),
                        Offset(left + (right - left) * progress, y), strokeWidth = 1.dp.toPx())
                }
            }
        })
}

/** Decorative only: the enclosing disclosure control supplies its label and expanded state. */
@Composable
internal fun DisclosureChevron(expanded: Boolean, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f,
        tween(180), label = "disclosureRotation")
    Icon(Icons.Outlined.ExpandMore, contentDescription = null,
        modifier = modifier.graphicsLayer { rotationZ = rotation },
        tint = MaterialTheme.colorScheme.onSurfaceVariant)
}
