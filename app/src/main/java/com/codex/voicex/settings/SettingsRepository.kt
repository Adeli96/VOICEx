package com.codex.voicex.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App settings — persisted, reactive. Made by CODEx Team.
 * Defaults: language = Persian (fa), appearance = system.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("voicex_settings", Context.MODE_PRIVATE)

    /** "fa" (default) or "en" */
    var language by mutableStateOf(prefs.getString("language", "fa") ?: "fa")
        private set

    /** "light" | "dark" | "system" (default) */
    var theme by mutableStateOf(prefs.getString("theme", "system") ?: "system")
        private set

    var sampleRate by mutableIntStateOf(prefs.getInt("sampleRate", 44100))
        private set

    /** "low" | "medium" | "high" */
    var recordingQuality by mutableStateOf(prefs.getString("recQuality", "high") ?: "high")
        private set

    /** "fast" | "balanced" | "high" */
    var processingQuality by mutableStateOf(prefs.getString("procQuality", "balanced") ?: "balanced")
        private set

    /** "wav" | "m4a" — format used when sharing. */
    var outputFormat by mutableStateOf(prefs.getString("outFormat", "wav") ?: "wav")
        private set

    var keepOriginals by mutableStateOf(prefs.getBoolean("keepOriginals", true))
        private set

    var autoSaveProcessed by mutableStateOf(prefs.getBoolean("autoSave", false))
        private set

    /** "date" | "name" | "duration" */
    var librarySort by mutableStateOf(prefs.getString("libSort", "date") ?: "date")
        private set

    var showFavoritesInLibrary by mutableStateOf(prefs.getBoolean("showFavLib", true))
        private set

    fun setLanguage(v: String) { language = v; prefs.edit().putString("language", v).apply() }
    fun setTheme(v: String) { theme = v; prefs.edit().putString("theme", v).apply() }
    fun setSampleRate(v: Int) { sampleRate = v; prefs.edit().putInt("sampleRate", v).apply() }
    fun setRecordingQuality(v: String) { recordingQuality = v; prefs.edit().putString("recQuality", v).apply() }
    fun setProcessingQuality(v: String) { processingQuality = v; prefs.edit().putString("procQuality", v).apply() }
    fun setOutputFormat(v: String) { outputFormat = v; prefs.edit().putString("outFormat", v).apply() }
    fun setKeepOriginals(v: Boolean) { keepOriginals = v; prefs.edit().putBoolean("keepOriginals", v).apply() }
    fun setAutoSaveProcessed(v: Boolean) { autoSaveProcessed = v; prefs.edit().putBoolean("autoSave", v).apply() }
    fun setLibrarySort(v: String) { librarySort = v; prefs.edit().putString("libSort", v).apply() }
    fun setShowFavoritesInLibrary(v: Boolean) { showFavoritesInLibrary = v; prefs.edit().putBoolean("showFavLib", v).apply() }
}
