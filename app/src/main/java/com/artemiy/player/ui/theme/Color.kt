package com.artemiy.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Every color the UI draws with, for one theme. See [PlayerColors] for how screens read it. */
@Immutable
data class PlayerPalette(
    val background: Color,
    /** Cards, buttons, text fields — one step off the background. */
    val surface: Color,
    /** Mini player, dialogs, unselected chips. */
    val surfaceDim: Color,
    val border: Color,
    /** Dropdown menus and other popups. */
    val menu: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** Fill of accented controls (the "Слушать" button, active toggles, sliders...). */
    val accent: Color,
    /** Text/icons drawn on top of [accent]. */
    val onAccent: Color,
    /** The accent as a text/icon color straight on the background (readable version of it). */
    val accentStandalone: Color,
    val isLight: Boolean,
)

val LocalPlayerPalette = staticCompositionLocalOf { NowPlayingPalette }

/**
 * The current theme's colors. Same names screens have always used — they just follow the theme
 * now instead of being fixed. Readable from any @Composable code.
 */
object PlayerColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.surface
    val SurfaceDim: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.surfaceDim
    val Border: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.border
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.textSecondary
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.textTertiary
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.onAccent
    val AccentStandalone: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.accentStandalone
}

/**
 * The Now Playing screen's own fixed dark look, used by its live/static blur backgrounds no
 * matter what the app theme is (they're drawn over album art, not over the app background).
 * These are exactly the colors the whole app used before themes existed.
 */
val NowPlayingPalette = PlayerPalette(
    background = Color(0xFF1C1C1E),
    surface = Color(0xFF2C2C2E),
    surfaceDim = Color(0xFF232325),
    border = Color(0xFF313133),
    menu = Color(0xFF2C2C2E),
    textPrimary = Color.White,
    textSecondary = Color(0xFF9A9A9E),
    textTertiary = Color(0xFF6D6D70),
    accent = Color.White,
    onAccent = Color.Black,
    accentStandalone = Color.White,
    isLight = false,
)

/** The three dark backgrounds, darkest first. */
enum class DarkVariant(val label: String) { AMOLED("AMOLED"), MATERIAL("Material"), GNOME("GNOME") }

/** Pure black — pixels off on AMOLED screens; surfaces just barely lifted off it. */
private val AmoledDark = PlayerPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF161616),
    surfaceDim = Color(0xFF0E0E0E),
    border = Color(0xFF262626),
    menu = Color(0xFF1C1C1C),
    textPrimary = Color.White,
    textSecondary = Color(0xFF9A9A9A),
    textTertiary = Color(0xFF6A6A6A),
    accent = Color.White,
    onAccent = Color.Black,
    accentStandalone = Color.White,
    isLight = false,
)

/** Material You's monochrome dark: neutral #131313 surface family, no tint. */
private val MaterialDark = PlayerPalette(
    background = Color(0xFF131313),
    surface = Color(0xFF252525),
    surfaceDim = Color(0xFF1C1C1C),
    border = Color(0xFF333333),
    menu = Color(0xFF2A2A2A),
    textPrimary = Color(0xFFE5E2E1),
    textSecondary = Color(0xFF9C9A99),
    textTertiary = Color(0xFF6C6A69),
    accent = Color(0xFFE5E2E1),
    onAccent = Color(0xFF131313),
    accentStandalone = Color(0xFFE5E2E1),
    isLight = false,
)

/** GNOME's stock dark theme (libadwaita): window #222226, cards white 8% on top, popovers #36363a. */
private val GnomeDark = PlayerPalette(
    background = Color(0xFF222226),
    surface = Color(0xFF343438),
    surfaceDim = Color(0xFF2E2E32),
    border = Color(0xFF3A3A3E),
    menu = Color(0xFF36363A),
    textPrimary = Color.White,
    textSecondary = Color(0xFF9B9B9E),
    textTertiary = Color(0xFF6E6E72),
    accent = Color.White,
    onAccent = Color.Black,
    accentStandalone = Color.White,
    isLight = false,
)

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/** The three light backgrounds: plain white, "concrete but lighter", and the tone between. */
enum class LightVariant(val label: String) { WHITE("Белый"), SOFT("Между"), CONCRETE("Бетон") }

