package com.codex.voicex.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun LiveWaveform(levels: List<Float>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    Canvas(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val n = 72
        val barW = size.width / (n * 1.6f)
        val gap = barW * 0.6f
        val midY = size.height / 2f
        val list = levels.takeLast(n)
        for (i in 0 until n) {
            val v = if (i < n - list.size) 0.05f else list[i - (n - list.size)]
            val h = max(size.height * v, size.height * 0.05f)
            val x = i * (barW + gap)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(color, soft)),
                topLeft = Offset(x, midY - h / 2),
                size = Size(barW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2, barW / 2)
            )
        }
    }
}

@Composable
fun StaticWaveform(bars: List<Float>, progress: Float, modifier: Modifier = Modifier) {
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Canvas(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val n = bars.size.coerceAtLeast(1)
        val barW = size.width / (n * 1.5f)
        val gap = barW * 0.5f
        val midY = size.height / 2f
        val cutoff = (n * progress).toInt()
        for (i in 0 until n) {
            val h = max(size.height * bars[i], size.height * 0.08f)
            val x = i * (barW + gap)
            drawRoundRect(
                color = if (i <= cutoff) on else off,
                topLeft = Offset(x, midY - h / 2),
                size = Size(barW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2, barW / 2)
            )
        }
    }
}

@Composable
fun IdleWaveIndicator(modifier: Modifier = Modifier) {
    Box(modifier.height(56.dp).fillMaxWidth()) {
        StaticWaveform(bars = List(48) { 0.15f + 0.15f * ((it % 5) / 5f) }, progress = 0f, modifier = Modifier.fillMaxSize())
    }
}

fun waveformFromSamples(samples: FloatArray, bars: Int = 64): List<Float> {
    if (samples.isEmpty()) return List(bars) { 0.1f }
    val step = (samples.size / bars).coerceAtLeast(1)
    val out = ArrayList<Float>(bars)
    var maxG = 0f
    for (i in 0 until bars) {
        var peak = 0f
        val start = i * step
        val end = (start + step).coerceAtMost(samples.size)
        var j = start
        while (j < end) {
            val a = if (samples[j] < 0) -samples[j] else samples[j]
            if (a > peak) peak = a
            j += 2
        }
        if (peak > maxG) maxG = peak
        out.add(peak)
    }
    val norm = if (maxG > 0f) 0.95f / maxG else 1f
    return out.map { (it * norm).coerceIn(0.05f, 1f) }
}
