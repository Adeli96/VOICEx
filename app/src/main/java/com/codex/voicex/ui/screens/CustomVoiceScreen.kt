package com.codex.voicex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.voicex.dsp.DspParams
import com.codex.voicex.dsp.VoiceProcessor
import com.codex.voicex.localization.s
import com.codex.voicex.ui.AppState
import com.codex.voicex.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CustomVoiceScreen(state: AppState) {
    var pitch by remember { mutableStateOf(0f) }
    var formant by remember { mutableStateOf(0f) }
    var speed by remember { mutableStateOf(1f) }
    var bass by remember { mutableStateOf(0f) }
    var treble by remember { mutableStateOf(0f) }
    var reverb by remember { mutableStateOf(0f) }
    var echo by remember { mutableStateOf(0f) }
    var delayMs by remember { mutableStateOf(0f) }
    var distortion by remember { mutableStateOf(0f) }
    var chorus by remember { mutableStateOf(0f) }
    var flanger by remember { mutableStateOf(0f) }
    var phaser by remember { mutableStateOf(0f) }
    var volume by remember { mutableStateOf(1f) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetName by remember { mutableStateOf("") }
    var savedMsg by remember { mutableStateOf<String?>(null) }

    val params = DspParams(
        pitchSemitones = pitch,
        formantSemitones = formant,
        speed = speed,
        bassDb = bass,
        trebleDb = treble,
        drive = distortion,
        chorus = chorus,
        flanger = flanger,
        phaser = phaser,
        delayMs = delayMs.toInt(),
        delayFeedback = if (echo > 0f) 0.45f else 0f,
        delayMix = echo,
        reverb = reverb,
        volume = volume
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TopBar(title = s("custom_title")) { state.back() }
        Spacer(Modifier.height(8.dp))

        val original = state.workingOriginal
        if (original == null) {
            Text(
                s("custom_need_recording"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            )
            Button(
                onClick = { state.navigate(Screen.Recorder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text(s("home_record")) }
            return@Column
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SliderRow(s("custom_pitch"), pitch, -12f..12f, "%.1f".format(pitch)) { pitch = it }
            SliderRow(s("custom_formant"), formant, -6f..6f, "%.1f".format(formant)) { formant = it }
            SliderRow(s("custom_speed"), speed, 0.5f..2f, "%.2fx".format(speed)) { speed = it }
            SliderRow(s("custom_bass"), bass, -12f..12f, "%.0f dB".format(bass)) { bass = it }
            SliderRow(s("custom_treble"), treble, -12f..12f, "%.0f dB".format(treble)) { treble = it }
            SliderRow(s("custom_reverb"), reverb, 0f..1f, "%.0f%%".format(reverb * 100)) { reverb = it }
            SliderRow(s("custom_echo"), echo, 0f..1f, "%.0f%%".format(echo * 100)) { echo = it }
            SliderRow(s("custom_delay"), delayMs, 0f..800f, "%.0f ms".format(delayMs)) { delayMs = it }
            SliderRow(s("custom_distortion"), distortion, 0f..1f, "%.0f%%".format(distortion * 100)) { distortion = it }
            SliderRow(s("custom_chorus"), chorus, 0f..1f, "%.0f%%".format(chorus * 100)) { chorus = it }
            SliderRow(s("custom_flanger"), flanger, 0f..1f, "%.0f%%".format(flanger * 100)) { flanger = it }
            SliderRow(s("custom_phaser"), phaser, 0f..1f, "%.0f%%".format(phaser * 100)) { phaser = it }
            SliderRow(s("custom_volume"), volume, 0f..1.5f, "%.0f%%".format(volume * 100)) { volume = it }
        }

        if (state.processing) {
            LinearProgressIndicator(
                progress = { state.processingProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    if (state.player.isPlaying) state.player.stop()
                    else runCustomPreview(state, params)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(if (state.player.isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(6.dp)); Text(s("custom_preview"))
            }
            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text(s("custom_save"))
            }
        }

        savedMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(s("custom_save")) },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text(s("custom_name")) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = presetName.trim().ifBlank { "My Voice ${state.presetStore.customPresets.size + 1}" }
                    state.presetStore.add(name, name, params)
                    savedMsg = s("custom_saved") + ": " + name
                    presetName = ""
                    showSaveDialog = false
                }) { Text(s("recs_ok")) }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text(s("recs_cancel")) } }
        )
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, valueLabel: String, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            Text(valueLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

private fun runCustomPreview(state: AppState, params: DspParams) {
    val original = state.workingOriginal ?: return
    val input = state.recordings.file(original)
    val out = File(state.previewDir, "custom_preview_${System.currentTimeMillis()}.wav")
    state.errorKey = null
    state.processing = true
    state.processingProgress = 0f
    state.scope.launch {
        val res = VoiceProcessor.process(input, out, params, state.settings.processingQuality) { p -> state.processingProgress = p }
        state.processing = false
        when (res) {
            is VoiceProcessor.Result.Success -> {
                state.lastProcessedFile?.takeIf { it.parentFile == state.previewDir }?.delete()
                state.lastProcessedFile = res.file
                state.player.play(res.file)
            }
            is VoiceProcessor.Result.Failure -> {
                state.errorKey = processErrorKey(res.message)
            }
        }
    }
}
