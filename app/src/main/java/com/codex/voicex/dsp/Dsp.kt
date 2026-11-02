package com.codex.voicex.dsp

import kotlin.math.*

/**
 * VoiceX DSP engine — 100% offline, pure Kotlin (no native libs required).
 * Made by CODEx Team.
 *
 * Every effect is implemented locally on PCM float samples. Advanced effects
 * (formant shifting, animal timbres) use the closest reliable offline
 * approximation and are isolated in their own functions so they can be
 * upgraded later without touching the rest of the engine.
 */

/** All tunable parameters of a voice preset / custom voice. */
data class DspParams(
    val pitchSemitones: Float = 0f,      // pitch shift, tempo preserved (SOLA)
    val formantSemitones: Float = 0f,    // extra pitch-only shift (formant approx)
    val speed: Float = 1f,               // 0.5..2.0, changes pitch+tempo together
    val bassDb: Float = 0f,              // low shelf +/- dB @200Hz
    val trebleDb: Float = 0f,            // high shelf +/- dB @4kHz
    val lowpassHz: Int = 0,              // 0 = off
    val drive: Float = 0f,               // 0..1 waveshaper distortion
    val bitDepth: Int = 16,              // <16 enables bit crushing
    val bitDownsample: Int = 1,          // >1 sample-hold decimation
    val ringModHz: Float = 0f,           // 0 = off, ring modulation carrier
    val tremoloHz: Float = 0f,
    val tremoloDepth: Float = 0f,        // 0..1
    val chorus: Float = 0f,              // 0..1
    val flanger: Float = 0f,             // 0..1
    val phaser: Float = 0f,              // 0..1
    val delayMs: Int = 0,
    val delayFeedback: Float = 0f,       // 0..0.9
    val delayMix: Float = 0f,            // 0..1
    val reverb: Float = 0f,              // 0..1
    val noiseLevel: Float = 0f,          // 0..1 white noise mix
    val gate: Float = 0f,                // 0..1 noise gate threshold
    val volume: Float = 1f,
    val normalize: Boolean = true,
    val ringMix: Float = 1f              // 0..1 wet amount of the ring modulator
) {
    val isIdentity: Boolean
        get() = this == DspParams()
}

object Dsp {

    /** Processing quality, selectable in Settings. */
    enum class Quality {
        FAST, BALANCED, HIGH;
        companion object {
            fun from(key: String?): Quality = when (key) {
                "fast" -> FAST
                "high" -> HIGH
                else -> BALANCED
            }
        }
    }

    /**
     * Render the full offline chain. onProgress(0..1) is called between stages.
     *
     * Pitch/formant semantics:
     *  - FAST: legacy behaviour, pitch and formant are simply summed into one pitch shift.
     *  - BALANCED / HIGH: [DspParams.pitchSemitones] moves the pitch; if
     *    [DspParams.formantSemitones] != 0 it is the total shift of the vocal-tract
     *    colour (formants) relative to the original voice, applied by a spectral
     *    envelope shifter. If it is 0 the formants simply follow the pitch.
     */
    fun render(
        input: FloatArray,
        sr: Int,
        p: DspParams,
        quality: Quality = Quality.BALANCED,
        onProgress: (Float) -> Unit = {}
    ): FloatArray {
        var x = input
        val stages = 17
        var stage = 0
        fun tick() { stage++; onProgress(stage.toFloat() / stages) }

        if (p.gate > 0f) x = gate(x, sr, p.gate * 0.08f); tick()
        if (p.speed != 1f) x = resampleSpeed(x, p.speed); tick()
        if (quality == Quality.FAST) {
            val total = p.pitchSemitones + p.formantSemitones
            if (total != 0f) x = pitchShift(x, sr, total, quality)
            tick(); tick()
        } else {
            tick()
            if (p.pitchSemitones != 0f) x = pitchShift(x, sr, p.pitchSemitones, quality)
            if (p.formantSemitones != 0f) {
                val appliedPitch = p.pitchSemitones.coerceIn(-24f, 24f)
                val fftSize = if (quality == Quality.HIGH) 2048 else 1024
                x = Spectral.shiftFormants(x, sr, p.formantSemitones - appliedPitch, fftSize)
            }
            tick()
        }
        if (p.bassDb != 0f) x = Biquad.lowShelf(sr, 200f, p.bassDb).process(x); tick()
        if (p.trebleDb != 0f) x = Biquad.highShelf(sr, 4000f, p.trebleDb).process(x); tick()
        if (p.lowpassHz > 0) x = lowpass(x, sr, p.lowpassHz.toFloat()); tick()
        if (p.drive > 0f) x = distort(x, p.drive); tick()
        if (p.bitDepth < 16 || p.bitDownsample > 1) x = bitCrush(x, p.bitDepth, p.bitDownsample); tick()
        if (p.ringModHz > 0f) x = ringMod(x, sr, p.ringModHz, p.ringMix); tick()
        if (p.tremoloDepth > 0f && p.tremoloHz > 0f) x = tremolo(x, sr, p.tremoloHz, p.tremoloDepth); tick()
        if (p.chorus > 0f) x = chorus(x, sr, p.chorus); tick()
        if (p.flanger > 0f) x = flanger(x, sr, p.flanger); tick()
        if (p.phaser > 0f) x = phaser(x, sr, p.phaser); tick()
        if (p.delayMs > 0 && p.delayMix > 0f) x = echo(x, sr, p.delayMs, p.delayFeedback, p.delayMix); tick()
        if (p.reverb > 0f) {
            // leave room for the reverb tail so it is not cut off
            val tail = (sr * 1.2f * p.reverb).toInt()
            x = reverb(x.copyOf(x.size + tail), sr, p.reverb)
        }
        tick()
        if (p.noiseLevel > 0f) x = addNoise(x, p.noiseLevel * 0.08f); tick()
        x = gain(x, p.volume)
        if (p.normalize) x = normalizeTo(x, 0.95f) else x = clip(x)
        return x
    }

