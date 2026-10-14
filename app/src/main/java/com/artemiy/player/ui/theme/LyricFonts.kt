@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.artemiy.player.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.artemiy.player.R

/**
 * The fonts the hieroglyphs of the lyrics are set in, picked in Settings → Плеер. For each of the
 * three scripts a choice: 0 is the phone's own font, 1..3 are the ones bundled here (cut down to the
 * characters songs use; anything rarer Android fills in from its own fonts, as it always did).
 * Chinese also names a [zhBackup]: the font for the characters the chosen one doesn't have (Smiley
 * Sans, say, has only simplified ones).
 */
data class LyricFontChoice(val ja: Int = 0, val ko: Int = 0, val zh: Int = 0, val zhBackup: Int = 0) {
    val isDefault: Boolean get() = ja == 0 && ko == 0 && zh == 0
}

/** The choice and how to change it, handed down from where the settings live. */
class LyricFontSetting(val choice: LyricFontChoice, val onChange: (LyricFontChoice) -> Unit)

val LocalLyricFontSetting = staticCompositionLocalOf { LyricFontSetting(LyricFontChoice()) {} }

/** One font to pick: its name (null = the phone's own) and the family to set text in (null = leave it). */
class LyricFontOption(val name: String?, val sample: String, val family: FontFamily?)

object LyricFonts {
    private fun single(res: Int): FontFamily = FontFamily((400..900 step 100).map { Font(res, FontWeight(it)) })

    private fun variable(res: Int): FontFamily = FontFamily(
        (400..900 step 100).map { Font(res, FontWeight(it), variationSettings = FontVariation.Settings(FontVariation.weight(it))) },
    )

    private val zenKaku: FontFamily by lazy {
        FontFamily(
            listOf(400, 500, 600).map { Font(R.font.lyric_zenkaku_medium, FontWeight(it)) } +
                listOf(700, 800, 900).map { Font(R.font.lyric_zenkaku_black, FontWeight(it)) },
        )
    }
    private val murecho by lazy { variable(R.font.lyric_murecho) }
    private val dela by lazy { single(R.font.lyric_dela) }
    private val jua by lazy { single(R.font.lyric_jua) }
    private val doHyeon by lazy { single(R.font.lyric_dohyeon) }
    private val blackHan by lazy { single(R.font.lyric_blackhan) }
    private val neoXiHei by lazy { single(R.font.lyric_neoxihei) }
    private val smiley by lazy { single(R.font.lyric_smiley) }
    private val wenKai by lazy { single(R.font.lyric_wenkai) }

    val ja: List<LyricFontOption> by lazy {
        listOf(
            LyricFontOption(null, "夜明け", null),
            LyricFontOption("Zen Kaku Gothic New", "夜明け", zenKaku),
            LyricFontOption("Murecho", "夜明け", murecho),
            LyricFontOption("Dela Gothic One", "夜明け", dela),
        )
    }
    val ko: List<LyricFontOption> by lazy {
        listOf(
            LyricFontOption(null, "새벽", null),
            LyricFontOption("Jua", "새벽", jua),
            LyricFontOption("Do Hyeon", "새벽", doHyeon),
            LyricFontOption("Black Han Sans", "새벽", blackHan),
        )
    }
    val zh: List<LyricFontOption> by lazy {
        listOf(
            LyricFontOption(null, "黎明", null),
            LyricFontOption("Lumine Sans (LXGW Neo XiHei)", "黎明", neoXiHei),
            LyricFontOption("Smiley Sans", "黎明", smiley),
            LyricFontOption("LXGW WenKai TC", "黎明", wenKai),
        )
    }
    /** The font behind Smiley Sans for what it doesn't have: the system's own, or one that has all of them. */
    val zhBackup: List<LyricFontOption> by lazy {
        listOf(
            LyricFontOption(null, "黎明", null),
            LyricFontOption("Lumine Sans (LXGW Neo XiHei)", "黎明", neoXiHei),
            LyricFontOption("LXGW WenKai TC", "黎明", wenKai),
        )
    }

