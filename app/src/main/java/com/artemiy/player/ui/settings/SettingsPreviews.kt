package com.artemiy.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.artemiy.player.data.NowPlayingBackgroundMode
import com.artemiy.player.data.PlayerStyle
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.UiStyle

/** A line of "text" in a preview: just a rounded bar. */
@Composable
private fun Bar(width: Dp, height: Dp = 6.dp, color: Color = PlayerColors.TextPrimary, modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(width).height(height).clip(RoundedCornerShape(height / 2)).background(color))
}

@Composable
private fun PreviewFrame(modifier: Modifier = Modifier, background: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PlayerColors.Background)
            .border(1.dp, PlayerColors.Border, RoundedCornerShape(18.dp)),
    ) {
        background()
        content()
    }
}

/**
 * A small drawing of what the interface style looks like: the page heading, a few list rows and the bar
 * at the bottom — in the colours of the app as it is now.
 */
@Composable
fun UiStylePreview(style: UiStyle, modifier: Modifier = Modifier) {
    val dim = PlayerColors.TextPrimary.copy(alpha = 0.55f)
    PreviewFrame(modifier = modifier) {
        when (style) {
            UiStyle.CLASSIC -> Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Bar(112.dp, 13.dp)
                repeat(3) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(PlayerColors.Surface))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Bar(130.dp, 7.dp); Bar(80.dp, 5.dp, dim) }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PlayerColors.Border))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    repeat(4) { Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(if (it == 1) PlayerColors.AccentMark else dim)) }
                }
            }
            UiStyle.EXPRESSIVE -> Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Bar(124.dp, 15.dp)
                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PlayerColors.Surface).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(PlayerColors.AccentMark.copy(alpha = 0.3f)))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Bar(130.dp, 7.dp); Bar(80.dp, 5.dp, dim) }
                    }
                }
                Box(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(CircleShape).background(PlayerColors.SurfaceDim).padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(54.dp).height(22.dp).clip(CircleShape).background(PlayerColors.AccentMark.copy(alpha = 0.25f)))
                    repeat(3) { Box(modifier = Modifier.padding(horizontal = 10.dp).size(8.dp).clip(CircleShape).background(dim)) }
                }
            }
            UiStyle.DISC -> Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Bar(116.dp, 14.dp)
                Bar(84.dp, 14.dp, PlayerColors.TextTertiary)
                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(PlayerColors.TextPrimary.copy(alpha = 0.07f)).padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(PlayerColors.TextPrimary.copy(alpha = 0.12f)))
                        Bar(150.dp, 7.dp)
                    }
                }
                Box(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (it == 2) PlayerColors.TextPrimary else PlayerColors.TextPrimary.copy(alpha = 0.1f)),
                        )
                    }
                }
            }
        }
    }
}


/** Seven icons of the chosen set, the very ones the player draws (skip, pause, skip, shuffle, repeat, lyrics, queue), so the set's look is seen at once. */
@Composable
fun IconSetPreview(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PlayerColors.TextPrimary.copy(alpha = 0.06f))
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(AppIcons.SkipPrevious, AppIcons.Pause, AppIcons.SkipNext, AppIcons.Shuffle, AppIcons.Repeat, AppIcons.Lyrics, AppIcons.Queue).forEach {
            Icon(it, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
    }
}

data class PreviewKey(val style: PlayerStyle, val card: Boolean, val background: NowPlayingBackgroundMode, val island: Boolean, val roundKeys: Boolean)

/**
 * A small drawing of the player in the chosen style — its layout, and the background behind it (blurred
 * colours of the cover, or the app's own plain page).
 */
