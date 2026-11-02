package com.codex.voicex.audio

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * WAV (PCM) read/write utilities — fully offline, no external libs.
 * Reading and writing are streamed in small chunks so that long recordings do
 * not need a second full copy of the file in memory. Made by CODEx Team.
 */
object PcmFile {

    class UnsupportedAudioException(msg: String) : Exception(msg)

    data class Audio(val samples: FloatArray, val sampleRate: Int) {
        val durationMs: Long get() = if (sampleRate > 0) samples.size * 1000L / sampleRate else 0L
    }

    /** Header facts about a WAV file (no sample data loaded). */
    data class Info(
        val format: Int, val channels: Int, val sampleRate: Int, val bits: Int,
        val dataOffset: Long, val dataSize: Long
    ) {
        val frames: Long get() = dataSize / (channels * (bits / 8))
        val durationMs: Long get() = if (sampleRate > 0) frames * 1000L / sampleRate else 0L
    }

    private const val FORMAT_PCM = 1
    private const val FORMAT_FLOAT = 3
    private const val FORMAT_EXTENSIBLE = 0xFFFE

    /** Hard ceiling to protect the device from OOM: ~16M mono samples (~6 min @ 44.1 kHz). */
    const val MAX_SAMPLES = 16_000_000L

    // ------------------------------------------------------------------ write

    /** Write mono 16-bit PCM WAV. */
    fun writeWav(file: File, samples: FloatArray, sampleRate: Int) {
        val dataSize = samples.size.toLong() * 2
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.write(header(dataSize, sampleRate))
            val chunk = ByteBuffer.allocate(8192 * 2).order(ByteOrder.LITTLE_ENDIAN)
            var i = 0
            while (i < samples.size) {
                chunk.clear()
                var n = 0
                while (i < samples.size && n < 8192) {
                    val v = (samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
                    chunk.putShort(v)
                    i++; n++
                }
                raf.write(chunk.array(), 0, n * 2)
            }
        }
    }