    /**
     * Whether the phone's own font draws hieroglyphs in a bold weight at all (some, like a bare Android build, only
     * have the regular one, and then every bold title in Japanese looks thin). Found by drawing a few characters
     * at a regular and at a very bold weight and seeing if they differ. Looked up once.
     */
    val systemHasBoldCjk: Boolean by lazy {
        runCatching {
            fun render(weight: Int): android.graphics.Bitmap {
                val bmp = android.graphics.Bitmap.createBitmap(160, 60, android.graphics.Bitmap.Config.ALPHA_8)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = 40f
                    typeface = if (android.os.Build.VERSION.SDK_INT >= 28) android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, weight, false)
                    else android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, if (weight >= 600) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                }
                android.graphics.Canvas(bmp).drawText("日あ한", 4f, 44f, paint)
                return bmp
            }
            val a = render(400)
            val b = render(900)
            val pa = IntArray(160 * 60)
            val pb = IntArray(160 * 60)
            a.getPixels(pa, 0, 160, 0, 0, 160, 60)
            b.getPixels(pb, 0, 160, 0, 0, 160, 60)
            !pa.contentEquals(pb)
        }.getOrDefault(true)
    }

    /** Index of the Chinese choice that only knows simplified characters (and so needs a backup). */
    const val ZH_SMILEY = 2

    /** The characters Smiley Sans has (ranges, high to low pairs), read once from assets. */
    @Volatile private var smileyRanges: IntArray? = null

    fun smileyCoverage(context: Context): IntArray = smileyRanges ?: synchronized(this) {
        smileyRanges ?: run {
            val out = ArrayList<Int>()
            context.assets.open("fontcov/smiley.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val (a, b) = line.trim().split('-').takeIf { it.size == 2 } ?: return@forEach
                    out.add(a.toInt(16)); out.add(b.toInt(16))
                }
            }
            out.toIntArray().also { smileyRanges = it }
        }
    }
}

/** Sets text in the chosen fonts, a script at a time. */
class LyricTextFonts(private val choice: LyricFontChoice, private val smiley: IntArray?) {
    private val jaFamily = LyricFonts.ja.getOrNull(choice.ja)?.family
    private val koFamily = LyricFonts.ko.getOrNull(choice.ko)?.family
    private val zhFamily = LyricFonts.zh.getOrNull(choice.zh)?.family
    private val zhBackupFamily = LyricFonts.zhBackup.getOrNull(choice.zhBackup)?.family

    private fun covered(cp: Int): Boolean {
        val r = smiley ?: return true
        var lo = 0
        var hi = r.size / 2 - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            when {
                cp < r[mid * 2] -> hi = mid - 1
                cp > r[mid * 2 + 1] -> lo = mid + 1
                else -> return true
            }
        }
        return false
    }

    /**
     * [text] with its hieroglyphs in the chosen fonts. [hint] is the whole line it belongs to: Chinese
     * characters are Japanese ones if the line has kana, Korean if it has hangul, else Chinese.
     */
    fun annotate(text: String, hint: String = text): AnnotatedString {
        if (choice.isDefault) return AnnotatedString(text)
        var kana = false
        var hangul = false
        var cjk = false
        for (ch in hint) {
            when {
                ch in '぀'..'ヿ' || ch in 'ㇰ'..'ㇿ' -> kana = true
                ch in '가'..'힣' || ch in 'ᄀ'..'ᇿ' || ch in '㄰'..'㆏' -> hangul = true
                ch >= '⺀' -> cjk = true
            }
        }
        if (!kana && !hangul && !cjk) return AnnotatedString(text)
        // The family for a character the line's own script would set (Han and CJK punctuation).
        val lineFamily = when {
            kana -> jaFamily
            hangul -> koFamily
            else -> zhFamily
        }
        val chinese = !kana && !hangul
        return buildAnnotatedString {
            append(text)
            var runStart = 0
            var runFamily: FontFamily? = null
            var i = 0
            while (i < text.length) {
                val cp = text.codePointAt(i)
                val width = Character.charCount(cp)
                val family: FontFamily? = when {
                    cp in 0x3040..0x30FF || cp in 0x31F0..0x31FF -> jaFamily
                    cp in 0xAC00..0xD7A3 || cp in 0x1100..0x11FF || cp in 0x3130..0x318F -> koFamily
                    cp >= 0x2E80 && cp != 0xFE0F -> {
                        // A Chinese line set in a font that lacks the character: the backup, or the system's own.
                        if (chinese && choice.zh == LyricFonts.ZH_SMILEY && !covered(cp)) zhBackupFamily else lineFamily
                    }
                    else -> null
                }
                if (family !== runFamily) {
                    if (runFamily != null) addStyle(SpanStyle(fontFamily = runFamily), runStart, i)
                    runStart = i
                    runFamily = family
                }
                i += width
            }
            if (runFamily != null) addStyle(SpanStyle(fontFamily = runFamily), runStart, text.length)
        }
    }
}

/** The fonts for the lyrics in the current setting. */
@Composable
fun rememberLyricTextFonts(): LyricTextFonts {
    val choice = LocalLyricFontSetting.current.choice
    val context = LocalContext.current
    return remember(choice) {
        LyricTextFonts(choice, if (choice.zh == LyricFonts.ZH_SMILEY) LyricFonts.smileyCoverage(context) else null)
    }
}

/** [text] with its hieroglyphs in the chosen fonts; [hint] is the whole line it is part of. */
@Composable
fun lyricAnnotated(text: String, hint: String = text): AnnotatedString {
    val fonts = rememberLyricTextFonts()
    return remember(fonts, text, hint) { fonts.annotate(text, hint) }
}
