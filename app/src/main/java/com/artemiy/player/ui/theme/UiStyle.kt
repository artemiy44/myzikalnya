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
}

val LocalUiStyle = staticCompositionLocalOf { UiStyle.CLASSIC }

/** Shorthand for the shared components: is the expressive look on? */
val expressiveUi: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalUiStyle.current == UiStyle.EXPRESSIVE

/** A tab's or page's big title: poster-heavy and tighter in the expressive style. */
val pageTitleStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalTextStyle.current.merge(
        if (expressiveUi) {
            TextStyle(fontSize = 38.sp, lineHeight = 42.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp)
        } else {
            TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        },
    )

/** A section's title inside a page ("Mixes for you", "Albums"…). */
val sectionTitleStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = LocalTextStyle.current.merge(
        if (expressiveUi) {
            TextStyle(fontSize = 21.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)
        } else {
            TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold)
        },
    )

/** How much of the tab area's bottom the floating mini player + tab bar cover. The expressive
 * style lets pages run on under them (no backdrop), so scrolling pages pad their ends by this. */
val LocalBarsInset = compositionLocalOf { 0.dp }

/** Room at a scrolling page's end so its last rows can come out from under the floating bars. */
@Composable
fun Modifier.barsInset(): Modifier = padding(bottom = LocalBarsInset.current)
