package com.artemiy.player.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.artemiy.player.R

/**
 * The typeface of the "Диск" player (Settings → Player): the app's own by default, or the pixel font DotGothic16 (SIL OFL, cut down
 * to its Latin, Cyrillic and kana). Glyphs it lacks come from the usual font. The lyrics are never set in it.
 */
enum class DiscFont(val label: String, private val resource: Int?) {
    STANDARD("", null),
    DOTGOTHIC("DotGothic16", R.font.disc_dotgothic16);

    /** One file stands for every weight (these fonts have one), so no faux-bold is smeared over it. */
    val family: FontFamily? = resource?.let { res ->
        FontFamily(
            Font(res, FontWeight.Normal),
            Font(res, FontWeight.Medium),
            Font(res, FontWeight.SemiBold),
            Font(res, FontWeight.Bold),
            Font(res, FontWeight.ExtraBold),
            Font(res, FontWeight.Black),
        )
    }
}

/** The Dial player's typeface chosen in Settings; null = the app's usual one. */
val LocalDiscFont = compositionLocalOf<FontFamily?> { null }
