package com.artemiy.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.artemiy.player.data.SettingsRepository

private val PlayerDarkColors = darkColorScheme(
    background = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    surface = androidx.compose.ui.graphics.Color(0xFF232325),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2C2C2E),
    onBackground = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    primary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),
    // Menus/popups use the surfaceContainer family; left unset they fall back to Material's own
    // blue-violet tint instead of our neutral grays — one step lighter than the background here.
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF232325),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF2C2C2E),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF2C2C2E),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF333336),
)

/** App-wide text scale on top of the system font-scale setting. Adjustable from Settings. */
@Composable
fun PlayerTheme(appTextScale: Float = SettingsRepository.DEFAULT_FONT_SCALE, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val scaledDensity = Density(
        density = density.density,
        fontScale = density.fontScale * appTextScale,
    )
    MaterialTheme(colorScheme = PlayerDarkColors) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            content()
        }
    }
}
