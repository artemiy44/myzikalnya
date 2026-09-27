package com.artemiy.player.ui.nowplaying

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.ART_SIZE_FULL
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.rememberAlbumArtBitmap
import com.artemiy.player.ui.components.stackBlur
import com.artemiy.player.ui.theme.LocalPlayerPalette
import com.artemiy.player.ui.theme.NowPlayingPalette
import com.artemiy.player.ui.theme.PaletteScope
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos

// Now Playing's background: live/static blur, scrim, and the adaptive gray text color.

/** Minimum contrast ratio (WCAG formula) the gray must keep against the background. The plain
 * gray on the app's flat dark background is ~6:1, so this only kicks in over bright art. */
internal const val SECONDARY_MIN_CONTRAST = 3.5f

/**
 * Prepares the "живой блюр" background bitmap: crop to square, downscale HARD (blur cost scales
 * with pixel count, so a tiny source blurs in a fraction of a millisecond), boost saturation so
 * it doesn't read as washed-out, then run a classic software stack blur — no live
 * RenderEffect/`Modifier.blur()` anywhere in this, which is what caused banding on this device
 * every previous time this project tried a live-blurred version of the art. Runs once per song
 * change, off the main thread; the result is a small, already-blurred, static [Bitmap].
 */
@Composable
internal fun rememberLivingBackgroundBitmap(uri: android.net.Uri?): android.graphics.Bitmap? {
    val source = rememberAlbumArtBitmap(uri, ART_SIZE_THUMB)
    var result by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(source) {
        val bmp = source
        result = if (bmp == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runCatching { prepareLivingBackgroundBitmap(bmp) }.getOrNull()
            }
        }
    }
    return result
}

internal fun prepareLivingBackgroundBitmap(source: android.graphics.Bitmap): android.graphics.Bitmap {
    val size = minOf(source.width, source.height)
    val xOffset = (source.width - size) / 2
    val yOffset = (source.height - size) / 2
    val square = android.graphics.Bitmap.createBitmap(source, xOffset, yOffset, size, size)
    val targetPx = 72
    val small = if (size > targetPx) {
        android.graphics.Bitmap.createScaledBitmap(square, targetPx, targetPx, true)
    } else {
        square
    }

    val saturated = android.graphics.Bitmap.createBitmap(
        small.width,
        small.height,
        android.graphics.Bitmap.Config.ARGB_8888,
    )
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = android.graphics.ColorMatrixColorFilter(
            android.graphics.ColorMatrix().apply { setSaturation(2.2f) },
        )
    }
    android.graphics.Canvas(saturated).drawBitmap(small, 0f, 0f, paint)

    return stackBlur(saturated, radius = 14)
}

/** Wrapper so the bitmap can be passed to composables that should skip recomposition on the
 * ~100ms position ticks — a raw [android.graphics.Bitmap] isn't a type Compose treats as stable. */
@androidx.compose.runtime.Immutable
internal class BackgroundArt(val bitmap: android.graphics.Bitmap)

/** Lower intensity = more scrim = the blurred art reads as a softer, more muted wash. */
internal fun liveBlurScrimAlpha(intensity: LiveBlurIntensity): Float = when (intensity) {
    LiveBlurIntensity.MUTED -> 0.66f
    LiveBlurIntensity.NORMAL -> 0.52f
    LiveBlurIntensity.VIVID -> 0.38f
}

/**
 * [PlayerColors.TextSecondary], lifted toward white only as far as needed to keep
 * [SECONDARY_MIN_CONTRAST] against the brightest part of the background the text can land on.
 *
 * Measures the art as it actually appears on screen: each pixel is first darkened by the scrim
 * (blended in sRGB, the way the scrim layer is drawn), then only the central vertical strip is
 * sampled — a tall phone screen crops a square cover to roughly its middle 45%, and Ken Burns
 * zooms in further. Uses the 85th-percentile luminance rather than the average, so one bright
 * region on an otherwise dark cover still counts — gray text can end up on exactly that region.
 */
