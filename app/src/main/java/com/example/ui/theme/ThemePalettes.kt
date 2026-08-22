package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A complete app theme palette. Dark themes actually render dark. */
data class AppPalette(
    val id: String,
    val name: String,
    val isDark: Boolean,
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

    // ---------- LIGHT THEMES ----------
    val purple = AppPalette(
        id = "purple", name = "Purple", isDark = false,
        background = Color(0xFFFDF7FF), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFF3EDF7),
        primary = Color(0xFF6750A4), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE8DEF8), onPrimaryContainer = Color(0xFF21005D),
        accent = Color(0xFF21005D), border = Color(0xFFCAC4D0), secondaryText = Color(0xFF49454F)
    )
    val emerald = AppPalette(
        id = "emerald", name = "Emerald", isDark = false,
        background = Color(0xFFF0FDF6), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE6F7EE),
        primary = Color(0xFF0E9F6E), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD1FAE5), onPrimaryContainer = Color(0xFF064E3B),
        accent = Color(0xFF064E3B), border = Color(0xFFB7E4CE), secondaryText = Color(0xFF3B4A43)
    )
    val ocean = AppPalette(
        id = "ocean", name = "Ocean Blue", isDark = false,
        background = Color(0xFFF2F8FF), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE9F1FC),
        primary = Color(0xFF0B6BCB), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6E9FF), onPrimaryContainer = Color(0xFF0A2E5C),
        accent = Color(0xFF0A2E5C), border = Color(0xFFC3D8F2), secondaryText = Color(0xFF414F61)
    )
    val sunset = AppPalette(
        id = "sunset", name = "Sunset", isDark = false,
        background = Color(0xFFFFF6F0), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFFDECE1),
        primary = Color(0xFFE8590C), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFE3D0), onPrimaryContainer = Color(0xFF7A2E05),
        accent = Color(0xFF7A2E05), border = Color(0xFFF2CDB8), secondaryText = Color(0xFF56422F)
    )
    val rose = AppPalette(
        id = "rose", name = "Rose", isDark = false,
        background = Color(0xFFFFF4F8), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFFCEAF0),
        primary = Color(0xFFD6336C), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCE8), onPrimaryContainer = Color(0xFF6B0F33),
        accent = Color(0xFF6B0F33), border = Color(0xFFF0C3D5), secondaryText = Color(0xFF59404A)
    )
    val gold = AppPalette(
        id = "gold", name = "Royal Gold", isDark = false,
        background = Color(0xFFFFFBF2), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFFAF0DE),
        primary = Color(0xFFB45309), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFDE7C8), onPrimaryContainer = Color(0xFF713F12),
        accent = Color(0xFF713F12), border = Color(0xFFE8D5B2), secondaryText = Color(0xFF594B31)
    )
    val teal = AppPalette(
        id = "teal", name = "Teal", isDark = false,
        background = Color(0xFFF0FBFB), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE3F5F5),
        primary = Color(0xFF0F9B9B), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCCF2F2), onPrimaryContainer = Color(0xFF0B4F4F),
        accent = Color(0xFF0B4F4F), border = Color(0xFFB8E2E2), secondaryText = Color(0xFF3C4F4F)
    )

    // ---------- DARK THEMES (what you asked for) ----------
    val midnight = AppPalette(
        id = "midnight", name = "Midnight Navy", isDark = true,
        background = Color(0xFF0B1220), surface = Color(0xFF141E2E), surfaceVariant = Color(0xFF1C2A40),
        primary = Color(0xFF5B8DEF), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF243B5E), onPrimaryContainer = Color(0xFFDCE7FF),
        accent = Color(0xFF93B4F8), border = Color(0xFF2C3E5C), secondaryText = Color(0xFFA9B7D0)
    )
    val graphite = AppPalette(
        id = "graphite", name = "Pure Black", isDark = true,
        background = Color(0xFF000000), surface = Color(0xFF0D0D0F), surfaceVariant = Color(0xFF17171B),
        primary = Color(0xFF8AB4FF), onPrimary = Color(0xFF00102A),
        primaryContainer = Color(0xFF1E2A3A), onPrimaryContainer = Color(0xFFD6E2FF),
        accent = Color(0xFFE5E7EB), border = Color(0xFF2A2A30), secondaryText = Color(0xFF9CA3AF)
    )
    val royalDark = AppPalette(
        id = "royaldark", name = "Royal Dark", isDark = true,
        background = Color(0xFF0E0A1F), surface = Color(0xFF181228), surfaceVariant = Color(0xFF241B3A),
        primary = Color(0xFFB49DFF), onPrimary = Color(0xFF24005C),
        primaryContainer = Color(0xFF3A2A66), onPrimaryContainer = Color(0xFFEADDFF),
        accent = Color(0xFFD6C4FF), border = Color(0xFF3A3060), secondaryText = Color(0xFFB9ACC9)
    )
    val darkTeal = AppPalette(
        id = "darkteal", name = "Dark Teal", isDark = true,
        background = Color(0xFF021A1A), surface = Color(0xFF062A2A), surfaceVariant = Color(0xFF0B3A38),
        primary = Color(0xFF5BE0D0), onPrimary = Color(0xFF003731),
        primaryContainer = Color(0xFF0E4A47), onPrimaryContainer = Color(0xFFB8FFF6),
        accent = Color(0xFF9BF5E8), border = Color(0xFF17524E), secondaryText = Color(0xFF9CC5C0)
    )

    // Greenish-blue / Aqua (light)
    val aqua = AppPalette(
        id = "aqua", name = "Aqua", isDark = false,
        background = Color(0xFFEAF8F8), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFD9F0F0),
        primary = Color(0xFF00897B), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB2DFDB), onPrimaryContainer = Color(0xFF004D40),
        accent = Color(0xFF004D40), border = Color(0xFFB2DFDB), secondaryText = Color(0xFF3B514D)
    )

    // Slate Grey (light)
    val slateGrey = AppPalette(
        id = "grey", name = "Slate Grey", isDark = false,
        background = Color(0xFFF5F6F8), surface = Color(0xFFFFFFFF), surfaceVariant = Color(0xFFE9ECF1),
        primary = Color(0xFF546E7A), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD6DEE5), onPrimaryContainer = Color(0xFF263238),
        accent = Color(0xFF263238), border = Color(0xFFC6CFD8), secondaryText = Color(0xFF455A64)
    )

    // Final curated list: 1 white + black + navy + grey + green + greenish-blue
    val all: List<AppPalette> = listOf(
        purple,        // the one light/white theme
        graphite,      // Pure Black (dark)
        midnight,      // Midnight Navy (dark)
        slateGrey,     // Slate Grey
        emerald,       // Emerald Green
        aqua           // Greenish-blue / Aqua
    )

    val default: AppPalette = purple

    fun byId(id: String): AppPalette = all.firstOrNull { it.id == id } ?: purple
}

val LocalAppPalette = staticCompositionLocalOf { ThemePalettes.default }
