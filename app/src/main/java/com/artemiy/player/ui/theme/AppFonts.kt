@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.artemiy.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.artemiy.player.R

/**
 * The typeface the whole app is written in, picked in Settings → Оформление. All bundled ones
 * are variable fonts (every weight in one file, SIL Open Font License, credited in "О
 * приложении"); anything a font lacks — Japanese, say — Android fills in from its own fonts.
 */
enum class AppFont(private val fontName: String) {
    INTER("Inter"),
    ADWAITA_SANS("Adwaita Sans"),
    GEIST("Geist"),
    MANROPE("Manrope"),
    ONEST("Onest"),
    GOLOS_TEXT("Golos Text"),
    RUBIK("Rubik"),
    NUNITO("Nunito"),
    ROBOTO("Roboto"),
    SYSTEM(""),
    ;

    val family: FontFamily by lazy {
        when (this) {
            INTER -> variable(R.font.app_inter, 100, 900)
            ADWAITA_SANS -> variable(R.font.app_adwaita_sans, 100, 900)
            GEIST -> variable(R.font.app_geist, 100, 900)
            MANROPE -> variable(R.font.app_manrope, 200, 800)
            ONEST -> variable(R.font.app_onest, 100, 900)
            GOLOS_TEXT -> variable(R.font.app_golos_text, 400, 900)
            RUBIK -> variable(R.font.app_rubik, 300, 900)
            NUNITO -> variable(R.font.app_nunito, 200, 1000)
            ROBOTO -> variable(R.font.app_roboto, 100, 900)
            SYSTEM -> FontFamily.Default
        }
    }

    /** The font's own name; the phone's font is just "System" in the app's language. */
    val label: String
        @Composable get() = if (this == SYSTEM) androidx.compose.ui.res.stringResource(R.string.font_system) else fontName

    companion object {
        val DEFAULT = INTER
    }
}

/** One variable font file as a family with every weight the app uses, each asking the file for
 * that weight (within what the font has). */
private fun variable(res: Int, minWeight: Int, maxWeight: Int): FontFamily = FontFamily(
    (300..900 step 100).map { weight ->
        Font(
            res,
            FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.coerceIn(minWeight, maxWeight))),
        )
    },
)

/** The app font's family, for text styles built by hand (lyrics, text fields...). */
val LocalAppFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** [this] style in the app's font, unless it already names a font of its own. */
@Composable
@ReadOnlyComposable
fun TextStyle.inAppFont(): TextStyle = if (fontFamily == null) copy(fontFamily = LocalAppFontFamily.current) else this

/** Material's text styles, all in [family] — Material's Text uses these by default. */
internal fun typographyIn(family: FontFamily): Typography {
    val t = Typography()
    return Typography(
        displayLarge = t.displayLarge.copy(fontFamily = family),
        displayMedium = t.displayMedium.copy(fontFamily = family),
        displaySmall = t.displaySmall.copy(fontFamily = family),
        headlineLarge = t.headlineLarge.copy(fontFamily = family),
        headlineMedium = t.headlineMedium.copy(fontFamily = family),
        headlineSmall = t.headlineSmall.copy(fontFamily = family),
        titleLarge = t.titleLarge.copy(fontFamily = family),
        titleMedium = t.titleMedium.copy(fontFamily = family),
        titleSmall = t.titleSmall.copy(fontFamily = family),
        bodyLarge = t.bodyLarge.copy(fontFamily = family),
        bodyMedium = t.bodyMedium.copy(fontFamily = family),
        bodySmall = t.bodySmall.copy(fontFamily = family),
        labelLarge = t.labelLarge.copy(fontFamily = family),
        labelMedium = t.labelMedium.copy(fontFamily = family),
        labelSmall = t.labelSmall.copy(fontFamily = family),
    )
}
