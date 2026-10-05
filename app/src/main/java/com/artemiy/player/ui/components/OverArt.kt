package com.artemiy.player.ui.components

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Icon
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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

/**
 * The colour the page should "bleed" into below a cover header: the average of the cover's lower
 * part, pulled dark enough for the white title on top of it. Null until the cover has loaded.
 */
@Composable
fun rememberHeroWash(art: Bitmap?, key: String? = null): Color? {
    val wash by produceState<Color?>(key?.let { washCache[it] }, art) {
        if (art != null) {
            value = withContext(Dispatchers.Default) { washFrom(art) }
            if (key != null) washCache[key] = value!!
        }
    }
    return wash
}

/** Wash colours already read, by page: coming back to a page (or scrolling its header back into
 * view) shows the colour at once instead of reading it again. */
private val washCache = java.util.concurrent.ConcurrentHashMap<String, Color>()

private fun washFrom(bitmap: Bitmap): Color {
    val w = bitmap.width
    val h = bitmap.height
    if (w == 0 || h == 0) return Color(0xFF1B1B1F)
    val y0 = (h * 0.7f).toInt().coerceIn(0, h - 1)
    val step = maxOf(1, w / 48)
    var r = 0f; var g = 0f; var b = 0f; var n = 0
    for (y in y0 until h step step) for (x in 0 until w step step) {
        val c = bitmap.getPixel(x, y)
        r += android.graphics.Color.red(c); g += android.graphics.Color.green(c); b += android.graphics.Color.blue(c); n++
    }
    if (n == 0) return Color(0xFF1B1B1F)
    var c = Color(r / n / 255f, g / n / 255f, b / n / 255f)
    return heroWashFor(c)
}

/** White text has to read on the wash: darken only as much as needed, so it stays lively. */
fun heroWashFor(color: Color): Color {
    var c = color
    var guard = 0
    while (c.luminance() > 0.18f && guard++ < 12) c = Color(c.red * 0.9f, c.green * 0.9f, c.blue * 0.9f)
    return c
}

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
/** The "Vivid" picture-page style (a setting): the picture's colour flows on down the page. */
val LocalHeroBleed = compositionLocalOf { true }

val LocalStatusBarIconsOverride = compositionLocalOf<MutableState<Boolean?>> { mutableStateOf(null) }

/** Space under the buttons where the fade is already the solid page background. */
private val HERO_FADE = 40.dp

/** How far the wash colour takes to drain into the page background below the header. */
val WASH_MAX_DRAIN = 360.dp

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
    height: Dp = 430.dp,
    /** The page's slim top bar is showing (the picture has scrolled away): the status bar then
     * sits on the page's own background and follows the theme, not the cover. */
    barShown: Boolean = false,
    /** When set, the usual fade into the page background is replaced by this colour, which then
     * drains into the page background over the first few rows below the header. */
    wash: Color? = null,
    /** How far the wash drains below the header (up to [WASH_MAX_DRAIN]). */
    washDrain: Dp = WASH_MAX_DRAIN,
    /** Stable id of this page, so its wash colour is remembered between visits. */
    washKey: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val density = LocalDensity.current
    var contentHeightPx by remember { mutableIntStateOf(0) }

    // The status bar sits on the cover here, not on the page background — its icons follow the
    // cover's brightness (same call as the back arrow) instead of the theme.
    val statusBarOverride = LocalStatusBarIconsOverride.current
    val darkStatusIcons = topTint != Color.White
    DisposableEffect(darkStatusIcons, barShown) {
        statusBarOverride.value = if (barShown) null else darkStatusIcons
        onDispose { statusBarOverride.value = null }
    }

    // "Vivid" style: the picture's own colour flows on down the page. A cover passes [wash] (read
    // from its bitmap); a drawn picture is photographed once it's up and the colour read off that.
    val vivid = LocalHeroBleed.current
    val layer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    var capturing by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf(washKey?.let { washCache[it] }) }
    if (vivid && wash == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            for (waitMs in if (captured == null) longArrayOf(0, 600, 1500) else longArrayOf(1500)) {
                kotlinx.coroutines.delay(waitMs)
                capturing = true
                androidx.compose.runtime.withFrameNanos { }
                androidx.compose.runtime.withFrameNanos { }
                val color = runCatching {
                    val soft = layer.toImageBitmap().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
                    washFrom(soft)
                }.getOrNull()
                capturing = false
                if (color != null) {
                    captured = color
                    if (washKey != null) washCache[washKey] = color
                }
            }
        }
    }
    val target = if (vivid) (wash ?: captured) else null
    val washState by androidx.compose.animation.animateColorAsState(
        targetValue = target ?: Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "heroWash",
    )
    // How much of the vivid look is in: it takes over from the classic fade smoothly once the
    // colour is known (at once when it's already cached).
    val v by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (target != null) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(550),
        label = "heroVivid",
    )
    val pageBg = PlayerColors.Background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height + statusTop)
            .then(
                if (!vivid) Modifier else Modifier.drawBehind {
                    // Runs out of the header's bounds, behind the rows below it.
                    drawRect(
                        brush = Brush.verticalGradient(listOf(washState, pageBg), startY = size.height, endY = size.height + washDrain.toPx()),
                        alpha = v,
                        topLeft = Offset(0f, size.height),
                        size = androidx.compose.ui.geometry.Size(size.width, washDrain.toPx()),
                    )
                },
            ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().drawWithContent {
                if (capturing) {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                } else {
                    drawContent()
                }
            },
            content = art,
        )
        val contentHeight = with(density) { contentHeightPx.toDp() }
        if (vivid) {
            // The picture melts into the wash colour behind the title and buttons.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(HERO_FADE + contentHeight + HERO_FADE_ABOVE_TITLE + 60.dp)
                    .alpha(v)
                    .background(Brush.verticalGradient(0f to Color.Transparent, 0.75f to washState.copy(alpha = 0.9f), 1f to washState)),
            )
        }
        if (!vivid || v < 1f) {
            val classic = if (vivid) 1f - v else 1f
            // A soft dark scrim under the title and buttons (white text reads on any cover over it)...
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(HERO_FADE + contentHeight + HERO_FADE_ABOVE_TITLE)
                    .alpha(classic)
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
                    .alpha(classic)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, PlayerColors.Background))),
            )
        }
        Icon(
            imageVector = AppIcons.Back,
            contentDescription = stringResource(R.string.cd_back),
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
