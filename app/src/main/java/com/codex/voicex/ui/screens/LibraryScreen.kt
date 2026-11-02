package com.codex.voicex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.voicex.localization.LocalLanguage
import com.codex.voicex.localization.s
import com.codex.voicex.presets.VoiceCategory
import com.codex.voicex.presets.VoicePreset
import com.codex.voicex.presets.VoicePresets
import com.codex.voicex.ui.AppState
import com.codex.voicex.ui.navigation.Screen

@Composable
fun LibraryScreen(state: AppState) {
    var query by remember { mutableStateOf("") }
    val lang = LocalLanguage.current
    val results = remember(query) { VoicePresets.search(query, state.presetStore.customPresets.toList()) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp)
    ) {
        TopBar(title = s("lib_title")) { state.back() }
        Spacer(Modifier.height(12.dp))

        // search
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text(s("lib_search")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(12.dp))

        if (query.isNotBlank()) {
            if (results.isEmpty()) {
                EmptyState(s("lib_empty_search"))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.id }) { p ->
                        PresetRow(p, state) { state.navigate(Screen.Preset(p.id)) }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(VoicePresets.categories, key = { it.id }) { cat ->
                    CategoryCard(cat) {
                        if (cat.id == "custom") state.navigate(Screen.CustomVoice)
                        else state.navigate(Screen.Category(cat.id))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(cat: VoiceCategory, onClick: () -> Unit) {
    val lang = LocalLanguage.current
    val count = VoicePresets.byCategory(cat.id).size
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(cat.icon, fontSize = 26.sp)
            Spacer(Modifier.weight(1f))
            Text(
                if (lang == "fa") cat.fa else cat.en,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Text(
                "$count " + s("lib_category_count"),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PresetRow(p: VoicePreset, state: AppState, onClick: () -> Unit) {
    val lang = LocalLanguage.current
    val cat = VoicePresets.category(p.catId)
    val isFav = state.favorites.isFavorite(p.id)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(cat?.icon ?: "🎙️", fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                p.name(lang == "fa"),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                cat?.let { if (lang == "fa") it.fa else it.en } ?: "",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { state.favorites.toggle(p.id) }) {
            Icon(
                if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                null,
                tint = if (isFav) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(msg: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(msg, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
fun CategoryScreen(state: AppState, catId: String) {
    val lang = LocalLanguage.current
    val cat = VoicePresets.category(catId)
    val presets = VoicePresets.byCategory(catId)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TopBar(title = (cat?.let { if (lang == "fa") it.fa else it.en } ?: "")) { state.back() }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets, key = { it.id }) { p ->
                PresetRow(p, state) { state.navigate(Screen.Preset(p.id)) }
            }
        }
    }
}

@Composable
fun FavoritesScreen(state: AppState) {
    val lang = LocalLanguage.current
    val list = state.favorites.favorites.mapNotNull {
        VoicePresets.byId(it) ?: state.presetStore.byId(it)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TopBar(title = s("fav_title")) { state.back() }
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) EmptyState(s("fav_empty"))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { p -> PresetRow(p, state) { state.navigate(Screen.Preset(p.id)) } }
        }
    }
}

@Composable
fun RecentScreen(state: AppState) {
    val list = state.favorites.recents.mapNotNull {
        VoicePresets.byId(it) ?: state.presetStore.byId(it)
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        TopBar(title = s("recent_title")) { state.back() }
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) EmptyState(s("recent_empty"))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { p -> PresetRow(p, state) { state.navigate(Screen.Preset(p.id)) } }
        }
    }
}
