package com.fares.glass.ui

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fares.glass.sys.NowPlaying
import kotlinx.coroutines.delay

private fun fmt(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Round button: plain icon at rest, a white circle with a dark icon when selected. */
@Composable
private fun MediaButton(icon: ImageVector, iconSize: Dp, focused: Boolean) {
    val f by animateFloatAsState(if (focused) 1f else 0f, spring(stiffness = 600f), label = "mediaButton")
    val fg = lerp(Color.White, Color(0xFF111114), f)
    Box(
        Modifier
            .size(iconSize + 18.dp)
            .graphicsLayer { scaleX = 1f + 0.12f * f; scaleY = 1f + 0.12f * f }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.94f * f)),
        contentAlignment = Alignment.Center,
    ) {
        Image(rememberVectorPainter(icon), null, Modifier.size(iconSize), colorFilter = ColorFilter.tint(fg))
    }
}

/**
 * Glass card with what is playing. It fades in and out by itself.
 * focus: -1 = none, 0 = previous, 1 = play/pause, 2 = next.
 */
@Composable
fun NowPlayingCard(backdrop: Backdrop, focus: Int, modifier: Modifier = Modifier) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(NowPlaying.active) {
        while (NowPlaying.active) {
            now = SystemClock.elapsedRealtime()
            delay(1000)
        }
    }

    AnimatedVisibility(
        NowPlaying.active, modifier,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.95f),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.95f),
    ) {
        Glass(backdrop, Modifier.width(290.dp), RoundedCornerShape(24.dp), tint = Color.White.copy(alpha = 0.10f)) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val art = NowPlaying.art
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (art != null) {
                            Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Image(
                                rememberVectorPainter(Icons.Rounded.MusicNote), null, Modifier.size(24.dp),
                                colorFilter = ColorFilter.tint(Color.White),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            NowPlaying.title.ifBlank { "Now playing" },
                            style = txt(Color.White, 15.sp, FontWeight.SemiBold),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        if (NowPlaying.artist.isNotBlank()) {
                            BasicText(
                                NowPlaying.artist,
                                style = txt(Color.White.copy(alpha = 0.65f), 12.sp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MediaButton(Icons.Rounded.SkipPrevious, 26.dp, focus == 0)
                    MediaButton(
                        if (NowPlaying.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        32.dp, focus == 1,
                    )
                    MediaButton(Icons.Rounded.SkipNext, 26.dp, focus == 2)
                }

                if (NowPlaying.durationMs > 0) {
                    Spacer(Modifier.height(6.dp))
                    val pos = NowPlaying.position(now)
                    val fraction = (pos.toFloat() / NowPlaying.durationMs).coerceIn(0f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(fmt(pos), style = txt(Color.White.copy(alpha = 0.65f), 11.sp))
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.weight(1f).height(4.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp)),
                        ) {
                            Box(
                                Modifier.fillMaxWidth(fraction).height(4.dp)
                                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp)),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        BasicText(fmt(NowPlaying.durationMs), style = txt(Color.White.copy(alpha = 0.65f), 11.sp))
                    }
                }
            }
        }
    }
}
