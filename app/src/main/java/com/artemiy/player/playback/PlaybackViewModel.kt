package com.artemiy.player.playback

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.artemiy.player.data.AppDatabase
import com.artemiy.player.data.InfinitePlayMode
import com.artemiy.player.data.PlayHistoryEntity
import com.artemiy.player.data.SettingsRepository
import com.artemiy.player.data.SkipEventEntity
import com.artemiy.player.data.Song
import com.artemiy.player.lyrics.LyricsExtractor
import com.artemiy.player.lyrics.LyricsRomanizer
import com.artemiy.player.lyrics.ParsedLyrics
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PlaybackViewModel"
private const val INFINITE_PLAY_TOPUP_THRESHOLD = 5
private const val INFINITE_PLAY_BATCH_SIZE = 20
private const val SKIP_WINDOW_MS = 30_000L

class PlaybackViewModel(app: Application) : AndroidViewModel(app) {

    private val playHistoryDao = AppDatabase.get(app).playHistoryDao()
    private val settingsRepository = SettingsRepository(app)
    private var infinitePlayMode = InfinitePlayMode.RANDOM

    private var controller: MediaController? = null
    private var songsById: Map<Long, Song> = emptyMap()
    private var pendingQueue: List<Song>? = null
    private var pendingStartIndex: Int = 0
    /** Song IDs inserted via [addToQueue], in play order, not yet reached — the "Queue" section.
     * Everything else upcoming in the controller's own playlist is the "Continue Playing"
     * section; see [refreshDerivedQueues]. */
    private val manualQueueIds = mutableListOf<Long>()
    /** The whole library, so infinite-play has something to draw more songs from — kept up to
     * date by [setLibrary], called whenever MainActivity's own library scan changes. */
    private var library: List<Song> = emptyList()

    var currentSong by mutableStateOf<Song?>(null)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var positionMs by mutableStateOf(0L)
        private set

    var durationMs by mutableStateOf(0L)
        private set

    /** Upcoming songs explicitly queued via "Играть следующим", in order. */
    var manualQueue by mutableStateOf<List<Song>>(emptyList())
        private set

    /** The rest of whatever was originally playing, continuing after the manual queue. */
    var continueQueue by mutableStateOf<List<Song>>(emptyList())
        private set

    var shuffleEnabled by mutableStateOf(false)
        private set

    var repeatEnabled by mutableStateOf(false)
        private set

    /** When the queue naturally runs out, keep going by shuffling in more of the library instead
     * of just stopping — meant for "I tapped one song from search, don't leave me in silence." */
    var infinitePlayEnabled by mutableStateOf(false)
        private set

    var lyrics by mutableStateOf<ParsedLyrics?>(null)
        private set

    private var lyricsJob: Job? = null

    private fun loadLyricsFor(song: Song?) {
        lyricsJob?.cancel()
        lyrics = null
        if (song == null) return
        lyricsJob = viewModelScope.launch {
            // MetadataRetriever spins up its own decoder pipeline to read tags. Give the
            // main playback decoder a moment to settle first so the two don't race for
            // codec resources (this was observed to silently kill active playback).
            delay(1200)
            val start = System.currentTimeMillis()
            Log.d(TAG, "Loading lyrics for ${song.title}")
            val result = runCatching { LyricsExtractor.extract(getApplication(), song.uri) }
                .onFailure { Log.w(TAG, "Lyrics extraction crashed for ${song.title}", it) }
                .getOrNull()
            Log.d(TAG, "Lyrics for ${song.title}: ${result?.let { it::class.simpleName } ?: "none"} (${System.currentTimeMillis() - start}ms)")
            lyrics = result
            // Shown right away without romanization, then swapped for the romanized version — the
            // first Japanese song has to load Kuromoji's dictionary, which takes a moment.
            if (result != null) {
                val romanized = withContext(Dispatchers.Default) {
                    runCatching { LyricsRomanizer.romanize(result) }
                        .onFailure { Log.w(TAG, "Romanization failed for ${song.title}", it) }
                        .getOrNull()
                }
                if (romanized != null && romanized !== result) lyrics = romanized
            }
        }
    }

