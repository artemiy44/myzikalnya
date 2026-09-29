package com.artemiy.player.ui.nowplaying

import com.artemiy.player.R
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.artemiy.player.data.LiveBlurIntensity
import com.artemiy.player.data.NowPlayingBackgroundMode
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.util.lerp
import com.artemiy.player.ui.components.MINI_ART_CORNER
import com.artemiy.player.ui.components.MiniPlayerAnchors
import com.artemiy.player.ui.theme.PlayerColors
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.EnterTransition
import androidx.compose.runtime.SideEffect
import com.artemiy.player.data.PlayerStyle
import com.artemiy.player.data.Song
import com.artemiy.player.playback.PlaySource
import com.artemiy.player.lyrics.ParsedLyrics
import com.artemiy.player.lyrics.hasRomanization
import com.artemiy.player.playback.openOutputSwitcher
import com.artemiy.player.ui.components.ART_SIZE_FULL
import com.artemiy.player.ui.components.AlbumArt
import kotlinx.coroutines.delay

@Composable
fun NowPlayingScreen(
    song: Song?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    manualQueue: List<Song>,
    continueQueue: List<Song>,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    infinitePlayEnabled: Boolean,
    lyrics: ParsedLyrics?,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onQueueItemClick: (Song) -> Unit,
    onClearManualQueue: () -> Unit,
    onMoveInQueue: (from: Int, to: Int) -> Unit,
    onRemoveFromQueue: (position: Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleInfinitePlay: () -> Unit,
    allSongs: List<Song>,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    nowPlayingBackgroundMode: NowPlayingBackgroundMode = NowPlayingBackgroundMode.LIVE_BLUR,
    liveBlurIntensity: LiveBlurIntensity = LiveBlurIntensity.NORMAL,
    lyricsRomanization: Boolean = true,
    onToggleLyricsRomanization: () -> Unit = {},
    lyricsTapPlays: Boolean = false,
    playerStyle: PlayerStyle = PlayerStyle.CLASSIC,
    playingFrom: PlaySource? = null,
    lyricsLoading: Boolean = false,
    /** Classic style: how far the player is open (0 = still the mini player, 1 = open). */
    expand: () -> Float = { 1f },
    /** Classic style: where the mini player's pieces are — the player grows out of it. */
    mini: MiniPlayerAnchors? = null,
    memory: NowPlayingMemory = remember { NowPlayingMemory() },
) {
    // The big cover decoded as soon as the song changes, not the moment it's first shown.
    com.artemiy.player.ui.components.rememberAlbumArtBitmap(song?.uri, com.artemiy.player.ui.components.ART_SIZE_FULL)
    // …and the next song's too (both sizes: the cover, and the blurred background made from the
    // small one), so flicking to it finds them ready instead of decoding them only then.
    val nextUp = (manualQueue.firstOrNull() ?: continueQueue.firstOrNull())?.uri
    com.artemiy.player.ui.components.rememberAlbumArtBitmap(nextUp, com.artemiy.player.ui.components.ART_SIZE_FULL)
    com.artemiy.player.ui.components.rememberAlbumArtBitmap(nextUp)
    if (playerStyle == PlayerStyle.EXPRESSIVE) {
        ExpressiveNowPlaying(
            song = song, isPlaying = isPlaying, positionMs = positionMs, durationMs = durationMs,
            manualQueue = manualQueue, continueQueue = continueQueue,
            shuffleEnabled = shuffleEnabled, repeatEnabled = repeatEnabled, infinitePlayEnabled = infinitePlayEnabled,
            lyrics = lyrics, playingFrom = playingFrom,
            onClose = onClose, onTogglePlayPause = onTogglePlayPause, onSkipNext = onSkipNext, onSkipPrevious = onSkipPrevious,
            onSeek = onSeek, onQueueItemClick = onQueueItemClick, onClearManualQueue = onClearManualQueue,
            onMoveInQueue = onMoveInQueue, onRemoveFromQueue = onRemoveFromQueue,
            onToggleShuffle = onToggleShuffle, onToggleRepeat = onToggleRepeat, onToggleInfinitePlay = onToggleInfinitePlay,
            allSongs = allSongs, onPlayNext = onPlayNext, onAddToQueue = onAddToQueue, onAddToPlaylist = onAddToPlaylist,
            onGoToAlbum = onGoToAlbum, onGoToArtist = onGoToArtist,
            backgroundMode = nowPlayingBackgroundMode, blurIntensity = liveBlurIntensity,
            lyricsRomanization = lyricsRomanization, onToggleLyricsRomanization = onToggleLyricsRomanization,
            lyricsTapPlays = lyricsTapPlays,
            lyricsLoading = lyricsLoading,
            memory = memory,
            expand = expand,
            mini = mini,
        )
        return
    }
    var centerMode by memory::centerMode
    // Moving between the cover and lyrics/queue: the cover flies up into the header's thumbnail
    // (and back down out of it), the title between the panel and the header — the same kind of
    // flight as opening from the mini player. 1 = settled; lyrics <-> queue has nothing to fly.
    var modeFlight by remember { mutableFloatStateOf(1f) }
    var modeFlightFromArt by remember { mutableStateOf(true) }
    var modeFlightRun by remember { mutableIntStateOf(0) }
    // Lyrics or the queue — whichever end of the flight isn't the cover.
    var modeFlightOther by remember { mutableStateOf(CenterMode.Queue) }
    var sideFlight by remember { mutableFloatStateOf(1f) }
    var sideFrom by remember { mutableStateOf(CenterMode.Lyrics) }
    var sideFlightRun by remember { mutableIntStateOf(0) }
    // How fast each flight is moving right now, to hand over when one is turned around midway.
    val flightVelocity = remember { floatArrayOf(0f, 0f) }
    var sideStartVelocity by remember { mutableFloatStateOf(0f) }
    var modeStartVelocity by remember { mutableFloatStateOf(0f) }
    // Which run of each flight may still move it. An animation that has just been replaced (or
    // stopped) can deliver one more frame before it's cancelled; without this, that stale frame
    // landed on top of the new start and the flight jumped — seen when tapping lyrics/queue fast.
    val flightRuns = remember { intArrayOf(0, 0) }
    fun switchMode(next: CenterMode) {
        if (next == centerMode) return
        val crossesArt = (next == CenterMode.Art) != (centerMode == CenterMode.Art)
        if (!crossesArt) {
            // Turning back halfway: carry on from the same spot and at the same speed, the other
            // way round — starting from standstill made quick back-and-forth taps look jerky.
            sideStartVelocity = if (sideFlight < 1f) -flightVelocity[0] else 0f
            sideFlight = if (sideFlight < 1f) 1f - sideFlight else 0f
            sideFrom = centerMode
            sideFlightRun++
            flightRuns[0] = sideFlightRun
        } else {
            flightRuns[0] = -1
            sideFlight = 1f
        }
        if (crossesArt) {
            // Turning back halfway: carry on from the same spot, the other way round.
            modeStartVelocity = if (modeFlight < 1f) -flightVelocity[1] else 0f
            modeFlight = if (modeFlight < 1f) 1f - modeFlight else 0f
            modeFlightFromArt = centerMode == CenterMode.Art
            modeFlightOther = if (next == CenterMode.Art) centerMode else next
            modeFlightRun++
            flightRuns[1] = modeFlightRun
        }
        centerMode = next
    }
    LaunchedEffect(sideFlightRun) {
        if (sideFlightRun == 0) return@LaunchedEffect
        val run = sideFlightRun
        androidx.compose.animation.core.animate(sideFlight, 1f, sideStartVelocity, MODE_FLIGHT_SPRING) { value, velocity ->
            if (flightRuns[0] != run) return@animate
            sideFlight = value
            flightVelocity[0] = velocity
        }
    }
    LaunchedEffect(modeFlightRun) {
        if (modeFlightRun == 0) return@LaunchedEffect
        val run = modeFlightRun
        androidx.compose.animation.core.animate(modeFlight, 1f, modeStartVelocity, MODE_FLIGHT_SPRING) { value, velocity ->
            if (flightRuns[1] != run) return@animate
            modeFlight = value
            flightVelocity[1] = velocity
        }
    }
    // The player starting to close (dragged down, or the handle) while the cover is still flying
    // between the big view and the header: that flight ends here and then — the cover, the title
    // and the rest go along with the closing instead, rather than finishing their landing in the
    // big player and vanishing the moment it was gone.
    val closing by remember { derivedStateOf { expand() < 0.999f } }
    LaunchedEffect(closing) {
        if (closing) {
            flightRuns[0] = -1
            flightRuns[1] = -1
            sideFlight = 1f
            modeFlight = 1f
        }
    }
    // Where the pieces sit at either end, as last laid out.
    var headerArtBounds by remember { mutableStateOf<Rect?>(null) }
    var headerTextBounds by remember { mutableStateOf<Rect?>(null) }
    var panelTextBounds by remember { mutableStateOf<Rect?>(null) }
    // The "⋯" song menu: beside the title under the cover, and in the header on the queue.
    var panelMenuBounds by remember { mutableStateOf<Rect?>(null) }
    var headerMenuBounds by remember { mutableStateOf<Rect?>(null) }
    // It flies along with the title only between the cover and the queue (lyrics has none).
    val menuFlight = { if (modeFlightOther == CenterMode.Queue) modeFlight else 1f }
    var controlsVisible by remember { mutableStateOf(true) }
    var closingFromHidden by remember { mutableStateOf(false) }
    var controlsVisibleBeforeDrag by remember { mutableStateOf(true) }
    var showAddToQueuePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Reset to visible whenever the center content changes mode.
    LaunchedEffect(centerMode) {
        controlsVisible = true
    }

    // Lyrics: auto-hide after a few seconds of inactivity, but only while a song is actually
    // playing — pausing forces the controls back so they're not stuck hidden while paused.
    LaunchedEffect(centerMode, controlsVisible, isPlaying) {
        if (centerMode == CenterMode.Lyrics && controlsVisible && isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }
    LaunchedEffect(isPlaying) {
        if (!isPlaying && centerMode == CenterMode.Lyrics) controlsVisible = true
    }

    val queueListState = rememberLazyListState()
    val queueNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -2f) controlsVisible = false
                else if (available.y > 2f) controlsVisible = true
                return Offset.Zero
            }
        }
    }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.92f,
        animationSpec = tween(300),
        label = "artScale",
    )
    // While opening: the background comes in over the first half, the cover flies all the way,
    // and everything else only shows up over the last quarter.
    val chromeAlpha = { ((expand() - 0.75f) / 0.25f).coerceIn(0f, 1f) }
    val panelAlpha = { if (closingFromHidden) 0f else chromeAlpha() }
    // Play/next when the panel was hidden: out of nothing over the first quarter of closing.
    val growFromNothing = { if (closingFromHidden) ((1f - expand()) / 0.25f).coerceIn(0f, 1f) else 1f }
    var finalArtBounds by remember { mutableStateOf<Rect?>(null) }
    val miniCornerPx = with(LocalDensity.current) { MINI_ART_CORNER.toPx() }
    val artCornerPx = with(LocalDensity.current) { 16.dp.toPx() }
    // The sheet starts as the mini player bar itself (in its own color) and grows to the full screen.
    // The mini player's own colors are read here, before the player's palette takes over.
    val miniColor = mini?.color ?: PlayerColors.SurfaceDim
    val miniTitleColor = PlayerColors.TextPrimary
    val miniArtistColor = PlayerColors.TextSecondary
    val miniIconColor = PlayerColors.TextPrimary
    var titleTarget by remember { mutableStateOf<Rect?>(null) }
    val playerActive = LocalNowPlayingActive.current
    val coverShadow = nowPlayingBackgroundMode != NowPlayingBackgroundMode.NONE
    // Back from lyrics/queue the cover lands as a flying copy without a shadow: once the real one
    // is in place its shadow fades in, instead of appearing all at once.
    // (Read through derivedStateOf: the flight changes every frame, the answer only twice.)
    val flyingBack by remember { derivedStateOf { !modeFlightFromArt && modeFlight < 1f } }
    val landedShadow by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (flyingBack) 0f else 1f,
        animationSpec = if (flyingBack) androidx.compose.animation.core.snap() else tween(COVER_SHADOW_FADE_MS),
        label = "coverShadow",
    )
    // What sits at the right end of the header: the romanization switch on lyrics, the song's
    // "⋯" menu on the queue (flying there from beside the title under the cover).
    @Composable
    fun HeaderTrailing(mode: CenterMode) {
        when {
            mode == CenterMode.Lyrics && lyrics?.hasRomanization() == true -> RomanizationToggle(
                enabled = lyricsRomanization,
                onToggle = onToggleLyricsRomanization,
            )
            mode == CenterMode.Queue && song != null -> Box(
                modifier = Modifier.flyFrom(
                    panelMenuBounds,
                    { if (modeFlightFromArt) menuFlight() else 1f },
                    fromCenter = true,
                    startScale = 1f,
                    onTarget = { headerMenuBounds = it },
                ),
            ) {
                CurrentSongMenuButton(
                    song = song,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onAddToPlaylist = onAddToPlaylist,
                    onGoToAlbum = onGoToAlbum,
                    onGoToArtist = onGoToArtist,
                )
            }
        }
    }
    NowPlayingSurface(
        song = song,
        mode = nowPlayingBackgroundMode,
        intensity = liveBlurIntensity,
        openFrom = mini?.bar,
        openProgress = expand,
        openTint = miniColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // top only, not bottom — the control panel below already gets its own
                // navigationBarsPadding lower down; a bottom value here too double-reserves
                // space and brings back the "ledge" above the gesture bar.
                .padding(start = 22.dp, end = 22.dp, top = 20.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 5.dp)
                        .graphicsLayer { alpha = chromeAlpha() }
                        .clip(RoundedCornerShape(3.dp))
                        .background(LocalAdaptiveSecondaryColor.current)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onClose() },
                )
            }

            var controlsHeightPx by remember { mutableStateOf(0) }
            var headerHeightPx by remember { mutableStateOf(0) }
            var toggleRowHeightPx by remember { mutableStateOf(0) }
            val density = LocalDensity.current

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Lyrics <-> queue: the one leaving slides off to its side and fades while the
                // other slides in from the other side (lyrics' button is left of the queue's).
                val sideOutgoing = sideFrom.takeIf { sideFlight < 1f && it != centerMode }
                listOfNotNull(sideOutgoing, centerMode).forEach { mode ->
                key(mode) {
                Box(
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        if (sideOutgoing == null) return@graphicsLayer
                        val toQueue = centerMode == CenterMode.Queue
                        val shift = size.width * SIDE_SHIFT * (if (toQueue) 1f else -1f)
                        val p = sideFlight
                        if (mode == centerMode) {
                            translationX = shift * (1f - p)
                            alpha = ((p - 0.15f) / 0.85f).coerceIn(0f, 1f)
                        } else {
                            translationX = -shift * p
                            alpha = (1f - p / 0.6f).coerceIn(0f, 1f)
                        }
                    },
                ) {
                when (mode) {
                    CenterMode.Art -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 10.dp)
                            .padding(bottom = with(density) { controlsHeightPx.toDp() }),
                        contentAlignment = Alignment.Center,
                    ) {
                        AlbumArt(
                            uri = song?.uri,
                            size = ART_SIZE_FULL,
                            crossfade = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .onGloballyPositioned { finalArtBounds = it.boundsInRoot() }
                                // Flying in from (or back to) the mini player's cover: drawn at the
                                // in-between position and size, its corners in between too.
                                .graphicsLayer {
                                    val p = FLIGHT_EASING.transform(expand())
                                    val to = finalArtBounds
                                    val from = mini?.art
                                    if (playerActive && p < 1f && to != null && from != null && to.width > 0f) {
                                        // Aim for the cover as it'll actually sit — a touch smaller
                                        // while paused — so there's no jump when it lands.
                                        val endW = to.width * artScale
                                        val endH = to.height * artScale
                                        val endLeft = to.center.x - endW / 2
                                        val endTop = to.center.y - endH / 2
                                        val left = lerp(from.left, endLeft, p)
                                        val top = lerp(from.top, endTop, p)
                                        val width = lerp(from.width, endW, p)
                                        val height = lerp(from.height, endH, p)
                                        transformOrigin = TransformOrigin(0f, 0f)
                                        translationX = left - to.left
                                        translationY = top - to.top
                                        scaleX = width / to.width
                                        scaleY = height / to.height
                                        // The clip is drawn before the scaling, so the on-screen radius
                                        // is divided back up by the scale.
                                        val onScreen = lerp(miniCornerPx, artCornerPx, p)
                                        shape = RoundedCornerShape(onScreen * to.width / width)
                                    } else {
                                        // Settled: the paused shrink happens here too, so the
                                        // rounded clip shrinks along with the picture.
                                        transformOrigin = TransformOrigin.Center
                                        scaleX = artScale
                                        scaleY = artScale
                                        shape = RoundedCornerShape(artCornerPx)
                                    }
                                    // Coming back from lyrics/queue: the flying copy lands here.
                                    alpha = if (modeFlight < 1f && !modeFlightFromArt) 0f else 1f
                                    // A faint shadow lifts the cover off a blurred background — only
                                    // once it's settled in place (it comes up as it lands), and not
                                    // over the plain background, where there's nothing to lift it off.
                                    // Opening, it grows with the cover over the second half of the flight.
                                    val rise = ((expand() - 0.5f) / 0.5f).coerceIn(0f, 1f)
                                    val settled = rise * rise * (3f - 2f * rise) * landedShadow
                                    if (coverShadow && settled > 0f) {
                                        shadowElevation = COVER_SHADOW.toPx() * settled
                                        spotShadowColor = Color.Black.copy(alpha = 0.6f)
                                        ambientShadowColor = Color.Black.copy(alpha = 0.35f)
                                    } else {
                                        shadowElevation = 0f
                                    }
                                    clip = true
                                },
                        )
                    }

                    CenterMode.Lyrics -> Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = chromeAlpha() }.modeContentIn { modeFlight }) {
                        // Reading along: the screen stays awake (only while the player is open —
                        // it's kept around, hidden, after closing).
                        if (LocalNowPlayingActive.current) com.artemiy.player.ui.components.KeepScreenOn()
                        LyricsView(
                            lyrics = lyrics,
                            positionMs = positionMs,
                            isPlaying = isPlaying,
                            topFadePx = { headerHeightPx },
                            // The control panel auto-hides while a Lyrics track plays — no panel
                            // up means nothing for the list to fade out from under, so the fade
                            // boundary collapses to the true bottom edge instead of staying
                            // pinned to the (now invisible) panel's last known height.
                            bottomFadePx = { if (controlsVisible) controlsHeightPx else 0 },
                            anchorTopPx = headerHeightPx,
                            anchorBottomPx = if (controlsVisible) controlsHeightPx else 0,
                            onLineClick = { timeMs ->
                                onSeek(timeMs)
                                if (lyricsTapPlays && !isPlaying) onTogglePlayPause()
                            },
                            showRomanization = lyricsRomanization,
                            loading = lyricsLoading,
                            // Extra FADE_SPAN at both ends: otherwise the first/last lines can't
                            // scroll out of the fade zone (nothing above/below them to scroll)
                            // and stay stuck half-faded, reading as gray.
                            contentPadding = PaddingValues(
                                top = with(density) { headerHeightPx.toDp() } + FADE_SPAN + 8.dp,
                                bottom = with(density) { controlsHeightPx.toDp() } + FADE_SPAN + 8.dp,
                            ),
                        )
                        if (!controlsVisible) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { controlsVisible = true },
                            )
                        }
                    }

                    // The toggle row floats fixed under the header (same idea as the header
                    // itself) — the list scrolls underneath BOTH of them and fades out as it
                    // approaches, exactly like Lyrics does under its header.
                    CenterMode.Queue -> Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = chromeAlpha() }.modeContentIn { modeFlight }) {
                        QueueList(
                            manualQueue = manualQueue,
                            continueQueue = continueQueue,
                            onItemClick = onQueueItemClick,
                            onClearManualQueue = onClearManualQueue,
                            onAddSongsClick = { showAddToQueuePicker = true },
                            onMove = onMoveInQueue,
                            onRemove = onRemoveFromQueue,
                            songActions = remember(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist) {
                                QueueSongActions(onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)
                            },
                            // The control panel covers the bottom of the list, so a picked-up
                            // song couldn't be dragged down there — hide it for the duration of
                            // the drag, then put it back the way it was.
                            onDragActiveChange = { active ->
                                if (active) {
                                    controlsVisibleBeforeDrag = controlsVisible
                                    controlsVisible = false
                                } else {
                                    controlsVisible = controlsVisibleBeforeDrag
                                }
                            },
                            state = queueListState,
                            topFadePx = { headerHeightPx + toggleRowHeightPx },
                            // Same reasoning as Lyrics — the panel hides on scroll in Queue too,
                            // so the fade should collapse with it instead of hanging around.
                            bottomFadePx = { if (controlsVisible) controlsHeightPx else 0 },
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(queueNestedScrollConnection),
                            contentPadding = PaddingValues(
                                top = with(density) { (headerHeightPx + toggleRowHeightPx).toDp() } + 8.dp,
                                bottom = with(density) { controlsHeightPx.toDp() } + 24.dp,
                            ),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(top = with(density) { headerHeightPx.toDp() })
                                .onGloballyPositioned { toggleRowHeightPx = it.size.height },
                        ) {
                            QueueToggleRow(
                                shuffleEnabled = shuffleEnabled,
                                repeatEnabled = repeatEnabled,
                                infinitePlayEnabled = infinitePlayEnabled,
                                onToggleShuffle = onToggleShuffle,
                                onToggleRepeat = onToggleRepeat,
                                onToggleInfinitePlay = onToggleInfinitePlay,
                            )
                        }
                    }
                }
                }
                }
                }

                // Floating mini-header — Lyrics/Queue only (Art shows title in the control panel
                // below instead). Overlaps the scrollable content instead of pushing it down — no
                // background/blur of its own at all now; the list underneath fades itself out via
                // fadeInList() before it gets here, so there's nothing to hide a hard edge from.
                if (centerMode != CenterMode.Art) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .onGloballyPositioned { headerHeightPx = it.size.height }
                            // Absorbs taps anywhere in this panel's bounds so they don't fall
                            // through to whatever's scrolling underneath (was letting stray taps
                            // near the header hit list rows behind it).
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                            .padding(top = 10.dp),
                    ) {
                        NowPlayingMiniHeader(
                            song = song,
                            onCollapse = { switchMode(CenterMode.Art) },
                            onGoToAlbum = onGoToAlbum,
                            onGoToArtist = onGoToArtist,
                            // Opening/closing on lyrics or the queue: the mini player's cover and
                            // text fly to (and from) this header instead of the big cover.
                            artModifier = Modifier
                                .onGloballyPositioned { headerArtBounds = it.boundsInRoot() }
                                // The big cover is flying up into here — this one shows once it lands.
                                .graphicsLayer { alpha = if (modeFlight < 1f && modeFlightFromArt) 0f else 1f }
                                .flyFrom(mini?.art, expand, fromCenter = true),
                            textModifier = Modifier.flyFrom(
                                panelTextBounds,
                                { if (modeFlightFromArt) modeFlight else 1f },
                                fromCenter = false,
                                startScale = 20f / 17f,
                                onTarget = { headerTextBounds = it },
                            ).flyFrom(
                                mini?.text,
                                expand,
                                fromCenter = false,
                                startScale = 13f / 17f,
                                fadeInUntil = TEXT_HANDOFF,
                                onTarget = { titleTarget = it },
                            ),
                            trailing = {
                                // Lyrics <-> queue: the one leaving fades out as the other fades in
                                // in the same spot (the romanization switch / the "⋯" menu).
                                val sideLeaving = sideFrom.takeIf { sideFlight < 1f && it != centerMode }
                                Box(modifier = Modifier.graphicsLayer { alpha = chromeAlpha() }) {
                                    listOfNotNull(sideLeaving, centerMode).forEach { mode ->
                                        key(mode) {
                                            Box(
                                                modifier = Modifier.graphicsLayer {
                                                    if (sideLeaving != null) {
                                                        alpha = if (mode == centerMode) sideFlight else 1f - sideFlight
                                                    }
                                                },
                                            ) {
                                                HeaderTrailing(mode)
                                            }
                                        }
                                    }
                                }
                            },
                        )
                    }
                }

                // While opening or closing, the panel is always there (at once, no fade): play and
                // next fly between it and the mini player — hidden with the panel (lyrics/queue hide
                // it), they'd just pop into the mini player at the end.
                // Moving between closed and open — fully closed doesn't count (the player is kept, just
                // hidden, so otherwise one closing ran straight on into the next opening and it never
                // noticed a new flight had started).
                val inFlight by remember { derivedStateOf { expand() in 0.001f..0.999f } }
                // Closing while the panel was hidden (lyrics/queue hide it): the rest of the panel
                // stays out of sight, and play/next grow out of nothing before flying down.
                // Opening: the panel is shown, so play/next have somewhere to land.
                // Decided in the same frame the flight starts (a LaunchedEffect would be a frame late,
                // flashing the panel for that frame).
                var wasInFlight by remember { mutableStateOf(false) }
                SideEffect {
                    if (inFlight != wasInFlight) {
                        wasInFlight = inFlight
                        if (!inFlight) {
                            closingFromHidden = false
                        } else if (expand() > 0.5f) {
                            closingFromHidden = !controlsVisible
                        } else {
                            controlsVisible = true
                            closingFromHidden = false
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible || inFlight,
                    enter = if (inFlight) EnterTransition.None else fadeIn(tween(250)),
                    exit = fadeOut(tween(250)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onGloballyPositioned { controlsHeightPx = it.size.height }
                        // Same tap-absorption as the header above — otherwise taps landing in the
                        // gaps between the slider/buttons fall through to the list underneath.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                        .navigationBarsPadding(),
                ) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    // Redundant with NowPlayingMiniHeader once we're in Lyrics/Queue mode — that
                    // already shows title/artist up top. Only show it below the big cover in the
                    // plain Art view. (A "slides up into the mini header" transition animation is
                    // a nice follow-up, not this pass.)
                    if (centerMode == CenterMode.Art) {
                        SongTitleRow(
                            song = song,
                            onGoToAlbum = onGoToAlbum,
                            onGoToArtist = onGoToArtist,
                            // Title and artist fly over from the mini player's text.
                            textModifier = Modifier.flyFrom(
                                headerTextBounds,
                                { if (!modeFlightFromArt) modeFlight else 1f },
                                fromCenter = false,
                                startScale = 17f / 20f,
                                onTarget = { panelTextBounds = it },
                            ).flyFrom(
                                mini?.text,
                                expand,
                                fromCenter = false,
                                startScale = MINI_TITLE_SCALE,
                                fadeInUntil = TEXT_HANDOFF,
                                onTarget = { titleTarget = it },
                            ),
                            trailing = {
                                if (song != null) Box(
                                    modifier = Modifier
                                        .flyFrom(
                                            headerMenuBounds,
                                            { if (!modeFlightFromArt) menuFlight() else 1f },
                                            fromCenter = true,
                                            startScale = 1f,
                                            onTarget = { panelMenuBounds = it },
                                        )
                                        .graphicsLayer {
                                            // Back from lyrics (no "⋯" up there to fly from): fades in.
                                            val fromLyrics = !modeFlightFromArt && modeFlightOther == CenterMode.Lyrics
                                            alpha = panelAlpha() * (if (fromLyrics) modeFlight else 1f)
                                        },
                                ) {
                                    CurrentSongMenuButton(
                                        song = song,
                                        onPlayNext = onPlayNext,
                                        onAddToQueue = onAddToQueue,
                                        onAddToPlaylist = onAddToPlaylist,
                                        onGoToAlbum = onGoToAlbum,
                                        onGoToArtist = onGoToArtist,
                                    )
                                }
                            },
                        )
                    }

                    SeekBar(
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeek = onSeek,
                        modifier = Modifier.padding(top = 18.dp).graphicsLayer { alpha = panelAlpha() },
                    )

                    val playerIconColor = PlayerColors.TextPrimary
                    TransportControls(
                        isPlaying = isPlaying,
                        onSkipPrevious = onSkipPrevious,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 12.dp),
                        prevModifier = Modifier.graphicsLayer { alpha = panelAlpha() },
                        // Play and next fly over from the mini player's buttons.
                        playModifier = Modifier.flyFrom(mini?.play, expand, fromCenter = true).graphicsLayer {
                            val grow = growFromNothing()
                            alpha = grow
                            scaleX = grow
                            scaleY = grow
                        },
                        nextModifier = Modifier.flyFrom(mini?.next, expand, fromCenter = true).graphicsLayer {
                            val grow = growFromNothing()
                            alpha = grow
                            scaleX = grow
                            scaleY = grow
                        },
                        // In the mini player's colors at first (dark on a light theme), turning
                        // into the player's own as they fly up — and back when closing.
                        iconTint = {
                            val t = (expand() / TEXT_HANDOFF).coerceIn(0f, 1f)
                            androidx.compose.ui.graphics.lerp(miniIconColor, playerIconColor, t)
                        },
                    )

                    VolumeRow(modifier = Modifier.padding(top = 28.dp).graphicsLayer { alpha = panelAlpha() })

                    BottomQuickActionsRow(
                        // ModulateAlpha: an offscreen fade would clip the little modes badge that
                        // sticks out above the queue button.
                        modifier = Modifier.padding(top = 30.dp).graphicsLayer {
                            alpha = panelAlpha()
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                        },
                        lyricsActive = centerMode == CenterMode.Lyrics,
                        queueActive = centerMode == CenterMode.Queue,
                        shuffleEnabled = shuffleEnabled,
                        repeatEnabled = repeatEnabled,
                        infinitePlayEnabled = infinitePlayEnabled,
                        onLyricsClick = {
                            switchMode(if (centerMode == CenterMode.Lyrics) CenterMode.Art else CenterMode.Lyrics)
                        },
                        onDeviceClick = { openOutputSwitcher(context) },
                        onQueueClick = {
                            switchMode(if (centerMode == CenterMode.Queue) CenterMode.Art else CenterMode.Queue)
                        },
                    )
                }
            }
            }
        }

        // The cover on its way between the big view and the header's thumbnail (either way).
        val big = finalArtBounds
        val thumb = headerArtBounds
        if (modeFlight < 1f && big != null && thumb != null && playerActive) {
            val headerCornerPx = with(LocalDensity.current) { 10.dp.toPx() }
            AlbumArt(
                uri = song?.uri,
                size = ART_SIZE_FULL,
                modifier = Modifier
                    .offset { IntOffset(big.left.roundToInt(), big.top.roundToInt()) }
                    .size(with(LocalDensity.current) { big.width.toDp() }, with(LocalDensity.current) { big.height.toDp() })
                    .graphicsLayer {
                        val p = FLIGHT_EASING.transform(modeFlight)
                        // The big cover as it actually sits (a touch smaller while paused).
                        val bigW = big.width * artScale
                        val bigH = big.height * artScale
                        val bigRect = Rect(big.center.x - bigW / 2, big.center.y - bigH / 2, big.center.x + bigW / 2, big.center.y + bigH / 2)
                        val from = if (modeFlightFromArt) bigRect else thumb
                        val to = if (modeFlightFromArt) thumb else bigRect
                        val left = lerp(from.left, to.left, p)
                        val top = lerp(from.top, to.top, p)
                        val width = lerp(from.width, to.width, p)
                        val height = lerp(from.height, to.height, p)
                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = left - big.left
                        translationY = top - big.top
                        scaleX = width / big.width
                        scaleY = height / big.height
                        val fromCorner = if (modeFlightFromArt) artCornerPx else headerCornerPx
                        val toCorner = if (modeFlightFromArt) headerCornerPx else artCornerPx
                        shape = RoundedCornerShape(lerp(fromCorner, toCorner, p) * big.width / width)
                        clip = true
                    },
            )
        }

        // A look-alike of the mini player's text — same size, weight and colors — riding along at
        // first and fading out as the player's own title fades in, so grabbing the bar doesn't
        // change how its text looks.
        val miniText = mini?.text?.takeIf { playerActive }
        // How much bigger the text it hands over to is: the Art view's title, or the header's.
        val handoffScale = if (centerMode == CenterMode.Art) 20f / 13f else 17f / 13f
        if (miniText != null) {
            Column(
                modifier = Modifier
                    .offset { IntOffset(miniText.left.roundToInt(), miniText.top.roundToInt()) }
                    .width(with(LocalDensity.current) { miniText.width.toDp() })
                    .graphicsLayer {
                        alpha = (1f - expand() / TEXT_HANDOFF).coerceIn(0f, 1f)
                        val p = FLIGHT_EASING.transform(expand())
                        val to = titleTarget
                        if (to != null) {
                            transformOrigin = TransformOrigin(0f, 0f)
                            translationX = (to.left - miniText.left) * p
                            translationY = (to.top - miniText.top) * p
                            val scale = lerp(1f, handoffScale, p)
                            scaleX = scale
                            scaleY = scale
                        }
                    },
            ) {
                Text(
                    text = song?.title ?: stringResource(R.string.nothing_playing),
                    color = miniTitleColor,
                    fontSize = 13.sp,
                    lineHeight = 15.6.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song?.artist ?: "",
                    color = miniArtistColor,
                    fontSize = 11.sp,
                    lineHeight = 15.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (showAddToQueuePicker) {
            AddToQueuePicker(
                songs = allSongs,
                onAdd = onAddToQueue,
                onDismiss = { showAddToQueuePicker = false },
            )
        }
    }
}

/** How far (of the width) lyrics and the queue slide when switching between them. */
private const val SIDE_SHIFT = 0.22f

/** How high the settled cover seems to float over a blurred background. */
private val COVER_SHADOW = 14.dp
private const val COVER_SHADOW_FADE_MS = 450

/** Cover <-> lyrics/queue flight: the same soft spring as opening the player. */
private val MODE_FLIGHT_SPRING = androidx.compose.animation.core.spring<Float>(dampingRatio = 1f, stiffness = 320f, visibilityThreshold = 0.0005f)

/** Lyrics/queue coming in as the cover flies off: they fade and rise into place over the second
 * half of the flight, once the cover is out of their way. */
private fun Modifier.modeContentIn(progress: () -> Float): Modifier = graphicsLayer {
    val t = ((progress() - 0.35f) / 0.65f).coerceIn(0f, 1f)
    alpha = t
    translationY = (1f - androidx.compose.animation.core.LinearOutSlowInEasing.transform(t)) * 24.dp.toPx()
}

/** How the flying pieces (cover, text, buttons) move against the opening — evenly, in step with
 * the sheet's edge. (Easing them on their own made them run ahead of it.) */
internal val FLIGHT_EASING: Easing = LinearEasing

/** The mini player's title (13sp) against the big player's (20sp). */
private const val MINI_TITLE_SCALE = 13f / 20f

/** How far into the opening the mini player's text has handed over to the player's own. */
private const val TEXT_HANDOFF = 0.15f
