package com.codex.voicex.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.codex.voicex.audio.AacEncoder
import com.codex.voicex.audio.AudioImporter
import com.codex.voicex.audio.PcmFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder

/**
 * Local audio library management.
 * Files live in the app's private storage; metadata is kept in one JSON index
 * (atomic writes, survives odd characters, easy to extend).
 * Sharing goes through a FileProvider — no storage permission and no duplicate
 * copies left in the user's Music folder.
 * Original and processed files are always kept separately.
 * Made by CODEx Team.
 */
data class Recording(
    val id: String,          // file name, e.g. "recording_001.wav"
    var name: String,        // display name
    val timestamp: Long,
    val durationMs: Long,
    val isProcessed: Boolean,
    val effectName: String,  // e.g. "Girl 18" or ""
    val categoryName: String // e.g. "Girl Voices" or ""
)

class RecordingRepository(private val context: Context) {

    private val dir = File(context.filesDir, "recordings").apply { mkdirs() }
    private val indexFile = File(context.filesDir, "recordings_index.json")
    private val meta = LinkedHashMap<String, Recording>()

    init {
        loadIndex()
        migrateLegacyPrefs()
        cleanLeftovers()
    }

    // ---------------------------------------------------------------- index

    private fun loadIndex() {
        try {
            if (!indexFile.exists()) return
            val root = JSONObject(indexFile.readText(Charsets.UTF_8))
            for (id in root.keys()) {
                val o = root.getJSONObject(id)
                meta[id] = Recording(
                    id, o.getString("name"), o.getLong("ts"), o.getLong("dur"),
                    o.getBoolean("proc"), o.optString("effect", ""), o.optString("cat", "")
                )
            }
        } catch (_: Exception) { /* corrupt index: it is rebuilt from the wav files in list() */ }
    }

    private fun saveIndex() {
        try {
            val root = JSONObject()
            for ((id, r) in meta) {
                root.put(id, JSONObject()
                    .put("name", r.name).put("ts", r.timestamp).put("dur", r.durationMs)
                    .put("proc", r.isProcessed).put("effect", r.effectName).put("cat", r.categoryName))
            }
            val tmp = File(indexFile.parentFile, "recordings_index.json.tmp")
            tmp.writeText(root.toString(), Charsets.UTF_8)
            if (!tmp.renameTo(indexFile)) { indexFile.delete(); tmp.renameTo(indexFile) }
        } catch (_: Exception) { }
    }

    /** Older versions kept metadata in SharedPreferences as "a|b|c" lines. */
    private fun migrateLegacyPrefs() {
        val prefs = context.getSharedPreferences("voicex_recording_meta", Context.MODE_PRIVATE)
        if (prefs.all.isEmpty()) return
        fun unesc(s: String) = try { URLDecoder.decode(s, "UTF-8") } catch (e: Exception) { s }
        for ((k, v) in prefs.all) {
            if (!k.startsWith("m_") || v !is String) continue
            val id = k.removePrefix("m_")
            if (meta.containsKey(id)) continue
            try {
                val t = v.split("|")
                meta[id] = Recording(id, unesc(t[0]), t[1].toLong(), t[2].toLong(), t[3].toBoolean(), unesc(t[4]), unesc(t[5]))
            } catch (_: Exception) { }
        }
        saveIndex()
        prefs.edit().clear().apply()
    }

    private fun isTemp(name: String) =
        name.startsWith("preview_") || name.startsWith("custom_preview_") || name.startsWith("tmp_")

    /** Remove preview/temp files left behind by earlier versions. */
    private fun cleanLeftovers() {
        dir.listFiles()?.filter { isTemp(it.name) }?.forEach { it.delete() }
    }

    // ----------------------------------------------------------------- query

    fun file(r: Recording): File = File(dir, r.id)

