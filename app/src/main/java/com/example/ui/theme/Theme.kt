package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeId: String = "purple",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = ThemePalettes.byId(themeId)
    val colorScheme = lightColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        secondary = palette.accent,
        onSecondary = palette.onPrimary,
        secondaryContainer = palette.primaryContainer,
        onSecondaryContainer = palette.onPrimaryContainer,
        background = palette.background,
        onBackground = Color(0xFF1D1B20),
        surface = palette.surface,
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = palette.surfaceVariant,
        onSurfaceVariant = palette.secondaryText,
        outline = palette.border,
        outlineVariant = palette.border,
        error = palette.expenseRed
    )

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
