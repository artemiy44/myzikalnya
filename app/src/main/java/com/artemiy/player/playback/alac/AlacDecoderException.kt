package com.artemiy.player.playback.alac

import androidx.media3.decoder.DecoderException

class AlacDecoderException : DecoderException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
}
