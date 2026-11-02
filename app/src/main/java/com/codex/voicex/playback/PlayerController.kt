package com.codex.voicex.playback

import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * Local audio player (MediaPlayer) with position tracking and seek.
 * Made by CODEx Team.
 */
class PlayerController(private val scope: CoroutineScope) {

    var currentFile by mutableStateOf<File?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var positionMs by mutableStateOf(0); private set
    var durationMs by mutableStateOf(0); private set
    var error by mutableStateOf<String?>(null); private set

    private var player: MediaPlayer? = null
    private var ticker: Job? = null

    /** Play (or toggle) the given file. */
    fun toggle(file: File) {
        if (currentFile == file && player != null) {
            if (isPlaying) pause() else resume()
            return
        }
        play(file)
    }

    fun play(file: File) {
        stopInternal()
        error = null
        try {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener {
                isPlaying = false
                positionMs = 0
                ticker?.cancel()
            }
            mp.setOnErrorListener { _, _, _ ->
                error = "play_failed"
                isPlaying = false
                true
            }
            mp.prepare()
            mp.start()
            player = mp
            currentFile = file
            durationMs = mp.duration
            positionMs = 0
            isPlaying = true
            ticker = scope.launch(Dispatchers.Main) {
                while (isPlaying) {
                    positionMs = try { mp.currentPosition } catch (e: Exception) { 0 }
                    delay(120)
                }
            }
        } catch (e: Exception) {
            error = "play_failed"
            isPlaying = false
            currentFile = null
        }
    }

    fun pause() {
        try { player?.pause() } catch (e: Exception) { error = "play_failed" }
        isPlaying = false
        ticker?.cancel()
    }

    fun resume() {
        try {
            player?.start()
            isPlaying = true
            ticker = scope.launch(Dispatchers.Main) {
                while (isPlaying) {
                    positionMs = try { player?.currentPosition ?: 0 } catch (e: Exception) { 0 }
                    delay(120)
                }
            }
        } catch (e: Exception) { error = "play_failed" }
    }

    fun seekTo(ms: Int) {
        try {
            player?.seekTo(ms.coerceIn(0, durationMs))
            positionMs = ms.coerceIn(0, durationMs)
        } catch (e: Exception) { error = "play_failed" }
    }

    fun stop() = stopInternal()

    private fun stopInternal() {
        ticker?.cancel()
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
        isPlaying = false
        positionMs = 0
    }

    fun release() {
        stopInternal()
        currentFile = null
    }
}
