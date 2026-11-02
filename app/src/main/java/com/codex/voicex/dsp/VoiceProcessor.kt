package com.codex.voicex.dsp

import com.codex.voicex.audio.PcmFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Offline voice processing entry point.
 * Runs entirely on a background dispatcher — never blocks the UI thread.
 * Made by CODEx Team.
 */
object VoiceProcessor {

    sealed class Result {
        data class Success(val file: File, val durationMs: Long) : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun process(
        input: File,
        output: File,
        params: DspParams,
        quality: String = "balanced",
        onProgress: (Float) -> Unit
    ): Result = withContext(Dispatchers.Default) {
        try {
            if (!input.exists() || input.length() < 100L) {
                return@withContext Result.Failure("corrupted")
            }
            val audio = try {
                PcmFile.readWav(input)
            } catch (e: PcmFile.UnsupportedAudioException) {
                return@withContext Result.Failure(if (e.message == "Too long") "too_long" else "unsupported")
            }
            if (audio.samples.isEmpty()) return@withContext Result.Failure("corrupted")

            val out = Dsp.render(audio.samples, audio.sampleRate, params, Dsp.Quality.from(quality)) { p ->
                onProgress(p * 0.9f)
            }
            PcmFile.writeWav(output, out, audio.sampleRate)
            onProgress(1f)
            Result.Success(output, out.size * 1000L / audio.sampleRate)
        } catch (e: OutOfMemoryError) {
            output.delete()
            Result.Failure("memory")
        } catch (e: java.io.IOException) {
            output.delete()
            Result.Failure("storage")
        } catch (e: Exception) {
            output.delete()
            Result.Failure("process")
        }
    }
}
