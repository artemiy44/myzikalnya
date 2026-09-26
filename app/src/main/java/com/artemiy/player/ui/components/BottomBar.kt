package com.artemiy.player.ui.components

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

enum class AppTab(val label: String) {
    Mood("Настроение"),
    Home("Главная"),
    Library("Медиатека"),
    Search("Поиск"),
}

@Composable
fun PlayerBottomBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
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
                // The selected tab's icon is the filled (or bolder) version, eased in and out.
                Crossfade(targetState = active, animationSpec = tween(180), label = "tabIcon") { filled ->
                    Icon(
                        imageVector = when (tab) {
                            AppTab.Mood -> if (filled) AppIcons.MoodBold else AppIcons.Mood
                            AppTab.Home -> if (filled) AppIcons.HomeFilled else AppIcons.Home
                            AppTab.Library -> if (filled) AppIcons.LibraryFilled else AppIcons.ViewGrid
                            AppTab.Search -> if (filled) AppIcons.SearchFilled else AppIcons.Search
                        },
                        contentDescription = tab.label,
                        tint = if (active) PlayerColors.AccentStandalone else PlayerColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = tab.label,
                    color = if (active) PlayerColors.AccentStandalone else PlayerColors.TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
