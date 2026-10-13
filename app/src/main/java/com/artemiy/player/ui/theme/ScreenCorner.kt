package com.artemiy.player.ui.theme

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The round corners of the phone's own screen, for the Dial player to follow (its card and the
 * outer tiles keep their corners even with the screen's). Android 12+ tells the app the radius;
 * a phone that doesn't (or an older Android) gets a slider in Settings instead — [manualDp].
 */
class ScreenCornerSetting(val manualDp: Int, val onChange: (Int) -> Unit)

val LocalScreenCornerSetting = staticCompositionLocalOf { ScreenCornerSetting(0) {} }

/** The screen's corner radius as the system reports it, or null when it doesn't (no data, or a square screen). */
@Composable
fun systemScreenCornerDp(): Dp? {
    if (Build.VERSION.SDK_INT < 31) return null
    val view = LocalView.current
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    var radiusPx by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(view, config) {
        // The insets only exist once the window is attached: try again a moment later if not yet.
        repeat(4) {
            val insets = view.rootWindowInsets
            if (insets != null) {
                radiusPx = listOf(
                    RoundedCorner.POSITION_TOP_LEFT, RoundedCorner.POSITION_TOP_RIGHT,
                    RoundedCorner.POSITION_BOTTOM_LEFT, RoundedCorner.POSITION_BOTTOM_RIGHT,
                ).mapNotNull { insets.getRoundedCorner(it)?.radius }.maxOrNull()?.takeIf { it > 0 }
                return@LaunchedEffect
            }
            delay(300)
        }
    }
    return radiusPx?.let { with(density) { it.toDp() } }
}

/** The corner radius to follow: the system's if it tells, else what the slider says (0 = none, the usual look). */
@Composable
fun screenCornerDp(): Dp = systemScreenCornerDp() ?: LocalScreenCornerSetting.current.manualDp.dp
