package com.fares.glass.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Weather(
    val tempC: Int,
    val description: String,
    val highC: Int,
    val lowC: Int,
    val rainPct: Int,
    val sunriseMs: Long,
    val sunsetMs: Long,
)

/** Open-Meteo: free, no API key. Batangas City. */
object WeatherApi {
    const val CITY = "Batangas City"
    private const val LAT = 13.7567
    private const val LON = 121.0584

    suspend fun fetch(): Weather = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON" +
            "&current=temperature_2m,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset,precipitation_probability_max" +
            "&timezone=Asia%2FManila&timeformat=unixtime&forecast_days=1"
        val j = JSONObject(Http.get(url))
        val cur = j.getJSONObject("current")
        val d = j.getJSONObject("daily")
        Weather(
            tempC = Math.round(cur.getDouble("temperature_2m")).toInt(),
            description = describe(cur.getInt("weather_code")),
            highC = Math.round(d.getJSONArray("temperature_2m_max").getDouble(0)).toInt(),
            lowC = Math.round(d.getJSONArray("temperature_2m_min").getDouble(0)).toInt(),
            rainPct = d.getJSONArray("precipitation_probability_max").optInt(0),
            sunriseMs = d.getJSONArray("sunrise").getLong(0) * 1000,
            sunsetMs = d.getJSONArray("sunset").getLong(0) * 1000,
        )
    }

    private fun describe(code: Int) = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67, in 80..82 -> "Rain"
        in 95..99 -> "Thunderstorm"
        else -> "Cloudy"
    }
}