    init {
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, sessionToken).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            isPlaying = c.isPlaying
            shuffleEnabled = c.shuffleModeEnabled
            repeatEnabled = c.repeatMode != Player.REPEAT_MODE_OFF
            // The session/service can outlive this ViewModel (e.g. the task was swiped away and
            // the app reopened) — in that case we're attaching to a session that's already mid-
            // playback, and no `onMediaItemTransition` will ever fire for the item that's already
            // current. Without this, the UI is stuck showing "nothing playing" despite audio
            // actually running. Hydrate straight from the controller's live state instead of
            // waiting for a transition that already happened before we existed.
            c.currentMediaItem?.let { item ->
                val id = item.mediaId.toLongOrNull()
                currentSong = id?.let { songsById[it] } ?: item.toSongOrNull()
                positionMs = c.currentPosition.coerceAtLeast(0)
                durationMs = c.duration.let { if (it > 0) it else 0L }
                loadLyricsFor(currentSong)
            }
            refreshDerivedQueues()
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    Log.d(TAG, "isPlaying -> $playing (playbackState=${c.playbackState})")
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    Log.d(TAG, "playbackState -> $playbackState")
                    if (playbackState == Player.STATE_ENDED && infinitePlayEnabled) {
                        appendInfinitePlayBatch()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e(TAG, "Player error: ${error.errorCodeName}", error)
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // Switched away by hand (next, or picking another song) within the first
                    // seconds: a skip — "not this one now". A song that just ended isn't one.
                    val previous = currentSong
                    if (previous != null && reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK && positionMs in 1..SKIP_WINDOW_MS) {
                        logSkip(previous.id)
                    }
                    durationMs = 0L
                    positionMs = 0L
                    val id = mediaItem?.mediaId?.toLongOrNull()
                    currentSong = id?.let { songsById[it] } ?: mediaItem?.toSongOrNull()
                    if (manualQueueIds.firstOrNull() == id) manualQueueIds.removeAt(0)
                    refreshDerivedQueues()
                    Log.d(TAG, "Transition (reason=$reason) -> ${currentSong?.title}")
                    loadLyricsFor(currentSong)
                    if (id != null) logPlay(id)
                }
            })
            pendingQueue?.let { queue -> startQueue(queue, pendingStartIndex) }
        }, MoreExecutors.directExecutor())

        viewModelScope.launch {
            while (true) {
                controller?.let {
                    positionMs = it.currentPosition.coerceAtLeast(0)
                    val d = it.duration
                    durationMs = if (d > 0) d else 0L
                }
                delay(100)
            }
        }

        viewModelScope.launch {
            settingsRepository.infinitePlayMode.collect { infinitePlayMode = it }
        }
    }

    fun play(song: Song, playlist: List<Song> = listOf(song)) {
        songsById = songsById + playlist.associateBy { it.id }
        val index = playlist.indexOfFirst { it.id == song.id }.let { if (it >= 0) it else 0 }
        if (controller == null) {
            pendingQueue = playlist
            pendingStartIndex = index
        } else {
            startQueue(playlist, index)
        }
    }

    /** The library scan result, kept around purely so infinite-play has something to shuffle in
     * once the current queue runs out. */
    fun setLibrary(songs: List<Song>) {
        library = songs
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    /** The "continue playing" order from right before the most recent shuffle, so turning
     * shuffle back off restores it instead of just leaving the shuffled arrangement in place. */
    private var preShuffleOrder: List<MediaItem>? = null

    fun toggleShuffle() {
        controller ?: return
        shuffleEnabled = !shuffleEnabled
        // Not using ExoPlayer's own shuffleModeEnabled: its shuffle order is generated once per
        // timeline, so toggling it off then on again just replays that same order instead of a
        // fresh one. Reordering the upcoming items ourselves guarantees a new shuffle every time.
        if (shuffleEnabled) reshuffleContinueQueue() else restorePreShuffleOrder()
        refreshDerivedQueues()
    }

    /** Physically reorders the "continue playing" portion of the controller's timeline (after
     * the current song and any manually-queued ones, which shuffle must never touch), first
     * saving that order so it can be restored later. */
    private fun reshuffleContinueQueue() {
        val c = controller ?: return
        val start = c.currentMediaItemIndex + 1 + manualQueueIds.size
        val end = c.mediaItemCount
        if (end - start < 2) return
        val original = (start until end).map { c.getMediaItemAt(it) }
        preShuffleOrder = original
        c.removeMediaItems(start, end)
        c.addMediaItems(start, original.shuffled())
    }

    /** Restores [preShuffleOrder], dropping anything that's already been played past since —
     * playback may have advanced through part of the shuffled section before shuffle got turned
     * back off, so we can't just blindly re-insert the whole original snapshot. */
    private fun restorePreShuffleOrder() {
        val c = controller ?: return
        val snapshot = preShuffleOrder ?: return
        preShuffleOrder = null
        val start = c.currentMediaItemIndex + 1 + manualQueueIds.size
        val end = c.mediaItemCount
        if (end <= start) return
        val stillUpcoming = (start until end).map { c.getMediaItemAt(it).mediaId }.toSet()
        val restored = snapshot.filter { it.mediaId in stillUpcoming }
        if (restored.isEmpty()) return
        c.removeMediaItems(start, end)
        c.addMediaItems(start, restored)
    }

    fun toggleRepeat() {
        val c = controller ?: return
        repeatEnabled = !repeatEnabled
        c.repeatMode = if (repeatEnabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        refreshDerivedQueues()
    }

    fun toggleInfinitePlay() {
        infinitePlayEnabled = !infinitePlayEnabled
    }

    /** Puts the song at the top of "continue playing" — right under the "Queue" section, which
     * is only for songs added via [addToQueue]. */
    fun playNext(song: Song) {
        val c = controller ?: return
        songsById = songsById + (song.id to song)
        val insertAt = (c.currentMediaItemIndex + 1 + manualQueueIds.size).coerceAtMost(c.mediaItemCount)
        c.addMediaItem(insertAt, toMediaItem(song))
        refreshDerivedQueues()
    }

    /** Adds to the end of the "Queue" section (the one above "continue playing"). The same song
     * can be queued more than once on purpose. */
    fun addToQueue(song: Song) = addAllToQueue(listOf(song))

    /** Batch version of [addToQueue], e.g. a whole artist — lined up in order at the end of the
     * "Queue" section, duplicates and all. */
    fun addAllToQueue(songs: List<Song>) {
        val c = controller ?: return
        if (songs.isEmpty()) return
        songsById = songsById + songs.associateBy { it.id }
        val insertAt = (c.currentMediaItemIndex + 1 + manualQueueIds.size).coerceAtMost(c.mediaItemCount)
        c.addMediaItems(insertAt, songs.map { toMediaItem(it) })
        manualQueueIds.addAll(songs.map { it.id })
        refreshDerivedQueues()
        Log.d(TAG, "Queued ${songs.size} at $insertAt -> manual=${manualQueue.size}, continue=${continueQueue.size}")
    }

    fun clearManualQueue() {
        val c = controller ?: return
        val count = manualQueueIds.size
        if (count == 0) return
        val start = c.currentMediaItemIndex + 1
        c.removeMediaItems(start, start + count)
        manualQueueIds.clear()
        refreshDerivedQueues()
    }

    /**
     * Moves the upcoming song at [from] to [to] — both positions in the Queue screen's combined
     * list (manual section first, then "continue playing"). Only within one section: the manual
     * section's length is how the two are told apart.
     *
     * With repeat on, "upcoming" wraps past the end of the timeline back to index 0, so part of it
     * physically sits *before* the current song. Rather than assuming contiguous indices, the song
     * is reinserted in the timeline right after whatever should now play just before it — walking
     * forward from the current song then yields exactly the new order either way.
     */
    fun moveInQueue(from: Int, to: Int) {
        val c = controller ?: return
        if (from == to) return
        val upcoming = upcomingIndicesInPlayOrder(c)
        if (from !in upcoming.indices || to !in upcoming.indices) return
        val manualCount = manualQueueIds.size.coerceAtMost(upcoming.size)
        if ((from < manualCount) != (to < manualCount)) return

        val order = upcoming.toMutableList().apply { add(to, removeAt(from)) }
        val moved = upcoming[from]
        val predecessor = if (to == 0) c.currentMediaItemIndex else order[to - 1]
        val predecessorAfterRemoval = if (predecessor > moved) predecessor - 1 else predecessor
        c.moveMediaItem(moved, predecessorAfterRemoval + 1)
        if (from < manualCount) manualQueueIds.add(to, manualQueueIds.removeAt(from))
        refreshDerivedQueues()
    }

    /** Removes the upcoming song at [position] (same indexing as [moveInQueue]). */
    fun removeFromQueue(position: Int) {
        val c = controller ?: return
        val upcoming = upcomingIndicesInPlayOrder(c)
        val index = upcoming.getOrNull(position) ?: return
        if (position < manualQueueIds.size.coerceAtMost(upcoming.size)) manualQueueIds.removeAt(position)
        c.removeMediaItem(index)
        refreshDerivedQueues()
    }

    fun playFromQueue(song: Song) {
        val c = controller ?: return
        val index = (0 until c.mediaItemCount).firstOrNull { c.getMediaItemAt(it).mediaId == song.id.toString() } ?: return
        c.seekTo(index, 0L)
        c.play()
    }

    /** Re-derives [manualQueue]/[continueQueue] from the controller's actual live playlist —
     * rather than tracking our own index in parallel, which would drift out of sync with every
     * insertion — split at [manualQueueIds]'s size, since manual items are always inserted
     * contiguously right after the current one. */
    private fun refreshDerivedQueues() {
        val c = controller ?: return
        val upcoming = upcomingIndicesInPlayOrder(c).mapNotNull { i ->
            val item = c.getMediaItemAt(i)
            val id = item.mediaId.toLongOrNull()
            id?.let { songsById[it] } ?: item.toSongOrNull()
        }
        val manualCount = manualQueueIds.size.coerceAtMost(upcoming.size)
        manualQueue = upcoming.take(manualCount)
        continueQueue = upcoming.drop(manualCount)
        // Top up *before* actually running dry — waiting for STATE_ENDED is a real fallback (see
        // onPlaybackStateChanged) but relying on it alone means a gap of silence while the next
        // batch buffers in. This keeps a cushion of upcoming songs at all times instead.
        if (infinitePlayEnabled && continueQueue.size < INFINITE_PLAY_TOPUP_THRESHOLD) {
            appendInfinitePlayBatch()
        }
    }

    /** Walks `Timeline.getNextWindowIndex` (repeat-mode aware) rather than just reading
     * `currentIndex+1..` in insertion order — shuffle itself is handled by physically reordering
     * the timeline in [reshuffleContinueQueue], not by ExoPlayer's own shuffle order, so this
     * always walks in plain sequential order. Capped at mediaItemCount so REPEAT_MODE_ALL doesn't
     * wrap the list around into a second lap. */
    private fun upcomingIndicesInPlayOrder(c: Player): List<Int> {
        val timeline = c.currentTimeline
        val indices = mutableListOf<Int>()
        var index = c.currentMediaItemIndex
        while (indices.size < c.mediaItemCount) {
            index = timeline.getNextWindowIndex(index, c.repeatMode, false)
            if (index == C.INDEX_UNSET) break
            indices.add(index)
        }
        return indices
    }

    private fun appendInfinitePlayBatch() {
        val c = controller ?: return
        if (library.isEmpty()) return
        val alreadyQueued = (manualQueue + continueQueue).mapTo(mutableSetOf()) { it.id } + setOfNotNull(currentSong?.id)
        val pool = when (infinitePlayMode) {
            InfinitePlayMode.RANDOM -> library
            InfinitePlayMode.GENRE_RADIO -> {
                val genre = currentSong?.genre
                if (genre != null) library.filter { it.genre.equals(genre, ignoreCase = true) }.ifEmpty { library } else library
            }
        }
        // Avoid immediately re-queuing something that's already lined up — a small pool (e.g. a
        // narrow genre) would otherwise keep picking the same handful of songs on every top-up.
        val candidates = pool.filterNot { it.id in alreadyQueued }.ifEmpty { pool }
        val batch = candidates.shuffled().take(INFINITE_PLAY_BATCH_SIZE)
        if (batch.isEmpty()) return
        songsById = songsById + batch.associateBy { it.id }
        c.addMediaItems(batch.map(::toMediaItem))
        c.prepare()
        c.play()
        refreshDerivedQueues()
    }

    private fun startQueue(playlist: List<Song>, startIndex: Int) {
        val c = controller ?: return
        c.setMediaItems(playlist.map(::toMediaItem), startIndex, 0L)
        c.prepare()
        c.play()
        manualQueueIds.clear()
        preShuffleOrder = null
        currentSong = playlist.getOrNull(startIndex)
        refreshDerivedQueues()
        loadLyricsFor(currentSong)
        currentSong?.let { logPlay(it.id) }
        pendingQueue = null
    }

    /** Best-effort reconstruction of a [Song] from a [MediaItem]'s own embedded metadata/URI, for
     * when we reattach to a session whose queue we never built (see [init]) — a real library scan
     * would know the song's genre/folder/exact duration, but those aren't needed just to show
     * what's playing and let playback controls work. */
    private fun MediaItem.toSongOrNull(): Song? {
        val id = mediaId.toLongOrNull() ?: return null
        val uri = localConfiguration?.uri ?: return null
        return Song(
            id = id,
            title = mediaMetadata.title?.toString() ?: "Без названия",
            artist = mediaMetadata.artist?.toString() ?: "Неизвестный артист",
            album = mediaMetadata.albumTitle?.toString() ?: "",
            durationMs = 0L,
            dateAddedMs = 0L,
            uri = uri,
        )
    }

    private fun toMediaItem(song: Song): MediaItem =
        MediaItem.Builder()
            .setUri(song.uri)
            .setMediaId(song.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build()
            )
            .build()

    private fun logSkip(songId: Long) {
        viewModelScope.launch {
            playHistoryDao.insertSkip(SkipEventEntity(songId = songId, skippedAt = System.currentTimeMillis()))
        }
    }

    private fun logPlay(songId: Long) {
        viewModelScope.launch {
            playHistoryDao.insert(PlayHistoryEntity(songId = songId, playedAt = System.currentTimeMillis()))
        }
    }

    override fun onCleared() {
        controller?.release()
        controller = null
    }
}
