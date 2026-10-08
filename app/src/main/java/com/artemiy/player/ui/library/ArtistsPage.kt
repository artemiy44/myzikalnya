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
import com.artemiy.player.ui.components.expandAnchor
import com.artemiy.player.ui.settings.SettingsViewModel
import com.artemiy.player.ui.theme.PlayerColors

@Composable
internal fun ArtistsList(groups: List<ArtistGroup>, state: LazyListState, onArtistClick: (ArtistGroup) -> Unit) {
    val entrance = rememberEntrance()
    LazyColumn(modifier = Modifier.fillMaxWidth(), state = state, contentPadding = PaddingValues(bottom = LocalBarsInset.current)) {
        itemsIndexed(groups, key = { _, group -> group.name }) { index, group ->
            val coverUri = remember(group) { group.songs.minByOrNull { it.album.lowercase() }?.uri }
            val expressive = com.artemiy.player.ui.theme.expressiveUi
            val interaction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .then(if (expressive) Modifier.padding(horizontal = 12.dp) else Modifier)
                    .staggeredEntrance(index, entrance)
                    .groupedCard(index, groups.size)
                    .fillMaxWidth()
                    .clickable(interactionSource = interaction, indication = com.artemiy.player.ui.components.SoftPress) { onArtistClick(group) }
                    .padding(horizontal = if (expressive) 0.dp else 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumArt(
                    uri = coverUri,
                    modifier = Modifier
                        .size(52.dp)
                        .expandAnchor(com.artemiy.player.ui.components.LibraryAnchors, "artist:${group.name}", cornerDp = 16.dp, cornerFraction = 0.4f, washKey = "artist:${group.name}")
                        .clip(com.artemiy.player.ui.components.artistShape(group.name, interaction)),
                )
                Text(
                    text = group.name,
                    color = PlayerColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                Text(
                    text = "${group.songs.size}",
                    color = PlayerColors.TextTertiary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
internal fun ArtistsGrid(groups: List<ArtistGroup>, columns: Int, state: LazyGridState, onArtistClick: (ArtistGroup) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        // The gaps as the grid's own padding, not around it: clipped 4 dp short, the tiles left a
        // bare strip under the floating mini player.
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 4.dp + LocalBarsInset.current),
        modifier = Modifier.fillMaxSize(),
    ) {
        gridItems(groups, key = { it.name }) { group ->
            val coverUri = remember(group) { group.songs.minByOrNull { it.album.lowercase() }?.uri }
            val interaction = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .clickable(interactionSource = interaction, indication = com.artemiy.player.ui.components.SoftPress) { onArtistClick(group) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AlbumArt(
                    uri = coverUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .expandAnchor(com.artemiy.player.ui.components.LibraryAnchors, "artist:${group.name}", cornerDp = 16.dp, cornerFraction = 0.4f, washKey = "artist:${group.name}")
                        .clip(com.artemiy.player.ui.components.artistShape(group.name, interaction)),
                )
                Text(
                    text = group.name,
                    color = PlayerColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "${group.songs.size}",
                    color = PlayerColors.TextTertiary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
