package com.fares.glass.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Title(val id: Int, val name: String, val year: String, val posterUrl: String?)

object Tmdb {
    suspend fun trending(apiKey: String): List<Title> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext emptyList()
        val json = JSONObject(Http.get("https://api.themoviedb.org/3/trending/all/week?api_key=$apiKey"))
        val arr = json.getJSONArray("results")
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val poster = o.optString("poster_path").takeIf { it.isNotBlank() && it != "null" }
            val name = o.optString("title").ifBlank { o.optString("name") }
            if (name.isBlank()) return@mapNotNull null
            val date = o.optString("release_date").ifBlank { o.optString("first_air_date") }
            Title(o.getInt("id"), name, date.take(4), poster?.let { "https://image.tmdb.org/t/p/w185$it" })
        }.take(20)
    }
}
