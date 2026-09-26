package com.artemiy.player.lyrics

private val TIME_TAG = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

/** Enhanced-LRC (a.k.a. eLRC / A2 extension) per-word tag: `<mm:ss.xx>` inline inside a line. */
private val WORD_TAG = Regex("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>")

/** Duet-style eLRC files mark each line's singer with a leading `v1:`/`v2:`. */
private val VOICE_PREFIX = Regex("^(v1|v2)\\s*:\\s*", RegexOption.IGNORE_CASE)

/** Background vocals, eLRC style: a whole physical line `[bg: <mm:ss.xx>word <mm:ss.xx>word ...]`
 * with no leading line timestamp — it belongs to the main line right above it. */
private val BACKGROUND_LINE = Regex("^\\s*\\[bg:(.*)]\\s*$", RegexOption.IGNORE_CASE)

private fun parseTimeMs(minutes: String, seconds: String, frac: String): Long {
    val fracMs = when (frac.length) {
        0 -> 0L
        1 -> frac.toLong() * 100
        2 -> frac.toLong() * 10
        else -> frac.take(3).toLong()
    }
    return minutes.toLong() * 60_000 + seconds.toLong() * 1000 + fracMs
}

private class WordTagResult(val words: List<LyricWord>, val endMs: Long?)

/**
 * Splits an enhanced-LRC line body on its inline `<mm:ss.xx>` word tags into timed words.
 * Returns null when the line has no such tags (plain LRC line). A trailing tag with no text
 * after it doesn't become an empty word — it marks the end time of the word before it, used to
 * animate that last word's karaoke sweep to completion instead of holding it forever.
 */
private fun parseWordTags(text: String): WordTagResult? {
    val tags = WORD_TAG.findAll(text).toList()
    if (tags.isEmpty()) return null
    val words = mutableListOf<LyricWord>()
    var endMs: Long? = null
    for (i in tags.indices) {
        val tag = tags[i]
        val start = tag.range.last + 1
        val end = if (i + 1 < tags.size) tags[i + 1].range.first else text.length
        val wordText = text.substring(start, end)
        val timeMs = parseTimeMs(tag.groupValues[1], tag.groupValues[2], tag.groupValues[3])
        if (wordText.isBlank()) {
            if (i == tags.lastIndex) endMs = timeMs
            continue
        }
        words.add(LyricWord(timeMs, wordText))
    }
    return words.takeIf { it.isNotEmpty() }?.let { WordTagResult(it, endMs) }
}

/**
 * Parses plain or enhanced LRC text (`[mm:ss.xx]line`, possibly multiple tags per line, and
 * optionally per-word `<mm:ss.xx>` tags inside the line body for karaoke-style highlighting).
 */
fun parsePlainLrc(raw: String): List<LyricLine> {
    val lines = mutableListOf<LyricLine>()
    raw.lineSequence().forEach { rawLine ->
        BACKGROUND_LINE.matchEntire(rawLine)?.let { match ->
            val owner = lines.lastOrNull() ?: return@forEach
            val body = match.groupValues[1]
            val wordResult = parseWordTags(body)
            val text = wordResult?.words?.joinToString("") { it.text }?.trim() ?: body.trim()
            if (text.isEmpty()) return@forEach
            val background = LyricLine(
                timeMs = wordResult?.words?.first()?.timeMs ?: owner.timeMs,
                text = text,
                words = wordResult?.words,
                endTimeMs = wordResult?.endMs,
            )
            lines[lines.lastIndex] = owner.copy(background = background)
            return@forEach
        }
        val tags = TIME_TAG.findAll(rawLine).toList()
        if (tags.isEmpty()) return@forEach

        // Text between one tag and the next belongs to that tag. Two conventions collide here:
        // the classic LRC "chorus repeats at these times" feature crams several tags back-to-back
        // with nothing between them, all meant to share the *trailing* text — so an empty slice
        // inherits the next non-empty slice found ahead of it. But some exports (seen in the
        // wild: a whole translation block crammed onto one physical line, one tag+text pair after
        // another with no newlines) put *distinct* text after every tag — those must each keep
        // their own text instead of collapsing onto the line's last fragment.
        val slices = tags.mapIndexed { i, tag ->
            val start = tag.range.last + 1
            val end = if (i + 1 < tags.size) tags[i + 1].range.first else rawLine.length
            rawLine.substring(start, end).trim()
        }.toMutableList()
        var carry = ""
        for (i in slices.indices.reversed()) {
            if (slices[i].isEmpty()) slices[i] = carry else carry = slices[i]
        }

        tags.forEachIndexed { i, match ->
            val voice = when (VOICE_PREFIX.find(slices[i])?.groupValues?.get(1)?.lowercase()) {
                "v1" -> LyricVoice.V1
                "v2" -> LyricVoice.V2
                else -> null
            }
            val rest = VOICE_PREFIX.replace(slices[i], "")
            if (rest.isEmpty()) return@forEachIndexed
            val wordResult = parseWordTags(rest)
            val text = wordResult?.words?.joinToString("") { it.text }?.trim() ?: rest
            if (text.isEmpty()) return@forEachIndexed
            val timeMs = parseTimeMs(match.groupValues[1], match.groupValues[2], match.groupValues[3])
            lines.add(LyricLine(timeMs, text, wordResult?.words, wordResult?.endMs, voice = voice))
        }
    }
    return groupSimultaneousLines(lines)
}

/**
 * Lines sharing the exact same timestamp aren't competing alternatives — they're a translation
 * or a second singer's part meant to show alongside the main line. The first one encountered in
 * the file (source order, before the final time-sort) becomes primary; the rest ride along as
 * `secondary`, rendered as smaller "comment" text instead of fighting for which one is active.
 */
private fun groupSimultaneousLines(lines: List<LyricLine>): List<LyricLine> =
    lines.groupBy { it.timeMs }
        .map { (_, group) -> if (group.size > 1) group.first().copy(secondary = group.drop(1)) else group.first() }
        .sortedBy { it.timeMs }

/**
 * Groups word/syllable-level SYLT segments into lines. Per the ID3 spec, a newline in a
 * segment's text marks a line boundary; whitespace belongs to the segment that follows it.
 */
fun buildLinesFromSylt(segments: List<UsltFrameDecoder.SyltLine>): List<LyricLine> {
    val result = mutableListOf<LyricLine>()
    var words = mutableListOf<LyricWord>()
    var lineStart: Long? = null

    fun flush() {
        val text = words.joinToString("") { it.text }.trim()
        if (text.isNotEmpty()) {
            result.add(LyricLine(lineStart ?: words.first().timeMs, text, words.toList()))
        }
        words = mutableListOf()
        lineStart = null
    }

    for (segment in segments) {
        val parts = segment.text.split('\n')
        if (parts.size == 1) {
            if (lineStart == null) lineStart = segment.timestampMs
            words.add(LyricWord(segment.timestampMs, segment.text))
            continue
        }
        if (parts.first().isNotEmpty()) {
            if (lineStart == null) lineStart = segment.timestampMs
            words.add(LyricWord(segment.timestampMs, parts.first()))
        }
        flush()
        for (mid in 1 until parts.size - 1) {
            if (parts[mid].isNotBlank()) result.add(LyricLine(segment.timestampMs, parts[mid].trim()))
        }
        val last = parts.last()
        if (last.isNotEmpty()) {
            lineStart = segment.timestampMs
            words.add(LyricWord(segment.timestampMs, last))
        }
    }
    flush()
    return result
}
