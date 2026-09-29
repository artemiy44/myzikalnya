package com.artemiy.player.ui.home

import com.artemiy.player.ui.theme.barsInset
import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import com.artemiy.player.ui.icons.AppIcons
import com.artemiy.player.data.Recap
import com.artemiy.player.data.Mix
import com.artemiy.player.data.MIX_MIN_STAT_DAYS
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemiy.player.data.Mood
import com.artemiy.player.data.Song
import com.artemiy.player.ui.components.AnimatedBackStack
import com.artemiy.player.playback.PlayOrigin
import com.artemiy.player.playback.SourceArt
import com.artemiy.player.playback.SourcePlace
import com.artemiy.player.ui.components.AlbumArt
import com.artemiy.player.ui.components.SongActionsMenuPopup
import com.artemiy.player.ui.components.songLongPressTrigger
import com.artemiy.player.ui.components.StatusBarFade
import com.artemiy.player.ui.theme.PlayerColors
import kotlin.math.cos
import kotlin.math.sin

/** Home's own little navigation: the main page, or one of the pages opened from it. */
private sealed interface HomeRoute {
    data object Main : HomeRoute
    data class OpenMix(val id: String) : HomeRoute
    data object RecentlyAddedAll : HomeRoute
    data object WeekRecap : HomeRoute
}

@Composable
fun HomeScreen(
    /** Goes up each time the Home tab is tapped while already open: back to the main page, or to
     * its top when already there. */
    rootRequest: Int,
    mixes: List<Mix>,
    statDays: Int,
    quickPicks: List<Song>,
    recentlyAdded: List<Song>,
    recentlyAddedAll: List<Song>,
    recap: Recap?,
    /** The third value says where the songs came from, for Now Playing's "Играет из". */
    onSongClick: (Song, List<Song>, PlayOrigin) -> Unit,
    onSaveMix: (Mix) -> Unit,
    onSettingsClick: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
) {
    var route by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = HomeRouteSaver) { mutableStateOf<HomeRoute>(HomeRoute.Main) }
    val back = { route = HomeRoute.Main }
    // Where playback started from, for the "Играет из" line — in the app's language.
    val fromRecentlyAdded = stringResource(R.string.recently_added)
    val fromRecap = stringResource(R.string.weekly_recap)
    val fromQuickPicks = stringResource(R.string.home_quick_picks)

    // Kept out here so the page's scroll position survives opening a mix and coming back.
    val mainScroll = rememberScrollState()
    LaunchedEffect(rootRequest) {
        if (rootRequest == 0) return@LaunchedEffect
        if (route != HomeRoute.Main) route = HomeRoute.Main else mainScroll.animateScrollTo(0)
    }
    val stack = if (route == HomeRoute.Main) listOf<HomeRoute>(HomeRoute.Main) else listOf(HomeRoute.Main, route)
    AnimatedBackStack(stack = stack, onBack = back) { r ->
        when (r) {
            is HomeRoute.OpenMix -> {
                val mix = mixes.firstOrNull { it.id == r.id }
                if (mix == null) {
                    LaunchedEffect(Unit) { route = HomeRoute.Main }
                } else {
                    MixScreen(mix, back, { song, list -> onSongClick(song, list, PlayOrigin(mix.title, SourceArt.MixCard(mix.colorIndex, mix.motif))) }, onSaveMix, onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)
                }
            }
            HomeRoute.RecentlyAddedAll -> {
                RecentlyAddedScreen(recentlyAddedAll, back, { song, list -> onSongClick(song, list, PlayOrigin(fromRecentlyAdded, SourceArt.Place(SourcePlace.RECENTLY_ADDED))) }, onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)
            }
            HomeRoute.WeekRecap -> {
                if (recap == null) LaunchedEffect(Unit) { route = HomeRoute.Main } else RecapScreen(recap, back) { song, list -> onSongClick(song, list, PlayOrigin(fromRecap, SourceArt.Place(SourcePlace.RECAP))) }
            }
            HomeRoute.Main -> {
                // The header scrolls away with the page instead of being pinned under the status bar, and the
                // page runs edge-to-edge behind the status bar — only a soft fade keeps its icons readable.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PlayerColors.Background),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(mainScroll).barsInset()
                            .statusBarsPadding()
                            .padding(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp, 20.dp, 20.dp, 0.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.tab_home),
                                color = PlayerColors.TextPrimary,
                                style = com.artemiy.player.ui.theme.pageTitleStyle,
                            )
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(PlayerColors.Surface)
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress) { onSettingsClick() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = AppIcons.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                    tint = PlayerColors.TextSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        val menuActions = SongMenuActions(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)
                        MixesSection(mixes = mixes, statDays = statDays, onOpen = { route = HomeRoute.OpenMix(it.id) })
                        SongRowSection(
                            title = stringResource(R.string.home_quick_picks),
                            songs = quickPicks,
                            emptyHint = stringResource(R.string.quick_picks_empty),
                            onSongClick = { song -> onSongClick(song, quickPicks, PlayOrigin(fromQuickPicks, SourceArt.Place(SourcePlace.QUICK_PICKS))) },
                            menuActions = menuActions,
                        )
                        SongRowSection(
                            title = stringResource(R.string.home_recently_added),
                            songs = recentlyAdded,
                            emptyHint = null,
                            onSongClick = { song -> onSongClick(song, recentlyAdded, PlayOrigin(fromRecentlyAdded, SourceArt.Place(SourcePlace.RECENTLY_ADDED))) },
                            menuActions = menuActions,
                            onSeeAll = { route = HomeRoute.RecentlyAddedAll },
                        )
                        RecapCard(recap = recap, onOpen = { route = HomeRoute.WeekRecap })
                    }
                    StatusBarFade()
                }
            }
        }
    }
}

