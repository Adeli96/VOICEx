package com.codex.voicex.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import java.io.File
import java.nio.ByteOrder

/**
 * Decodes any audio file the platform understands (mp3, m4a/aac, ogg, flac, wav, opus…)
 * into a mono 16-bit WAV that the DSP engine can read. Fully offline, uses the
 * built-in MediaExtractor/MediaCodec — no extra libraries, no storage permission
 * (the file comes from the system document picker). Made by CODEx Team.
 */
object AudioImporter {

    class ImportException(val key: String) : Exception(key)

    /** @return duration in ms of the written WAV. Throws [ImportException] with an error key. */
    fun importToWav(context: Context, uri: Uri, out: File): Long {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        var sink: PcmFile.WavSink? = null
        try {
            try {
                extractor.setDataSource(context, uri, null)
            } catch (e: Exception) {
                throw ImportException("err_unsupported")
            }
            var track = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                if ((f.getString(MediaFormat.KEY_MIME) ?: "").startsWith("audio/")) { track = i; format = f; break }
            }
            if (track < 0 || format == null) throw ImportException("err_unsupported")
            extractor.selectTrack(track)

            val mime = format.getString(MediaFormat.KEY_MIME)!!
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
            if (format.containsKey(MediaFormat.KEY_DURATION)) {
                val seconds = format.getLong(MediaFormat.KEY_DURATION) / 1_000_000.0
                if (seconds * sampleRate > PcmFile.MAX_SAMPLES) throw ImportException("err_too_long")
            }

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            sink = PcmFile.WavSink(out, sampleRate)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var scratch = FloatArray(16384)

            while (!outputDone) {
                if (!inputDone) {
                    val inIx = codec.dequeueInputBuffer(10_000)
                    if (inIx >= 0) {
                        val buf = codec.getInputBuffer(inIx)!!
                        val size = extractor.readSampleData(buf, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inIx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIx, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIx = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    outIx >= 0 -> {
                        if (info.size > 0) {
                            val ob = codec.getOutputBuffer(outIx)!!
                            ob.position(info.offset)
                            ob.limit(info.offset + info.size)
                            val shorts = ob.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val frames = shorts.remaining() / channels
                            if (scratch.size < frames) scratch = FloatArray(frames)
                            for (f in 0 until frames) {
                                var acc = 0f
                                for (c in 0 until channels) acc += shorts.get() / 32768f
                                scratch[f] = acc / channels
                            }
                            sink!!.write(scratch, frames)
                            if (sink!!.samplesWritten > PcmFile.MAX_SAMPLES) throw ImportException("err_too_long")
                        }
                        codec.releaseOutputBuffer(outIx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                    outIx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val nf = codec.outputFormat
                        if (nf.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                            channels = nf.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                        if (nf.containsKey(MediaFormat.KEY_SAMPLE_RATE) &&
                            nf.getInteger(MediaFormat.KEY_SAMPLE_RATE) != sampleRate) {
                            // decoder changed rate after the header was written: header is rewritten on close
                            sampleRate = nf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            sink!!.close()
                            sink = PcmFile.WavSink(out, sampleRate)
                        }
                    }
                }
            }
            val samples = sink!!.samplesWritten
            sink!!.close(); sink = null
            if (samples < sampleRate / 10) throw ImportException("err_corrupt")
            return samples * 1000L / sampleRate
        } catch (e: ImportException) {
            out.delete(); throw e
        } catch (e: OutOfMemoryError) {
            out.delete(); throw ImportException("err_memory")
        } catch (e: Exception) {
            out.delete(); throw ImportException("err_unsupported")
        } finally {
            try { sink?.close() } catch (_: Exception) {}
            try { codec?.stop() } catch (_: Exception) {}
            try { codec?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}
