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
internal fun LibraryHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 20.dp, 20.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            Icon(
                imageVector = AppIcons.Back,
                contentDescription = stringResource(R.string.cd_back),
                tint = PlayerColors.TextPrimary,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onBack() },
            )
        }
        Text(
            text = title,
            color = PlayerColors.TextPrimary,
            style = com.artemiy.player.ui.theme.pageTitleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
internal fun LibraryHomeList(
    playlistCount: Int,
    artistCount: Int,
    albumCount: Int,
    songCount: Int,
    yearCount: Int,
    genreCount: Int,
    onOpenYears: () -> Unit,
    onOpenGenres: () -> Unit,
    recentSongs: List<Song>,
    onOpenPlaylists: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenSongs: () -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    songMenu: @Composable (Song, Boolean, () -> Unit) -> Unit,
) {
    val entrance = rememberEntrance()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()).barsInset()
            .padding(bottom = 20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = com.artemiy.player.ui.components.pageGutter)) {
            LibraryRow(stringResource(R.string.playlists), playlistCount, AppIcons.Playlist, onOpenPlaylists, Modifier.staggeredEntrance(0, entrance).groupedCard(0, 6))
            LibraryRow(stringResource(R.string.artists), artistCount, AppIcons.Artist, onOpenArtists, Modifier.staggeredEntrance(1, entrance).groupedCard(1, 6))
            LibraryRow(stringResource(R.string.albums), albumCount, AppIcons.Album, onOpenAlbums, Modifier.staggeredEntrance(2, entrance).groupedCard(2, 6))
            LibraryRow(stringResource(R.string.tracks), songCount, AppIcons.Songs, onOpenSongs, Modifier.staggeredEntrance(3, entrance).groupedCard(3, 6))
            LibraryRow(stringResource(R.string.years), yearCount, AppIcons.Years, onOpenYears, Modifier.staggeredEntrance(4, entrance).groupedCard(4, 6))
            LibraryRow(stringResource(R.string.genres), genreCount, AppIcons.Genres, onOpenGenres, Modifier.staggeredEntrance(5, entrance).groupedCard(5, 6))
        }

        if (recentSongs.isNotEmpty()) {
            Text(
                text = stringResource(R.string.recently_added),
                color = PlayerColors.TextPrimary,
                style = com.artemiy.player.ui.theme.sectionTitleStyle,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                recentSongs.chunked(2).forEach { rowSongs ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowSongs.forEach { song ->
                            var menuOpen by remember { mutableStateOf(false) }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .songLongPressTrigger(onClick = { onSongClick(song, recentSongs) }, onLongPress = { menuOpen = true }),
                            ) {
                                songMenu(song, menuOpen) { menuOpen = false }
                                AlbumArt(
                                    uri = song.uri,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp)),
                                )
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
                        if (rowSongs.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LibraryRow(label: String, count: Int, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val expressive = com.artemiy.player.ui.theme.expressiveUi
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onClick() }
            .padding(vertical = if (expressive) 10.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(if (expressive) 16.dp else 10.dp))
                .background(com.artemiy.player.ui.components.tonalAccent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = if (expressive) PlayerColors.AccentStandalone else PlayerColors.TextPrimary, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            color = PlayerColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f).padding(start = 14.dp),
        )
        Text(text = "$count", color = PlayerColors.TextTertiary, fontSize = 13.sp, modifier = Modifier.padding(end = 2.dp))
    }
}

@Composable
internal fun <T> ListToolbar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    sortOptions: List<T>,
    sortOptionLabel: @Composable (T) -> String,
    currentSort: String,
    onSortSelect: (T) -> Unit,
    viewMode: ViewMode,
    onViewModeCycle: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fadesWithHeader()
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(PlayerColors.Surface)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = AppIcons.Search, contentDescription = null, tint = PlayerColors.TextSecondary, modifier = Modifier.size(16.dp))
            com.artemiy.player.ui.components.HintTextField(
                value = query,
                onValueChange = onQueryChange,
                hint = placeholder,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Icon(
            imageVector = viewMode.icon,
            contentDescription = stringResource(R.string.view_mode, stringResource(viewMode.descriptionRes)),
            tint = PlayerColors.TextSecondary,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(20.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onViewModeCycle() },
        )
        // No sort options (a list with a fixed order) = no sort button at all.
        if (sortOptions.isNotEmpty()) Box {
            Icon(
                imageVector = AppIcons.Sort,
                contentDescription = stringResource(R.string.sort_mode, currentSort),
                tint = PlayerColors.TextSecondary,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(20.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { menuExpanded = true },
            )
            AppDropdownMenu(expanded = menuExpanded, onDismiss = { menuExpanded = false }) {
                sortOptions.forEach { option ->
                    AppMenuItem(
                        text = sortOptionLabel(option),
                        selected = sortOptionLabel(option) == currentSort,
                        onClick = {
                            onSortSelect(option)
                            menuExpanded = false
                        },
                    )
                }
            }
        }
    }
}
