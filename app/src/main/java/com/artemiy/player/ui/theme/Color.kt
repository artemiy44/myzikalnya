package com.artemiy.player.ui.theme

import com.artemiy.player.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    /** The accent for marks that aren't reading text — tab icons, the logo, the scroller thumb: on a
     * light theme it keeps pastel accents pastel instead of darkening them to what text needs. */
    val accentMark: Color = accentStandalone,
    /** An accent set's second colour: the "on" and selected states (switches, chosen chips). Same as [accent] when the accent is a single colour. */
    val accent2: Color = accent,
    val onAccent2: Color = onAccent,
    /** An accent set's third colour: sliders and the scroller. Same as [accent] for a single colour. */
    val accent3: Color = accent,
)

val LocalPlayerPalette = staticCompositionLocalOf { NowPlayingPalette }

/** The app's own theme, even inside the player (which swaps in its own palette) — for things that
 * belong to the app rather than the player, like the track info sheet. */
val LocalAppPalette = staticCompositionLocalOf { NowPlayingPalette }

/**
 * The current theme's colors. Same names screens have always used — they just follow the theme
 * now instead of being fixed. Readable from any @Composable code.
 */
object PlayerColors {
    val Background: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.surface
    val SurfaceDim: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.surfaceDim
    val Border: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.border
    val Menu: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.menu
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.textSecondary
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.textTertiary
    val Accent: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.accent
    val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.onAccent
    val Accent2: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.accent2
    val OnAccent2: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.onAccent2
    val Accent3: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.accent3
    val AccentMark: Color @Composable @ReadOnlyComposable get() = LocalPlayerPalette.current.accentMark
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
/** Which shelf of backgrounds a variant sits on in Settings: the neutral ones, the ones with a tint, or the one made from the wallpaper. */
enum class BackgroundGroup { NEUTRAL, TINTED, SCHEMES, WALLPAPER }

enum class DarkVariant(@androidx.annotation.StringRes val labelRes: Int, val group: BackgroundGroup = BackgroundGroup.NEUTRAL) {
    AMOLED(R.string.dark_amoled), MATERIAL(R.string.dark_material), GNOME(R.string.dark_gnome),
    MIDNIGHT(R.string.dark_midnight, BackgroundGroup.TINTED), COFFEE(R.string.dark_coffee, BackgroundGroup.TINTED),
    FOREST(R.string.dark_forest, BackgroundGroup.TINTED), PLUM(R.string.dark_plum, BackgroundGroup.TINTED),
    // The well-loved editor colour schemes, in spirit: a dusky violet, a cold fjord, glowing embers, rosewood, indigo, ink.
    DUSK(R.string.dark_dusk, BackgroundGroup.SCHEMES), FJORD(R.string.dark_fjord, BackgroundGroup.SCHEMES),
    EMBER(R.string.dark_ember, BackgroundGroup.SCHEMES), ROSEWOOD(R.string.dark_rosewood, BackgroundGroup.SCHEMES),
    INDIGO(R.string.dark_indigo, BackgroundGroup.SCHEMES), INK(R.string.dark_ink, BackgroundGroup.SCHEMES),
    WALLPAPER(R.string.bg_wallpaper, BackgroundGroup.WALLPAPER),
}

val DarkVariant.label: String @Composable get() = androidx.compose.ui.res.stringResource(labelRes)

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
enum class LightVariant(@androidx.annotation.StringRes val labelRes: Int, val group: BackgroundGroup = BackgroundGroup.NEUTRAL) {
    WHITE(R.string.light_white), SOFT(R.string.light_soft), CONCRETE(R.string.light_concrete),
    PAPER(R.string.light_paper, BackgroundGroup.TINTED), MIST(R.string.light_mist, BackgroundGroup.TINTED),
    SNOW(R.string.light_snow, BackgroundGroup.SCHEMES), CREAM(R.string.light_cream, BackgroundGroup.SCHEMES),
    DAWN(R.string.light_dawn, BackgroundGroup.SCHEMES), SAGE(R.string.light_sage, BackgroundGroup.SCHEMES),
    LILAC(R.string.light_lilac, BackgroundGroup.SCHEMES),
    WALLPAPER(R.string.bg_wallpaper, BackgroundGroup.WALLPAPER),
}

val LightVariant.label: String @Composable get() = androidx.compose.ui.res.stringResource(labelRes)

fun lightPalette(variant: LightVariant): PlayerPalette {
    val (background, surface, surfaceDim, border) = when (variant) {
        LightVariant.WHITE -> listOf(Color(0xFFFFFFFF), Color(0xFFF0F0F2), Color(0xFFF5F5F7), Color(0xFFE2E2E4))
        LightVariant.SOFT -> listOf(Color(0xFFF2F2F1), Color(0xFFFFFFFF), Color(0xFFE8E8E7), Color(0xFFDADAD8))
        LightVariant.CONCRETE -> listOf(Color(0xFFE3E3E0), Color(0xFFF3F3F1), Color(0xFFD8D8D5), Color(0xFFCBCBC7))
        // Warm paper and a cool mist: the same set of steps, with a faint tint.
        LightVariant.PAPER -> listOf(Color(0xFFF4EFE6), Color(0xFFFFFCF6), Color(0xFFEAE4D8), Color(0xFFDCD5C6))
        LightVariant.MIST -> listOf(Color(0xFFE9EFF3), Color(0xFFF6F9FB), Color(0xFFDDE5EB), Color(0xFFCCD6DE))
        LightVariant.SNOW -> listOf(Color(0xFFECEFF4), Color(0xFFF8FAFC), Color(0xFFE0E5EC), Color(0xFFCFD6E0))
        LightVariant.CREAM -> listOf(Color(0xFFF5EED3), Color(0xFFFFFBEA), Color(0xFFEAE2C2), Color(0xFFDBD2AE))
        LightVariant.DAWN -> listOf(Color(0xFFFAF1E8), Color(0xFFFFFAF4), Color(0xFFEFE3D5), Color(0xFFE2D3C2))
        LightVariant.SAGE -> listOf(Color(0xFFE7EDE4), Color(0xFFF5F9F3), Color(0xFFDAE3D6), Color(0xFFC8D4C3))
        LightVariant.LILAC -> listOf(Color(0xFFEEEAF6), Color(0xFFFAF8FD), Color(0xFFE2DCEF), Color(0xFFD1C9E2))
        // Made from the wallpaper: a very pale wash of its main colour.
        LightVariant.WALLPAPER -> {
            val bg = wallpaperTint(lightness = 0.95f, chroma = 0.014f) ?: Color(0xFFF2F2F1)
            listOf(bg, lerp(bg, Color.White, 0.75f), lerp(bg, Color.Black, 0.04f), lerp(bg, Color.Black, 0.10f))
        }
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

enum class AccentFamily(@androidx.annotation.StringRes val labelRes: Int) {
    STOCK(R.string.accent_stock), PASTEL(R.string.accent_pastel), ALTERNATIVE(R.string.accent_alternative),
    /** Android 12+: the accent colours the system made from the wallpaper (Material You). */
    WALLPAPER(R.string.accent_wallpaper),
    /** Five ready sets of three accents each (every three swatches are one set). */
    SETS(R.string.accent_sets),
}

/** The system's wallpaper colours, nine tones from its three accent families; read again when the app comes back to the front. */
object WallpaperAccents {
    var colors by androidx.compose.runtime.mutableStateOf<List<Color>>(emptyList())
        private set

    val available: Boolean get() = android.os.Build.VERSION.SDK_INT >= 31

    fun refresh(context: android.content.Context) {
        if (!available) return
        val ids = intArrayOf(
            android.R.color.system_accent1_300, android.R.color.system_accent1_500, android.R.color.system_accent1_700,
            android.R.color.system_accent2_300, android.R.color.system_accent2_500, android.R.color.system_accent2_700,
            android.R.color.system_accent3_300, android.R.color.system_accent3_500, android.R.color.system_accent3_700,
        )
        val now = ids.map { Color(context.getColor(it)) }
        if (now != colors) colors = now
    }
}

val AccentFamily.label: String @Composable get() = androidx.compose.ui.res.stringResource(labelRes)

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

/** Six sets of three accents (every three colours are one set): haze (grey, soft blue, butter), candy, sunset, forest, ocean, scarlet. */
private val SET_ACCENTS = listOf(
    Color(0xFF8E9AA6), Color(0xFF8DB4EC), Color(0xFFF2C94C),
    Color(0xFFFF8FB1), Color(0xFF6FD3B0), Color(0xFFB39BFF),
    Color(0xFFFF7A59), Color(0xFFFFB547), Color(0xFF9B5DE5),
    Color(0xFF7BA05B), Color(0xFF3FA796), Color(0xFFD9B26F),
    Color(0xFF3A6EA5), Color(0xFF38BDF8), Color(0xFF7DD3C0),
    Color(0xFFE63946), Color(0xFFF1A7A1), Color(0xFF457B9D),
)

/** How many accent sets there are. */
val ACCENT_SET_COUNT: Int get() = SET_ACCENTS.size / 3

fun accentColors(family: AccentFamily): List<Color> = when (family) {
    AccentFamily.SETS -> SET_ACCENTS
    AccentFamily.STOCK -> GNOME_ACCENTS
    AccentFamily.PASTEL -> GNOME_ACCENTS.map { lerp(it, Color.White, 0.45f) }
    AccentFamily.ALTERNATIVE -> ALTERNATIVE_ACCENTS
    // Without Material You (or before it's read) a saved choice still has to show something.
    AccentFamily.WALLPAPER -> WallpaperAccents.colors.ifEmpty { GNOME_ACCENTS }
}

/** A chosen accent: a family + position in it, or null for monochrome (the default). */
data class AccentChoice(val family: AccentFamily, val index: Int) {
    fun color(): Color = accentColors(family).let { it[index.coerceIn(0, it.lastIndex)] }

    fun toKey() = "${family.name}:$index"

    /** The three colours of the chosen set, when the accent is one of the sets (null for a single colour). */
    fun set(): List<Color>? = if (family == AccentFamily.SETS) accentColors(family).let { c ->
        val start = (index.coerceIn(0, c.lastIndex) / 3) * 3
        c.subList(start, start + 3)
    } else null

    companion object {
        fun fromKey(key: String?): AccentChoice? {
            val (family, index) = key?.split(":")?.takeIf { it.size == 2 } ?: return null
            return runCatching { AccentChoice(AccentFamily.valueOf(family), index.toInt()) }.getOrNull()
        }
    }
}

/** GNOME's trick for accent-colored text: clamp its lightness so it stays readable on the
 * background (Oklab L ≤ 0.5 on light themes, ≥ 0.85 on dark ones). */
/** Oklab lightness an accent needs as text/icons on a dark ground: about 5:1 against it. */
private const val STANDALONE_MIN_LIGHTNESS_ON_DARK = 0.70f

private fun standaloneAccent(accent: Color, onLight: Boolean, lightCap: Float = 0.5f): Color {
    val oklab = accent.convert(ColorSpaces.Oklab)
    // Only as much lighter (on dark) or darker (on light) as it takes to read well — lifted all
    // the way to 0.85 a saturated red came out pastel pink, not the red that was picked.
    val l = if (onLight) minOf(oklab.red, lightCap) else maxOf(oklab.red, STANDALONE_MIN_LIGHTNESS_ON_DARK)
    // A saturated color pushed that light doesn't exist on screen; letting the conversion clip it
    // shifts its hue (GNOME blue turned turquoise). Instead give up just enough saturation to fit,
    // keeping the hue itself.
    var low = 0f
    var high = 1f
    repeat(20) {
        val mid = (low + high) / 2
        if (inSrgbGamut(l, oklab.green * mid, oklab.blue * mid)) low = mid else high = mid
    }
    return Color(l, oklab.green * low, oklab.blue * low, 1f, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
}

/** Whether this Oklab color can be shown in sRGB without clipping (Björn Ottosson's formulas). */
private fun inSrgbGamut(lightness: Float, a: Float, b: Float): Boolean {
    val l = (lightness + 0.3963377774f * a + 0.2158037573f * b).let { it * it * it }
    val m = (lightness - 0.1055613458f * a - 0.0638541728f * b).let { it * it * it }
    val s = (lightness - 0.0894841775f * a - 1.2914855480f * b).let { it * it * it }
    val r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
    val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
    val bl = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    val eps = 0.0005f
    return r in -eps..1f + eps && g in -eps..1f + eps && bl in -eps..1f + eps
}

/** A dark background with a tint: the steps above it (cards, menus, hairlines) are the same few percent of white on any of them. */
private fun tintedDark(background: Color) = GnomeDark.copy(
    background = background,
    surface = lerp(background, Color.White, 0.08f),
    surfaceDim = lerp(background, Color.White, 0.045f),
    border = lerp(background, Color.White, 0.13f),
    menu = lerp(background, Color.White, 0.10f),
)

private val MidnightDark = tintedDark(Color(0xFF0E1522))
private val CoffeeDark = tintedDark(Color(0xFF1B1613))
private val ForestDark = tintedDark(Color(0xFF101A14))
private val PlumDark = tintedDark(Color(0xFF1A1322))
private val DuskDark = tintedDark(Color(0xFF1E1E2E))
private val FjordDark = tintedDark(Color(0xFF242933))
private val EmberDark = tintedDark(Color(0xFF282320))
private val RosewoodDark = tintedDark(Color(0xFF1F1824))
private val IndigoDark = tintedDark(Color(0xFF1A1B26))
private val InkDark = tintedDark(Color(0xFF282A36))

/**
 * The wallpaper's main colour (the system's accent) turned down to a background's lightness and a
 * whisper of colour, or null when the phone doesn't hand the colours over (Android before 12).
 */
private fun wallpaperTint(lightness: Float, chroma: Float): Color? {
    val source = WallpaperAccents.colors.getOrNull(1) ?: return null
    val lab = source.convert(ColorSpaces.Oklab)
    val a = lab.green
    val b = lab.blue
    val length = kotlin.math.sqrt(a * a + b * b)
    if (length < 1e-4f) return Color(lightness, 0f, 0f, 1f, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
    return Color(lightness, a / length * chroma, b / length * chroma, 1f, ColorSpaces.Oklab).convert(ColorSpaces.Srgb)
}

fun darkPalette(variant: DarkVariant): PlayerPalette = when (variant) {
    DarkVariant.AMOLED -> AmoledDark
    DarkVariant.MATERIAL -> MaterialDark
    DarkVariant.GNOME -> GnomeDark
    DarkVariant.MIDNIGHT -> MidnightDark
    DarkVariant.COFFEE -> CoffeeDark
    DarkVariant.FOREST -> ForestDark
    DarkVariant.PLUM -> PlumDark
    DarkVariant.DUSK -> DuskDark
    DarkVariant.FJORD -> FjordDark
    DarkVariant.EMBER -> EmberDark
    DarkVariant.ROSEWOOD -> RosewoodDark
    DarkVariant.INDIGO -> IndigoDark
    DarkVariant.INK -> InkDark
    DarkVariant.WALLPAPER -> wallpaperTint(lightness = 0.17f, chroma = 0.03f)?.let { tintedDark(it) } ?: GnomeDark
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
    fun on(c: Color) = if (c.luminance() > 0.45f) Color(0xFF1B1B1F) else Color.White
    val set = accent.set()
    // A set of three: the first is the accent as ever; the second and third go to the places that have an accent
    // of their own to give (switches and chosen chips; sliders and the scroller).
    val second = set?.get(1) ?: color
    val third = set?.get(2) ?: color
    return base.copy(
        accent = color,
        onAccent = on(color),
        accentStandalone = standaloneAccent(color, onLight = light),
        accentMark = standaloneAccent(color, onLight = light, lightCap = 0.8f),
        accent2 = second,
        onAccent2 = on(second),
        accent3 = third,
    )
}
