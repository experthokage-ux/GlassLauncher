package com.fares.glass.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object Http {
    fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        return try { c.inputStream.bufferedReader().use { it.readText() } } finally { c.disconnect() }
    }

    fun bitmap(url: String): Bitmap? {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 8000
        return try { c.inputStream.use { BitmapFactory.decodeStream(it) } } finally { c.disconnect() }
    }
}

object ImageCache {
    private val lru = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun load(url: String): Bitmap? =
        lru.get(url) ?: withContext(Dispatchers.IO) {
            runCatching { Http.bitmap(url) }.getOrNull()
        }?.also { lru.put(url, it) }
}
