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
            .height(150.dp)
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
                Bar(70.dp, 11.dp)
                repeat(3) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)).background(PlayerColors.Surface))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Bar(80.dp); Bar(50.dp, 4.dp, dim) }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PlayerColors.Border))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    repeat(4) { Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(if (it == 1) PlayerColors.AccentMark else dim)) }
                }
            }
            UiStyle.EXPRESSIVE -> Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Bar(80.dp, 13.dp)
                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PlayerColors.Surface).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(modifier = Modifier.size(22.dp).clip(RoundedCornerShape(8.dp)).background(PlayerColors.AccentMark.copy(alpha = 0.3f)))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Bar(80.dp); Bar(50.dp, 4.dp, dim) }
                    }
                }
                Box(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(CircleShape).background(PlayerColors.SurfaceDim).padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.width(44.dp).height(18.dp).clip(CircleShape).background(PlayerColors.AccentMark.copy(alpha = 0.25f)))
                    repeat(3) { Box(modifier = Modifier.padding(horizontal = 10.dp).size(8.dp).clip(CircleShape).background(dim)) }
                }
            }
            UiStyle.DISC -> Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Bar(76.dp, 12.dp)
                Bar(54.dp, 12.dp, PlayerColors.TextTertiary)
                repeat(2) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(PlayerColors.TextPrimary.copy(alpha = 0.07f)).padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(modifier = Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(PlayerColors.TextPrimary.copy(alpha = 0.12f)))
                        Bar(80.dp)
                    }
                }
                Box(modifier = Modifier.weight(1f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (it == 2) PlayerColors.TextPrimary else PlayerColors.TextPrimary.copy(alpha = 0.1f)),
                        )
                    }
                }
            }
        }
    }
}


/** Six icons of the chosen set: pause, back, forward and three more, so the set's look is seen at once. */
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
        listOf(AppIcons.SkipBack, AppIcons.Pause, AppIcons.SkipForward, AppIcons.Shuffle, AppIcons.Search, AppIcons.Settings).forEach {
            Icon(it, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
    }
}

/**
 * A small drawing of the player in the chosen style — its layout, and the background behind it (blurred
 * colours of the cover, or the app's own plain page).
 */
@Composable
fun PlayerStylePreview(style: PlayerStyle, dialCard: Boolean, background: NowPlayingBackgroundMode, modifier: Modifier = Modifier) {
    val blurred = background != NowPlayingBackgroundMode.NONE
    val tint = Brush.linearGradient(listOf(Color(0xFF8A5C5C), Color(0xFF3F3F63), Color(0xFF6A4A33)))
    val onBlur = blurred && !(style == PlayerStyle.DIAL && dialCard)
    val ink = if (onBlur) Color.White else PlayerColors.TextPrimary
    val fill = ink.copy(alpha = 0.16f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
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
            Column(modifier = Modifier.width(120.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (style) {
                    PlayerStyle.CLASSIC -> {
                        Box(modifier = Modifier.size(78.dp).clip(RoundedCornerShape(10.dp)).background(tint))
                        Column(modifier = Modifier.padding(top = 9.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Bar(70.dp, 7.dp, ink); Bar(44.dp, 5.dp, ink.copy(alpha = 0.55f)); Bar(120.dp, 3.dp, fill, Modifier.padding(top = 4.dp))
                        }
                        Row(modifier = Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(fill))
                            Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(ink))
                            Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(fill))
                        }
                    }
                    PlayerStyle.EXPRESSIVE -> {
                        Box(modifier = Modifier.size(78.dp).clip(RoundedCornerShape(22.dp)).background(tint))
                        Column(modifier = Modifier.padding(top = 9.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Bar(70.dp, 7.dp, ink); Bar(44.dp, 5.dp, ink.copy(alpha = 0.55f)); Bar(120.dp, 3.dp, fill, Modifier.padding(top = 4.dp))
                        }
                        Row(modifier = Modifier.padding(top = 9.dp).fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp, topEnd = 5.dp, bottomEnd = 5.dp)).background(fill))
                            Box(modifier = Modifier.weight(2f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(ink))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 13.dp, bottomEnd = 13.dp)).background(fill))
                        }
                    }
                    PlayerStyle.DIAL -> {
                        // The card up top (with the blur in it), the title, and the grid of six tiles under it.
                        val cardMod = Modifier.fillMaxWidth().height(70.dp).clip(RoundedCornerShape(14.dp))
                        Box(modifier = if (dialCard) cardMod.background(if (blurred) tint else Brush.linearGradient(listOf(fill, fill))) else cardMod) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Bar(60.dp, 8.dp, if (dialCard && blurred) Color.White else ink); Bar(38.dp, 8.dp, (if (dialCard && blurred) Color.White else ink).copy(alpha = 0.6f))
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
                                                .height(26.dp)
                                                .clip(RoundedCornerShape(8.dp))
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
