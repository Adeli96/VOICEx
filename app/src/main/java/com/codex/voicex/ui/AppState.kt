package com.codex.voicex.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.codex.voicex.favorites.FavoritesStore
import com.codex.voicex.playback.PlayerController
import com.codex.voicex.presets.PresetStore
import com.codex.voicex.recording.RecorderController
import com.codex.voicex.settings.SettingsRepository
import com.codex.voicex.storage.Recording
import com.codex.voicex.storage.RecordingRepository
import com.codex.voicex.ui.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * Application-wide state holder. Made by CODEx Team.
 * Groups all repositories and controllers so screens can reach them
 * without a full DI framework.
 */
class AppState(
    context: Context,
    val scope: CoroutineScope
) {
    val settings = SettingsRepository(context)
    val favorites = FavoritesStore(context)
    val recordings = RecordingRepository(context)
    val presetStore = PresetStore(context)
    val recorder = RecorderController(context) { settings.sampleRate }
    val player = PlayerController(scope)

    /** Preview renders live here (cache), never in the recordings library. */
    val previewDir: File = File(context.cacheDir, "previews").apply { mkdirs() }

    init {
        // drop previews left over from a previous session
        previewDir.listFiles()?.forEach { it.delete() }
    }

    // Navigation stack
    private val stack = ArrayDeque<Screen>()
    var current by mutableStateOf<Screen>(Screen.Splash)
        private set

    fun navigate(to: Screen) {
        errorKey = null
        if (current::class != to::class) stack.addLast(current)
        current = to
    }

    fun back(): Boolean {
        if (stack.isEmpty()) return false
        errorKey = null
        current = stack.removeLast()
        return true
    }

    fun resetTo(to: Screen) {
        stack.clear()
        current = to
    }

    // Working recording that flows through the pipeline
    var workingOriginal by mutableStateOf<Recording?>(null)
    var lastProcessedFile by mutableStateOf<File?>(null)
    var lastPresetId by mutableStateOf<String?>(null)
    var lastCategoryId by mutableStateOf<String?>(null)

    var processingProgress by mutableStateOf(0f)
    var processing by mutableStateOf(false)
    /** id of the preset whose processed preview was already saved to the library (avoids duplicates). */
    var savedPresetId by mutableStateOf<String?>(null)
    var savedRecording by mutableStateOf<Recording?>(null)
    var errorKey by mutableStateOf<String?>(null)

    /** Make [rec] the recording that effects are applied to; clears any stale preview. */
    fun useOriginal(rec: Recording?) {
        player.stop()
        lastProcessedFile?.takeIf { it.parentFile == previewDir }?.delete()
        lastProcessedFile = null
        lastPresetId = null
        savedPresetId = null
        savedRecording = null
        errorKey = null
        workingOriginal = rec
    }

    fun release() {
        player.release()
        if (recorder.isActive) recorder.cancel()
    }
}
