package com.codex.voicex.presets

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.codex.voicex.dsp.DspParams
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Persists user-created custom voice presets (Custom Voice editor saves).
 * Simple flat serialization — one preset per line. Made by CODEx Team.
 */
class PresetStore(context: Context) {

    private val prefs = context.getSharedPreferences("voicex_custom_presets", Context.MODE_PRIVATE)
    val customPresets = mutableStateListOf<VoicePreset>()

    init { load() }

    private fun esc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun unesc(s: String) = try { URLDecoder.decode(s, "UTF-8") } catch (e: Exception) { s }

    private fun serialize(p: VoicePreset): String {
        val d = p.params
        return listOf(
            esc(p.id), esc(p.en), esc(p.fa),
            d.pitchSemitones, d.formantSemitones, d.speed, d.bassDb, d.trebleDb, d.lowpassHz,
            d.drive, d.bitDepth, d.bitDownsample, d.ringModHz, d.tremoloHz, d.tremoloDepth,
            d.chorus, d.flanger, d.phaser, d.delayMs, d.delayFeedback, d.delayMix,
            d.reverb, d.noiseLevel, d.gate, d.volume, d.normalize
        ).joinToString("|")
    }

    private fun parse(line: String): VoicePreset? {
        return try {
            val t = line.split("|")
            if (t.size < 26) return null
            val d = DspParams(
                t[3].toFloat(), t[4].toFloat(), t[5].toFloat(), t[6].toFloat(), t[7].toFloat(),
                t[8].toInt(), t[9].toFloat(), t[10].toInt(), t[11].toInt(), t[12].toFloat(),
                t[13].toFloat(), t[14].toFloat(), t[15].toFloat(), t[16].toFloat(), t[17].toFloat(),
                t[18].toInt(), t[19].toFloat(), t[20].toFloat(), t[21].toFloat(), t[22].toFloat(),
                t[23].toFloat(), t[24].toFloat(), t[25].toBoolean()
            )
            VoicePreset(unesc(t[0]), "custom", unesc(t[1]), unesc(t[2]), d, custom = true)
        } catch (e: Exception) { null }
    }

    fun load() {
        customPresets.clear()
        val all = prefs.getStringSet("presets", emptySet()) ?: emptySet()
        all.mapNotNull { parse(it) }.sortedBy { it.en }.forEach { customPresets.add(it) }
    }

    private fun persist() {
        prefs.edit().putStringSet("presets", customPresets.map { serialize(it) }.toSet()).apply()
    }

    /** Save a new custom preset. Returns the created preset. */
    fun add(name: String, nameFa: String, params: DspParams): VoicePreset {
        val id = "custom_${System.currentTimeMillis()}"
        val p = VoicePreset(id, "custom", name, nameFa.ifBlank { name }, params, custom = true)
        customPresets.add(p)
        persist()
        return p
    }

    fun delete(id: String) {
        customPresets.removeAll { it.id == id }
        persist()
    }

    fun byId(id: String): VoicePreset? = customPresets.firstOrNull { it.id == id }
}
