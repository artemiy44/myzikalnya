package com.artemiy.player.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.artemiy.player.data.SettingsRepository

/** Material's own components (menus, switches, text cursors) colored from our palette, so they
 * don't fall back to Material's default blue-violet tints. */
fun colorSchemeFor(palette: PlayerPalette): ColorScheme {
    val base = if (palette.isLight) lightColorScheme() else darkColorScheme()
    return base.copy(
        background = palette.background,
        surface = palette.surfaceDim,
        surfaceVariant = palette.surface,
        onBackground = palette.textPrimary,
        onSurface = palette.textPrimary,
        onSurfaceVariant = palette.textSecondary,
        primary = palette.accent,
        onPrimary = palette.onAccent,
        outline = palette.border,
        outlineVariant = palette.border,
        surfaceContainerLowest = palette.background,
        surfaceContainerLow = palette.surfaceDim,
        surfaceContainer = palette.menu,
        surfaceContainerHigh = palette.menu,
        surfaceContainerHighest = palette.surface,
    )
}

/** Provides [palette] to everything inside — both our [PlayerColors] and Material's components. */
@Composable
fun PaletteScope(palette: PlayerPalette, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPlayerPalette provides palette) {
        MaterialTheme(colorScheme = colorSchemeFor(palette), content = content)
    }
}

/** App-wide text scale on top of the system font-scale setting. Adjustable from Settings. */
@Composable
fun PlayerTheme(
    palette: PlayerPalette,
    appTextScale: Float = SettingsRepository.DEFAULT_FONT_SCALE,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val scaledDensity = Density(
        density = density.density,
        fontScale = density.fontScale * appTextScale,
    )
    PaletteScope(palette) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            content()
        }
    }
}
