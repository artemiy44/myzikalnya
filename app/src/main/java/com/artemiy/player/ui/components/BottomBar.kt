package com.artemiy.player.ui.components

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.ui.theme.PlayerColors
import com.artemiy.player.ui.theme.expressiveUi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

enum class AppTab(@androidx.annotation.StringRes val labelRes: Int) {
    Mood(R.string.tab_mood),
    Home(R.string.tab_home),
    Library(R.string.tab_library),
    Search(R.string.tab_search),
}

val AppTab.label: String @Composable get() = stringResource(labelRes)

@Composable
fun PlayerBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    if (expressiveUi) {
        ExpressiveBottomBar(selected, onSelect)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlayerColors.SurfaceDim)
            .navigationBarsPadding()
            .padding(top = 6.dp, bottom = 4.dp)
            .padding(horizontal = 28.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppTab.entries.forEach { tab ->
            val active = tab == selected
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                TabIcon(tab, active, if (active) PlayerColors.AccentMark else PlayerColors.TextSecondary, 20.dp)
                Text(
                    text = tab.label,
                    color = if (active) PlayerColors.AccentMark else PlayerColors.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** The selected tab's icon is the filled (or bolder) version, eased in and out. */
@Composable
private fun TabIcon(tab: AppTab, active: Boolean, tint: Color, size: Dp) {
    Crossfade(targetState = active, animationSpec = tween(180), label = "tabIcon") { filled ->
        Icon(
            imageVector = when (tab) {
                AppTab.Mood -> if (filled) AppIcons.MoodBold else AppIcons.Mood
                AppTab.Home -> if (filled) AppIcons.HomeFilled else AppIcons.Home
                AppTab.Library -> if (filled) AppIcons.LibraryFilled else AppIcons.ViewGrid
                AppTab.Search -> if (filled) AppIcons.SearchFilled else AppIcons.Search
            },
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(size),
        )
    }
}

/**
 * The expressive style's tab bar: a floating pill, where the selected tab swells into a tonal
 * capsule with its name beside the icon while the others shrink back to just icons.
 */
@Composable
private fun ExpressiveBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp)
            .padding(top = 8.dp, bottom = 8.dp)
            .clip(CircleShape)
            .background(PlayerColors.SurfaceDim)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTab.entries.forEach { tab ->
            val active = tab == selected
            val interaction = remember { MutableInteractionSource() }
            val fill by animateColorAsState(
                if (active) PlayerColors.AccentMark.copy(alpha = 0.16f) else Color.Transparent,
                tween(220),
                label = "tabPill",
            )
            val ink by animateColorAsState(
                if (active) PlayerColors.AccentMark else PlayerColors.TextSecondary,
                tween(220),
                label = "tabInk",
            )
            Row(
                modifier = Modifier
                    .pressScale(interaction, pressedScale = 0.9f)
                    .clip(CircleShape)
                    .background(fill)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(tab) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabIcon(tab, active, ink, 22.dp)
                AnimatedVisibility(
                    visible = active,
                    enter = expandHorizontally(spring(dampingRatio = 0.7f, stiffness = 500f)) + fadeIn(tween(200, delayMillis = 60)),
                    exit = shrinkHorizontally(spring(dampingRatio = 1f, stiffness = 700f)) + fadeOut(tween(100)),
                ) {
                    Text(
                        text = tab.label,
                        color = ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
