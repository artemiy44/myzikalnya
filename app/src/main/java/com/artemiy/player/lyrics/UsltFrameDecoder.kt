package com.artemiy.player.lyrics

import androidx.media3.common.util.ParsableByteArray
import java.nio.charset.Charset

/**
 * Decodes ID3v2 USLT (unsynchronised lyrics) and SYLT (synchronised lyrics) frames.
 * media3 exposes these only as raw [androidx.media3.extractor.metadata.id3.BinaryFrame] bytes —
 * there is no built-in decoder (see https://github.com/androidx/media/issues/922).
 *
 * Adapted from the MIT-licensed decoder at
 * https://github.com/yoheimuta/ExoPlayerMusic/blob/77cfb989b59f6906b1170c9b2d565f9b8447db41/app/src/main/java/com/github/yoheimuta/amplayer/playback/UsltFrameDecoder.kt
 * Frame layout: http://id3.org/id3v2.4.0-frames
 */
object UsltFrameDecoder {

    data class Uslt(val language: String, val description: String, val text: String)
    data class SyltLine(val timestampMs: Long, val text: String)
    data class Sylt(val language: String, val contentType: Int, val description: String, val lines: List<SyltLine>)

    private const val ENCODING_ISO_8859_1 = 0
    private const val ENCODING_UTF_16 = 1
    private const val ENCODING_UTF_16BE = 2
    private const val ENCODING_UTF_8 = 3

    fun decodeUslt(data: ParsableByteArray): Uslt? {
        if (data.limit() < 4) return null

        val encoding = data.readUnsignedByte()
        val charset = charsetFor(encoding)
        val lang = ByteArray(3)
        data.readBytes(lang, 0, 3)
        val language = decodeString(lang, 0, 3, Charsets.ISO_8859_1)

        val rest = ByteArray(data.limit() - 4)
        data.readBytes(rest, 0, data.limit() - 4)

        val descEnd = indexOfEos(rest, 0, encoding)
        val description = decodeString(rest, 0, descEnd, charset)
        val textStart = descEnd + delimiterLength(encoding)
        val textEnd = indexOfEos(rest, textStart, encoding)
        val text = decodeString(rest, textStart, textEnd, charset)
        return Uslt(language, description, text)
    }

    /** @param sampleRate needed because SYLT timestamps may be MPEG frame counts, not ms. */
    fun decodeSylt(data: ParsableByteArray, sampleRate: Int): Sylt? {
        if (data.limit() < 1) return null
        val encoding = data.readUnsignedByte()
        if (data.limit() < 8 + 2 * delimiterLength(encoding)) return null

        val charset = charsetFor(encoding)
        val lang = ByteArray(3)
        data.readBytes(lang, 0, 3)
        val language = decodeString(lang, 0, 3, Charsets.ISO_8859_1)
        val timestampFormat = data.readUnsignedByte()
        val contentType = data.readUnsignedByte()

        val rest = ByteArray(data.limit() - 6)
        data.readBytes(rest, 0, data.limit() - 6)

        val descEnd = indexOfEos(rest, 0, encoding)
        val description = decodeString(rest, 0, descEnd, charset)
        var pos = descEnd + delimiterLength(encoding)

        val lines = mutableListOf<SyltLine>()
        while (rest.size - pos > 1) {
            val textEnd = indexOfEos(rest, pos, encoding)
            val text = decodeString(rest, pos, textEnd, charset)
            pos = textEnd + delimiterLength(encoding) + 4
            if (pos > rest.size) break // malformed
            val timestampMs = decodeTimestamp(rest, pos - 4, timestampFormat, sampleRate)
            lines.add(SyltLine(timestampMs, text))
        }
        return Sylt(language, contentType, description, lines)
    }

    private fun charsetFor(encoding: Int): Charset = when (encoding) {
        ENCODING_UTF_16 -> Charsets.UTF_16
        ENCODING_UTF_16BE -> Charsets.UTF_16BE
        ENCODING_UTF_8 -> Charsets.UTF_8
        ENCODING_ISO_8859_1 -> Charsets.ISO_8859_1
        else -> Charsets.UTF_8
    }

    private fun delimiterLength(encoding: Int) =
        if (encoding == ENCODING_ISO_8859_1 || encoding == ENCODING_UTF_8) 1 else 2

    private fun indexOfZeroByte(data: ByteArray, from: Int): Int {
        for (i in from until data.size) if (data[i] == 0.toByte()) return i
        return data.size
    }

    private fun indexOfEos(data: ByteArray, from: Int, encoding: Int): Int {
        var pos = indexOfZeroByte(data, from)
        if (encoding == ENCODING_ISO_8859_1 || encoding == ENCODING_UTF_8) return pos
        while (pos < data.size - 1) {
            if (pos % 2 == 0 && data[pos + 1] == 0.toByte()) return pos
            pos = indexOfZeroByte(data, pos + 1)
        }
        return data.size
    }

    private fun decodeString(data: ByteArray, from: Int, to: Int, charset: Charset): String =
        if (to <= from || to > data.size) "" else String(data, from, to - from, charset)

    private fun decodeTimestamp(data: ByteArray, pos: Int, format: Int, sampleRate: Int): Long {
        val value = ((data[pos].toLong() and 0xff) shl 24) or
            ((data[pos + 1].toLong() and 0xff) shl 16) or
            ((data[pos + 2].toLong() and 0xff) shl 8) or
            (data[pos + 3].toLong() and 0xff)
        return when (format) {
            1 -> mpegFramesToMs(sampleRate, value) // frames since start of audio
            else -> value // already milliseconds
        }
    }

    private fun mpegFramesToMs(sampleRate: Int, framePosition: Long): Long {
        val samplesPerFrame = when (sampleRate) {
            32000, 44100, 48000 -> 1152 // MPEG-1
            16000, 22050, 24000, 8000, 11025, 12000 -> 576 // MPEG-2 / 2.5
            else -> 1152
        }
        return (framePosition * samplesPerFrame * 1000L) / sampleRate
    }
}
