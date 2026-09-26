package com.artemiy.player.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The two icon sets the user can switch between in Settings → Внешний вид. */
enum class IconSet(val label: String) {
    /** Soft, rounded — close to GNOME/GTK4. */
    LUCIDE("Lucide"),

    /** Thinner and crisper — closer to Apple Music. */
    TABLER("Tabler"),
}

val LocalIconSet = staticCompositionLocalOf { IconSet.LUCIDE }

/**
 * Every icon the app draws, by what it means rather than by set — the current [IconSet] decides
 * which drawing you get. Main player controls (play, pause, skips) are the filled versions in both
 * sets; everything else is outline.
 */
object AppIcons {
    val Play: ImageVector @Composable @ReadOnlyComposable get() = pick(LucidePlay, TablerPlay)
    val Pause: ImageVector @Composable @ReadOnlyComposable get() = pick(LucidePause, TablerPause)
    val SkipNext: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideSkipNext, TablerSkipNext)
    val SkipPrevious: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideSkipPrevious, TablerSkipPrevious)
    val Add: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideAdd, TablerAdd)
    val Shuffle: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideShuffle, TablerShuffle)
    val Repeat: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideRepeat, TablerRepeat)
    val Infinite: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideInfinite, TablerInfinite)
    val Search: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideSearch, TablerSearch)
    val Playlist: ImageVector @Composable @ReadOnlyComposable get() = pick(LucidePlaylist, TablerPlaylist)
    val Queue: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideQueue, TablerQueue)
    val AddToQueue: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideAddToQueue, TablerAddToQueue)
    val PlayNext: ImageVector @Composable @ReadOnlyComposable get() = pick(LucidePlayNext, TablerPlayNext)
    val AddToPlaylist: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideAddToPlaylist, TablerAddToPlaylist)
    val TextSize: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideTextSize, TablerTextSize)
    val Artist: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideArtist, TablerArtist)
    val MoreVertical: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideMoreVertical, TablerMoreVertical)
    val More: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideMore, TablerMore)
    val Info: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideInfo, TablerInfo)
    val Library: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideLibrary, TablerLibrary)
    val ViewGrid: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideViewGrid, TablerViewGrid)
    val ViewGridDense: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideViewGridDense, TablerViewGridDense)
    val ViewList: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideViewList, TablerViewList)
    val Equalizer: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideEqualizer, TablerEqualizer)
    val Close: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideClose, TablerClose)
    val Back: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideBack, TablerBack)
    val Forward: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideForward, TablerForward)
    val Album: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideAlbum, TablerAlbum)
    val Mood: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideMood, TablerMood)
    val VolumeUp: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideVolumeUp, TablerVolumeUp)
    val VolumeDown: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideVolumeDown, TablerVolumeDown)
    val General: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideGeneral, TablerGeneral)
    val Romanization: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideRomanization, TablerRomanization)
    val Tap: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideTap, TablerTap)
    val Sort: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideSort, TablerSort)
    val Share: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideShare, TablerShare)
    val Settings: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideSettings, TablerSettings)
    val RemoveFromPlaylist: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideRemoveFromPlaylist, TablerRemoveFromPlaylist)
    val Refresh: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideRefresh, TablerRefresh)
    val Player: ImageVector @Composable @ReadOnlyComposable get() = pick(LucidePlayer, TablerPlayer)
    val Songs: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideSongs, TablerSongs)
    val Home: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideHome, TablerHome)
    val Lyrics: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideLyrics, TablerLyrics)
    val LyricsFilled: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideLyricsFilled, TablerLyricsFilled)
    val MoodSettings: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideMoodSettings, TablerMoodSettings)
    val Edit: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideEdit, TablerEdit)
    val DragHandle: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideDragHandle, TablerDragHandle)
    val Delete: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideDelete, TablerDelete)
    val ChevronRight: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideChevronRight, TablerChevronRight)
    val Check: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideCheck, TablerCheck)
    val Cast: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideCast, TablerCast)
    val Background: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideBackground, TablerBackground)

    // Selected bottom tabs: filled where the drawing allows it, just bolder where it's only lines.
    val HomeFilled: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideHomeFilled, TablerHomeFilled)
    val LibraryFilled: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideLibraryFilled, TablerLibraryFilled)
    val SearchFilled: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideSearchFilled, TablerSearchFilled)
    val MoodBold: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideMoodBold, TablerMoodBold)

    // Where the sound is going (Now Playing's device button).
    val DevicePhone: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideDevicePhone, TablerDevicePhone)
    val DeviceHeadphones: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideDeviceHeadphones, TablerDeviceHeadphones)
    val DeviceSpeaker: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideDeviceSpeaker, TablerDeviceSpeaker)
    val DeviceBluetooth: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideDeviceBluetooth, TablerDeviceBluetooth)
    val DeviceUsb: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideDeviceUsb, TablerDeviceUsb)
    val DeviceTv: ImageVector @Composable @ReadOnlyComposable get() = pick(LucideDeviceTv, TablerDeviceTv)
}

@Composable
@ReadOnlyComposable
private fun pick(lucide: ImageVector, tabler: ImageVector): ImageVector =
    if (LocalIconSet.current == IconSet.LUCIDE) lucide else tabler

/** Lucide's drawings fill more of their 24×24 box than Tabler's, so they read a bit bigger at the
 * same size — shrunk slightly (around the center) to sit level with the rest of the UI. */
internal const val LUCIDE_SCALE = 0.97f

/** Both sets are drawn on a 24×24 grid. Colors here are placeholders — Icon() tints them. */
internal fun iconVector(name: String, scale: Float = 1f, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addGroup(scaleX = scale, scaleY = scale, pivotX = 12f, pivotY = 12f)
        .apply(block)
        .clearGroup()
        .build()

internal fun ImageVector.Builder.path(pathData: String, fill: Boolean, stroke: Boolean, width: Float = 2f, evenOdd: Boolean = false) {
    addPath(
        pathData = addPathNodes(pathData),
        pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
        fill = if (fill) SolidColor(Color.Black) else null,
        stroke = if (stroke) SolidColor(Color.Black) else null,
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )
}
