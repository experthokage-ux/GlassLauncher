package com.fares.glass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fares.glass.data.Weather

/** Small glass pill with the weather icon, temperature and a short description. */
@Composable
fun WeatherWidget(weather: Weather, modifier: Modifier = Modifier) {
    val now = System.currentTimeMillis()
    val day = now > weather.sunrise && now < weather.sunset
    val (icon, label) = describe(weather.code, day)

    Row(
        modifier
            .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(icon, style = TextStyle(fontSize = 32.sp))
        Spacer(Modifier.width(12.dp))
        Column {
            BasicText(
                "${weather.tempC}°C",
                style = TextStyle(color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
            )
            BasicText(
                label,
                style = TextStyle(color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp),
            )
        }
    }
}

private fun describe(code: Int, day: Boolean): Pair<String, String> = when (code) {
    0 -> if (day) "☀️" to "Clear" else "🌙" to "Clear"
    1 -> if (day) "🌤️" to "Mostly clear" else "🌙" to "Mostly clear"
    2 -> "⛅" to "Partly cloudy"
    3 -> "☁️" to "Overcast"
    45, 48 -> "🌫️" to "Fog"
    in 51..57 -> "🌦️" to "Drizzle"
    in 61..67 -> "🌧️" to "Rain"
    in 71..77 -> "❄️" to "Snow"
    in 80..82 -> "🌧️" to "Showers"
    in 95..99 -> "⛈️" to "Thunderstorm"
    else -> "☁️" to "Cloudy"
}
