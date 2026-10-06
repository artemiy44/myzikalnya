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

sealed class LibraryRoute {
    data object Home : LibraryRoute()
    data object Playlists : LibraryRoute()
    data object Artists : LibraryRoute()
    data object Albums : LibraryRoute()
    data object Songs : LibraryRoute()
    data object Years : LibraryRoute()
    data object Genres : LibraryRoute()
    /** The folders section, opened on the top of the music (see startFolder). */
    data object Folders : LibraryRoute()
    /** A folder, by its path from the storage root. */
    data class FolderDetail(val path: List<String>) : LibraryRoute()
    /** A year's page; null = songs whose tags have no year. */
    data class YearDetail(val year: Int?) : LibraryRoute()
    /** A genre's page, by its GenreNames key; null = songs whose tags have no genre. */
    data class GenreDetail(val key: String?) : LibraryRoute()
    data class ArtistDetail(val artist: String) : LibraryRoute()
    data class AlbumDetail(val album: String, val artist: String) : LibraryRoute()
    data class PlaylistDetail(val playlistId: Long, val name: String) : LibraryRoute()
}

internal data class ArtistGroup(val name: String, val songs: List<Song>)

internal data class AlbumGroup(val album: String, val artist: String, val songs: List<Song>)

/** Songs gathered by a tag — a year, or a genre. [key] null = the tag is missing; [name] is how
 * it's shown (the library's usual spelling of the genre). */
internal data class TagGroup(val key: String?, val name: String?, val songs: List<Song>)


/**
 * What makes two songs the same album: its name and its MAIN artist (the first one in the artist
 * line), ignoring case and spacing. A guest on one track ("Bring Me The Horizon feat. Underoath",
 * "Porter Robinson/Totally Enormous Extinct Dinosaurs") doesn't split the album off.
 */
internal fun albumKey(album: String, artist: String): String =
    ArtistNames.key(album) + "\u0000" + ArtistNames.key(ArtistNames.split(artist).firstOrNull() ?: artist)

/** The spelling most of [spellings] use (ties go to the first one seen). */
internal fun mostCommon(spellings: List<String>): String =
    spellings.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: spellings.first()

/** Cycled by a single toolbar icon, in this order: list rows, then 2-wide grid, then 3-wide. */
internal enum class ViewMode(@androidx.annotation.StringRes val descriptionRes: Int) {
    LIST(R.string.view_list),
    GRID_2(R.string.view_grid_2),
    GRID_3(R.string.view_grid_3);

    /** Read at draw time — the icon depends on the chosen icon set. */
    val icon: ImageVector
        @Composable get() = when (this) {
            LIST -> AppIcons.ViewList
            GRID_2 -> AppIcons.ViewGrid
            GRID_3 -> AppIcons.ViewGridDense
        }

    fun next(): ViewMode = entries[(ordinal + 1) % entries.size]
}

internal enum class ArtistSort(val labelRes: Int) { COUNT(R.string.sort_by_count), RECENT(R.string.sort_recent), NAME(R.string.sort_by_name) }

internal enum class AlbumSort(val labelRes: Int) { RECENT(R.string.sort_recent), NAME(R.string.sort_by_name), COUNT(R.string.sort_by_count), ARTIST(R.string.sort_by_artist) }

internal enum class PlaylistSongSort(val labelRes: Int) { ORDER(R.string.sort_added_order), TITLE(R.string.sort_by_title), ARTIST(R.string.sort_by_artist), RECENT(R.string.sort_recent_library) }

internal enum class SongSort(val labelRes: Int) { RECENT(R.string.sort_recent), RELEASE_DATE(R.string.sort_release_date), TITLE(R.string.sort_by_title), ARTIST(R.string.sort_by_artist) }

/** "'24" for 2024 — how a year is drawn on its tile and behind its page's name. */
internal fun shortYear(year: String): String = "'" + year.takeLast(2)

/** How far apart the earliest and latest of [times] are (for the fast scroller's date pill). */
internal fun dateSpan(times: List<Long>): Long = if (times.isEmpty()) 0L else times.max() - times.min()


/** The remembered sort of a library tab, or [default] until one is chosen. */
internal inline fun <reified E : Enum<E>> com.artemiy.player.ui.settings.SettingsViewModel.sortOf(tab: String, default: E): E =
    sortName(tab)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
