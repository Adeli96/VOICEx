package com.codex.voicex.recording

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.codex.voicex.audio.PcmFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Offline microphone recorder based on AudioRecord (raw PCM -> WAV).
 * Supports pause/resume/cancel, elapsed timer and live waveform peaks.
 *
 * - Runs a foreground service (microphone type) while recording so capture keeps
 *   working when the screen turns off or the user switches apps.
 * - Finishing (raw -> WAV) is streamed and runs off the main thread.
 * - Capture stops growing at [PcmFile.MAX_SAMPLES] to protect memory ([limitReached]).
 * Made by CODEx Team.
 */
class RecorderController(
    private val context: Context,
    private val sampleRateProvider: () -> Int = { 44100 }
) {

    enum class State { IDLE, RECORDING, PAUSED }

    var state by mutableStateOf(State.IDLE); private set
    var elapsedMs by mutableStateOf(0L); private set
    var error by mutableStateOf<String?>(null); private set
    var limitReached by mutableStateOf(false); private set
    val waveform = mutableStateListOf<Float>()

    var sampleRate = 44100; private set

    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var samplesWritten = 0L
    private var thread: Thread? = null
    private var rawFile: File? = null

    val isActive: Boolean get() = state == State.RECORDING || state == State.PAUSED

    @SuppressLint("MissingPermission") // caller guarantees RECORD_AUDIO grant
    fun start(): Boolean {
        if (isActive) return true
        if (thread?.isAlive == true) return false // previous capture is still shutting down
        error = null
        limitReached = false
        elapsedMs = 0L
        samplesWritten = 0L
        waveform.clear()
        sampleRate = sampleRateProvider()

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) { error = "rec_failed"; return false }

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
        } catch (e: Exception) { error = "rec_failed"; return false }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            error = "rec_failed"
            return false
        }

        val raw = File(context.cacheDir, "voicex_capture_${System.currentTimeMillis()}.raw")
        rawFile = raw
        running = true
        paused = false
        startService()

        val rate = sampleRate
        thread = Thread {
            try {
                BufferedOutputStream(FileOutputStream(raw)).use { bos ->
                    val buf = ShortArray(2048)
                    val bytes = ByteArray(buf.size * 2)
                    record.startRecording()
                    while (running) {
                        val n = record.read(buf, 0, buf.size)
                        if (n <= 0) continue
                        if (paused) continue // keep draining mic buffer while paused
                        if (samplesWritten >= PcmFile.MAX_SAMPLES) { limitReached = true; continue }
                        var sum = 0.0
                        var peak = 0f
                        var bi = 0
                        for (i in 0 until n) {
                            val v = buf[i]
                            val f = v / 32768f
                            val a = abs(f)
                            if (a > peak) peak = a
                            sum += f * f
                            bytes[bi++] = (v.toInt() and 0xFF).toByte()
                            bytes[bi++] = (v.toInt() shr 8).toByte()
                        }
                        bos.write(bytes, 0, n * 2)
                        samplesWritten += n
                        elapsedMs = samplesWritten * 1000L / rate
                        val rms = sqrt(sum / n).toFloat()
                        val level = (peak * 0.7f + rms * 2.2f).coerceIn(0.03f, 1f)
                        synchronized(waveform) {
                            waveform.add(level)
                            if (waveform.size > 72) waveform.removeAt(0)
                        }
                    }
                    record.stop()
                }
            } catch (e: Exception) {
                error = "rec_failed"
            } finally {
                try { record.release() } catch (_: Exception) {}
            }
        }.also { it.start() }

        state = State.RECORDING
        return true
    }

    fun pause() {
        if (state == State.RECORDING) {
            paused = true
            state = State.PAUSED
        }
    }

    fun resume() {
        if (state == State.PAUSED) {
            paused = false
            state = State.RECORDING
        }
    }

    /**
     * Stop capturing and convert to a WAV file. Heavy work runs on [Dispatchers.IO].
     * Returns null when failed or silent; [error] then holds the reason.
     */
    suspend fun finish(): File? {
        if (!isActive) return null
        running = false
        paused = false
        state = State.IDLE
        stopService()
        val raw = rawFile
        val t = thread
        rawFile = null
        thread = null
        if (raw == null) return null

        return withContext(Dispatchers.IO) {
            try { t?.join(3000) } catch (_: Exception) {}
            elapsedMs = samplesWritten * 1000L / sampleRate
            if (error != null) { raw.delete(); return@withContext null }
            if (samplesWritten < sampleRate / 4) { // shorter than 250 ms
                raw.delete(); error = "rec_failed"; return@withContext null
            }
            try {
                val out = File(context.cacheDir, "voicex_record_${System.currentTimeMillis()}.wav")
                val peak = PcmFile.rawToWav(raw, out, sampleRate)
                raw.delete()
                if (peak < 0.004f) {
                    out.delete()
                    error = "silent"
                    null
                } else {
                    error = null
                    out
                }
            } catch (e: java.io.IOException) {
                raw.delete(); error = "storage"; null
            } catch (e: Exception) {
                raw.delete(); error = "rec_failed"; null
            }
        }
    }

    /** Discard the current capture. Cheap, safe on the main thread. */
    fun cancel() {
        if (!isActive) return
        running = false
        paused = false
        state = State.IDLE
        stopService()
        val raw = rawFile
        val t = thread
        rawFile = null
        thread = null
        error = null
        Thread {
            try { t?.join(3000) } catch (_: Exception) {}
            raw?.delete()
        }.start()
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, RecordingService::class.java))
        } catch (_: Exception) { /* recording still works while the app is visible */ }
    }

    private fun stopService() {
        try { context.stopService(Intent(context, RecordingService::class.java)) } catch (_: Exception) {}
    }
}
