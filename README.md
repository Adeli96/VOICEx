# VoiceX — Offline Voice Changer
### Made by CODEx Team

A complete Android application project.  100% offline. No internet permission is declared. No cloud, no APIs, no Firebase, no root, no Shizuku, no ADB, no virtual microphone, no system microphone modification.

## Open in Android Studio
Requires Android Studio Iguana+ (AGP 8.5, Kotlin 2.0, JDK 17, min SDK 24, target 34).

1. **File → Open** → select the project root (`voicex/`)
2. Let Gradle sync; then **Run** on any device or emulator with API 24+.
3. On first launch the app appears in **Persian (RTL)** with **System** theme.

## Architecture
```
app/src/main/java/com/codex/voicex/
├── MainActivity.kt          single-Activity host + navigation
├── audio/                   PcmFile (streaming WAV), AudioImporter (decode any audio), AacEncoder (m4a)
├── dsp/                     offline DSP engine (pure Kotlin)
│   ├── Spectral.kt          FFT + formant shifter
│   ├── Dsp.kt               SOLA time-stretch, pitch, filters, reverb, echo, chorus, flanger, phaser, ring-mod, bitcrush, distortion, tremolo
│   └── VoiceProcessor.kt    async processing entry point
├── recording/               AudioRecord-based capture with pause/resume/waveform
├── playback/                MediaPlayer wrapper with position + seek
├── presets/                 8 categories · 112 presets · user PresetStore
├── favorites/               favorites + recent-effects store
├── storage/                 recordings library (scoped-storage, Music share)
├── settings/                persistent settings (language, theme, sort, quality)
├── localization/            English + Persian (extensible)
└── ui/
    ├── theme/               Material3 light/dark/system
    ├── components/          waveform composables
    ├── navigation/          sealed Screen graph
    ├── AppState.kt          global reactive state holder
    └── screens/             Splash, Home, Recorder, Library, Category, Preset, Custom Voice, Recordings, Favorites, Recent, Settings, About
```

## Behavior highlights
- **Permissions:** `RECORD_AUDIO` (requested lazily on first Record tap) plus foreground-service/notification permissions so a recording survives screen-off and app switching. No `INTERNET`, no storage permissions.
- **Recording:** streamed to disk, converted to WAV off the main thread, capped at ~16M samples (~6 min @ 44.1 kHz) to protect memory. Sample rate follows Settings (Low 16 kHz / Medium 22.05 kHz / High 44.1 kHz).
- **Import:** any audio the device can decode (mp3, m4a, ogg, flac, wav…) via the system picker → mono WAV (MediaExtractor/MediaCodec).
- **Export / share:** WAV or M4A (AAC) selectable in Settings, shared through a FileProvider (no leftover copies in Music).
- **DSP:** SOLA pitch shift (normalised correlation) + cepstral **formant shifter** (pitch and vocal-tract colour are now independent), smooth noise gate, feedback echo with tail, reverb with tail.
  - Quality setting: *Fast* = legacy summed pitch shift, *Balanced* = SOLA + 1024-pt formant shifter, *High* = finer SOLA search + 2048-pt formant shifter.
  - `formantSemitones != 0` means the total shift of the formants relative to the original voice; `0` means formants simply follow the pitch.
- **Settings wired:** recording quality/sample rate, processing quality, output format, keep originals (off = processed version replaces the original), auto-save processed (off by default).
- **Library:** metadata in a JSON index (migrates the old SharedPreferences format); previews live in cache, never in the library.
- **Persian default + RTL, Light/Dark/System themes, 44 categories, user presets.**
- **Release build:** R8 minify + resource shrinking enabled. Unit tests for DSP/WAV in `app/src/test` (`./gradlew test`).

## Age-label & animal disclaimer
Age-labeled voices (e.g. *Girl 18*, *Man 25*) are **approximate stylistic presets**, not claims about a real person's biological age. Presets imitating children/minors were intentionally removed.  Animal presets are DSP character approximations, not scientifically accurate animal synthesis.

## Category list (8)
Girl · Women · Boy · Men · Elderly · Animal · Cat · Dog · Bird · Wild · Farm · Funny Animals · Robot · Android · Cyborg · Alien · Monster · Ghost · Fantasy · Fairy · Wizard · Zombie · Vampire · Hero · Villain · Cartoon · Funny · Gaming · NPC · Radio · Telephone · Walkie-Talkie · Megaphone · Stadium · Cave · Tunnel · Underwater · Space · Electronic · Glitch · Sci-Fi · Horror · Music/Audio Effects · Custom Voice.


---
## Build with GitHub Actions
Push this repository to GitHub — `.github/workflows/android.yml` builds `VoiceX-debug.apk`
(and a signed release APK when keystore secrets exist). Step-by-step guide (Persian): `docs/GITHUB_BUILD_FA.md`.
The advanced browser version (112 voices, 50-min streaming export) is in `docs/web-demo/voicex-studio.html`.

## Catalog
8 categories (Girls & Women, Men, Animals, Robots & Space, Fantasy, Places, Communication, Effects), 10+ voices each,
plus user-saved custom voices. No presets imitating children are included.