    private fun header(dataSize: Long, sampleRate: Int): ByteArray {
        val bb = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray(Charsets.US_ASCII))
        bb.putInt((36 + dataSize).toInt())
        bb.put("WAVE".toByteArray(Charsets.US_ASCII))
        bb.put("fmt ".toByteArray(Charsets.US_ASCII))
        bb.putInt(16)              // PCM chunk size
        bb.putShort(1)             // PCM format
        bb.putShort(1)             // mono
        bb.putInt(sampleRate)
        bb.putInt(sampleRate * 2)  // byte rate
        bb.putShort(2)             // block align
        bb.putShort(16)            // bits per sample
        bb.put("data".toByteArray(Charsets.US_ASCII))
        bb.putInt(dataSize.toInt())
        return bb.array()
    }

    /**
     * Convert a raw 16-bit mono PCM file into a WAV file, streaming.
     * @return the peak amplitude (0..1) of the audio, used to detect silent recordings.
     */
    fun rawToWav(raw: File, out: File, sampleRate: Int): Float {
        var peak = 0
        FileInputStream(raw).use { input ->
            RandomAccessFile(out, "rw").use { raf ->
                raf.setLength(0)
                raf.write(header(raw.length() / 2 * 2, sampleRate))
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    val even = n and 1.inv()
                    var i = 0
                    while (i < even) {
                        val v = (buf[i].toInt() and 0xFF) or (buf[i + 1].toInt() shl 8)
                        val a = abs(v.toShort().toInt())
                        if (a > peak) peak = a
                        i += 2
                    }
                    raf.write(buf, 0, even)
                }
            }
        }
        return peak / 32768f
    }

    /** Streaming WAV writer for decoders / importers (mono 16-bit). */
    class WavSink(private val file: File, private val sampleRate: Int) : AutoCloseable {
        private val os = BufferedOutputStream(FileOutputStream(file), 64 * 1024)
        private var written = 0L
        private val tmp = ByteBuffer.allocate(8192 * 2).order(ByteOrder.LITTLE_ENDIAN)

        init { os.write(ByteArray(44)) }

        val samplesWritten: Long get() = written / 2

        fun write(samples: FloatArray, count: Int) {
            var i = 0
            while (i < count) {
                tmp.clear()
                var n = 0
                while (i < count && n < 8192) {
                    tmp.putShort((samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort())
                    i++; n++
                }
                os.write(tmp.array(), 0, n * 2)
                written += n * 2L
            }
        }

        override fun close() {
            os.flush(); os.close()
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                raf.write(header(written, sampleRate))
            }
        }
    }

    // ------------------------------------------------------------------- read

    /** Parse the WAV header only. */
    fun readInfo(file: File): Info {
        if (file.length() < 44) throw UnsupportedAudioException("File too small")
        RandomAccessFile(file, "r").use { raf ->
            val head = ByteArray(12)
            raf.readFully(head)
            if (String(head, 0, 4, Charsets.US_ASCII) != "RIFF" || String(head, 8, 4, Charsets.US_ASCII) != "WAVE")
                throw UnsupportedAudioException("Not a WAV file")

            var format = 1; var channels = 1; var sampleRate = 44100; var bits = 16
            var dataOffset = -1L; var dataSize = 0L
            val len = raf.length()
            var pos = 12L
            val ch = ByteArray(8)
            while (pos + 8 <= len) {
                raf.seek(pos)
                raf.readFully(ch)
                val id = String(ch, 0, 4, Charsets.US_ASCII)
                val size = (ByteBuffer.wrap(ch, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int).toLong() and 0xFFFFFFFFL
                val body = pos + 8
                if (id == "fmt " && size >= 16) {
                    val f = ByteArray(minOf(size, 40L).toInt())
                    raf.readFully(f)
                    val bb = ByteBuffer.wrap(f).order(ByteOrder.LITTLE_ENDIAN)
                    format = bb.short.toInt() and 0xFFFF
                    channels = bb.short.toInt()
                    sampleRate = bb.int
                    bb.int; bb.short
                    bits = bb.short.toInt()
                    if (format == FORMAT_EXTENSIBLE && f.size >= 26) {
                        format = bb.getShort(24).toInt() and 0xFFFF // sub-format GUID first two bytes
                    }
                } else if (id == "data") {
                    dataOffset = body
                    // streamed/odd files may declare a wrong size: clamp to what exists
                    dataSize = minOf(size, len - body)
                    break
                }
                pos = body + size + (size and 1L)
            }
            val ok = (format == FORMAT_PCM && (bits == 8 || bits == 16 || bits == 24 || bits == 32)) ||
                (format == FORMAT_FLOAT && bits == 32)
            if (!ok) throw UnsupportedAudioException("Unsupported WAV format (fmt=$format bits=$bits)")
            if (dataOffset < 0 || dataSize <= 0) throw UnsupportedAudioException("No data chunk")
            if (channels !in 1..2) throw UnsupportedAudioException("Unsupported channel count")
            if (sampleRate !in 4000..192000) throw UnsupportedAudioException("Unsupported sample rate")
            return Info(format, channels, sampleRate, bits, dataOffset, dataSize)
        }
    }

    /** Duration without loading samples. */
    fun durationMs(file: File): Long = try { readInfo(file).durationMs } catch (e: Exception) { 0L }

    /**
     * Read a WAV file (8/16/24/32-bit PCM or 32-bit float, mono or stereo -> mono).
     * @throws UnsupportedAudioException for non-WAV / unknown files or files over [MAX_SAMPLES].
     */
    fun readWav(file: File): Audio {
        val info = readInfo(file)
        if (info.frames > MAX_SAMPLES) throw UnsupportedAudioException("Too long")
        val frames = info.frames.toInt()
        if (frames <= 0) throw UnsupportedAudioException("Empty audio")
        val bytesPerSample = info.bits / 8
        val frameBytes = info.channels * bytesPerSample
        val out = FloatArray(frames)

        FileInputStream(file).use { fis ->
            fis.channel.position(info.dataOffset)
            val chunkFrames = 16384
            val buf = ByteArray(chunkFrames * frameBytes)
            val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
            var done = 0
            while (done < frames) {
                val want = minOf(chunkFrames, frames - done) * frameBytes
                var got = 0
                while (got < want) {
                    val r = fis.read(buf, got, want - got)
                    if (r <= 0) break
                    got += r
                }
                val n = got / frameBytes
                if (n <= 0) break
                bb.clear()
                for (f in 0 until n) {
                    var acc = 0f
                    for (c in 0 until info.channels) {
                        acc += when {
                            info.format == FORMAT_FLOAT -> bb.float
                            info.bits == 16 -> bb.short / 32768f
                            info.bits == 8 -> ((bb.get().toInt() and 0xFF) - 128) / 128f
                            info.bits == 24 -> {
                                val b0 = bb.get().toInt() and 0xFF
                                val b1 = bb.get().toInt() and 0xFF
                                val b2 = bb.get().toInt()          // signed
                                ((b2 shl 16) or (b1 shl 8) or b0) / 8388608f
                            }
                            else -> bb.int / 2147483648f
                        }
                    }
                    out[done + f] = acc / info.channels
                }
                done += n
                if (n * frameBytes < want) break
            }
            if (done < frames) return Audio(out.copyOf(done), info.sampleRate)
        }
        return Audio(out, info.sampleRate)
    }
}
