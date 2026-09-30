package com.artemiy.player.ui.library

import com.artemiy.player.ui.theme.LocalBarsInset
import com.artemiy.player.ui.theme.barsInset
import com.artemiy.player.ui.components.pressScale
import androidx.compose.ui.res.pluralStringResource
import com.artemiy.player.R
import com.artemiy.player.ui.components.monthYear
import com.artemiy.player.ui.components.indexLetter
import com.artemiy.player.ui.components.rememberScrollTarget
import com.artemiy.player.ui.components.FastScroller
import com.artemiy.player.ui.components.fadesWithHeader
import com.artemiy.player.ui.home.drawGenreMotif
import com.artemiy.player.ui.library.ToneBackdrop
import com.artemiy.player.ui.components.inAlbumOrder
import com.artemiy.player.ui.components.groupedCard
import com.artemiy.player.ui.components.staggeredEntrance
import com.artemiy.player.ui.components.rememberEntrance
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.ui.theme.inAppFont
import com.artemiy.player.ui.components.AppDialog
import com.artemiy.player.ui.components.AppDropdownMenu
import com.artemiy.player.ui.components.AppMenuItem
import com.artemiy.player.ui.components.DialogButtons
import com.artemiy.player.ui.components.DialogMessage
import com.artemiy.player.ui.components.DialogTextField
import com.artemiy.player.ui.components.DialogTitle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.artemiy.player.data.ArtistNames
import com.artemiy.player.data.LibraryViewMode
import com.artemiy.player.data.artists
import com.artemiy.player.data.PlaylistWithCount
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.AnimatedBackStack
import com.artemiy.player.ui.components.BlurredCollageArt
import com.artemiy.player.ui.components.COLLAGE_HERO_HEIGHT
import com.artemiy.player.ui.components.CircleIconButton
import com.artemiy.player.ui.components.HeroOverArt
import com.artemiy.player.ui.components.HeroTextShadow
import com.artemiy.player.ui.components.PlayPillButton
import com.artemiy.player.ui.components.rememberArrowTint
import com.artemiy.player.ui.components.rememberBlurredCollage
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import com.artemiy.player.ui.components.songLongPressTrigger
import com.artemiy.player.ui.settings.SettingsViewModel
import com.artemiy.player.ui.theme.PlayerColors

