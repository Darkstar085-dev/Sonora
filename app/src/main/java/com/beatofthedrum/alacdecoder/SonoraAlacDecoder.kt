package com.beatofthedrum.alacdecoder

import androidx.media3.common.Format
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.decoder.SimpleDecoder
import androidx.media3.decoder.SimpleDecoderOutputBuffer

@Suppress("UNCHECKED_CAST")
internal class SonoraAlacDecoder(
    internal val inputFormat: Format,
    numInputBuffers: Int,
    numOutputBuffers: Int,
) : SimpleDecoder<DecoderInputBuffer, SimpleDecoderOutputBuffer, SonoraAlacDecoderException>(
    (arrayOfNulls<DecoderInputBuffer>(numInputBuffers) as Array<DecoderInputBuffer>),
    (arrayOfNulls<SimpleDecoderOutputBuffer>(numOutputBuffers) as Array<SimpleDecoderOutputBuffer>),
) {
    private val bitDepth = inputFormat.initializationData.first()[5].toInt() and 0xff
    private val channelCount = inputFormat.channelCount
    private val decoderFile: AlacFile
    private val pcmSamples: IntArray

    init {
        require(bitDepth == 16 || bitDepth == 24) { "Unsupported ALAC bit depth: $bitDepth" }
        val config = inputFormat.initializationData.first()
        val cookie = IntArray(24 + config.size)
        config.forEachIndexed { index, value -> cookie[index + 24] = value.toInt() and 0xff }
        decoderFile = AlacDecodeUtils.create_alac(bitDepth, channelCount)
        AlacDecodeUtils.alac_set_info(decoderFile, cookie)
        pcmSamples = IntArray(decoderFile.setinfo_max_samples_per_frame * channelCount * if (bitDepth == 24) 3 else 1)
        val maximumPacketSize = inputFormat.maxInputSize
            .takeIf { it != Format.NO_VALUE && it > 0 } ?: DEFAULT_PACKET_SIZE
        setInitialInputBufferSize(maximumPacketSize)
    }

    override fun getName(): String = "SonoraAlacDecoder"

    override fun createInputBuffer(): DecoderInputBuffer =
        DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL)

    override fun createOutputBuffer(): SimpleDecoderOutputBuffer =
        SimpleDecoderOutputBuffer { releaseOutputBuffer(it) }

    override fun createUnexpectedDecodeException(error: Throwable): SonoraAlacDecoderException =
        SonoraAlacDecoderException(error)

    override fun decode(
        inputBuffer: DecoderInputBuffer,
        outputBuffer: SimpleDecoderOutputBuffer,
        reset: Boolean,
    ): SonoraAlacDecoderException? {
        val source = inputBuffer.data ?: return SonoraAlacDecoderException("Missing ALAC packet data")
        return try {
            val encoded = ByteArray(source.remaining() + READ_PADDING)
            source.get(encoded, 0, encoded.size - READ_PADDING)
            val outputBytes = AlacDecodeUtils.decode_frame(decoderFile, encoded, pcmSamples, 0)
            if (outputBytes <= 0) return SonoraAlacDecoderException("ALAC decoder returned an empty frame")
            outputBuffer.init(inputBuffer.timeUs, outputBytes)
            val pcm = outputBuffer.data ?: return SonoraAlacDecoderException("Could not allocate PCM output")
            if (bitDepth == 24) {
                for (index in 0 until outputBytes) {
                    pcm.put(pcmSamples[index].toByte())
                }
            } else {
                val sampleCount = outputBytes / 2
                for (index in 0 until sampleCount) {
                    val sample = pcmSamples[index]
                    pcm.put(sample.toByte())
                    pcm.put((sample shr 8).toByte())
                }
            }
            pcm.flip()
            null
        } catch (error: Exception) {
            SonoraAlacDecoderException(error)
        } finally {
            decoderFile.input_buffer = null
        }
    }

    companion object {
        private const val DEFAULT_PACKET_SIZE = 16_384
        private const val READ_PADDING = 8
    }
}
