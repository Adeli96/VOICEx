package com.codex.voicex.ui.navigation

/** Central navigation destinations. Made by CODEx Team. */
sealed class Screen {
    data object Splash : Screen()
    data object Home : Screen()
    data object Recorder : Screen()
    data object Recordings : Screen()
    data object Library : Screen()
    data class Category(val id: String) : Screen()
    data class Preset(val presetId: String) : Screen()
    data object Favorites : Screen()
    data object Recent : Screen()
    data object CustomVoice : Screen()
    data object Settings : Screen()
    data object About : Screen()
}
