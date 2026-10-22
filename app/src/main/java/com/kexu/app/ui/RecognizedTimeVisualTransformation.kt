package com.kexu.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

internal class RecognizedTimeVisualTransformation(
    recognizedTokens: List<String>,
    private val accentColor: Color,
) : VisualTransformation {
    private val tokens = recognizedTokens
        .filter(String::isNotBlank)
        .distinct()
        .sortedByDescending(String::length)

    override fun filter(text: AnnotatedString): TransformedText {
        if (tokens.isEmpty() || text.text.isBlank()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val ranges = tokens
            .flatMap { token -> text.text.allRangesOf(token) }
            .sortedBy(IntRange::first)
            .fold(mutableListOf<IntRange>()) { merged, range ->
                val previous = merged.lastOrNull()
                if (
                    previous != null &&
                    range.first <= previous.last + 2 &&
                    (
                        range.first <= previous.last ||
                            text.text.substring(previous.last + 1, range.first)
                                .all { it.isWhitespace() || it in "，,。；;" }
                    )
                ) {
                    merged[merged.lastIndex] = previous.first..maxOf(previous.last, range.last)
                } else if (previous == null || range.first > previous.last) {
                    merged += range
                }
                merged
            }
        val highlighted = AnnotatedString.Builder(text).apply {
            ranges.forEach { range ->
                addStyle(
                    SpanStyle(
                        color = accentColor,
                        background = accentColor.copy(alpha = 0.13f),
                        textDecoration = TextDecoration.None,
                    ),
                    start = range.first,
                    end = range.last + 1,
                )
            }
        }.toAnnotatedString()
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

private fun String.allRangesOf(token: String): List<IntRange> {
    val ranges = mutableListOf<IntRange>()
    var startIndex = 0
    while (startIndex <= length - token.length) {
        val match = indexOf(token, startIndex)
        if (match < 0) break
        ranges += match until match + token.length
        startIndex = match + token.length
    }
    return ranges
}
