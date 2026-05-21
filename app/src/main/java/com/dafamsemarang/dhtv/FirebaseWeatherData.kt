package com.dafamsemarang.dhtv

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class FirebaseWeatherConfig(
    val city: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@IgnoreExtraProperties
data class DailyForecast(
    val date: String? = null,
    val sunrise: String? = null,
    val sunset: String? = null,
    val temp_max: Double? = null,
    val temp_min: Double? = null,
    val weather_code: Int? = null,
    val weather_label: String? = null
)

@IgnoreExtraProperties
data class HourlyForecast(
    val humidity: Int? = null,
    val precipitation_probability: Int? = null,
    val temperature: Double? = null,
    val time: String? = null,
    val weather_code: Int? = null,
    val weather_label: String? = null
)

@IgnoreExtraProperties
data class ForecastData(
    val city: String? = null,
    val last_updated: String? = null,
    val daily: List<DailyForecast>? = null,
    val hourly: List<HourlyForecast>? = null
)

@IgnoreExtraProperties
data class LiveWeather(
    val city: String? = null,
    val humidity: Int? = null,
    val is_day: Int? = null,
    val last_updated: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val temperature: Double? = null,
    val weather_code: Int? = null,
    val weather_label: String? = null,
    val wind_speed: Double? = null
)

@IgnoreExtraProperties
data class FirebaseWeatherData(
    val config: FirebaseWeatherConfig? = null,
    val forecast: ForecastData? = null,
    val liveWeather: LiveWeather? = null
) {
    companion object {
        private fun safeToInt(value: Any?): Int? {
            return when (value) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                else -> null
            }
        }

        private fun safeToDouble(value: Any?): Double? {
            return when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                else -> null
            }
        }

        fun parseLiveWeather(liveSnapshot: DataSnapshot): LiveWeather {
            return LiveWeather(
                city = liveSnapshot.child("city").getValue(String::class.java),
                humidity = safeToInt(liveSnapshot.child("humidity").value),
                is_day = safeToInt(liveSnapshot.child("is_day").value),
                last_updated = liveSnapshot.child("last_updated").getValue(String::class.java),
                latitude = safeToDouble(liveSnapshot.child("latitude").value),
                longitude = safeToDouble(liveSnapshot.child("longitude").value),
                temperature = safeToDouble(liveSnapshot.child("temperature").value),
                weather_code = safeToInt(liveSnapshot.child("weather_code").value),
                weather_label = liveSnapshot.child("weather_label").getValue(String::class.java),
                wind_speed = safeToDouble(liveSnapshot.child("wind_speed").value)
            )
        }

        fun parseForecastData(forecastSnapshot: DataSnapshot): ForecastData {
            val dailyList = mutableListOf<DailyForecast>()
            forecastSnapshot.child("daily").children.forEach { child ->
                val daily = DailyForecast(
                    date = child.child("date").getValue(String::class.java),
                    sunrise = child.child("sunrise").getValue(String::class.java),
                    sunset = child.child("sunset").getValue(String::class.java),
                    temp_max = safeToDouble(child.child("temp_max").value),
                    temp_min = safeToDouble(child.child("temp_min").value),
                    weather_code = safeToInt(child.child("weather_code").value),
                    weather_label = child.child("weather_label").getValue(String::class.java)
                )
                dailyList.add(daily)
            }

            val hourlyList = mutableListOf<HourlyForecast>()
            forecastSnapshot.child("hourly").children.forEach { child ->
                val hourly = HourlyForecast(
                    humidity = safeToInt(child.child("humidity").value),
                    precipitation_probability = safeToInt(child.child("precipitation_probability").value),
                    temperature = safeToDouble(child.child("temperature").value),
                    time = child.child("time").getValue(String::class.java),
                    weather_code = safeToInt(child.child("weather_code").value),
                    weather_label = child.child("weather_label").getValue(String::class.java)
                )
                hourlyList.add(hourly)
            }

            return ForecastData(
                city = forecastSnapshot.child("city").getValue(String::class.java),
                last_updated = forecastSnapshot.child("last_updated").getValue(String::class.java),
                daily = dailyList,
                hourly = hourlyList
            )
        }

        fun parseFromSnapshot(snapshot: DataSnapshot): FirebaseWeatherData {
            val configSnapshot = snapshot.child("config")
            val config = FirebaseWeatherConfig(
                city = configSnapshot.child("city").getValue(String::class.java),
                latitude = safeToDouble(configSnapshot.child("latitude").value),
                longitude = safeToDouble(configSnapshot.child("longitude").value)
            )

            val liveWeather = parseLiveWeather(snapshot.child("liveWeather"))
            val forecast = parseForecastData(snapshot.child("forecast"))

            return FirebaseWeatherData(config, forecast, liveWeather)
        }
    }
}