internal fun readableSecondaryColor(bitmap: android.graphics.Bitmap, scrimAlpha: Float): Color {
    val w = bitmap.width
    val h = bitmap.height
    if (w == 0 || h == 0) return NowPlayingPalette.textSecondary
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
    val scrim = NowPlayingPalette.background
    val x0 = (w * 0.25f).toInt()
    val x1 = (w * 0.75f).toInt().coerceAtLeast(x0 + 1)
    val lums = FloatArray((x1 - x0) * h)
    var n = 0
    for (y in 0 until h) {
        for (x in x0 until x1) {
            val p = pixels[y * w + x]
            val r = ((p shr 16) and 0xFF) / 255f * (1f - scrimAlpha) + scrim.red * scrimAlpha
            val g = ((p shr 8) and 0xFF) / 255f * (1f - scrimAlpha) + scrim.green * scrimAlpha
            val b = (p and 0xFF) / 255f * (1f - scrimAlpha) + scrim.blue * scrimAlpha
            lums[n++] = Color(r, g, b).luminance()
        }
    }
    lums.sort()
    val backgroundLum = lums[((n - 1) * 0.85f).toInt()]

    fun contrast(textLum: Float) = (textLum + 0.05f) / (backgroundLum + 0.05f)
    var t = 0f
    while (t <= 1f) {
        val candidate = lerp(NowPlayingPalette.textSecondary, NowPlayingPalette.textPrimary, t)
        if (contrast(candidate.luminance()) >= SECONDARY_MIN_CONTRAST) return candidate
        t += 0.05f
    }
    return NowPlayingPalette.textPrimary
}

/**
 * The "живой блюр" background: the album art itself, pre-blurred once (see above) and then
 * slowly panned/zoomed — a "Ken Burns" effect, the same trick a real, well-regarded open-source
 * Apple-Music-style player (github.com/shouryadixitisverycool/Flamingo) uses. No per-frame blur
 * recompute at all; the bitmap is already-blurred, so this is just a cheap continuous
 * scale/translate on a `graphicsLayer`.
 */
@Composable
internal fun LiveBlurBackground(art: BackgroundArt?, intensity: LiveBlurIntensity) {
    val scrimAlpha = liveBlurScrimAlpha(intensity)

    Box(modifier = Modifier.fillMaxSize().background(PlayerColors.Background)) {
        val bmp = art?.bitmap
        if (bmp != null) {
            // Two fully independent phases (scale vs. pan), each always read at a straight 1x
            // multiplier — the exact bug that made the old color-blob version "teleport" was
            // multiplying a single shared phase before feeding it to sin/cos, which breaks the
            // smooth wrap RepeatMode.Restart otherwise gives for free.
            // The drift's own clock — it only runs while the player is on screen, and picks up
            // where it left off. Two independent periods (scale vs. pan), each read at a straight
            // 1x multiplier: multiplying one shared phase before sin/cos is what once made an
            // older version "teleport" at the wrap-around.
            val active = LocalNowPlayingActive.current
            var elapsedMs by remember { mutableFloatStateOf(0f) }
            LaunchedEffect(active) {
                if (!active) return@LaunchedEffect
                var last = withFrameNanos { it }
                while (true) {
                    val now = withFrameNanos { it }
                    elapsedMs += (now - last) / 1_000_000f
                    last = now
                }
            }
            val scalePhase: () -> Float = { (elapsedMs % 26_000f) / 26_000f * (2 * Math.PI).toFloat() }
            val panPhase: () -> Float = { (elapsedMs % 34_000f) / 34_000f * (2 * Math.PI).toFloat() }
            val panAmplitudePx = with(LocalDensity.current) { 26.dp.toPx() }

            Crossfade(
                targetState = bmp,
                animationSpec = tween(600),
                modifier = Modifier.fillMaxSize(),
            ) { frame ->
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = 1.2f + 0.08f * kotlin.math.sin(scalePhase())
                            scaleX = scale
                            scaleY = scale
                            translationX = panAmplitudePx * kotlin.math.cos(panPhase())
                            translationY = panAmplitudePx * kotlin.math.sin(panPhase())
                        },
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PlayerColors.Background.copy(alpha = scrimAlpha)),
        )
    }
}

/**
 * Everything behind Now Playing's content, shared by every player style: the background (live
 * blur, static blur or plain), its scrim, the palette (the blur modes keep Now Playing's own fixed
 * dark look whatever the app theme is; only "no background" follows the theme) and the adaptive
 * gray for secondary text ([LocalAdaptiveSecondaryColor]) measured against the art.
 */
