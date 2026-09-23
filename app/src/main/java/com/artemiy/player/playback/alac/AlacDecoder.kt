package com.artemiy.player.playback.alac

import androidx.media3.common.Format
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.decoder.SimpleDecoder
import androidx.media3.decoder.SimpleDecoderOutputBuffer
import com.beatofthedrum.alacdecoder.AlacDecodeUtils
import com.beatofthedrum.alacdecoder.AlacFile
import java.nio.ByteBuffer

/**
 * Bridges media3's [SimpleDecoder] to the pure-Java ALAC decode routines in
 * [com.beatofthedrum.alacdecoder] (BSD-3-Clause, github.com/soiaf/Java-Apple-Lossless-decoder).
 * Android's platform MediaCodec has no ALAC decoder on most devices, so this fills that gap
 * entirely in software — no native/NDK build needed.
 */
@Suppress("UNCHECKED_CAST")
class AlacDecoder(val inputFormat: Format, numInputBuffers: Int, numOutputBuffers: Int) :
    SimpleDecoder<DecoderInputBuffer, SimpleDecoderOutputBuffer, AlacDecoderException>(
        arrayOfNulls<DecoderInputBuffer>(numInputBuffers) as Array<DecoderInputBuffer>,
        arrayOfNulls<SimpleDecoderOutputBuffer>(numOutputBuffers) as Array<SimpleDecoderOutputBuffer>,
    ) {

    companion object {
        private const val ALAC_MAX_PACKET_SIZE = 16384
    }

    private val file: AlacFile

    init {
        val bitDepth = inputFormat.initializationData[0][5].toInt()
        file = AlacDecodeUtils.create_alac(bitDepth, inputFormat.channelCount)
        file.channel_map = channelMapping(file.numchannels)
        AlacDecodeUtils.alac_set_info(file, ByteBuffer.wrap(inputFormat.initializationData[0]))
        val mp4MaxSize = if (inputFormat.maxInputSize != Format.NO_VALUE) inputFormat.maxInputSize else ALAC_MAX_PACKET_SIZE
        setInitialInputBufferSize(minOf(file.max_frame_bytes, mp4MaxSize))
    }

    override fun getName(): String = "AlacDecoder"

    override fun createInputBuffer(): DecoderInputBuffer =
        DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL)

    override fun createOutputBuffer(): SimpleDecoderOutputBuffer =
        SimpleDecoderOutputBuffer { outputBuffer -> releaseOutputBuffer(outputBuffer) }

    override fun createUnexpectedDecodeException(error: Throwable): AlacDecoderException =
        AlacDecoderException(error)

    override fun decode(
        inputBuffer: DecoderInputBuffer,
        outputBuffer: SimpleDecoderOutputBuffer,
        reset: Boolean,
    ): AlacDecoderException? {
        val data = inputBuffer.data ?: return AlacDecoderException("input has no data")
        if (!data.hasArray()) return AlacDecoderException("input has no array")
        if (inputBuffer.hasSupplementalData()) return AlacDecoderException("unexpected supplemental data")
        return try {
            val limit = AlacDecodeUtils.decode_frame(file, inputBuffer, outputBuffer)
            outputBuffer.data?.let {
                it.position(0)
                it.limit(limit)
            }
            null
        } catch (e: AlacDecoderException) {
            e
        } finally {
            file.input_buffer = null
        }
    }

    /** ALAC's channel order differs from Android's; remap per channel count (mono..7.1). */
    private fun channelMapping(channels: Int): IntArray = when (channels) {
        1 -> intArrayOf(0)
        2 -> intArrayOf(0, 1)
        3 -> intArrayOf(2, 0, 1)
        4 -> intArrayOf(2, 0, 1, 3)
        5 -> intArrayOf(2, 0, 1, 3, 4)
        6 -> intArrayOf(2, 0, 1, 4, 5, 3)
        7 -> intArrayOf(2, 0, 1, 4, 5, 6, 3)
        8 -> intArrayOf(2, 6, 7, 0, 1, 4, 5, 3)
        else -> throw UnsupportedOperationException("Unsupported ALAC channel count: $channels")
    }
}
