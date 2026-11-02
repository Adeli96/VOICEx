package com.codex.voicex.favorites

import android.content.Context
import androidx.compose.runtime.mutableStateListOf

/**
 * Favorite presets + recently used effects. Persisted in SharedPreferences.
 * Made by CODEx Team.
 */
class FavoritesStore(context: Context) {

    private val prefs = context.getSharedPreferences("voicex_favorites", Context.MODE_PRIVATE)

    val favorites = mutableStateListOf<String>()
    val recents = mutableStateListOf<String>()

    init {
        favorites.addAll(prefs.getStringSet("fav", emptySet()) ?: emptySet())
        recents.addAll(
            (prefs.getString("recent", "") ?: "").split(",").filter { it.isNotBlank() }
        )
    }

    fun isFavorite(id: String) = favorites.contains(id)

    fun toggle(id: String) {
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        prefs.edit().putStringSet("fav", favorites.toSet()).apply()
    }

    fun pushRecent(id: String) {
        recents.remove(id)
        recents.add(0, id)
        while (recents.size > 20) recents.removeAt(recents.size - 1)
        prefs.edit().putString("recent", recents.joinToString(",")).apply()
    }
}