@Composable
internal fun NowPlayingSurface(
    song: Song?,
    mode: NowPlayingBackgroundMode,
    intensity: LiveBlurIntensity,
    /** The classic player's opening: the background is a sheet growing out of [openFrom] (the
     * mini player bar, screen coordinates) to the whole screen as [openProgress] goes 0 → 1,
     * starting out in [openTint] (the bar's own color). */
    openFrom: Rect? = null,
    openProgress: () -> Float = { 1f },
    openTint: Color = Color.Transparent,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = if (mode == NowPlayingBackgroundMode.NONE) LocalPlayerPalette.current else NowPlayingPalette
    PaletteScope(palette) {
        // Same scrim strength on every Now Playing tab (Art/Lyrics/Queue) — this used to be lower on
        // the Lyrics tab specifically, which made the background visibly lighten when opening it.
        val scrimAlpha = 0.62f

        // No Haze anymore, anywhere in this screen. Studied a real working Apple-Music-style player
        // (github.com/shouryadixitisverycool/Flamingo) and its whole "living blur" is a completely
        // different, much cheaper trick: blur the album art ONCE in software (not live, not per
        // frame) at a tiny resolution, then just pan/zoom that already-blurred static bitmap
        // (Ken Burns style). No continuous backdrop capture of scrolling content at all — the
        // "smooth fade under the header/controls" people see in Apple Music isn't a blur behind
        // those elements either; it's the *list content itself* fading to transparent (per-row alpha)
        // as it nears the edges, revealing this same static background underneath. See
        // LiveBlurBackground below and the fadeInList() modifier further down.
        //
        // The small pre-blurred art bitmap is loaded once here (not inside LiveBlurBackground) because
        // it serves two purposes: it IS the live-blur background, and in both blur modes it's what we
        // measure to decide how bright the gray text has to be to stay readable over the art.
        val backgroundArt = if (mode != NowPlayingBackgroundMode.NONE) {
            val bitmap = rememberLivingBackgroundBitmap(song?.uri)
            remember(bitmap) { bitmap?.let { BackgroundArt(it) } }
        } else {
            null
        }
        val backgroundScrimAlpha = when (mode) {
            NowPlayingBackgroundMode.LIVE_BLUR -> liveBlurScrimAlpha(intensity)
            else -> scrimAlpha
        }
        val defaultSecondary = PlayerColors.TextSecondary
        val targetSecondaryColor by produceState(defaultSecondary, backgroundArt, backgroundScrimAlpha, defaultSecondary) {
            val art = backgroundArt
            value = if (art == null) {
                defaultSecondary
            } else {
                withContext(Dispatchers.Default) { readableSecondaryColor(art.bitmap, backgroundScrimAlpha) }
            }
        }
        // Eased so it doesn't snap while the background itself is still crossfading to the new track.
        val secondaryColor by animateColorAsState(targetSecondaryColor, tween(600), label = "secondaryColor")
        Box(modifier = Modifier.fillMaxSize()) {
          CompositionLocalProvider(LocalAdaptiveSecondaryColor provides secondaryColor) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = openProgress()
                        val from = openFrom
                        if (p < 1f && from != null) {
                            val top = from.top * (1f - p)
                            val bottom = from.bottom + (size.height - from.bottom) * p
                            val corner = SHEET_CORNER.toPx() * (1f - p).coerceAtMost(1f)
                            clip = true
                            shape = GenericShape { _, _ ->
                                addRoundRect(RoundRect(Rect(0f, top, size.width, bottom), CornerRadius(corner)))
                            }
                        } else {
                            clip = false
                        }
                    }
                    .background(PlayerColors.Background),
            ) {
            when (mode) {
                NowPlayingBackgroundMode.LIVE_BLUR -> LiveBlurBackground(
                    art = backgroundArt,
                    intensity = intensity,
                )

                NowPlayingBackgroundMode.STATIC_BLUR -> {
                    // Real per-track background: the album art itself, blurred and dimmed.
                    // Crossfades between tracks instead of cutting abruptly.
                    Crossfade(
                        targetState = song?.uri,
                        animationSpec = tween(450),
                        modifier = Modifier.fillMaxSize(),
                    ) { uri ->
                        AlbumArt(
                            uri = uri,
                            size = ART_SIZE_FULL,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(60.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PlayerColors.Background.copy(alpha = scrimAlpha)),
                    )
                }

                NowPlayingBackgroundMode.NONE -> {
                    // Just the app's own flat background — no album art at all.
                }
            }
            // Still the mini player's color at first, giving way to the real background.
            if (openFrom != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = (1f - openProgress() / 0.35f).coerceIn(0f, 1f) }
                        .background(openTint),
                )
            }
            }
            content()
            }
        }
    }
}

private val SHEET_CORNER = 18.dp
