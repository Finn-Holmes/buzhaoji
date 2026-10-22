package com.kexu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.kexu.app.settings.AppTheme
import com.kexu.app.settings.ThemeMode

data class KexuPalette(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val primaryContainer: Color,
)

fun paletteFor(theme: AppTheme): KexuPalette = when (theme) {
    AppTheme.DEFAULT -> KexuPalette(Color(0xFF5B5BD6), Color(0xFFF8F7FF), Color.White, Color(0xFFE9E8FF))
    AppTheme.SECRET_CYAN -> KexuPalette(Color(0xFF4CAFC5), Color(0xFFEAF8FC), Color(0xFFFAFEFF), Color(0xFFD5F1F7))
    AppTheme.TURQUOISE -> KexuPalette(Color(0xFF3DB398), Color(0xFFE9F7F3), Color(0xFFFBFFFE), Color(0xFFD4F1E9))
    AppTheme.REED -> KexuPalette(Color(0xFF7FA466), Color(0xFFF0F6EB), Color(0xFFFDFFFB), Color(0xFFDEECD4))
    AppTheme.APRICOT -> KexuPalette(Color(0xFFE09A43), Color(0xFFFFF4E5), Color(0xFFFFFCF7), Color(0xFFFFE4BA))
    AppTheme.PEACH -> KexuPalette(Color(0xFFDE7F9F), Color(0xFFFDF0F5), Color(0xFFFFFBFD), Color(0xFFF8D7E4))
    AppTheme.PURPLE -> KexuPalette(Color(0xFF8171D1), Color(0xFFF1EFFB), Color(0xFFFCFBFF), Color(0xFFDFDAF7))
    AppTheme.FROST -> KexuPalette(Color(0xFF718094), Color(0xFFF6F7F8), Color.White, Color(0xFFE7EAF0))
    AppTheme.ROCK -> KexuPalette(Color(0xFF62666E), Color(0xFFE9EAEC), Color(0xFFF7F7F8), Color(0xFFD9DBDF))
    AppTheme.NIGHT -> KexuPalette(Color(0xFF8199FF), Color(0xFF111318), Color(0xFF1D2026), Color(0xFF29375F))
    AppTheme.PRISM -> KexuPalette(Color(0xFF7467D9), Color(0xFFEEF6F2), Color(0xFFFCFFFD), Color(0xFFDDEBE8))
}

private fun opaqueTint(foreground: Color, background: Color, alpha: Float): Color =
    foreground.copy(alpha = alpha).compositeOver(background).copy(alpha = 1f)

private fun lightColors(palette: KexuPalette) = lightColorScheme(
    primary = palette.primary,
    onPrimary = Color.White,
    primaryContainer = palette.primaryContainer,
    onPrimaryContainer = Ink,
    secondary = palette.primary,
    background = palette.background,
    onBackground = Ink,
    surface = palette.surface,
    onSurface = Ink,
    surfaceBright = palette.surface,
    surfaceDim = palette.background,
    surfaceContainerLowest = palette.surface,
    surfaceContainerLow = palette.surface,
    surfaceContainer = opaqueTint(palette.primaryContainer, palette.surface, .18f),
    surfaceContainerHigh = opaqueTint(palette.primaryContainer, palette.surface, .36f),
    surfaceContainerHighest = opaqueTint(palette.primaryContainer, palette.surface, .58f),
    surfaceVariant = opaqueTint(palette.primaryContainer, palette.surface, .44f),
    onSurfaceVariant = Color(0xFF5F6570),
    outline = Color(0xFF9299A5),
    outlineVariant = opaqueTint(palette.primary, palette.surface, .16f),
    error = Coral,
)

private fun darkColors(palette: KexuPalette) = darkColorScheme(
    primary = palette.primary,
    onPrimary = Color(0xFF101318),
    primaryContainer = opaqueTint(palette.primary, DarkSurface, .30f),
    onPrimaryContainer = Color(0xFFE9ECF8),
    secondary = palette.primary,
    background = DarkBackground,
    onBackground = Color(0xFFE3E5DF),
    surface = DarkSurface,
    onSurface = Color(0xFFE3E5DF),
    surfaceBright = Color(0xFF292C32),
    surfaceDim = DarkBackground,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF292C32),
    surfaceContainerHighest = Color(0xFF30343B),
    surfaceVariant = Color(0xFF292C32),
    onSurfaceVariant = Color(0xFFB4B8C0),
    outline = Color(0xFF777B84),
    outlineVariant = Color(0xFF34373E),
    error = Color(0xFFFFB4AB),
)

@Composable
fun KexuTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = appTheme == AppTheme.NIGHT ||
        themeMode == ThemeMode.DARK ||
        (themeMode == ThemeMode.FOLLOW_SYSTEM && isSystemInDarkTheme())
    val palette = paletteFor(appTheme)
    MaterialTheme(
        colorScheme = if (dark) darkColors(palette) else lightColors(palette),
        content = content,
    )
}
