package com.fares.glass.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/** One timed line of a song. */
class LyricLine(val timeMs: Long, val text: String)

/** Finds synced lyrics (LRC) on LRCLIB: free, open, no API key. */
object LyricsRepository {

    private const val BASE = "https://lrclib.net/api"
    private const val USER_AGENT = "GlassLauncher (https://github.com/lwrnccena-ctrl/GlassLauncher)"

    // The last answer is kept, so the same song is not downloaded twice.
    private var cacheKey = ""
    private var cacheLines: List<LyricLine> = emptyList()

    /** Returns the timed lines, or an empty list if the song has none (or the network is down). */
    suspend fun fetch(title: String, artist: String, durationMs: Long): List<LyricLine> {
        val key = "$title|$artist|${durationMs / 1000}"
        if (key == cacheKey) return cacheLines
        val lines = withContext(Dispatchers.IO) {
            try {
                lookup(title, artist, durationMs / 1000)
            } catch (e: Exception) {
                Log.w("Glass", "Lyrics lookup failed", e)
                null
            }
        } ?: return emptyList() // a failure is not cached, so the next try can work
        cacheKey = key
        cacheLines = lines
        return lines
    }

    private fun lookup(title: String, artist: String, secs: Long): List<LyricLine> {
        // 1. Exact match by title, artist and length.
        val exact = url("get", "track_name" to title, "artist_name" to artist, "duration" to if (secs > 0) secs.toString() else "")
        getText(exact)?.let { body ->
            val lines = syncedOf(JSONObject(body))
            if (lines.isNotEmpty()) return lines
        }

        // 2. Fuzzy search with a cleaned title ("Song (Remastered)" -> "Song").
        val search = url("search", "track_name" to cleanTitle(title), "artist_name" to artist)
        val body = getText(search) ?: return emptyList()
        val results = JSONArray(body)
        var best: JSONObject? = null
        var bestDiff = Double.MAX_VALUE
        for (i in 0 until results.length()) {
            val o = results.getJSONObject(i)
            if (o.isNull("syncedLyrics") || o.optBoolean("instrumental")) continue
            val diff = if (secs > 0) abs(o.optDouble("duration", 0.0) - secs) else 0.0
            if (secs > 0 && diff > 6.0) continue // probably a different version of the song
            if (diff < bestDiff) {
                best = o
                bestDiff = diff
            }
        }
        return best?.let(::syncedOf) ?: emptyList()
    }

    private fun syncedOf(o: JSONObject): List<LyricLine> {
        if (o.optBoolean("instrumental") || o.isNull("syncedLyrics")) return emptyList()
        return parseLrc(o.getString("syncedLyrics"))
    }

    private fun url(path: String, vararg params: Pair<String, String>): String =
        params.filter { it.second.isNotBlank() }
            .joinToString("&", "$BASE/$path?") { "${it.first}=${URLEncoder.encode(it.second, "UTF-8")}" }

    private fun cleanTitle(title: String): String =
        title.replace(Regex("\\s*[(\\[].*?[)\\]]"), "").substringBefore(" - ").trim().ifEmpty { title }

    /** Body of the answer, or null if the server says "not found". */
    private fun getText(url: String): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 8_000
            c.readTimeout = 8_000
            c.setRequestProperty("User-Agent", USER_AGENT)
            val code = c.responseCode
            if (code == 404) return null
            if (code !in 200..299) throw IOException("HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private val stamp = Regex("\\[(\\d+):(\\d+(?:[.:]\\d+)?)]")

    /** "[01:23.45] text" lines to timed lines. A line may have several time stamps. */
    private fun parseLrc(lrc: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        for (raw in lrc.lineSequence()) {
            val stamps = stamp.findAll(raw).toList()
            if (stamps.isEmpty()) continue
            val text = raw.substring(stamps.last().range.last + 1).trim()
            for (m in stamps) {
                val minutes = m.groupValues[1].toLong()
                val seconds = m.groupValues[2].replace(':', '.').toDouble()
                out.add(LyricLine(minutes * 60_000L + (seconds * 1000).toLong(), text))
            }
        }
        out.sortBy { it.timeMs }
        return out
    }
}
