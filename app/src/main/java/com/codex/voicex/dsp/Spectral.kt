package com.codex.voicex.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Spectral tools: radix-2 FFT and a cepstral spectral-envelope (formant) shifter.
 * Pure Kotlin, offline. Made by CODEx Team.
 *
 * The formant shifter works like a time-varying filter: for each STFT frame it
 * estimates the smooth spectral envelope (cepstral liftering), warps that envelope
 * along the frequency axis and applies the ratio as a gain. Phase is untouched, so
 * the pitch (harmonic structure) stays where it is while the vocal-tract colour
 * moves up or down.
 */
class Fft(val n: Int) {
    private val cosT = FloatArray(n / 2) { cos(2.0 * PI * it / n).toFloat() }
    private val sinT = FloatArray(n / 2) { sin(2.0 * PI * it / n).toFloat() }

    init { require(n >= 2 && (n and (n - 1)) == 0) { "FFT size must be a power of two" } }

    fun transform(re: FloatArray, im: FloatArray, inverse: Boolean) {
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val half = len shr 1
            val step = n / len
            var i = 0
            while (i < n) {
                var k = 0
                for (jj in 0 until half) {
                    val wr = cosT[k]
                    val wi = if (inverse) sinT[k] else -sinT[k]
                    val a = i + jj
                    val b = a + half
                    val xr = re[b] * wr - im[b] * wi
                    val xi = re[b] * wi + im[b] * wr
                    re[b] = re[a] - xr; im[b] = im[a] - xi
                    re[a] += xr; im[a] += xi
                    k += step
                }
                i += len
            }
            len = len shl 1
        }
        if (inverse) {
            val inv = 1f / n
            for (i in 0 until n) { re[i] *= inv; im[i] *= inv }
        }
    }
}

object Spectral {

    /**
     * Shift the spectral envelope (formants) by [semitones] without touching pitch.
     * Positive = brighter / smaller vocal tract, negative = darker / bigger.
     * @param fftSize 1024 (balanced) or 2048 (high quality).
     */
    fun shiftFormants(x: FloatArray, sr: Int, semitones: Float, fftSize: Int = 1024): FloatArray {
        if (kotlin.math.abs(semitones) < 0.05f || x.size < fftSize) return x
        val n = fftSize
        val hop = n / 4
        val bins = n / 2 + 1
        val ratio = 2.0.pow(semitones / 12.0).toFloat()
        val fft = Fft(n)

        // sqrt-Hann analysis + synthesis window, hop = n/4  ->  sum of squares = 2
        val win = FloatArray(n) { sqrt(0.5f - 0.5f * cos(2f * PI.toFloat() * it / n)) }
        // cepstral lifter: keep quefrencies below ~ sr/500 (separates envelope from pitch harmonics)
        val lifter = max(12, min(n / 4, sr / 500))

        val padded = FloatArray(x.size + 2 * n)
        System.arraycopy(x, 0, padded, n, x.size)
        val out = FloatArray(padded.size)

        val re = FloatArray(n)
        val im = FloatArray(n)
        val cr = FloatArray(n)
        val ci = FloatArray(n)
        val env = FloatArray(bins)
        val gain = FloatArray(bins)

        var pos = 0
        while (pos + n <= padded.size) {
            for (i in 0 until n) { re[i] = padded[pos + i] * win[i]; im[i] = 0f }
            fft.transform(re, im, false)

            // log magnitude -> real cepstrum
            for (k in 0 until n) {
                val kk = if (k < bins) k else n - k
                val m = sqrt(re[kk] * re[kk] + im[kk] * im[kk]) + 1e-7f
                cr[k] = ln(m)
                ci[k] = 0f
            }
            fft.transform(cr, ci, true)
            // keep low quefrencies only (symmetric)
            for (q in 0 until n) {
                val qq = if (q <= n / 2) q else n - q
                if (qq > lifter) { cr[q] = 0f; ci[q] = 0f }
                else if (qq == lifter) { cr[q] *= 0.5f; ci[q] *= 0.5f }
            }
            fft.transform(cr, ci, false)
            for (k in 0 until bins) env[k] = exp(cr[k])

            // warped envelope: newEnv[k] = env[k / ratio]
            for (k in 0 until bins) {
                val src = k / ratio
                val s0 = src.toInt()
                val g = if (s0 >= bins - 1) env[bins - 1] else {
                    val f = src - s0
                    env[s0] * (1f - f) + env[s0 + 1] * f
                }
                gain[k] = (g / (env[k] + 1e-9f)).coerceIn(0.1f, 10f)
            }

            for (k in 0 until n) {
                val kk = if (k < bins) k else n - k
                re[k] *= gain[kk]; im[k] *= gain[kk]
            }
            fft.transform(re, im, true)
            for (i in 0 until n) out[pos + i] += re[i] * win[i] * 0.5f
            pos += hop
        }
        return out.copyOfRange(n, n + x.size)
    }
}
