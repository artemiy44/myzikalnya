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

/** A playlist's header — same build as an artist's: its songs' covers as a blurred collage, name
 * and song count over it, then shuffle / play / playlist actions. */
@Composable
internal fun PlaylistHero(
    barShown: Boolean = false,
    name: String,
    songCount: Int,
    songs: List<com.artemiy.player.data.Song>,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExportM3u: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }
    HeroOverArt(
        // The drawn picture is always deep and dark: a white arrow reads on it.
        topTint = Color.White,
        onBack = onBack,
        height = COLLAGE_HERO_HEIGHT,
        barShown = barShown,
        art = { ArtistGenreArt(name, songs) },
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = HeroTextShadow).inAppFont(),
        )
        Text(
            text = pluralStringResource(R.plurals.songs_count, songCount, songCount),
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            style = TextStyle(shadow = HeroTextShadow).inAppFont(),
            modifier = Modifier.padding(top = 4.dp),
        )
        Box(modifier = Modifier.padding(top = 18.dp)) {
            com.artemiy.player.ui.components.HeroButtons(
                onShuffle = onShuffle,
                onPlay = onPlay,
                trailingIcon = AppIcons.More,
                trailingDescription = stringResource(R.string.playlist_actions),
                onTrailing = { menuExpanded = true },
                trailingOverlay = {
                    AppDropdownMenu(expanded = menuExpanded, onDismiss = { menuExpanded = false }) {
                        AppMenuItem(text = stringResource(R.string.rename), icon = AppIcons.Edit, onClick = { menuExpanded = false; onRename() })
                        AppMenuItem(text = stringResource(R.string.playlist_export_m3u), icon = AppIcons.Upload, onClick = { menuExpanded = false; onExportM3u() })
                        AppMenuItem(text = stringResource(R.string.delete_playlist), icon = AppIcons.Delete, destructive = true, onClick = { menuExpanded = false; onDelete() })
                    }
                },
            )
        }
    }
}

@Composable
internal fun PlaylistsList(playlists: List<PlaylistWithCount>, onPlaylistClick: (PlaylistWithCount) -> Unit) {
    if (playlists.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_playlists),
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = LocalBarsInset.current)) {
        items(playlists, key = { it.id }) { playlist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onPlaylistClick(playlist) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PlayerColors.Surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = AppIcons.Playlist, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = playlist.name,
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                Text(text = "${playlist.songCount}", color = PlayerColors.TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun PlaylistNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AppDialog(onDismiss = onDismiss) {
        DialogTitle(title)
        DialogTextField(value = name, onValueChange = { name = it }, placeholder = stringResource(R.string.name), modifier = Modifier.padding(top = 16.dp))
        DialogButtons(
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = onDismiss,
            confirmLabel = confirmLabel,
            onConfirm = { if (name.isNotBlank()) onConfirm(name) },
            confirmEnabled = name.isNotBlank(),
        )
    }
}

@Composable
internal fun ConfirmDialog(title: String, message: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AppDialog(onDismiss = onDismiss) {
        DialogTitle(title)
        DialogMessage(message)
        DialogButtons(dismissLabel = stringResource(R.string.cancel), onDismiss = onDismiss, confirmLabel = confirmLabel, onConfirm = onConfirm, destructive = true)
    }
}