@Composable
private fun MixesSection(mixes: List<Mix>, statDays: Int, onOpen: (Mix) -> Unit) {
    Column {
        Text(
            text = stringResource(R.string.mixes_for_you),
            color = PlayerColors.TextPrimary,
            style = com.artemiy.player.ui.theme.sectionTitleStyle,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 10.dp),
        )
        val hint = when {
            statDays < MIX_MIN_STAT_DAYS ->
                stringResource(R.string.mixes_collecting, statDays, MIX_MIN_STAT_DAYS)
            mixes.isEmpty() -> stringResource(R.string.mixes_nothing_yet)
            else -> null
        }
        if (hint != null) {
            Text(text = hint, color = PlayerColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
            return
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            mixes.forEach { mix -> MixCard(mix = mix, onClick = { onOpen(mix) }) }
        }
    }
}

/** Bundles the five "⋮" menu callbacks so they thread through Home's several song sections as
 * one param instead of five. */
private data class SongMenuActions(
    val onPlayNext: (Song) -> Unit,
    val onAddToQueue: (Song) -> Unit,
    val onAddToPlaylist: (Song) -> Unit,
    val onGoToAlbum: (Song) -> Unit,
    val onGoToArtist: (Song) -> Unit,
)

@Composable
private fun SongRowSection(
    title: String,
    songs: List<Song>,
    emptyHint: String?,
    onSongClick: (Song) -> Unit,
    menuActions: SongMenuActions,
    onSeeAll: (() -> Unit)? = null,
) {
    val artSize = 120.dp
    Column {
        Text(
            text = title,
            color = PlayerColors.TextPrimary,
            style = com.artemiy.player.ui.theme.sectionTitleStyle,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp),
        )
        if (songs.isEmpty() && emptyHint != null) {
            Text(
                text = emptyHint,
                color = PlayerColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            return
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            songs.forEach { song ->
                var menuExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .width(artSize)
                        .songLongPressTrigger(
                            onClick = { onSongClick(song) },
                            onLongPress = { menuExpanded = true },
                        ),
                ) {
                    Box {
                        AlbumArt(
                            uri = song.uri,
                            modifier = Modifier
                                .size(artSize)
                                .clip(RoundedCornerShape(if (com.artemiy.player.ui.theme.expressiveUi) 22.dp else 12.dp)),
                        )
                        SongActionsMenuPopup(
                            song = song,
                            expanded = menuExpanded,
                            onDismiss = { menuExpanded = false },
                            onPlayNext = menuActions.onPlayNext,
                            onAddToQueue = menuActions.onAddToQueue,
                            onAddToPlaylist = menuActions.onAddToPlaylist,
                            onGoToAlbum = menuActions.onGoToAlbum,
                            onGoToArtist = menuActions.onGoToArtist,
                        )
                    }
                    Text(
                        text = song.title,
                        color = PlayerColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp),
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
            if (onSeeAll != null) {
                Column(
                    modifier = Modifier
                        .size(artSize)
                        .clip(RoundedCornerShape(if (com.artemiy.player.ui.theme.expressiveUi) 22.dp else 12.dp))
                        .background(PlayerColors.Surface)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = com.artemiy.player.ui.components.SoftPress, onClick = onSeeAll),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(imageVector = AppIcons.Forward, contentDescription = null, tint = PlayerColors.TextPrimary, modifier = Modifier.size(26.dp))
                    Text(text = stringResource(R.string.all), color = PlayerColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

/** Which Home page is open, kept as a short text so it survives switching tabs. */
private val HomeRouteSaver = androidx.compose.runtime.saveable.Saver<HomeRoute, String>(
    save = { route ->
        when (route) {
            HomeRoute.Main -> "main"
            is HomeRoute.OpenMix -> "mix:" + route.id
            HomeRoute.RecentlyAddedAll -> "recent"
            HomeRoute.WeekRecap -> "recap"
        }
    },
    restore = { text ->
        when {
            text.startsWith("mix:") -> HomeRoute.OpenMix(text.removePrefix("mix:"))
            text == "recent" -> HomeRoute.RecentlyAddedAll
            text == "recap" -> HomeRoute.WeekRecap
            else -> HomeRoute.Main
        }
    },
)