    @Synchronized
    fun list(sortBy: String = "date"): List<Recording> {
        val files = dir.listFiles { f -> f.extension == "wav" && !isTemp(f.name) } ?: return emptyList()
        var changed = false
        val names = files.map { it.name }.toSet()
        if (meta.keys.removeAll { it !in names }) changed = true
        val out = files.map { f ->
            meta[f.name] ?: run {
                // recover meta for unknown wav files (e.g. after a corrupt index)
                val r = Recording(f.name, f.nameWithoutExtension, f.lastModified(), PcmFile.durationMs(f), false, "", "")
                meta[f.name] = r; changed = true; r
            }
        }
        if (changed) saveIndex()
        return when (sortBy) {
            "name" -> out.sortedBy { it.name.lowercase() }
            "duration" -> out.sortedByDescending { it.durationMs }
            else -> out.sortedByDescending { it.timestamp }
        }
    }

    private fun nextId(base: String): String {
        var i = 1
        var id: String
        do {
            id = "${base}_${"%03d".format(i)}.wav"
            i++
        } while (File(dir, id).exists())
        return id
    }

    // ----------------------------------------------------------------- write

    @Synchronized
    private fun put(r: Recording): Recording { meta[r.id] = r; saveIndex(); return r }

    /** Import a freshly recorded (original) wav; the source file is moved. */
    fun addOriginal(source: File, durationMs: Long): Recording {
        val id = nextId("recording")
        moveInto(source, File(dir, id))
        return put(Recording(id, id.removeSuffix(".wav"), System.currentTimeMillis(), durationMs, false, "", ""))
    }

    /** Decode an audio file picked by the user (mp3, m4a, ogg, flac, wav…) into the library. */
    suspend fun importAudio(uri: Uri): Recording = withContext(Dispatchers.IO) {
        val id = nextId("import")
        val dest = File(dir, id)
        val duration = AudioImporter.importToWav(context, uri, dest)
        put(Recording(id, id.removeSuffix(".wav"), System.currentTimeMillis(), duration, false, "", ""))
    }

    /**
     * Save a processed version; original stays untouched. e.g. recording_001_girl_18.wav
     * The source file is copied, never consumed — the preview can still be played afterwards.
     */
    fun addProcessed(source: File, originalId: String?, presetId: String, effectName: String, categoryName: String, durationMs: Long): Recording {
        val base = (originalId ?: "recording").removeSuffix(".wav")
        val id = nextId("${base}_${presetId.replace(Regex("[^A-Za-z0-9_]"), "_")}")
        source.copyTo(File(dir, id), overwrite = true)
        return put(Recording(id, id.removeSuffix(".wav"), System.currentTimeMillis(), durationMs, true, effectName, categoryName))
    }

    private fun moveInto(source: File, dest: File) {
        if (!source.renameTo(dest)) { source.copyTo(dest, overwrite = true); source.delete() }
    }

    fun rename(r: Recording, newName: String) {
        r.name = newName.trim().ifBlank { r.name }
        put(r)
    }

    @Synchronized
    fun delete(r: Recording) {
        try { file(r).delete() } catch (_: Exception) {}
        meta.remove(r.id); saveIndex()
    }

    fun duplicate(r: Recording): Recording? {
        return try {
            val src = file(r)
            if (!src.exists()) return null
            val id = nextId(r.id.removeSuffix(".wav") + "_copy")
            src.copyTo(File(dir, id), overwrite = true)
            put(Recording(id, r.name + " (copy)", System.currentTimeMillis(), r.durationMs, r.isProcessed, r.effectName, r.categoryName))
        } catch (e: Exception) { null }
    }

    fun totalSizeBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    fun freeSpaceBytes(): Long = dir.usableSpace

    // ----------------------------------------------------------------- share

    /**
     * Build a share chooser for [r]. [format] is "wav" or "m4a" (AAC, much smaller).
     * Encoding runs on a background thread; returns null on failure.
     */
    suspend fun shareIntent(r: Recording, format: String = "wav"): Intent? = withContext(Dispatchers.IO) {
        try {
            val src = file(r)
            if (!src.exists()) return@withContext null
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            shareDir.listFiles()?.forEach { it.delete() }
            val safe = r.name.replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { "voicex" }
            val m4a = format == "m4a"
            val target = File(shareDir, safe + if (m4a) ".m4a" else ".wav")
            if (m4a) AacEncoder.wavToM4a(src, target) else src.copyTo(target, overwrite = true)

            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", target)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = if (m4a) "audio/mp4" else "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VoiceX — Made by CODEx Team")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(send, "VoiceX").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } catch (e: Exception) { null }
    }
}
