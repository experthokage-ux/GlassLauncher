package com.fares.glass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/** Light rain streaks drawn above the wallpaper and below the UI. */
@Composable
fun RainOverlay(modifier: Modifier = Modifier) {
    val count = 70
    // x, phase, speed, length, alpha
    val drops = remember {
        val rnd = Random(42)
        Array(count) {
            floatArrayOf(
                rnd.nextFloat(), rnd.nextFloat(),
                0.9f + rnd.nextFloat() * 0.9f,
                0.03f + rnd.nextFloat() * 0.04f,
                0.10f + rnd.nextFloat() * 0.18f,
            )
        }
    }
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            t = (t + (now - last) / 1_000_000_000f) % 1000f
            last = now
        }
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val time = t
        for (d in drops) {
            val len = d[3] * h
            val y = ((d[1] + time * d[2] * 0.9f) % 1f) * (h + len) - len
            val x = d[0] * (w + 200f) - 100f + y * 0.12f
            drawLine(
                Color.White.copy(alpha = d[4]),
                Offset(x, y), Offset(x + len * 0.12f, y + len),
                strokeWidth = 2f,
            )
        }
    }
}
