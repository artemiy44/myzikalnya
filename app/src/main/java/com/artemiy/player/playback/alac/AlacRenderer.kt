package com.artemiy.player.playback.alac

import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Util
import androidx.media3.decoder.CryptoConfig
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DecoderAudioRenderer

/** Claims `audio/alac` tracks and decodes them in software via [AlacDecoder]. */
class AlacRenderer(
    eventHandler: Handler,
    eventListener: AudioRendererEventListener,
    audioSink: AudioSink,
) : DecoderAudioRenderer<AlacDecoder>(eventHandler, eventListener, audioSink) {

    override fun getName(): String = "AlacRenderer"

    override fun supportsFormatInternal(format: Format): Int {
        if (!MimeTypes.AUDIO_ALAC.equals(format.sampleMimeType, ignoreCase = true)) {
            return C.FORMAT_UNSUPPORTED_TYPE
        }
        if (format.cryptoType != C.CRYPTO_TYPE_NONE) {
            return C.FORMAT_UNSUPPORTED_DRM
        }
        val bitDepth = format.initializationData[0][5].toInt()
        val pcmEncoding = if (bitDepth == 20) C.ENCODING_PCM_24BIT else Util.getPcmEncoding(bitDepth)
        if (!sinkSupportsFormat(Util.getPcmFormat(pcmEncoding, format.channelCount, format.sampleRate))) {
            return C.FORMAT_UNSUPPORTED_SUBTYPE
        }
        return C.FORMAT_HANDLED
    }

    override fun createDecoder(format: Format, cryptoConfig: CryptoConfig?): AlacDecoder =
        AlacDecoder(format, 16, 16)

    override fun getOutputFormat(decoder: AlacDecoder): Format {
        val format = decoder.inputFormat
        val bitDepth = format.initializationData[0][5].toInt()
        val pcmEncoding = if (bitDepth == 20) C.ENCODING_PCM_24BIT else Util.getPcmEncoding(bitDepth)
        // Mono/stereo (the vast majority of music) needs no special channel-mask handling;
        // this media3 version has no Format.channelMask to remap >2ch surround layouts with.
        return Util.getPcmFormat(pcmEncoding, format.channelCount, format.sampleRate)
    }
}