    // ---------- basic ops ----------

    fun gain(x: FloatArray, g: Float): FloatArray {
        if (g == 1f) return x
        return FloatArray(x.size) { (x[it] * g).coerceIn(-1.2f, 1.2f) }
    }

    fun clip(x: FloatArray): FloatArray =
        FloatArray(x.size) { x[it].coerceIn(-1f, 1f) }

    fun normalizeTo(x: FloatArray, peak: Float): FloatArray {
        var max = 1e-6f
        for (v in x) { val a = abs(v); if (a > max) max = a }
        val g = (peak / max).coerceAtMost(8f)
        return FloatArray(x.size) { (x[it] * g).coerceIn(-1f, 1f) }
    }

    fun peak(x: FloatArray): Float {
        var max = 0f
        for (v in x) { val a = abs(v); if (a > max) max = a }
        return max
    }

    /** Speed change that also shifts pitch (resampling). factor>1 = faster+higher. */
    fun resampleSpeed(x: FloatArray, factor: Float): FloatArray {
        val f = factor.coerceIn(0.4f, 2.5f)
        if (f == 1f) return x
        val outLen = (x.size / f).toInt().coerceAtLeast(1)
        return FloatArray(outLen) { i ->
            val pos = i * f
            val i0 = pos.toInt().coerceIn(0, x.size - 1)
            val i1 = (i0 + 1).coerceAtMost(x.size - 1)
            val frac = pos - i0
            x[i0] * (1 - frac) + x[i1] * frac
        }
    }

    // ---------- pitch shift (SOLA time-stretch + resample) ----------

    /** Pitch shift by semitones while preserving duration. Range clamped to ±12 per pass. */
    fun pitchShift(x: FloatArray, sr: Int, semitones: Float, quality: Quality = Quality.BALANCED): FloatArray {
        var s = semitones.coerceIn(-24f, 24f)
        var out = x
        var guard = 0
        while (abs(s) > 0.01f && guard < 3) {
            val step = s.coerceIn(-12f, 12f)
            val f = 2.0.pow(step / 12.0).toFloat()
            val stretched = solaStretch(out, sr, f, quality)
            out = resampleSpeed(stretched, f)
            s -= step
            guard++
        }
        return out
    }

