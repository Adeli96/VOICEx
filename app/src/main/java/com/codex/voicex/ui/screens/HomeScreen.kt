package com.codex.voicex.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.codex.voicex.audio.AudioImporter
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.voicex.localization.s
import com.codex.voicex.ui.AppState
import com.codex.voicex.ui.navigation.Screen

@Composable
fun HomeScreen(state: AppState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .padding(top = 32.dp, bottom = 20.dp)
    ) {
        // header
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    s("app_name"),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    s("subtitle"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { state.navigate(Screen.Settings) }) {
                Icon(Icons.Filled.Settings, null, tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        Spacer(Modifier.height(24.dp))

        // main record button
        RecordHero(onClick = { state.navigate(Screen.Recorder) })

        Spacer(Modifier.height(6.dp))
        Text(
            s("home_tap_record"),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                state.scope.launch {
                    try {
                        val rec = state.recordings.importAudio(uri)
                        state.useOriginal(rec)
                        state.navigate(Screen.Library)
                    } catch (e: AudioImporter.ImportException) {
                        state.errorKey = e.key
                    } catch (e: Exception) {
                        state.errorKey = "err_import"
                    }
                }
            }
        }

        val items = listOf(
            HomeTile("home_import", Icons.Filled.Add, MaterialTheme.colorScheme.tertiary) {
                state.errorKey = null
                importLauncher.launch(arrayOf("audio/*"))
            },
            HomeTile("home_library", Icons.Filled.LibraryMusic, MaterialTheme.colorScheme.primary) { state.navigate(Screen.Library) },
            HomeTile("home_recordings", Icons.Filled.Album, MaterialTheme.colorScheme.secondary) { state.navigate(Screen.Recordings) },
            HomeTile("home_favorites", Icons.Filled.Favorite, MaterialTheme.colorScheme.tertiary) { state.navigate(Screen.Favorites) },
            HomeTile("home_recent", Icons.Filled.History, MaterialTheme.colorScheme.primary) { state.navigate(Screen.Recent) },
            HomeTile("custom_title", Icons.Filled.Tune, MaterialTheme.colorScheme.secondary) { state.navigate(Screen.CustomVoice) },
            HomeTile("home_about", Icons.Filled.Info, MaterialTheme.colorScheme.tertiary) { state.navigate(Screen.About) }
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items) { tile -> HomeTileCard(tile) }
        }

        if (state.errorKey != null) {
            Text(
                s(state.errorKey ?: ""),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            s("made_by"),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private data class HomeTile(val label: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)

@Composable
private fun HomeTileCard(tile: HomeTile) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { tile.onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tile.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tile.icon, null, tint = tile.color)
            }
            Spacer(Modifier.weight(1f))
            Text(
                s(tile.label),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RecordHero(onClick: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        0.98f, 1.05f,
        infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(150.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = s("home_record"),
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
