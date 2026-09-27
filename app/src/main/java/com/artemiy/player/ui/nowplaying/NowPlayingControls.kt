package com.artemiy.player.ui.nowplaying

import android.content.Intent
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import com.artemiy.player.data.Song
import com.artemiy.player.playback.OutputKind
import com.artemiy.player.playback.rememberOutputDevice
import com.artemiy.player.ui.components.ART_SIZE_THUMB
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.MinimalSlider
import com.artemiy.player.ui.components.PlayPauseIcon
import com.artemiy.player.ui.components.SongActionsMenuPopup
import com.artemiy.player.ui.components.pressScale
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.PlayerColors
import kotlin.math.max
import kotlin.math.roundToInt

// Now Playing's controls: title, seek bar, transport, volume, bottom quick actions, headers.

@Composable
internal fun NowPlayingMiniHeader(
    song: Song?,
    onCollapse: () -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    trailing: @Composable () -> Unit = {},
    artModifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCollapse() }
            .padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(
            uri = song?.uri,
            size = ART_SIZE_THUMB,
            modifier = Modifier
                .size(52.dp)
                .then(artModifier)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp).then(textModifier)) {
            Text(
                text = song?.title ?: "Ничего не играет",
                color = PlayerColors.TextPrimary,
                fontSize = 17.sp,
                // Weight and line spacing matched to the mini player's text at this size, which
                // flies up into here when the player opens on lyrics or the queue.
                lineHeight = HEADER_LINE_HEIGHT,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (song != null) {
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGoToAlbum(song) }
                    } else Modifier,
                ),
            )
            Text(
                text = song?.artist ?: "",
                color = LocalAdaptiveSecondaryColor.current,
                fontSize = 15.sp,
                lineHeight = HEADER_LINE_HEIGHT,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (song != null) {
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onGoToArtist(song) }
                    } else Modifier,
                ),
            )
        }
        trailing()
    }
}

/** 15.6sp (the mini player's line spacing) × 17/13 (header title against the mini title). */
private val HEADER_LINE_HEIGHT = 20.4.sp

/** The "⋮" next to the current song's title (Art view and Queue header). */
@Composable
internal fun CurrentSongMenuButton(
    song: Song,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(start = 12.dp)) {
        Icon(
            imageVector = AppIcons.More,
            contentDescription = "Действия с треком",
            tint = PlayerColors.TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { menuExpanded = true },
        )
        SongActionsMenuPopup(
            song = song,
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onAddToPlaylist = onAddToPlaylist,
            onGoToAlbum = onGoToAlbum,
            onGoToArtist = onGoToArtist,
        )
    }
}

/** Lyrics-view switch for the romanized lines — same see-through look as the Queue toggles. */
@Composable
internal fun RomanizationToggle(enabled: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 12.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background((if (enabled) PlayerColors.Accent else PlayerColors.Surface).copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Romanization,
            contentDescription = if (enabled) "Скрыть романизацию" else "Показать романизацию",
            tint = if (enabled) PlayerColors.OnAccent else PlayerColors.TextPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun VolumeRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService<AudioManager>() }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1 }
    fun systemVolume() = (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0).toFloat()
    var volume by remember { mutableFloatStateOf(systemVolume()) }
    var dragging by remember { mutableStateOf(false) }
    // A step we asked for but Android didn't apply — the "listening at high volume" guard with
    // headphones. Asked once per drag through the system UI so its warning dialog shows up.
    var warnedThisDrag by remember { mutableStateOf(false) }

    // Follows the phone's own volume buttons (and anything else that changes it) while open.
    DisposableEffect(audioManager) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: Intent?) {
                if (!dragging) volume = systemVolume()
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = AppIcons.VolumeDown, contentDescription = null, tint = LocalAdaptiveSecondaryColor.current, modifier = Modifier.size(18.dp))
        MinimalSlider(
            value = volume,
            onValueChange = {
                dragging = true
                volume = it
                val target = it.roundToInt()
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
                if (!warnedThisDrag && systemVolume() < target) {
                    warnedThisDrag = true
                    audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                }
            },
            onValueChangeFinished = {
                dragging = false
                warnedThisDrag = false
                volume = systemVolume()
            },
            valueRange = 0f..maxVolume.toFloat(),
            modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
        )
        Icon(imageVector = AppIcons.VolumeUp, contentDescription = null, tint = LocalAdaptiveSecondaryColor.current, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun BottomQuickActionsRow(
    modifier: Modifier = Modifier,
    lyricsActive: Boolean,
    queueActive: Boolean,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    infinitePlayEnabled: Boolean,
    onLyricsClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onQueueClick: () -> Unit,
) {
    Row(
        // Side buttons pulled in a little toward the middle one.
        modifier = modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            // Filled while the lyrics are open, outline otherwise.
            imageVector = if (lyricsActive) AppIcons.LyricsFilled else AppIcons.Lyrics,
            contentDescription = "Текст песни",
            tint = if (lyricsActive) PlayerColors.TextPrimary else LocalAdaptiveSecondaryColor.current,
            modifier = Modifier
                .size(24.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onLyricsClick() },
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(120.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDeviceClick() },
        ) {
            val output = rememberOutputDevice()
            Icon(
                imageVector = when (output.kind) {
                    OutputKind.PHONE -> AppIcons.DevicePhone
                    OutputKind.HEADPHONES -> AppIcons.DeviceHeadphones
                    OutputKind.SPEAKER -> AppIcons.DeviceSpeaker
                    OutputKind.BLUETOOTH -> AppIcons.DeviceBluetooth
                    OutputKind.USB -> AppIcons.DeviceUsb
                    OutputKind.TV -> AppIcons.DeviceTv
                },
                contentDescription = "Устройство воспроизведения",
                // Lit up while playing through something other than the phone itself.
                tint = if (output.kind == OutputKind.PHONE) LocalAdaptiveSecondaryColor.current else PlayerColors.TextPrimary,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = output.name ?: "Это устройство",
                color = if (output.kind == OutputKind.PHONE) LocalAdaptiveSecondaryColor.current else PlayerColors.TextPrimary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Box(
            modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onQueueClick() },
        ) {
            Icon(
                imageVector = AppIcons.Queue,
                contentDescription = "Очередь",
                tint = if (queueActive) PlayerColors.TextPrimary else LocalAdaptiveSecondaryColor.current,
                modifier = Modifier.size(24.dp),
            )
            QueueModesBadge(
                shuffle = shuffleEnabled,
                repeat = repeatEnabled,
                infinite = infinitePlayEnabled,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-6).dp),
            )
        }
    }
}

