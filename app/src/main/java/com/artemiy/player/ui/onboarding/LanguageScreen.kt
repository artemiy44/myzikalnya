package com.artemiy.player.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.R
import com.artemiy.player.ui.components.pressScale
import com.artemiy.player.ui.i18n.AppLanguage
import com.artemiy.player.ui.i18n.LanguagePrefs
import com.artemiy.player.ui.i18n.inLanguage
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.PlayerColors
import kotlinx.coroutines.delay

/**
 * The very first screen on first launch (and before the welcome screens when they're shown
 * again): which language the app speaks. Every language is written in itself — the heading even
 * cycles through "choose a language" in each of them — and Russian and English also offer their
 * livelier, expressive wording. The button is in the language picked.
 */
@Composable
fun LanguageScreen(onPicked: (AppLanguage) -> Unit) {
    val context = LocalContext.current
    val plain = remember { AppLanguage.entries.filter { !it.expressive } }
    val start = remember { LanguagePrefs.effective(context) }
    var picked by remember { mutableStateOf(plain.first { it.tag.substringBefore('-') == start.tag.substringBefore('-') }) }
    var expressive by remember { mutableStateOf(start.expressive) }
    val headings = remember { plain.map { context.inLanguage(it).getString(R.string.lang_choose) } }
    var heading by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            heading = (heading + 1) % headings.size
        }
    }
    val words = remember(picked) { context.inLanguage(picked) }
    val hasExpressive = picked == AppLanguage.RUSSIAN || picked == AppLanguage.ENGLISH

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayerColors.Background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 36.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(PlayerColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Language, contentDescription = null, tint = PlayerColors.AccentStandalone, modifier = Modifier.size(28.dp))
        }
        Crossfade(targetState = heading, animationSpec = tween(500), label = "chooseLanguage") { i ->
            Text(
                text = headings[i],
                color = PlayerColors.TextPrimary,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 18.dp, bottom = 16.dp),
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(plain, key = { it.name }) { language ->
                val chosen = language == picked
                val fill by animateColorAsState(if (chosen) PlayerColors.Accent else PlayerColors.Surface, tween(200), label = "langFill")
                val ink by animateColorAsState(if (chosen) PlayerColors.OnAccent else PlayerColors.TextPrimary, tween(200), label = "langInk")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(fill)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            if (!chosen) {
                                picked = language
                                expressive = false
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Text(text = language.nativeName, color = ink, fontSize = 17.sp, fontWeight = if (chosen) FontWeight.Bold else FontWeight.SemiBold)
                    // Russian and English: plain or expressive wording.
                    AnimatedVisibility(
                        visible = chosen && hasExpressive,
                        enter = expandVertically(tween(260)) + fadeIn(tween(260)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                    ) {
                        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToneChip(words.getString(R.string.lang_plain_chip), !expressive) { expressive = false }
                            ToneChip(words.getString(R.string.lang_expressive_chip), expressive) { expressive = true }
                        }
                    }
                }
            }
        }
        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 20.dp)
                .fillMaxWidth()
                .height(54.dp)
                .pressScale(interaction, pressedScale = 0.96f)
                .clip(CircleShape)
                .background(PlayerColors.Accent)
                .clickable(interactionSource = interaction, indication = null) {
                    onPicked(
                        when {
                            expressive && picked == AppLanguage.RUSSIAN -> AppLanguage.RUSSIAN_EXPRESSIVE
                            expressive && picked == AppLanguage.ENGLISH -> AppLanguage.ENGLISH_EXPRESSIVE
                            else -> picked
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = words.getString(R.string.onb_next), label = "langNext") { label ->
                Text(text = label, color = PlayerColors.OnAccent, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ToneChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        color = if (selected) PlayerColors.Accent else PlayerColors.OnAccent,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) PlayerColors.OnAccent else PlayerColors.OnAccent.copy(alpha = 0.18f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
