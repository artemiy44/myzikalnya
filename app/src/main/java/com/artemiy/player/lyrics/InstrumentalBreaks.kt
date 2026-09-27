package com.artemiy.player.lyrics

/*
 * Where nothing is sung, the lyrics show three dots filling up until the next line, instead of
 * the last line just hanging there lit (or an empty line, which is how some files mark a break).
 */

/** A long enough intro before the first line gets dots right from the start. */
private const val INTRO_MIN_MS = 8_000L

/** Silence between one line being done and the next starting that counts as a break. */
private const val GAP_MIN_MS = 6_000L

/** Blank "break" lines shorter than this are just dropped — dots flashing by would only distract. */
private const val SHORTEST_BREAK_MS = 2_000L

/** When a line's last word doesn't say how long it's held, it's taken as held this long. */
private const val HELD_LAST_WORD_MS = 2_000L

/**
 * Plain LRC doesn't say how long a line is sung at all, so dots never start sooner than this after
 * the line began (unless it looks even longer) — and then only if there's still [LRC_MIN_DOTS_MS]
 * left before the next line, not just a flash.
 */
private const val LRC_MIN_LINE_MS = 9_000L
private const val LRC_MIN_DOTS_MS = 3_000L

/**
 * [lines] with instrumental breaks added as their own "lines" (see [LyricLine.instrumentalUntilMs]):
 * - lines with no text at all (how some files mark a break) become one;
 * - an intro of [INTRO_MIN_MS] or more before the first line gets one;
 * - [GAP_MIN_MS] or more of nothing between a line being done and the next one gets one.
 */
fun withInstrumentalBreaks(lines: List<LyricLine>): List<LyricLine> {
    if (lines.isEmpty()) return lines
    val out = ArrayList<LyricLine>(lines.size + 8)
    val first = lines.first()
    if (!first.isBlankLine() && first.timeMs >= INTRO_MIN_MS) {
        out += LyricLine(timeMs = 0L, text = "", voice = first.voice, instrumentalUntilMs = first.timeMs)
    }
    lines.forEachIndexed { i, line ->
        val next = lines.getOrNull(i + 1)
        if (line.isBlankLine()) {
            val until = next?.timeMs ?: line.endTimeMs ?: return@forEachIndexed
            if (until - line.timeMs >= SHORTEST_BREAK_MS) {
                out += line.copy(text = "", words = null, wordReadings = null, ruby = null, instrumentalUntilMs = until)
            }
            return@forEachIndexed
        }
        out += line
        if (next == null || next.isBlankLine()) return@forEachIndexed
        val done = line.estimatedEndMs(next.timeMs)
        // Timed words say when the line is really done; for plain LRC [done] already includes a
        // generous wait, so a shorter stretch after it is enough.
        val minGap = if (line.words.isNullOrEmpty() && line.endTimeMs == null) LRC_MIN_DOTS_MS else GAP_MIN_MS
        if (next.timeMs - done >= minGap) {
            out += LyricLine(timeMs = done, text = "", voice = line.voice, instrumentalUntilMs = next.timeMs)
        }
    }
    return out
}

private fun LyricLine.isBlankLine(): Boolean = text.isBlank() && secondary.isEmpty() && background == null

/**
 * When this line is actually done being sung. The file says so when its last word has an end
 * time before the next line; many files instead stretch the last word right up to the next line,
 * so then it's taken as held [HELD_LAST_WORD_MS]. Plain LRC lines have no word timing at all —
 * taken as lasting at least [LRC_MIN_LINE_MS], longer if their length suggests so.
 */
private fun LyricLine.estimatedEndMs(nextStartMs: Long): Long {
    val end = endTimeMs
    if (end != null && end < nextStartMs - 200) return end
    val words = words
    if (!words.isNullOrEmpty()) {
        val last = words.last()
        return if (last.text.isBlank()) last.timeMs else minOf(nextStartMs, last.timeMs + HELD_LAST_WORD_MS)
    }
    return minOf(nextStartMs, timeMs + maxOf(LRC_MIN_LINE_MS, singingGuessMs(text)))
}

/**
 * Roughly how long a line takes to sing, from its text: a Japanese/Chinese/Korean character is a
 * whole syllable (~0.3s), a Latin/Cyrillic letter a fraction of one.
 */
private fun singingGuessMs(text: String): Long {
    var ms = 800L
    for (c in text) {
        if (c.isWhitespace()) continue
        ms += when (Character.UnicodeScript.of(c.code)) {
            Character.UnicodeScript.HAN, Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA, Character.UnicodeScript.HANGUL -> 300L
            else -> 75L
        }
    }
    return ms
}