@Composable
fun PlayerStylePreview(style: PlayerStyle, dialCard: Boolean, background: NowPlayingBackgroundMode, modifier: Modifier = Modifier, island: Boolean = false, roundKeys: Boolean = false) {
    val blurred = background != NowPlayingBackgroundMode.NONE
    val tint = Brush.linearGradient(listOf(Color(0xFF8A5C5C), Color(0xFF3F3F63), Color(0xFF6A4A33)))
    val onBlur = blurred && !(style == PlayerStyle.DIAL && dialCard)
    val ink = if (onBlur) Color.White else PlayerColors.TextPrimary
    val fill = ink.copy(alpha = 0.16f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(236.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PlayerColors.Background)
            .border(1.dp, PlayerColors.Border, RoundedCornerShape(18.dp)),
    ) {
        // The blur behind the whole player, when it covers all of it.
        if (onBlur) {
            Box(modifier = Modifier.fillMaxSize().background(tint))
            if (background == NowPlayingBackgroundMode.LIVE_BLUR) {
                Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0x66FFB4A0), Color.Transparent))))
            }
        }
        Row(modifier = Modifier.fillMaxSize().padding(14.dp), horizontalArrangement = Arrangement.Center) {
            Column(modifier = Modifier.width(190.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (style) {
                    PlayerStyle.CLASSIC -> {
                        Box(modifier = Modifier.size(104.dp).clip(RoundedCornerShape(13.dp)).background(tint))
                        Column(modifier = Modifier.padding(top = 9.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Bar(120.dp, 9.dp, ink); Bar(76.dp, 6.dp, ink.copy(alpha = 0.55f)); Bar(190.dp, 4.dp, fill, Modifier.padding(top = 5.dp))
                        }
                        Row(modifier = Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(fill))
                            Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(ink))
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(fill))
                        }
                    }
                    PlayerStyle.EXPRESSIVE -> if (island) {
                        Box(modifier = Modifier.size(70.dp).clip(RoundedCornerShape(20.dp)).background(tint))
                        Column(modifier = Modifier.padding(top = 6.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Bar(100.dp, 8.dp, ink); Bar(62.dp, 5.dp, ink.copy(alpha = 0.55f))
                        }
                        val key: @Composable () -> Unit = {
                            if (roundKeys) Box(modifier = Modifier.size(24.dp).background(fill, RoundedCornerShape(9.dp)))
                            else Box(modifier = Modifier.size(24.dp).drawBehind { drawPath(com.artemiy.player.ui.nowplaying.cloverPath(size, 0f), fill) })
                        }
                        Column(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .fillMaxWidth()
                                .background(if (onBlur) Color.Black.copy(alpha = 0.3f) else ink.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.weight(1f).height(24.dp).background(ink, RoundedCornerShape(12.dp)))
                                key()
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                key()
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.width(44.dp).height(3.dp).background(ink, CircleShape))
                                    Box(modifier = Modifier.size(7.dp).background(ink, CircleShape))
                                    Box(modifier = Modifier.weight(1f).height(3.dp).background(fill, CircleShape))
                                }
                            }
                        }
                        Row(modifier = Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(width = 24.dp, height = 16.dp).background(fill, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 3.dp, bottomEnd = 3.dp)))
                            Box(modifier = Modifier.size(width = 24.dp, height = 16.dp).background(fill, RoundedCornerShape(3.dp)))
                            Box(modifier = Modifier.size(width = 24.dp, height = 16.dp).background(fill, RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp)))
                        }
                    } else {
                        Box(modifier = Modifier.size(104.dp).clip(RoundedCornerShape(28.dp)).background(tint))
                        Column(modifier = Modifier.padding(top = 9.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Bar(120.dp, 9.dp, ink); Bar(76.dp, 6.dp, ink.copy(alpha = 0.55f)); Bar(190.dp, 4.dp, fill, Modifier.padding(top = 5.dp))
                        }
                        Row(modifier = Modifier.padding(top = 9.dp).fillMaxWidth().height(34.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp, topEnd = 5.dp, bottomEnd = 5.dp)).background(fill))
                            Box(modifier = Modifier.weight(2f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(ink))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 13.dp, bottomEnd = 13.dp)).background(fill))
                        }
                    }
                    PlayerStyle.DIAL -> {
                        // The card up top (with the blur in it), the title, and the grid of six tiles under it.
                        val cardMod = Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(16.dp))
                        Box(modifier = if (dialCard) cardMod.background(if (blurred) tint else Brush.linearGradient(listOf(fill, fill))) else cardMod) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Bar(96.dp, 10.dp, if (dialCard && blurred) Color.White else ink); Bar(60.dp, 10.dp, (if (dialCard && blurred) Color.White else ink).copy(alpha = 0.6f))
                            }
                        }
                        Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            val tile = if (onBlur) Color.White.copy(alpha = 0.18f) else PlayerColors.TextPrimary.copy(alpha = 0.12f)
                            repeat(2) { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(3) { col ->
                                        val play = row == 1 && col == 0
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (play) PlayerColors.Accent else tile),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
