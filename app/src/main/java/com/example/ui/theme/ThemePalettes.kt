package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A complete app theme palette (10 built-in themes). */
data class AppPalette(
    val id: String,
    val name: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val accent: Color,
    val border: Color,
    val secondaryText: Color,
    val positiveGreen: Color = Color(0xFF2E7D32),
    val expenseRed: Color = Color(0xFFD32F2F)
)

object ThemePalettes {

    val purple = AppPalette(
        id = "purple", name = "Purple",
        background = Color(0xFFFDF7FF), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFF3EDF7),
        primary = Color(0xFF6750A4), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE8DEF8), onPrimaryContainer = Color(0xFF21005D),
        accent = Color(0xFF21005D), border = Color(0xFFCAC4D0), secondaryText = Color(0xFF49454F)
    )

    val emerald = AppPalette(
        id = "emerald", name = "Emerald",
        background = Color(0xFFF0FDF6), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE6F7EE),
        primary = Color(0xFF0E9F6E), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD1FAE5), onPrimaryContainer = Color(0xFF064E3B),
        accent = Color(0xFF064E3B), border = Color(0xFFB7E4CE), secondaryText = Color(0xFF3B4A43)
    )

    val ocean = AppPalette(
        id = "ocean", name = "Ocean Blue",
        background = Color(0xFFF2F8FF), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE9F1FC),
        primary = Color(0xFF0B6BCB), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E9FF), onPrimaryContainer = Color(0xFF0A2E5C),
        accent = Color(0xFF0A2E5C), border = Color(0xFFC3D8F2), secondaryText = Color(0xFF414F61)
    )

    val sunset = AppPalette(
        id = "sunset", name = "Sunset",
        background = Color(0xFFFFF6F0), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFFDECE1),
        primary = Color(0xFFE8590C), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE3D0), onPrimaryContainer = Color(0xFF7A2E05),
        accent = Color(0xFF7A2E05), border = Color(0xFFF2CDB8), secondaryText = Color(0xFF56422F)
    )

    val rose = AppPalette(
        id = "rose", name = "Rose",
        background = Color(0xFFFFF4F8), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFFCEAF0),
        primary = Color(0xFFD6336C), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCE8), onPrimaryContainer = Color(0xFF6B0F33),
        accent = Color(0xFF6B0F33), border = Color(0xFFF0C3D5), secondaryText = Color(0xFF59404A)
    )

    val midnight = AppPalette(
        id = "midnight", name = "Midnight",
        background = Color(0xFFF4F6FB), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE8ECF5),
        primary = Color(0xFF33415C), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCE4F5), onPrimaryContainer = Color(0xFF0B1220),
        accent = Color(0xFF0B1220), border = Color(0xFFC7D0E2), secondaryText = Color(0xFF3E4757)
    )

    val royal = AppPalette(
        id = "royal", name = "Royal Indigo",
        background = Color(0xFFF6F6FF), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFEBEBFC),
        primary = Color(0xFF4F46E5), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE0E0FF), onPrimaryContainer = Color(0xFF1E1B4B),
        accent = Color(0xFF1E1B4B), border = Color(0xFFCFCFF0), secondaryText = Color(0xFF454260)
    )

    val gold = AppPalette(
        id = "gold", name = "Royal Gold",
        background = Color(0xFFFFFBF2), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFFAF0DE),
        primary = Color(0xFFB45309), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFDE7C8), onPrimaryContainer = Color(0xFF713F12),
        accent = Color(0xFF713F12), border = Color(0xFFE8D5B2), secondaryText = Color(0xFF594B31)
    )

    val teal = AppPalette(
        id = "teal", name = "Teal",
        background = Color(0xFFF0FBFB), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE3F5F5),
        primary = Color(0xFF0F9B9B), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCCF2F2), onPrimaryContainer = Color(0xFF0B4F4F),
        accent = Color(0xFF0B4F4F), border = Color(0xFFB8E2E2), secondaryText = Color(0xFF3C4F4F)
    )

    val graphite = AppPalette(
        id = "graphite", name = "Graphite",
        background = Color(0xFFF7F7F8), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFEDEDEF),
        primary = Color(0xFF4B5563), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE5E7EB), onPrimaryContainer = Color(0xFF111827),
        accent = Color(0xFF111827), border = Color(0xFFD1D5DB), secondaryText = Color(0xFF41454C)
    )

    val all: List<AppPalette> = listOf(
        purple, emerald, ocean, sunset, rose, midnight, royal, gold, teal, graphite
    )

    val default: AppPalette = purple

    fun byId(id: String): AppPalette = all.firstOrNull { it.id == id } ?: purple
}

val LocalAppPalette = staticCompositionLocalOf { ThemePalettes.default }
