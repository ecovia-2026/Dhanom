package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BentoPrimaryPurpleDark,
    onPrimary = BentoOnPrimaryDark,
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = BentoLavenderContainer,
    secondary = BentoLilacContainer,
    onSecondary = BentoDeepPurple,
    secondaryContainer = Color(0xFF332D41),
    onSecondaryContainer = BentoLavenderContainer,
    background = BentoBackgroundDark,
    onBackground = BentoOnBackgroundDark,
    surface = BentoSurfaceDark,
    onSurface = BentoOnSurfaceDark,
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = BentoBorderDark,
    outlineVariant = Color(0xFF49454F)
)

private val LightColorScheme = lightColorScheme(
    primary = BentoPrimaryPurple,
    onPrimary = BentoOnPrimary,
    primaryContainer = BentoLavenderContainer,
    onPrimaryContainer = BentoDeepPurple,
    secondary = BentoLilacContainer,
    onSecondary = BentoDeepPurple,
    secondaryContainer = BentoSurfaceLight,
    onSecondaryContainer = BentoDeepPurple,
    background = BentoBackgroundLight,
    onBackground = BentoOnBackgroundLight,
    surface = Color.White,
    onSurface = BentoOnSurface,
    surfaceVariant = BentoSurfaceLight,
    onSurfaceVariant = BentoSecondaryText,
    outline = BentoBorder,
    outlineVariant = BentoBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
