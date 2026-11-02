package com.codex.voicex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.voicex.localization.LocalLanguage
import com.codex.voicex.localization.s
import com.codex.voicex.storage.Recording
import com.codex.voicex.ui.AppState
import com.codex.voicex.ui.navigation.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordingsScreen(state: AppState) {
    val ctx = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    val list = remember(refreshKey, state.settings.librarySort) { state.recordings.list(state.settings.librarySort) }
    var toRename by remember { mutableStateOf<Recording?>(null) }
    var toDelete by remember { mutableStateOf<Recording?>(null) }
    var newName by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TopBar(title = s("recs_title")) { state.back() }
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) EmptyState(s("recs_empty"))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { r ->
                RecordingRow(
                    r = r,
                    isPlaying = state.player.isPlaying && state.player.currentFile == state.recordings.file(r),
                    onPlay = { state.player.toggle(state.recordings.file(r)) },
                    onApply = {
                        state.useOriginal(r)
                        state.navigate(Screen.Library)
                    },
                    onRename = { toRename = r; newName = r.name },
                    onDuplicate = { state.recordings.duplicate(r); refreshKey++ },
                    onDelete = { toDelete = r },
                    onShare = {
                        state.scope.launch {
                            val intent = state.recordings.shareIntent(r, state.settings.outputFormat)
                            if (intent != null) ctx.startActivity(intent) else state.errorKey = "err_share"
                        }
                    }
                )
            }
        }
    }

    if (toRename != null) {
        AlertDialog(
            onDismissRequest = { toRename = null },
            title = { Text(s("recs_rename")) },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(s("recs_new_name")) }) },
            confirmButton = {
                TextButton(onClick = {
                    toRename?.let { state.recordings.rename(it, newName) }
                    toRename = null; refreshKey++
                }) { Text(s("recs_ok")) }
            },
            dismissButton = { TextButton(onClick = { toRename = null }) { Text(s("recs_cancel")) } }
        )
    }

    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(s("recs_confirm_delete")) },
            confirmButton = {
                TextButton(onClick = {
                    toDelete?.let {
                        if (state.player.currentFile == state.recordings.file(it)) state.player.stop()
                        if (state.workingOriginal?.id == it.id) state.useOriginal(null)
                        state.recordings.delete(it)
                    }
                    toDelete = null; refreshKey++
                }) { Text(s("recs_ok")) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(s("recs_cancel")) } }
        )
    }
}

@Composable
private fun RecordingRow(
    r: Recording,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onApply: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val lang = LocalLanguage.current
    var menuOpen by remember { mutableStateOf(false) }
    val fmt = SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault())
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPlay) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(r.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(fmt.format(Date(r.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatElapsed(r.durationMs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, null) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(s("recs_apply_voice")) }, onClick = { menuOpen = false; onApply() }, leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) })
                    DropdownMenuItem(text = { Text(s("recs_rename")) }, onClick = { menuOpen = false; onRename() }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
                    DropdownMenuItem(text = { Text(s("recs_duplicate")) }, onClick = { menuOpen = false; onDuplicate() }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) })
                    DropdownMenuItem(text = { Text(s("recs_share")) }, onClick = { menuOpen = false; onShare() }, leadingIcon = { Icon(Icons.Filled.Share, null) })
                    DropdownMenuItem(text = { Text(s("recs_delete")) }, onClick = { menuOpen = false; onDelete() }, leadingIcon = { Icon(Icons.Filled.Delete, null) })
                }
            }
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(
                onClick = {},
                label = { Text(if (r.isProcessed) s("recs_processed") else s("recs_original"), fontSize = 11.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (r.isProcessed) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
            if (r.effectName.isNotBlank()) {
                Spacer(Modifier.width(6.dp))
                Text(r.effectName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
