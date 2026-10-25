package com.fares.glass.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dimmed wallpaper with a clock that drifts to a new corner every 45 seconds. */
@Composable
fun Screensaver(wallpaper: ImageBitmap?, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    var slot by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        var ticks = 0
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
            ticks++
            if (ticks % 45 == 0) slot++
        }
    }

    val spots = listOf(
        Alignment.TopStart, Alignment.CenterEnd, Alignment.BottomStart,
        Alignment.TopEnd, Alignment.CenterStart, Alignment.BottomEnd,
    )
    val time = SimpleDateFormat("h:mm", Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(now))

    Box(modifier.fillMaxSize().background(Color.Black)) {
        if (wallpaper != null) {
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.35f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.align(spots[slot % spots.size]).padding(64.dp)) {
            BasicText(time, style = TextStyle(color = Color.White, fontSize = 96.sp))
            BasicText(date, style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 24.sp))
        }
    }
}
