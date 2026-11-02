package com.codex.voicex.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Encodes a mono 16-bit PCM WAV into an AAC-LC .m4a file using the platform
 * MediaCodec + MediaMuxer (offline, no libraries). Used for compact sharing.
 * Made by CODEx Team.
 */
object AacEncoder {

    fun wavToM4a(wav: File, out: File, bitRate: Int = 96_000) {
        val info = PcmFile.readInfo(wav)
        if (info.bits != 16 || info.channels != 1) throw IllegalArgumentException("Expected mono 16-bit WAV")

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, info.sampleRate, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        var muxer: MediaMuxer? = null
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val mux = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mux
            var muxTrack = -1
            var muxStarted = false

            val bufInfo = MediaCodec.BufferInfo()
            val bytesPerSec = info.sampleRate * 2L
            var pcmPos = 0L
            var inputDone = false
            var outputDone = false
            val chunk = ByteArray(8192)

            java.io.RandomAccessFile(wav, "r").use { raf ->
                raf.seek(info.dataOffset)
                while (!outputDone) {
                    if (!inputDone) {
                        val ix = codec.dequeueInputBuffer(10_000)
                        if (ix >= 0) {
                            val ib: ByteBuffer = codec.getInputBuffer(ix)!!
                            ib.clear()
                            val want = minOf(chunk.size, ib.remaining(), (info.dataSize - pcmPos).toInt())
                            val got = if (want > 0) raf.read(chunk, 0, want) else -1
                            if (got <= 0) {
                                codec.queueInputBuffer(ix, 0, 0, pcmPos * 1_000_000L / bytesPerSec,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                ib.put(chunk, 0, got)
                                codec.queueInputBuffer(ix, 0, got, pcmPos * 1_000_000L / bytesPerSec, 0)
                                pcmPos += got
                            }
                        }
                    }
                    val oi = codec.dequeueOutputBuffer(bufInfo, 10_000)
                    when {
                        oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            muxTrack = mux.addTrack(codec.outputFormat)
                            mux.start()
                            muxStarted = true
                        }
                        oi >= 0 -> {
                            val ob = codec.getOutputBuffer(oi)!!
                            if (bufInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) bufInfo.size = 0
                            if (bufInfo.size > 0 && muxStarted) {
                                ob.position(bufInfo.offset)
                                ob.limit(bufInfo.offset + bufInfo.size)
                                mux.writeSampleData(muxTrack, ob, bufInfo)
                            }
                            codec.releaseOutputBuffer(oi, false)
                            if (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                        }
                    }
                }
            }
        } finally {
            try { codec.stop() } catch (_: Exception) {}
            try { codec.release() } catch (_: Exception) {}
            try { muxer?.stop() } catch (_: Exception) {}
            try { muxer?.release() } catch (_: Exception) {}
        }
    }
}
