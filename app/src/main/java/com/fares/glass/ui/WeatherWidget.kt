package com.fares.glass.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fares.glass.data.Weather

/** Plain text weather next to the clock, e.g. "☀️ 31°C Sunny". */
@Composable
fun WeatherWidget(weather: Weather, modifier: Modifier = Modifier) {
    val now = System.currentTimeMillis()
    val day = now > weather.sunrise && now < weather.sunset
    val (icon, label) = describe(weather.code, day)

    BasicText(
        "$icon ${weather.tempC}°C $label",
        modifier = modifier,
        style = TextStyle(
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            // Soft shadow so it stays readable on bright skies.
            shadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 6f),
        ),
    )
}

private fun describe(code: Int, day: Boolean): Pair<String, String> = when (code) {
    0, 1 -> if (day) "☀️" to "Sunny" else "🌙" to "Clear"
    2 -> "⛅" to "Partly cloudy"
    3 -> "☁️" to "Cloudy"
    45, 48 -> "🌫️" to "Foggy"
    in 51..57 -> "🌦️" to "Drizzle"
    in 61..67 -> "🌧️" to "Rainy"
    in 71..77 -> "❄️" to "Snow"
    in 80..82 -> "🌧️" to "Rainy"
    in 95..99 -> "⛈️" to "Thunderstorm"
    else -> "☁️" to "Cloudy"
}