    /** SOLA time stretch. ratio>1 => longer output, pitch preserved. */
    private fun solaStretch(x: FloatArray, sr: Int, ratio: Float, quality: Quality): FloatArray {
        val r = ratio.coerceIn(0.5f, 2.0f)
        if (abs(r - 1f) < 0.01f) return x
        val win = (sr * 0.024f).toInt()            // 24 ms window
        val hopA = win / 4                          // analysis hop
        val hopS = (hopA * r).toInt().coerceAtLeast(1) // synthesis hop
        val search = (sr * 0.006f).toInt()          // +-6 ms correlation search
        val offStep = when (quality) { Quality.FAST -> 4; Quality.BALANCED -> 2; Quality.HIGH -> 1 }
        val decim = when (quality) { Quality.FAST -> 8; Quality.BALANCED -> 4; Quality.HIGH -> 2 }
        val outLen = (x.size * r).toInt() + win * 2
        val out = FloatArray(outLen)
        // Hann window
        val hann = FloatArray(win) { i -> 0.5f - 0.5f * cos(2f * PI.toFloat() * i / (win - 1)) }

        // first window
        for (i in 0 until min(win, x.size)) out[i] = x[i] * hann[i]
        var inPos = hopA
        var outPos = hopS

        while (inPos + win <= x.size && outPos + win < outLen) {
            // find best alignment offset around inPos: normalised cross-correlation on the overlap
            val ovLen = min(hopS, win)
            var bestOff = 0
            var bestCorr = -Float.MAX_VALUE
            var off = -search
            while (off <= search) {
                val ip = inPos + off
                if (ip >= 0 && ip + win <= x.size) {
                    var corr = 0f
                    var energy = 1e-6f
                    var i = 0
                    while (i < ovLen) {
                        val xv = x[ip + i]
                        corr += out[outPos + i] * xv
                        energy += xv * xv
                        i += decim
                    }
                    val score = corr / sqrt(energy)
                    if (score > bestCorr) { bestCorr = score; bestOff = off }
                }
                off += offStep
            }
            val ip = (inPos + bestOff).coerceIn(0, x.size - win)
            // overlap-add
            for (i in 0 until win) {
                out[outPos + i] += x[ip + i] * hann[i]
            }
            inPos += hopA
            outPos += hopS
        }
        // trim trailing silence beyond expected length
        val expected = (x.size * r).toInt() + win
        return if (expected < out.size) out.copyOf(expected) else out
    }

    // ---------- filters ----------

    class Biquad(
        private val b0: Float, private val b1: Float, private val b2: Float,
        private val a1: Float, private val a2: Float
    ) {
        fun process(x: FloatArray): FloatArray {
            val y = FloatArray(x.size)
            var x1 = 0f; var x2 = 0f; var y1 = 0f; var y2 = 0f
            for (i in x.indices) {
                val xn = x[i]
                val yn = b0 * xn + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
                y[i] = yn
                x2 = x1; x1 = xn; y2 = y1; y1 = yn
            }
            return y
        }

        companion object {
            /** RBJ audio EQ cookbook low shelf. */
            fun lowShelf(sr: Int, f0: Float, dbGain: Float): Biquad {
                val a = 10.0.pow(dbGain / 40.0).toFloat()
                val w0 = 2f * PI.toFloat() * f0 / sr
                val cosW = cos(w0); val sinW = sin(w0)
                val alpha = sinW / 2f * sqrt(2f)
                val twoSqrtAAlpha = 2f * sqrt(a) * alpha
                val b0 = a * ((a + 1) - (a - 1) * cosW + twoSqrtAAlpha)
                val b1 = 2f * a * ((a - 1) - (a + 1) * cosW)
                val b2 = a * ((a + 1) - (a - 1) * cosW - twoSqrtAAlpha)
                val a0 = (a + 1) + (a - 1) * cosW + twoSqrtAAlpha
                val a1 = -2f * ((a - 1) + (a + 1) * cosW)
                val a2 = (a + 1) + (a - 1) * cosW - twoSqrtAAlpha
                return Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
            }

            fun highShelf(sr: Int, f0: Float, dbGain: Float): Biquad {
                val a = 10.0.pow(dbGain / 40.0).toFloat()
                val w0 = 2f * PI.toFloat() * f0 / sr
                val cosW = cos(w0); val sinW = sin(w0)
                val alpha = sinW / 2f * sqrt(2f)
                val twoSqrtAAlpha = 2f * sqrt(a) * alpha
                val b0 = a * ((a + 1) + (a - 1) * cosW + twoSqrtAAlpha)
                val b1 = -2f * a * ((a - 1) + (a + 1) * cosW)
                val b2 = a * ((a + 1) + (a - 1) * cosW - twoSqrtAAlpha)
                val a0 = (a + 1) - (a - 1) * cosW + twoSqrtAAlpha
                val a1 = 2f * ((a - 1) - (a + 1) * cosW)
                val a2 = (a + 1) - (a - 1) * cosW - twoSqrtAAlpha
                return Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
            }
        }
    }

