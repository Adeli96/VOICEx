package com.codex.voicex.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.codex.voicex.localization.s
import com.codex.voicex.recording.RecorderController
import com.codex.voicex.ui.AppState
import com.codex.voicex.ui.components.LiveWaveform
import com.codex.voicex.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun RecorderScreen(state: AppState) {
    val ctx = LocalContext.current
    var permissionOk by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionOk = granted
        if (granted) startRecording(state) else state.errorKey = "perm_mic_denied"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        TopBar(title = s("rec_title")) { state.back() }
        Spacer(Modifier.height(24.dp))

        val elapsedText = formatElapsed(state.recorder.elapsedMs)

        // display timer
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    elapsedText,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                val stateText = when (state.recorder.state) {
                    RecorderController.State.RECORDING -> "● " + s("rec_recording")
                    RecorderController.State.PAUSED -> s("rec_pause")
                    else -> s("home_tap_record")
                }
                Text(stateText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(20.dp))
        LiveWaveform(
            state.recorder.waveform.toList(),
            Modifier
                .fillMaxWidth()
                .height(96.dp)
        )
        Spacer(Modifier.height(28.dp))

        // controls
        when (state.recorder.state) {
            RecorderController.State.IDLE -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    BigCircleButton(icon = Icons.Filled.Mic, color = MaterialTheme.colorScheme.primary, size = 108.dp) {
                        if (permissionOk) startRecording(state)
                        else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
            RecorderController.State.RECORDING -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallCircleButton(Icons.Filled.Close, MaterialTheme.colorScheme.error) {
                        state.recorder.cancel()
                    }
                    BigCircleButton(Icons.Filled.Stop, MaterialTheme.colorScheme.primary, 96.dp) {
                        finishRecording(state)
                    }
                    SmallCircleButton(Icons.Filled.Pause, MaterialTheme.colorScheme.secondary) {
                        state.recorder.pause()
                    }
                }
            }
            RecorderController.State.PAUSED -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallCircleButton(Icons.Filled.Close, MaterialTheme.colorScheme.error) {
                        state.recorder.cancel()
                    }
                    BigCircleButton(Icons.Filled.PlayArrow, MaterialTheme.colorScheme.primary, 96.dp) {
                        state.recorder.resume()
                    }
                    SmallCircleButton(Icons.Filled.Save, MaterialTheme.colorScheme.secondary) {
                        finishRecording(state)
                    }
                }
            }
        }

        if (state.recorder.limitReached) {
            Text(
                s("rec_limit"),
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        AnimatedVisibility(state.errorKey != null) {
            Text(
                s(state.errorKey ?: ""),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun startRecording(state: AppState) {
    state.errorKey = null
    if (!state.recorder.start()) state.errorKey = "err_${state.recorder.error ?: "rec_failed"}"
}

/** Stop capture, convert off the main thread, add to the library and open the voice library. */
private fun finishRecording(state: AppState) {
    state.scope.launch {
        val file = state.recorder.finish()
        if (file != null) {
            val rec = try {
                withContext(Dispatchers.IO) { state.recordings.addOriginal(file, state.recorder.elapsedMs) }
            } catch (e: Exception) { null }
            if (rec != null) {
                state.useOriginal(rec)
                state.navigate(Screen.Library)
            } else {
                state.errorKey = "err_storage"
            }
        } else if (state.recorder.error != null) {
            state.errorKey = "err_${state.recorder.error}"
        }
    }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, s("back"), tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun BigCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(size * 0.45f))
    }
}

@Composable
fun SmallCircleButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
    }
}

fun formatElapsed(ms: Long): String {
    val total = ms / 1000
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}
