package com.fares.glass.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class Weather(
    val tempC: Int,
    val code: Int,
    val sunrise: Long,
    val sunset: Long,
    val isPlaceholder: Boolean = false,
) {
    /** Drizzle, rain, showers, thunderstorm. */
    val isRaining get() = code in 51..67 || code in 80..82 || code in 95..99

    companion object {
        /** Shown before the first real answer arrives. */
        val Placeholder = Weather(0, 0, 0L, 0L, isPlaceholder = true)
    }
}

object WeatherRepository {
    const val DEFAULT_LAT = 13.6663
    const val DEFAULT_LON = 121.1030
    private val tz: TimeZone = TimeZone.getTimeZone("Asia/Manila")

    /** Returns null when offline or on any error. Call from a coroutine. */
    suspend fun fetch(lat: Double = DEFAULT_LAT, lon: Double = DEFAULT_LON): Weather? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,weather_code&daily=sunrise,sunset" +
                        "&timezone=Asia%2FManila&forecast_days=1"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                val body = try { conn.inputStream.bufferedReader().use { it.readText() } } finally { conn.disconnect() }
                val json = JSONObject(body)
                val cur = json.getJSONObject("current")
                val daily = json.getJSONObject("daily")
                val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = tz }
                Weather(
                    tempC = cur.getDouble("temperature_2m").roundToInt(),
                    code = cur.getInt("weather_code"),
                    sunrise = fmt.parse(daily.getJSONArray("sunrise").getString(0))!!.time,
                    sunset = fmt.parse(daily.getJSONArray("sunset").getString(0))!!.time,
                )
            } catch (e: Exception) {
                null
            }
        }

    /** Offline fallback: about 5:40 AM and 5:50 PM, close to the Philippines year-round. */
    fun fallbackTimes(now: Long = System.currentTimeMillis()): Pair<Long, Long> {
        fun at(h: Int, m: Int) = Calendar.getInstance(tz).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return at(5, 40) to at(17, 50)
    }
}
