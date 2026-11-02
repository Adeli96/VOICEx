package com.codex.voicex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.voicex.audio.PcmFile
import com.codex.voicex.dsp.VoiceProcessor
import com.codex.voicex.localization.LocalLanguage
import com.codex.voicex.localization.s
import com.codex.voicex.presets.VoicePresets
import com.codex.voicex.ui.AppState
import com.codex.voicex.ui.components.StaticWaveform
import com.codex.voicex.ui.components.waveformFromSamples
import com.codex.voicex.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PresetScreen(state: AppState, presetId: String) {
    val ctx = LocalContext.current
    val lang = LocalLanguage.current
    val preset = remember(presetId) { VoicePresets.byId(presetId) ?: state.presetStore.byId(presetId) }
    val cat = preset?.let { VoicePresets.category(it.catId) }

    var bars by remember { mutableStateOf(List(56) { 0.15f }) }
    var progress by remember { mutableStateOf(0f) }

    // Recompute waveform for the currently active file
    LaunchedEffect(state.player.currentFile, state.player.durationMs) {
        state.player.currentFile?.let { f ->
            val computed = withContext(Dispatchers.Default) {
                runCatching { PcmFile.readWav(f) }.getOrNull()?.let { waveformFromSamples(it.samples, 64) }
            }
            if (computed != null) bars = computed
        }
    }
    progress = if (state.player.durationMs > 0)
        state.player.positionMs.toFloat() / state.player.durationMs else 0f

    val original = state.workingOriginal
    val hasRecording = original != null

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        TopBar(title = preset?.name(lang == "fa") ?: "") { state.back() }
        Spacer(Modifier.height(12.dp))

        // preset header card
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cat?.icon ?: "🎙️", fontSize = 34.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        preset?.name(lang == "fa") ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        cat?.let { if (lang == "fa") it.fa else it.en } ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val fav = preset?.let { state.favorites.isFavorite(it.id) } == true
                IconButton(onClick = { preset?.let { state.favorites.toggle(it.id) } }) {
                    Icon(
                        if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        null,
                        tint = if (fav) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        if (!hasRecording) {
            Text(
                s("rec_ready_choose"),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
            Button(
                onClick = { state.navigate(Screen.Recorder) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(8.dp))
                Text(s("home_record"))
            }
            return@Column
        }

        // waveform
        StaticWaveform(
            bars = bars,
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(88.dp)
        )
        Spacer(Modifier.height(14.dp))

        // processing indicator
        if (state.processing) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { state.processingProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.height(8.dp))
                Text(s("rec_processing"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else if (state.lastProcessedFile != null && state.lastPresetId == presetId) {
            Text(
                if (state.savedPresetId == presetId) s("saved") else s("rec_ready"),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(16.dp))

        // controls
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            val hasProcessed = state.lastProcessedFile != null && state.lastPresetId == presetId
            val toPlay = if (hasProcessed) state.lastProcessedFile!! else state.recordings.file(original!!)
            val isPlaying = state.player.isPlaying && state.player.currentFile == toPlay
            SmallCircleButton(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                MaterialTheme.colorScheme.primary
            ) { state.player.toggle(toPlay) }
            SmallCircleButton(Icons.Filled.Refresh, MaterialTheme.colorScheme.secondary) {
                runProcess(state, presetId)
            }
            SmallCircleButton(Icons.Filled.Save, MaterialTheme.colorScheme.tertiary) {
                saveProcessed(state, presetId)
            }
            SmallCircleButton(Icons.Filled.Share, MaterialTheme.colorScheme.primary) {
                val rec = saveProcessed(state, presetId) ?: return@SmallCircleButton
                state.scope.launch {
                    val intent = state.recordings.shareIntent(rec, state.settings.outputFormat)
                    if (intent != null) ctx.startActivity(intent) else state.errorKey = "err_share"
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state.errorKey != null) {
            Text(
                s(state.errorKey ?: ""),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // auto-process on first arrival
        LaunchedEffect(presetId, original?.id) {
            if (state.lastPresetId != presetId || state.lastProcessedFile == null) {
                runProcess(state, presetId)
            }
        }
    }
}

fun runProcess(state: AppState, presetId: String) {
    val preset = VoicePresets.byId(presetId) ?: state.presetStore.byId(presetId) ?: return
    val original = state.workingOriginal ?: return
    val input = state.recordings.file(original)
    val out = File(state.previewDir, "preview_${System.currentTimeMillis()}.wav")
    state.errorKey = null
    state.processing = true
    state.processingProgress = 0f
    state.lastPresetId = presetId
    state.lastCategoryId = preset.catId
    state.savedPresetId = null
    state.savedRecording = null
    state.favorites.pushRecent(presetId)
    state.scope.launch {
        val res = VoiceProcessor.process(input, out, preset.params, state.settings.processingQuality) { p ->
            state.processingProgress = p
        }
        state.processing = false
        when (res) {
            is VoiceProcessor.Result.Success -> {
                state.lastProcessedFile?.takeIf { it.parentFile == state.previewDir }?.delete()
                state.lastProcessedFile = res.file
                state.player.play(res.file)
                if (state.settings.autoSaveProcessed) saveProcessed(state, presetId)
            }
            is VoiceProcessor.Result.Failure -> {
                state.errorKey = processErrorKey(res.message)
            }
        }
    }
}

fun processErrorKey(message: String): String = when (message) {
    "unsupported" -> "err_unsupported"
    "corrupted" -> "err_corrupt"
    "memory" -> "err_memory"
    "too_long" -> "err_too_long"
    "storage" -> "err_storage"
    else -> "err_process"
}

/** Save the current processed preview to the library once (repeat calls return the same recording). */
fun saveProcessed(state: AppState, presetId: String): com.codex.voicex.storage.Recording? {
    if (state.savedPresetId == presetId) state.savedRecording?.let { return it }
    val preset = VoicePresets.byId(presetId) ?: state.presetStore.byId(presetId) ?: return null
    val original = state.workingOriginal ?: return null
    val src = state.lastProcessedFile ?: return null
    val cat = VoicePresets.category(preset.catId)
    val rec = try {
        state.recordings.addProcessed(
            source = src,
            originalId = original.id,
            presetId = preset.id,
            effectName = preset.en,
            categoryName = cat?.en ?: "",
            durationMs = PcmFile.durationMs(src)
        )
    } catch (e: Exception) {
        state.errorKey = "err_storage"
        return null
    }
    state.savedPresetId = presetId
    state.savedRecording = rec
    if (!state.settings.keepOriginals) {
        // "keep originals" is off: the processed version replaces the original recording
        if (state.player.currentFile == state.recordings.file(original)) state.player.stop()
        state.recordings.delete(original)
        state.workingOriginal = rec
    }
    return rec
}