fun lightPalette(variant: LightVariant): PlayerPalette {
    val (background, surface, surfaceDim, border) = when (variant) {
        LightVariant.WHITE -> listOf(Color(0xFFFFFFFF), Color(0xFFF0F0F2), Color(0xFFF5F5F7), Color(0xFFE2E2E4))
        LightVariant.SOFT -> listOf(Color(0xFFF2F2F1), Color(0xFFFFFFFF), Color(0xFFE8E8E7), Color(0xFFDADAD8))
        LightVariant.CONCRETE -> listOf(Color(0xFFE3E3E0), Color(0xFFF3F3F1), Color(0xFFD8D8D5), Color(0xFFCBCBC7))
    }
    val text = Color(0xFF2E2E33)
    return PlayerPalette(
        background = background,
        surface = surface,
        surfaceDim = surfaceDim,
        border = border,
        menu = if (variant == LightVariant.WHITE) Color(0xFFF7F7F8) else Color.White,
        textPrimary = text,
        textSecondary = Color(0xFF6E6E73),
        textTertiary = Color(0xFF9A9A9E),
        accent = text,
        onAccent = Color.White,
        accentStandalone = text,
        isLight = true,
    )
}

enum class AccentFamily(val label: String) { STOCK("Сток GNOME"), PASTEL("Пастель"), ALTERNATIVE("Альтернатива") }

/** GNOME's nine stock accents (libadwaita --accent-*), in GNOME's own order. */
private val GNOME_ACCENTS = listOf(
    Color(0xFF3584E4), // blue
    Color(0xFF2190A4), // teal
    Color(0xFF3A944A), // green
    Color(0xFFC88800), // yellow
    Color(0xFFED5B00), // orange
    Color(0xFFE62D42), // red
    Color(0xFFD56199), // pink
    Color(0xFF9141AC), // purple
    Color(0xFF6F8396), // slate
)

/** A neighboring shade for each stock accent, same order. */
private val ALTERNATIVE_ACCENTS = listOf(
    Color(0xFF5B5BD6), // indigo
    Color(0xFF0F9FB8), // cyan
    Color(0xFF1F9D74), // emerald
    Color(0xFFB89A00), // mustard
    Color(0xFFF0654A), // coral
    Color(0xFFB83A2E), // brick
    Color(0xFFC13FA0), // fuchsia
    Color(0xFF7C4DFF), // violet
    Color(0xFF7A8F78), // sage
)

fun accentColors(family: AccentFamily): List<Color> = when (family) {
    AccentFamily.STOCK -> GNOME_ACCENTS
    AccentFamily.PASTEL -> GNOME_ACCENTS.map { lerp(it, Color.White, 0.45f) }
    AccentFamily.ALTERNATIVE -> ALTERNATIVE_ACCENTS
}

/** A chosen accent: a family + position in it, or null for monochrome (the default). */
data class AccentChoice(val family: AccentFamily, val index: Int) {
    fun color(): Color = accentColors(family)[index.coerceIn(0, 8)]

    fun toKey() = "${family.name}:$index"

    companion object {
        fun fromKey(key: String?): AccentChoice? {
            val (family, index) = key?.split(":")?.takeIf { it.size == 2 } ?: return null
            return runCatching { AccentChoice(AccentFamily.valueOf(family), index.toInt()) }.getOrNull()
        }
    }
}

/** GNOME's trick for accent-colored text: clamp its lightness so it stays readable on the
 * background (Oklab L ≤ 0.5 on light themes, ≥ 0.85 on dark ones). */
private fun standaloneAccent(accent: Color, onLight: Boolean): Color {
    val oklab = accent.convert(ColorSpaces.Oklab)
    val l = if (onLight) minOf(oklab.red, 0.5f) else maxOf(oklab.red, 0.85f)
    return Color(l, oklab.green, oklab.blue, 1f, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
}

fun darkPalette(variant: DarkVariant): PlayerPalette = when (variant) {
    DarkVariant.AMOLED -> AmoledDark
    DarkVariant.MATERIAL -> MaterialDark
    DarkVariant.GNOME -> GnomeDark
}

fun appPalette(
    mode: ThemeMode,
    lightVariant: LightVariant,
    darkVariant: DarkVariant,
    accent: AccentChoice?,
    systemIsDark: Boolean,
): PlayerPalette {
    val light = when (mode) {
        ThemeMode.SYSTEM -> !systemIsDark
        ThemeMode.DARK -> false
        ThemeMode.LIGHT -> true
    }
    val base = if (light) lightPalette(lightVariant) else darkPalette(darkVariant)
    val color = accent?.color() ?: return base
    return base.copy(
        accent = color,
        onAccent = if (color.luminance() > 0.45f) Color(0xFF1B1B1F) else Color.White,
        accentStandalone = standaloneAccent(color, onLight = light),
    )
}
