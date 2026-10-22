package com.kexu.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognizedTimeVisualTransformationTest {
    @Test
    fun adjacentDateAndTimeTokensAreHighlightedAsOnePhrase() {
        val transformed = RecognizedTimeVisualTransformation(
            recognizedTokens = listOf("明天", "下午三点"),
            accentColor = Color.Blue,
        ).filter(AnnotatedString("完成报告 明天下午三点")).text

        val colored = transformed.spanStyles.filter { it.item.color == Color.Blue }
        assertEquals(1, colored.size)
        assertEquals("明天下午三点", transformed.text.substring(colored.single().start, colored.single().end))
        assertTrue(colored.single().item.background.alpha > 0f)
    }
}
