package com.beatofthedrum.alacdecoder

import androidx.media3.decoder.DecoderException

internal class SonoraAlacDecoderException : DecoderException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
}