/**
 * A little accent dot on the queue icon showing which play modes are on: the mode's own tiny
 * icon when it's just one, the count when there are more. Pops in and out.
 */
@Composable
internal fun QueueModesBadge(shuffle: Boolean, repeat: Boolean, infinite: Boolean, modifier: Modifier = Modifier) {
    val active = listOfNotNull(
        AppIcons.Shuffle.takeIf { shuffle },
        AppIcons.Repeat.takeIf { repeat },
        AppIcons.Infinite.takeIf { infinite },
    )
    // Keeps showing the last content while the badge shrinks away, instead of going blank.
    var shown by remember { mutableStateOf(active) }
    if (active.isNotEmpty()) shown = active
    AnimatedVisibility(
        visible = active.isNotEmpty(),
        enter = scaleIn(tween(180)) + fadeIn(tween(180)),
        exit = scaleOut(tween(150)) + fadeOut(tween(150)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(15.dp)
                .clip(CircleShape)
                .background(PlayerColors.Accent),
            contentAlignment = Alignment.Center,
        ) {
            if (shown.size == 1) {
                Icon(imageVector = shown.first(), contentDescription = null, tint = PlayerColors.OnAccent, modifier = Modifier.size(10.dp))
            } else {
                Text(text = "${shown.size}", color = PlayerColors.OnAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, lineHeight = 9.sp)
            }
        }
    }
}


/** Title and artist under the big cover — tapping them goes to the album / artist. */
@Composable
internal fun SongTitleRow(
    song: Song?,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    trailing: @Composable () -> Unit,
    textModifier: Modifier = Modifier,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).then(textModifier)) {
            Text(
                text = song?.title ?: "Ничего не играет",
                color = PlayerColors.TextPrimary,
                fontSize = 20.sp,
                // Fixed, since the mini player's text is spaced to match it scaled down.
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (song != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onGoToAlbum(song) }
                    } else Modifier,
                ),
            )
            Text(
                text = song?.artist ?: "",
                color = LocalAdaptiveSecondaryColor.current,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (song != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onGoToArtist(song) }
                    } else Modifier,
                ),
            )
        }
        trailing()
    }
}

/** Seek bar with elapsed / total time under it. While dragged it shows the dragged position and
 * only seeks on release. */
@Composable
internal fun SeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val safeDuration = max(durationMs, 1L)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    val shownPosition = if (isDragging) dragValue else positionMs.toFloat()

    Column(modifier = modifier) {
        MinimalSlider(
            value = shownPosition.coerceIn(0f, safeDuration.toFloat()),
            onValueChange = {
                isDragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                onSeek(dragValue.toLong())
                isDragging = false
            },
            valueRange = 0f..safeDuration.toFloat(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = formatMs(shownPosition.toLong()), color = LocalAdaptiveSecondaryColor.current, fontSize = 11.sp)
            Text(text = formatMs(durationMs), color = LocalAdaptiveSecondaryColor.current, fontSize = 11.sp)
        }
    }
}

/** Previous / play-pause / next. */
@Composable
internal fun TransportControls(
    isPlaying: Boolean,
    onSkipPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    modifier: Modifier = Modifier,
    prevModifier: Modifier = Modifier,
    playModifier: Modifier = Modifier,
    nextModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val skipPreviousInteraction = remember { MutableInteractionSource() }
        Icon(
            imageVector = AppIcons.SkipPrevious,
            contentDescription = "Предыдущий трек",
            tint = PlayerColors.TextPrimary,
            modifier = Modifier
                .size(40.dp)
                .then(prevModifier)
                .pressScale(skipPreviousInteraction)
                .clickable(interactionSource = skipPreviousInteraction, indication = null) { onSkipPrevious() },
        )
        val playInteraction = remember { MutableInteractionSource() }
        PlayPauseIcon(
            isPlaying = isPlaying,
            tint = PlayerColors.TextPrimary,
            modifier = Modifier
                .padding(horizontal = 46.dp)
                .size(58.dp)
                .then(playModifier)
                .pressScale(playInteraction)
                .clickable(interactionSource = playInteraction, indication = null) { onTogglePlayPause() },
        )
        val skipNextInteraction = remember { MutableInteractionSource() }
        Icon(
            imageVector = AppIcons.SkipNext,
            contentDescription = "Следующий трек",
            tint = PlayerColors.TextPrimary,
            modifier = Modifier
                .size(40.dp)
                .then(nextModifier)
                .pressScale(skipNextInteraction)
                .clickable(interactionSource = skipNextInteraction, indication = null) { onSkipNext() },
        )
    }
}
