package com.artemiy.player.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** How the tabs look — Settings → Внешний вид → «Стиль интерфейса». The players have their own switch. */
enum class UiStyle {
    /** Calm and flat: the look the app has always had. */
    CLASSIC,

    /** Material 3 Expressive-ish: poster headers, floating bars, tonal cards, playful shapes. */
    EXPRESSIVE,

    /**
     * The "Диск" look, on the Dial player's design language: heavy two-tone headings, rounded-square
     * tiles, matte panels, one accent (only the main action), digits as part of the design. Where a
     * page has no special disc form yet it shows the calm classic one.
     */
    DISC,
}

val LocalUiStyle = staticCompositionLocalOf { UiStyle.CLASSIC }

/** Shorthand for the shared components: is the expressive look on? */
val expressiveUi: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalUiStyle.current == UiStyle.EXPRESSIVE

/** Shorthand for the shared components: is the "Диск" look on? */
val discUi: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalUiStyle.current == UiStyle.DISC

/** A tab's or page's big title: poster-heavy and tighter in the expressive style, huge and heavy in the disc one. */
val pageTitleStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalTextStyle.current.merge(
        when (LocalUiStyle.current) {
            UiStyle.DISC -> TextStyle(fontSize = 52.sp, lineHeight = 50.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp)
            UiStyle.EXPRESSIVE -> TextStyle(fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp)
            UiStyle.CLASSIC -> TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        },
    )

/** A section's title inside a page ("Mixes for you", "Albums"…). */
val sectionTitleStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalTextStyle.current.merge(
        when (LocalUiStyle.current) {
            UiStyle.DISC -> TextStyle(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
            UiStyle.EXPRESSIVE -> TextStyle(fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
            UiStyle.CLASSIC -> TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold)
        },
    )

/** How much of the tab area's bottom the floating mini player + tab bar cover. The expressive
 * style lets pages run on under them (no backdrop), so scrolling pages pad their ends by this. */
val LocalBarsInset = compositionLocalOf { 0.dp }

/** Room at a scrolling page's end so its last rows can come out from under the floating bars. */
@Composable
fun Modifier.barsInset(): Modifier = padding(bottom = LocalBarsInset.current)

/**
 * The measures and colours of the disc look, in one place so every page uses the same ones: the
 * rounded-square tiles (buttons, rows, tabs), the matte panels, and the second tone of the headings.
 */
object Disc {
    /** A tile's corner: the buttons, rows and tabs. */
    val tileCorner = 20.dp
    /** A small tile's corner (a square holding an icon, a tab). */
    val smallCorner = 14.dp
    /** A panel's corner: the mini player, the mixes panel, the queue window. */
    val panelCorner = 26.dp
    /** The height of a navigation tile in the bottom bar. */
    val navTileHeight = 56.dp
    /** The air at a page's sides. */
    val pageSide = 20.dp

    /** A tile on the page: a faint plate in the text's own colour. */
    val tileFill: androidx.compose.ui.graphics.Color
        @Composable @ReadOnlyComposable
        get() = PlayerColors.TextPrimary.copy(alpha = 0.07f)

    /** A panel: one step stronger than a tile. */
    val panelFill: androidx.compose.ui.graphics.Color
        @Composable @ReadOnlyComposable
        get() = PlayerColors.TextPrimary.copy(alpha = 0.10f)

    /** The second tone of a two-tone heading and of the quiet numbers. */
    val quiet: androidx.compose.ui.graphics.Color
        @Composable @ReadOnlyComposable
        get() = PlayerColors.TextPrimary.copy(alpha = 0.45f)
}
