package com.beatofthedrum.alacdecoder

import android.media.AudioFormat
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Util
import androidx.media3.decoder.CryptoConfig
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DecoderAudioRenderer

internal class SonoraAlacRenderer(
    eventHandler: Handler?,
    eventListener: AudioRendererEventListener?,
    audioSink: AudioSink,
) : DecoderAudioRenderer<SonoraAlacDecoder>(eventHandler, eventListener, audioSink) {

    override fun supportsFormatInternal(format: Format): Int {
        if (!MimeTypes.AUDIO_ALAC.equals(format.sampleMimeType, ignoreCase = true)) {
            return C.FORMAT_UNSUPPORTED_TYPE
        }
        if (format.cryptoType != C.CRYPTO_TYPE_NONE) return C.FORMAT_UNSUPPORTED_DRM
        val config = format.initializationData.firstOrNull()
            ?: return C.FORMAT_UNSUPPORTED_SUBTYPE
        val bitDepth = config.getOrNull(5)?.toInt()?.and(0xff)
            ?: return C.FORMAT_UNSUPPORTED_SUBTYPE
        if (bitDepth != 16 && bitDepth != 24) return C.FORMAT_UNSUPPORTED_SUBTYPE
        val pcmEncoding = Util.getPcmEncoding(bitDepth)
        if (pcmEncoding == C.ENCODING_INVALID) return C.FORMAT_UNSUPPORTED_SUBTYPE
        return if (sinkSupportsFormat(
                Util.getPcmFormat(
                    pcmEncoding,
                    format.channelCount,
                    format.sampleRate
                )
            )
        ) {
            C.FORMAT_HANDLED
        } else {
            C.FORMAT_UNSUPPORTED_SUBTYPE
        }
    }

    override fun createDecoder(format: Format, cryptoConfig: CryptoConfig?): SonoraAlacDecoder =
        SonoraAlacDecoder(format, INPUT_BUFFER_COUNT, OUTPUT_BUFFER_COUNT)

    override fun getOutputFormat(decoder: SonoraAlacDecoder): Format {
        val format = decoder.inputFormat
        val bitDepth = format.initializationData.first()[5].toInt() and 0xff
        val pcmEncoding = Util.getPcmEncoding(bitDepth)
        var output = Util.getPcmFormat(pcmEncoding, format.channelCount, format.sampleRate)
        if (format.channelMask != Format.NO_VALUE) {
            output = output.buildUpon().setChannelMask(format.channelMask).build()
        } else if (format.channelCount == 4) {
            output = output.buildUpon().setChannelMask(AudioFormat.CHANNEL_OUT_SURROUND).build()
        }
        return output
    }

    override fun getName(): String = "SonoraAlacRenderer"

    companion object {
        private const val INPUT_BUFFER_COUNT = 8
        private const val OUTPUT_BUFFER_COUNT = 8
    }
}