@Composable
internal fun SongsGrid(
    songs: List<Song>,
    columns: Int,
    state: LazyGridState,
    onSongClick: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = if (header != null) 0.dp else 4.dp, bottom = 4.dp + LocalBarsInset.current),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (header != null) {
            // Full width, reaching past the grid's side padding — the header draws edge to edge.
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.layout { measurable, constraints ->
                    val extra = 40.dp.roundToPx()
                    val placeable = measurable.measure(constraints.copy(minWidth = constraints.maxWidth + extra, maxWidth = constraints.maxWidth + extra))
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
                }) { header() }
            }
        }
        gridItems(songs, key = { it.id }) { song ->
            var menuExpanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier.songLongPressTrigger(
                    onClick = { onSongClick(song) },
                    onLongPress = { menuExpanded = true },
                ),
            ) {
                Box {
                    AlbumArt(
                        uri = song.uri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    com.artemiy.player.ui.components.SongActionsMenuPopup(
                        song = song,
                        expanded = menuExpanded,
                        onDismiss = { menuExpanded = false },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onGoToAlbum = onGoToAlbum,
                        onGoToArtist = onGoToArtist,
                        onRemoveFromPlaylist = onRemoveFromPlaylist,
                    )
                }
                Text(
                    text = song.title,
                    color = PlayerColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = song.artist,
                    color = PlayerColors.TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun SongList(
    songs: List<Song>,
    state: LazyListState = rememberLazyListState(),
    onSongClick: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
) {
    val entrance = rememberEntrance()
    LazyColumn(modifier = Modifier.fillMaxWidth(), state = state, contentPadding = PaddingValues(bottom = LocalBarsInset.current)) {
        if (header != null) item(key = "header") { header() }
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            val expressive = com.artemiy.player.ui.theme.expressiveUi
            Row(
                modifier = Modifier
                    .then(if (expressive) Modifier.padding(horizontal = 12.dp) else Modifier)
                    .staggeredEntrance(index, entrance)
                    .groupedCard(index, songs.size)
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onSongClick(song) }
                    .padding(horizontal = if (expressive) 0.dp else 20.dp, vertical = if (expressive) 12.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = PlayerColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = song.artist,
                        color = PlayerColors.TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                com.artemiy.player.ui.components.SongActionsMenu(
                    song = song,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onGoToAlbum = onGoToAlbum,
                    onGoToArtist = onGoToArtist,
                    onRemoveFromPlaylist = onRemoveFromPlaylist,
                    iconSize = 20.dp,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

@Composable
internal fun PlayShuffleRow(onPlay: () -> Unit, onShuffle: () -> Unit) {
    if (com.artemiy.player.ui.theme.expressiveUi) {
        ExpressivePlayShuffle(onPlay, onShuffle)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PlayShuffleButton(icon = AppIcons.Play, label = stringResource(R.string.action_listen), onClick = onPlay, modifier = Modifier.weight(1f))
        PlayShuffleButton(icon = AppIcons.Shuffle, label = stringResource(R.string.shuffle), onClick = onShuffle, modifier = Modifier.weight(1f))
    }
}

/**
 * Expressive: one connected button group — "Listen" filled, "Shuffle" tonal, round on the outside
 * and tight where they meet. The pressed half grows a bit wider (squeezing its neighbour) and
 * rounds its inner corners.
 */
@Composable
internal fun ExpressivePlayShuffle(onPlay: () -> Unit, onShuffle: () -> Unit) {
    val playInteraction = remember { MutableInteractionSource() }
    val shuffleInteraction = remember { MutableInteractionSource() }
    val playPressed by playInteraction.collectIsPressedAsState()
    val shufflePressed by shuffleInteraction.collectIsPressedAsState()
    val spec = androidx.compose.animation.core.spring<Float>(dampingRatio = 0.6f, stiffness = 600f)
    val playWeight by androidx.compose.animation.core.animateFloatAsState(if (playPressed) 1.18f else 1f, spec, label = "playWeight")
    val shuffleWeight by androidx.compose.animation.core.animateFloatAsState(if (shufflePressed) 1.18f else 1f, spec, label = "shuffleWeight")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GroupButton(
            icon = AppIcons.Play,
            label = stringResource(R.string.action_listen),
            fill = PlayerColors.Accent,
            ink = PlayerColors.OnAccent,
            shape = com.artemiy.player.ui.components.morphingShape(playInteraction, startPercent = 50, endPercent = 16, pressedPercent = 50),
            interaction = playInteraction,
            onClick = onPlay,
            modifier = Modifier.weight(playWeight),
        )
        GroupButton(
            icon = AppIcons.Shuffle,
            label = stringResource(R.string.shuffle),
            fill = androidx.compose.ui.graphics.lerp(PlayerColors.Surface, PlayerColors.AccentStandalone, 0.16f),
            ink = PlayerColors.TextPrimary,
            shape = com.artemiy.player.ui.components.morphingShape(shuffleInteraction, startPercent = 16, endPercent = 50, pressedPercent = 50),
            interaction = shuffleInteraction,
            onClick = onShuffle,
            modifier = Modifier.weight(shuffleWeight),
        )
    }
}

@Composable
internal fun GroupButton(
    icon: ImageVector,
    label: String,
    fill: Color,
    ink: Color,
    shape: androidx.compose.ui.graphics.Shape,
    interaction: MutableInteractionSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        Text(text = label, color = ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
internal fun PlayShuffleButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(44.dp)
            .pressScale(interaction, pressedScale = 0.95f)
            .clip(RoundedCornerShape(12.dp))
            .background(PlayerColors.Surface)
            .clickable(interactionSource = interaction, indication = com.artemiy.player.ui.components.SoftPress, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(17.dp))
        Text(text = label, color = PlayerColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}