    /** One-pole low-pass. */
    fun lowpass(x: FloatArray, sr: Int, cutoff: Float): FloatArray {
        val rc = 1f / (2f * PI.toFloat() * cutoff.coerceAtLeast(50f))
        val dt = 1f / sr
        val a = dt / (rc + dt)
        val y = FloatArray(x.size)
        var prev = 0f
        for (i in x.indices) {
            prev += a * (x[i] - prev)
            y[i] = prev
        }
        return y
    }

    // ---------- character effects ----------

    fun distort(x: FloatArray, drive: Float): FloatArray {
        val k = 1f + drive.coerceIn(0f, 1f) * 25f
        val norm = tanh(k.toDouble()).toFloat()
        return FloatArray(x.size) { tanh((k * x[it]).toDouble()).toFloat() / norm }
    }

    fun bitCrush(x: FloatArray, bits: Int, downsample: Int): FloatArray {
        val b = bits.coerceIn(4, 16)
        val ds = downsample.coerceIn(1, 24)
        if (b == 16 && ds == 1) return x
        val levels = (1 shl (b - 1)).toFloat()
        val y = FloatArray(x.size)
        var held = 0f
        for (i in x.indices) {
            if (i % ds == 0) held = round(x[i] * levels) / levels
            y[i] = held
        }
        return y
    }

    fun ringMod(x: FloatArray, sr: Int, hz: Float, mix: Float = 1f): FloatArray {
        val y = FloatArray(x.size)
        var phase = 0.0
        val inc = 2.0 * PI * hz / sr
        for (i in x.indices) {
            y[i] = (x[i] * (1.0 - mix + mix * sin(phase))).toFloat()
            phase += inc
            if (phase > 2 * PI) phase -= 2 * PI
        }
        return y
    }

    fun tremolo(x: FloatArray, sr: Int, hz: Float, depth: Float): FloatArray {
        val y = FloatArray(x.size)
        var phase = 0.0
        val inc = 2.0 * PI * hz / sr
        val d = depth.coerceIn(0f, 1f)
        for (i in x.indices) {
            val lfo = 1f - d * (0.5f + 0.5f * sin(phase).toFloat())
            y[i] = x[i] * lfo
            phase += inc
            if (phase > 2 * PI) phase -= 2 * PI
        }
        return y
    }

    fun chorus(x: FloatArray, sr: Int, amount: Float): FloatArray {
        val a = amount.coerceIn(0f, 1f)
        val y = x.copyOf()
        val base1 = (sr * 0.016).toInt(); val mod1 = (sr * 0.007).toInt()
        val base2 = (sr * 0.023).toInt(); val mod2 = (sr * 0.005).toInt()
        for (i in x.indices) {
            val l1 = (base1 + mod1 * sin(2 * PI * 0.9 * i / sr)).toInt()
            val l2 = (base2 + mod2 * sin(2 * PI * 1.3 * i / sr + PI / 2)).toInt()
            var wet = 0f
            if (i - l1 >= 0) wet += x[i - l1]
            if (i - l2 >= 0) wet += x[i - l2]
            y[i] = x[i] * (1f - 0.4f * a) + wet * 0.5f * 0.4f * a
        }
        return y
    }

    fun flanger(x: FloatArray, sr: Int, amount: Float): FloatArray {
        val a = amount.coerceIn(0f, 1f)
        val maxD = (sr * 0.006).toInt()
        val buf = FloatArray(maxD + 2)
        var idx = 0
        val y = FloatArray(x.size)
        for (i in x.indices) {
            val lfo = 0.5f + 0.5f * sin(2 * PI * 0.4 * i / sr).toFloat()
            val d = 1f + lfo * maxD
            val d0 = d.toInt(); val frac = d - d0
            val r0 = buf[(idx - d0 + buf.size) % buf.size]
            val r1 = buf[(idx - d0 - 1 + buf.size) % buf.size]
            val delayed = r0 * (1 - frac) + r1 * frac
            buf[idx] = x[i] + delayed * 0.5f * a
            y[i] = x[i] * (1f - 0.6f * a) + delayed * 0.6f * a
            idx = (idx + 1) % buf.size
        }
        return y
    }

