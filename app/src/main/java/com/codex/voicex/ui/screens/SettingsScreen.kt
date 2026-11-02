package com.codex.voicex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.voicex.localization.s
import com.codex.voicex.ui.AppState

@Composable
fun SettingsScreen(state: AppState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(title = s("set_title")) { state.back() }
        Spacer(Modifier.height(12.dp))

        Section(s("set_language")) {
            SegmentedTwo(
                left = s("set_language_fa"), right = s("set_language_en"),
                selected = state.settings.language,
                leftValue = "fa", rightValue = "en"
            ) { state.settings.setLanguage(it) }
        }

        Section(s("set_appearance")) {
            SegmentedThree(
                items = listOf("light" to s("set_light"), "dark" to s("set_dark"), "system" to s("set_system")),
                selected = state.settings.theme
            ) { state.settings.setTheme(it) }
        }

        Section(s("set_audio")) {
            SettingRow(s("set_format"), "WAV (PCM 16-bit)")
            RadioLine(
                label = s("set_output"),
                options = listOf("wav" to "WAV", "m4a" to "M4A (AAC)"),
                selected = state.settings.outputFormat
            ) { state.settings.setOutputFormat(it) }
            RadioLine(
                label = s("set_quality"),
                options = listOf(
                    "low" to s("q_low"),
                    "medium" to s("q_medium"),
                    "high" to s("q_high")
                ),
                selected = state.settings.recordingQuality
            ) {
                state.settings.setRecordingQuality(it)
                state.settings.setSampleRate(
                    when (it) { "low" -> 16000; "medium" -> 22050; else -> 44100 }
                )
            }
            SettingRow(s("set_sr"), "${state.settings.sampleRate} Hz")
        }

        Section(s("set_processing")) {
            RadioLine(
                label = s("set_proc_quality"),
                options = listOf(
                    "fast" to s("q_fast"),
                    "balanced" to s("q_balanced"),
                    "high" to s("q_high")
                ),
                selected = state.settings.processingQuality
            ) { state.settings.setProcessingQuality(it) }
            SwitchRow(s("set_keep_orig"), state.settings.keepOriginals) { state.settings.setKeepOriginals(it) }
            SwitchRow(s("set_auto_save"), state.settings.autoSaveProcessed) { state.settings.setAutoSaveProcessed(it) }
        }

        Section(s("set_library")) {
            RadioLine(
                label = s("set_sort"),
                options = listOf(
                    "date" to s("set_sort_date"),
                    "name" to s("set_sort_name"),
                    "duration" to s("set_sort_duration")
                ),
                selected = state.settings.librarySort
            ) { state.settings.setLibrarySort(it) }
            SwitchRow(s("set_show_fav"), state.settings.showFavoritesInLibrary) { state.settings.setShowFavoritesInLibrary(it) }
        }

        Section(s("set_about")) {
            SettingRow(s("app_name"), s("subtitle"))
            SettingRow(s("made_by"), "CODEx Team")
            SettingRow(s("set_version"), "1.0.0")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            s("made_by"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun AboutScreen(state: AppState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TopBar(title = s("home_about")) { state.back() }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(s("app_name"), fontSize = 40.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(6.dp))
            Text(s("subtitle"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text(s("made_by"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("v1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        content = content
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun RadioLine(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (v, lab) ->
                FilterChip(
                    selected = selected == v,
                    onClick = { onSelect(v) },
                    label = { Text(lab, fontSize = 12.sp) }
                )
            }
        }
    }
}

@Composable
private fun SegmentedTwo(left: String, right: String, selected: String, leftValue: String, rightValue: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(4.dp)
    ) {
        SegmentedItem(left, selected == leftValue, Modifier.weight(1f)) { onSelect(leftValue) }
        SegmentedItem(right, selected == rightValue, Modifier.weight(1f)) { onSelect(rightValue) }
    }
}

@Composable
private fun SegmentedThree(items: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(4.dp)
    ) {
        items.forEach { (v, lab) ->
            SegmentedItem(lab, selected == v, Modifier.weight(1f)) { onSelect(v) }
        }
    }
}

@Composable
private fun SegmentedItem(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
