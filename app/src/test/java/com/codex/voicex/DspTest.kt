package com.codex.voicex

import com.codex.voicex.audio.PcmFile
import com.codex.voicex.dsp.Dsp
import com.codex.voicex.dsp.DspParams
import com.codex.voicex.dsp.Fft
import com.codex.voicex.dsp.Spectral
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class DspTest {

    private val sr = 22050

    private fun tone(hz: Double, seconds: Double = 1.0) =
        FloatArray((sr * seconds).toInt()) { (0.5 * sin(2 * PI * hz * it / sr)).toFloat() }

    /** Dominant frequency estimated with the project FFT. */
    private fun dominantHz(x: FloatArray): Double {
        val n = 4096
        val start = (x.size - n) / 2
        val re = FloatArray(n) { x[start + it] * (0.5f - 0.5f * kotlin.math.cos(2f * PI.toFloat() * it / n)) }
        val im = FloatArray(n)
        Fft(n).transform(re, im, false)
        var best = 1; var bestMag = 0f
        for (k in 1 until n / 2) {
            val m = sqrt(re[k] * re[k] + im[k] * im[k])
            if (m > bestMag) { bestMag = m; best = k }
        }
        return best * sr.toDouble() / n
    }

    @Test fun fftRoundTrip() {
        val n = 256
        val src = FloatArray(n) { (it * 31 % 17) / 17f }
        val re = src.copyOf(); val im = FloatArray(n)
        val f = Fft(n)
        f.transform(re, im, false); f.transform(re, im, true)
        for (i in 0 until n) assertEquals(src[i].toDouble(), re[i].toDouble(), 1e-4)
    }

    @Test fun pitchShiftKeepsDurationAndMovesPitch() {
        val x = tone(200.0)
        val y = Dsp.pitchShift(x, sr, 12f)           // one octave up
        assertTrue(abs(y.size - x.size) < sr / 10)
        assertEquals(400.0, dominantHz(y), 30.0)
    }

    @Test fun formantShiftKeepsPitch() {
        val x = tone(200.0)
        val y = Spectral.shiftFormants(x, sr, 4f)
        assertEquals(x.size, y.size)
        assertEquals(200.0, dominantHz(y), 10.0)
    }

    @Test fun identityParamsKeepSignalShape() {
        val x = tone(300.0)
        val y = Dsp.render(x, sr, DspParams(normalize = false))
        assertEquals(x.size, y.size)
        assertEquals(300.0, dominantHz(y), 10.0)
    }

    @Test fun echoAndReverbAddTailWithoutNaN() {
        val x = tone(300.0, 0.5)
        val e = Dsp.echo(x, sr, 120, 0.4f, 0.5f)
        assertTrue(e.size > x.size)
        val r = Dsp.render(x, sr, DspParams(reverb = 0.8f))
        assertTrue(r.size > x.size)
        assertTrue(r.none { it.isNaN() || abs(it) > 1.0001f })
    }

    @Test fun wavRoundTripAndPeak() {
        val x = tone(440.0, 0.5)
        val f = File.createTempFile("vx", ".wav")
        try {
            PcmFile.writeWav(f, x, sr)
            val a = PcmFile.readWav(f)
            assertEquals(sr, a.sampleRate)
            assertEquals(x.size, a.samples.size)
            assertEquals(x[100].toDouble(), a.samples[100].toDouble(), 1e-3)
            assertTrue(abs(PcmFile.durationMs(f) - 500L) <= 2L)
        } finally { f.delete() }
    }

    @Test fun rawToWavReportsPeak() {
        val raw = File.createTempFile("vx", ".raw"); val wav = File.createTempFile("vx", ".wav")
        try {
            val bytes = ByteArray(2000)
            bytes[10] = 0x00; bytes[11] = 0x40   // 0x4000 = 0.5 full scale
            raw.writeBytes(bytes)
            val peak = PcmFile.rawToWav(raw, wav, sr)
            assertEquals(0.5, peak.toDouble(), 0.01)
            assertEquals(1000, PcmFile.readWav(wav).samples.size)
        } finally { raw.delete(); wav.delete() }
    }
}
