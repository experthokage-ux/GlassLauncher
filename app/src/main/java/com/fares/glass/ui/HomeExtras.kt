package com.fares.glass.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fares.glass.BuildConfig
import com.fares.glass.data.*
import com.fares.glass.gfx.Sky
import com.fares.glass.gfx.SkyWallpaper
import kotlinx.coroutines.delay

/** Plain text (no Material). */
@Composable
private fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) = BasicText(
    text, modifier,
    style = TextStyle(color = color, fontSize = fontSize, fontWeight = fontWeight),
    maxLines = maxLines, overflow = overflow,
)

// ---------- data holders ----------

@Composable
fun rememberWeather(): State<Weather?> = produceState<Weather?>(null) {
    while (true) {
        value = runCatching { WeatherApi.fetch() }.getOrNull() ?: value
        delay(30 * 60_000L)
    }
}

@Composable
fun rememberTrending(): State<List<Title>> = produceState(emptyList<Title>()) {
    value = runCatching { Tmdb.trending(BuildConfig.TMDB_API_KEY) }.getOrDefault(emptyList())
}

@Composable
fun rememberNowPlaying(): State<Track?> {
    val ctx = LocalContext.current.applicationContext
    return remember { NowPlaying.flow(ctx) }.collectAsState(null)
}

/** Re-evaluates every minute so the sky flips at dusk/evening without a restart. */
@Composable
fun rememberSky(weather: Weather?): State<Sky> {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); now = System.currentTimeMillis() } }
    return remember(weather) { derivedStateOf { SkyWallpaper.phase(now, weather?.sunriseMs, weather?.sunsetMs) } }
}

// ---------- UI (the remote is handled by HomeScreen, so nothing here takes focus) ----------

@Composable
private fun GlassPanel(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    radius: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val scale by animateFloatAsState(
        if (focused) 1.1f else 1f,
        spring(dampingRatio = 0.55f, stiffness = 320f),
        label = "focus",
    )
    Glass(
        backdrop = backdrop,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(radius),
        tint = Color.White.copy(alpha = if (focused) 0.2f else 0.07f),
        content = content,
    )
}

@Composable
fun NowPlayingTile(track: Track, backdrop: Backdrop, modifier: Modifier = Modifier) {
    val art = remember(track.art) { track.art?.asImageBitmap() }
    GlassPanel(backdrop, modifier.width(300.dp).height(76.dp)) {
        Row(Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.15f))) {
                if (art != null) Image(art, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    (if (track.playing) "NOW PLAYING" else "PAUSED") + " · " + track.app,
                    color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp,
                )
                Text(track.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (track.artist.isNotBlank()) {
                    Text(track.artist, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

/** [selected] = index of the highlighted poster, or -1 when the apps have the focus. */
@Composable
fun TmdbRow(titles: List<Title>, selected: Int, backdrop: Backdrop, modifier: Modifier = Modifier) {
    if (titles.isEmpty()) return
    val listState = rememberLazyListState()
    LaunchedEffect(selected) {
        if (selected >= 0) listState.animateScrollToItem((selected - 3).coerceAtLeast(0))
    }
    val current = titles.getOrNull(selected)
    Column(modifier.fillMaxWidth()) {
        Text(
            if (current != null) current.name + (if (current.year.isNotBlank()) "  (${current.year})" else "")
            else "Trending this week",
            modifier = Modifier.padding(start = Metrics.Margin),
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
        )
        LazyRow(
            state = listState,
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = Metrics.Margin, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(titles) { i, t -> Poster(t, i == selected, backdrop) }
        }
    }
}

@Composable
private fun Poster(t: Title, focused: Boolean, backdrop: Backdrop) {
    val bmp by produceState<android.graphics.Bitmap?>(null, t.posterUrl) {
        value = t.posterUrl?.let { ImageCache.load(it) }
    }
    GlassPanel(backdrop, Modifier.size(96.dp, 144.dp), focused, radius = 14.dp) {
        bmp?.let {
            Image(
                it.asImageBitmap(), t.name,
                Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
fun WeatherPill(weather: Weather?, backdrop: Backdrop, modifier: Modifier = Modifier) {
    if (weather == null) return
    GlassPanel(backdrop, modifier, radius = 18.dp) {
        Row(Modifier.height(36.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(WeatherApi.CITY, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Text("${weather.tempC}°  ${weather.description}", fontSize = 13.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                "H ${weather.highC}°  L ${weather.lowC}°  ·  Rain ${weather.rainPct}%",
                color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
            )
        }
    }
}
