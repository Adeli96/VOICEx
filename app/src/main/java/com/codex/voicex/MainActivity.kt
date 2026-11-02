package com.codex.voicex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.codex.voicex.localization.LocalLanguage
import com.codex.voicex.ui.AppState
import com.codex.voicex.ui.navigation.Screen
import com.codex.voicex.ui.screens.*
import com.codex.voicex.ui.theme.VoiceXTheme

/**
 * VoiceX — Offline Voice Changer — Made by CODEx Team.
 * Single-Activity architecture; Compose owns all navigation.
 */
class MainActivity : ComponentActivity() {

    private lateinit var state: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            state = remember { AppState(applicationContext, scope) }

            // language + RTL propagation
            val language = state.settings.language
            val direction = if (language == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr

            VoiceXTheme(themePref = state.settings.theme) {
                CompositionLocalProvider(
                    LocalLanguage provides language,
                    LocalLayoutDirection provides direction
                ) {
                    BackHandler(enabled = state.current != Screen.Home && state.current != Screen.Splash) {
                        if (!state.back()) finish()
                    }
                    when (val screen = state.current) {
                        Screen.Splash -> SplashScreen(onFinished = { state.resetTo(Screen.Home) })
                        Screen.Home -> HomeScreen(state)
                        Screen.Recorder -> RecorderScreen(state)
                        Screen.Recordings -> RecordingsScreen(state)
                        Screen.Library -> LibraryScreen(state)
                        is Screen.Category -> CategoryScreen(state, screen.id)
                        is Screen.Preset -> PresetScreen(state, screen.presetId)
                        Screen.Favorites -> FavoritesScreen(state)
                        Screen.Recent -> RecentScreen(state)
                        Screen.CustomVoice -> CustomVoiceScreen(state)
                        Screen.Settings -> SettingsScreen(state)
                        Screen.About -> AboutScreen(state)
                    }
                }
            }

            LaunchedEffect(Unit) { /* eager warm-up done inside AppState */ }
        }
    }

    override fun onStop() {
        if (::state.isInitialized) state.player.pause()
        super.onStop()
    }

    override fun onDestroy() {
        if (::state.isInitialized) state.release()
        super.onDestroy()
    }
}
