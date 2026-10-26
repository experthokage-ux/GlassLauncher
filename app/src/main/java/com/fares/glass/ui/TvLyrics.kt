package com.fares.glass.ui

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fares.glass.data.LyricLine
import com.fares.glass.data.LyricsRepository
import com.fares.glass.sys.NowPlaying
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Where the TV screen is inside SkyWallpaper (1280 x 720), as fractions of the picture.
 * From SkyWallpaper: tvX = 0.40 w, tvY = 0.23 h, tvW = 0.40 w, tvH = tvW * 9 / 16.
 * If you move or resize the TV in SkyWallpaper.kt, change these numbers too.
 */
object TvScreen {
    const val WALLPAPER_W = 1280f
    const val WALLPAPER_H = 720f
    const val LEFT = 0.40f
    const val TOP = 0.23f
    const val RIGHT = 0.80f
    const val BOTTOM = 0.63f
}

/** Synced lyrics of the song that is playing, drawn on the TV screen of the sky wallpaper. */
@Composable
fun TvLyrics(modifier: Modifier = Modifier) {
    var lines by remember { mutableStateOf<List<LyricLine>>(emptyList()) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    val title = NowPlaying.title
    val artist = NowPlaying.artist
    val duration = NowPlaying.durationMs

    // New song: look up its lyrics (the short delay skips songs that are skipped quickly).
    LaunchedEffect(title, artist, duration) {
        lines = emptyList()
        if (title.isBlank()) return@LaunchedEffect
        delay(500)
        lines = LyricsRepository.fetch(title, artist, duration)
    }

    // Clock for the current line.
    LaunchedEffect(NowPlaying.active) {
        while (NowPlaying.active) {
            now = SystemClock.elapsedRealtime()
            delay(200)
        }
    }

    if (!NowPlaying.active || lines.isEmpty()) return

    val position = NowPlaying.position(now) + 250 // show a line a little early
    var index = -1
    for (i in lines.indices) {
        if (lines[i].timeMs <= position) index = i else break
    }

    BoxWithConstraints(modifier) {
        // The wallpaper is drawn with ContentScale.Crop: work out where the TV ends up on screen.
        val sw = constraints.maxWidth.toFloat()
        val sh = constraints.maxHeight.toFloat()
        val scale = max(sw / TvScreen.WALLPAPER_W, sh / TvScreen.WALLPAPER_H)
        val dx = (sw - TvScreen.WALLPAPER_W * scale) / 2f
        val dy = (sh - TvScreen.WALLPAPER_H * scale) / 2f
        val left = dx + TvScreen.WALLPAPER_W * scale * TvScreen.LEFT
        val right = dx + TvScreen.WALLPAPER_W * scale * TvScreen.RIGHT
        val top = dy + TvScreen.WALLPAPER_H * scale * TvScreen.TOP
        val bottom = dy + TvScreen.WALLPAPER_H * scale * TvScreen.BOTTOM
        val density = LocalDensity.current

        Box(
            Modifier
                .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                .size(with(density) { (right - left).toDp() }, with(density) { (bottom - top).toDp() })
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(index, animationSpec = tween(350), label = "lyrics") { i ->
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val previous = lines.getOrNull(i - 1)?.text.orEmpty()
                    val current = lines.getOrNull(i)?.text.orEmpty().ifBlank { "\u266A" }
                    val next = lines.getOrNull(i + 1)?.text.orEmpty()
                    val center = TextAlign.Center
                    BasicText(
                        previous,
                        style = txt(Color.White.copy(alpha = 0.40f), 11.sp).copy(textAlign = center),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        current,
                        style = txt(Color.White, 17.sp, FontWeight.SemiBold).copy(textAlign = center),
                        maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        next,
                        style = txt(Color.White.copy(alpha = 0.40f), 11.sp).copy(textAlign = center),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