    fun phaser(x: FloatArray, sr: Int, amount: Float): FloatArray {
        val a = amount.coerceIn(0f, 1f)
        val stages = 4
        val xm1 = FloatArray(stages); val ym1 = FloatArray(stages)
        val y = FloatArray(x.size)
        for (i in x.indices) {
            val lfo = 0.5f + 0.5f * sin(2 * PI * 0.5 * i / sr).toFloat()
            val coeff = 0.2f + 0.7f * lfo
            var s = x[i]
            for (st in 0 until stages) {
                val out = -coeff * s + xm1[st] + coeff * ym1[st]
                xm1[st] = s
                ym1[st] = out
                s = out
            }
            y[i] = x[i] * (1f - 0.7f * a) + s * 0.7f * a
        }
        return y
    }

    fun echo(x: FloatArray, sr: Int, delayMs: Int, feedback: Float, mix: Float): FloatArray {
        val d = (sr.toLong() * delayMs / 1000).toInt().coerceAtLeast(1)
        val fb = feedback.coerceIn(0f, 0.9f)
        val m = mix.coerceIn(0f, 1f)
        val n = x.size + d * 6                  // room for the echo tail
        val line = FloatArray(n)                // feedback delay line
        val y = FloatArray(n)
        for (i in 0 until n) {
            val xi = if (i < x.size) x[i] else 0f
            val delayed = if (i >= d) line[i - d] else 0f
            line[i] = xi + delayed * fb
            y[i] = xi * (1f - 0.3f * m) + delayed * m
        }
        return y
    }

    /** Freeverb-style: 4 damped feedback combs in parallel + 2 allpass. */
    fun reverb(x: FloatArray, sr: Int, amount: Float): FloatArray {
        val a = amount.coerceIn(0f, 1f)
        val scale = sr / 44100f
        val combDelays = intArrayOf(1116, 1188, 1277, 1356).map { (it * scale).toInt() }
        val feedback = 0.70f + 0.25f * a
        val damp = 0.25f
        val combBufs = combDelays.map { FloatArray(it) }
        val combIdx = IntArray(combDelays.size)
        val combStore = FloatArray(combDelays.size)
        val wet = FloatArray(x.size)
        for (i in x.indices) {
            val inp = x[i]
            var acc = 0f
            for (c in combBufs.indices) {
                val buf = combBufs[c]
                val out = buf[combIdx[c]]
                combStore[c] = out * (1f - damp) + combStore[c] * damp
                buf[combIdx[c]] = inp + combStore[c] * feedback
                combIdx[c] = (combIdx[c] + 1) % buf.size
                acc += out
            }
            wet[i] = acc / combBufs.size
        }
        // two allpass diffusers
        var diffused = wet
        for (apDelayMs in intArrayOf(556, 441)) {
            val len = (apDelayMs * scale).toInt()
            val buf = FloatArray(len)
            var idx = 0
            val g = 0.5f
            val out = FloatArray(diffused.size)
            for (i in diffused.indices) {
                val bOut = buf[idx]
                val inp = diffused[i]
                out[i] = -g * inp + bOut
                buf[idx] = inp + bOut * g
                idx = (idx + 1) % len
            }
            diffused = out
        }
        return FloatArray(x.size) { x[it] * (1f - 0.5f * a) + diffused[it] * 0.9f * a }
    }

    fun addNoise(x: FloatArray, level: Float): FloatArray {
        val rnd = java.util.Random(42)
        return FloatArray(x.size) { (x[it] + (rnd.nextFloat() * 2f - 1f) * level).coerceIn(-1f, 1f) }
    }

    /** Smooth noise gate (peak follower + gain smoothing, no clicking). */
    fun gate(x: FloatArray, sr: Int, threshold: Float): FloatArray {
        val follow = 1f - exp(-1.0 / (sr * 0.004)).toFloat()
        val release = 1f - exp(-1.0 / (sr * 0.06)).toFloat()
        val openC = 1f - exp(-1.0 / (sr * 0.005)).toFloat()
        val closeC = 1f - exp(-1.0 / (sr * 0.08)).toFloat()
        var env = 0f
        var g = 1f
        val y = FloatArray(x.size)
        for (i in x.indices) {
            val a = abs(x[i])
            env += (a - env) * (if (a > env) follow else release)
            val target = if (env < threshold) 0.1f else 1f
            g += (target - g) * (if (target > g) openC else closeC)
            y[i] = x[i] * g
        }
        return y
    }
}
