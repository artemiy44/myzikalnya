package com.artemiy.player.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A soft fade behind the status bar so its icons stay readable over content scrolled under
 * it — a gradient, not a solid band. */
@Composable
fun BoxScope.StatusBarFade() {
    val background = PlayerColors.Background
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(Brush.verticalGradient(listOf(background.copy(alpha = 0.85f), background.copy(alpha = 0f)))),
    )
}

/**
 * White or near-black for the back arrow, from how bright the top of the cover art ([arts]: the
 * cover(s) at the top of the header) actually is. White until the covers have loaded.
 */
@Composable
fun rememberArrowTint(arts: List<Bitmap?>): Color {
    val tint by produceState(Color.White, arts) {
        value = withContext(Dispatchers.Default) {
            readableOn(arts.filterNotNull().map { averageLuminance(it, 0f, 0.3f) })
        }
    }
    return tint
}

/** The title and subtitle on the header are always white; this barely-there shadow is what
 * keeps them readable over bright art and over the fade alike. */
val HeroTextShadow = Shadow(color = Color.Black.copy(alpha = 0.35f), offset = Offset(0f, 2f), blurRadius = 10f)

private fun readableOn(luminances: List<Float>): Color {
    if (luminances.isEmpty()) return Color.White
    // Contrast of white vs. near-black against the average brightness behind the text.
    val lum = luminances.average().toFloat()
    val whiteContrast = 1.05f / (lum + 0.05f)
    val darkContrast = (lum + 0.05f) / (Color(0xFF1B1B1F).luminance() + 0.05f)
    return if (whiteContrast >= darkContrast) Color.White else Color(0xFF1B1B1F)
}

/** Mean relative luminance of the rows between [fromY] and [toY] (fractions of the height). */
private fun averageLuminance(bitmap: Bitmap, fromY: Float, toY: Float): Float {
    val w = bitmap.width
    val h = bitmap.height
    if (w == 0 || h == 0) return 0f
    val y0 = (h * fromY).toInt().coerceIn(0, h - 1)
    val y1 = (h * toY).toInt().coerceIn(y0 + 1, h)
    val step = maxOf(1, w / 48)
    var sum = 0f
    var n = 0
    for (y in y0 until y1 step step) {
        for (x in 0 until w step step) {
            sum += Color(bitmap.getPixel(x, y)).luminance()
            n++
        }
    }
    return if (n == 0) 0f else sum / n
}

/**
 * Lets a screen drawn edge to edge under the status bar choose the status bar icon color itself
 * (true = dark icons, for a bright image behind them). Null = follow the theme.
 */
val LocalStatusBarIconsOverride = compositionLocalOf<MutableState<Boolean?>> { mutableStateOf(null) }

/** Space under the buttons where the fade is already the solid page background. */
private val HERO_FADE = 40.dp

/** How far above the title the fade starts. */
private val HERO_FADE_ABOVE_TITLE = 56.dp

/**
 * Album/artist page header: the cover art runs edge to edge (up under the status bar), with the
 * title and buttons laid straight on it and the fade into the page starting just below them.
 */
@Composable
fun HeroOverArt(
    topTint: Color,
    onBack: () -> Unit,
    art: @Composable BoxScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    var contentHeightPx by remember { mutableIntStateOf(0) }

    // The status bar sits on the cover here, not on the page background — its icons follow the
    // cover's brightness (same call as the back arrow) instead of the theme.
    val statusBarOverride = LocalStatusBarIconsOverride.current
    val darkStatusIcons = topTint != Color.White
    DisposableEffect(darkStatusIcons) {
        statusBarOverride.value = darkStatusIcons
        onDispose { statusBarOverride.value = null }
    }

    Box(modifier = Modifier.fillMaxWidth().height(430.dp + statusTop)) {
        art()
        val contentHeight = with(density) { contentHeightPx.toDp() }
        // A soft dark scrim under the title and buttons (white text reads on any cover over it)...
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(HERO_FADE + contentHeight + HERO_FADE_ABOVE_TITLE)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Black.copy(alpha = 0.4f),
                        1f to Color.Black.copy(alpha = 0.55f),
                    ),
                ),
        )
        // ...which then blends into the page's own background just below the buttons.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(HERO_FADE + 24.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, PlayerColors.Background))),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Назад",
            tint = topTint,
            modifier = Modifier
                .padding(start = 16.dp, top = statusTop + 12.dp)
                .size(24.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() },
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 24.dp, end = 24.dp, bottom = HERO_FADE + 4.dp)
                .onGloballyPositioned { contentHeightPx = it.size.height },
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
