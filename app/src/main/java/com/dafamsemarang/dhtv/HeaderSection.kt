package com.dafamsemarang.dhtv

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import WeatherResponse
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.LottieCompositionSpec

@Composable
fun HeaderSection() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    var iconUrl by remember { mutableStateOf<String?>(null) }
    val database = Firebase.database.reference

    var fetchedWeatherData by remember { mutableStateOf<WeatherResponse?>(null) }
    val apiKey = "0a655bc76d9b2bd5c9e6422c5cc58455"

    // Helper and state for Time/Date in the header
    fun getCurrentTime(): String {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale("en", "ID"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale("en", "ID"))
        dateFormat.timeZone = TimeZone.getTimeZone("GMT+7")
        timeFormat.timeZone = TimeZone.getTimeZone("GMT+7")
        
        val currentDate = dateFormat.format(Date())
        val currentTime = timeFormat.format(Date())
        return "$currentDate $currentTime"
    }

    var currentTime by remember { mutableStateOf(getCurrentTime()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = getCurrentTime()
            delay(1000)
        }
    }

    // Fetch company icon URL from Firebase
    DisposableEffect(branchId) {
        var iconRef: com.google.firebase.database.DatabaseReference? = null
        var iconListener: ValueEventListener? = null

        if (branchId != null) {
            iconRef = database.child("BRANCHES").child(branchId).child("SETTING").child("COMPANY_ICON")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    iconUrl = snapshot.child("iconUrl").getValue(String::class.java)
                }
                override fun onCancelled(error: DatabaseError) { }
            }
            iconListener = listener
            iconRef.addValueEventListener(listener)
        }

        onDispose {
            if (iconRef != null && iconListener != null) {
                iconRef.removeEventListener(iconListener)
            }
        }
    }

    // Fetch weather data for the Header
    LaunchedEffect(branchId) {
        if (branchId != null) {
            coroutineScope {
                launch {
                    while (isActive) {
                        try {
                            val cityRef = database.child("BRANCHES")
                                .child(branchId)
                                .child("SETTING")
                                .child("WEATHER")
                                .child("CITY")
                            
                            val cityName = cityRef.get().await().getValue(String::class.java)
                            
                            if (cityName != null) {
                                val response = RetrofitInstance.api.getWeather(cityName, apiKey)
                                fetchedWeatherData = response
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        delay(60000)
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 58.dp, top = 24.dp, end = 58.dp, bottom = 16.dp), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left-aligned Column containing Live Weather
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center
            ) {
                // Weather Row
                fetchedWeatherData?.let { weather ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        AnimatedWeatherIcon(
                            iconCode = weather.weather.first().icon,
                            modifier = Modifier.size(38.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${weather.main.temp}°C, ${weather.weather.first().description.replaceFirstChar { it.uppercase() }}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Company Logo on the right side of the header
            if (iconUrl != null && iconUrl!!.isNotEmpty()) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = "Company Logo",
                    modifier = Modifier
                        .padding(4.dp)
                        .height(48.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(HeaderIcon),
                    error = painterResource(id = R.drawable.dafam) // Fallback to default icon
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.dafam),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .padding(4.dp)
                        .height(48.dp),
                    tint = HeaderIcon
                )
            }
        }
    }
}

@Composable
fun AnimatedWeatherIcon(iconCode: String, modifier: Modifier = Modifier) {
    val lottiePath = remember(iconCode) {
        val prefix = iconCode.take(2)
        val isDay = iconCode.endsWith("d")
        
        when (prefix) {
            // Sunny / Clear Sky
            "01" -> {
                if (isDay) "weather/Weather-sunny.json" else "weather/Weather-night.json"
            }
            // Few clouds / partly cloudy
            "02", "03" -> {
                if (isDay) "weather/Weather-partly cloudy.json" else "weather/Weather-cloudy(night).json"
            }
            // Overcast clouds
            "04" -> "weather/Weather-cloudy(night).json"
            // Rain / Shower
            "09" -> {
                if (isDay) "weather/Weather-partly shower.json" else "weather/Weather-rainy(night).json"
            }
            "10" -> {
                if (isDay) "weather/Weather-partly shower.json" else "weather/Weather-storm.json"
            }
            // Thunderstorm
            "11" -> {
                if (isDay) "weather/Weather-storm&showers(day).json" else "weather/Weather-thunder.json"
            }
            // Snow / Windy
            "13" -> "weather/Weather-windy.json"
            // Mist / Fog / Haze
            "50" -> {
                if (isDay) "weather/Weather-mist.json" else "weather/Foggy.json"
            }
            // Default fallback
            else -> {
                if (isDay) "weather/Weather-sunny.json" else "weather/Weather-night.json"
            }
        }
    }

    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(lottiePath))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

@Composable
fun AnimatedClockIcon(modifier: Modifier = Modifier) {
    // Read the real current time to rotate clock hands dynamically
    var calendar by remember { mutableStateOf(java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+7"))) }
    
    LaunchedEffect(Unit) {
        while (isActive) {
            calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+7"))
            delay(1000)
        }
    }
    
    val hour = calendar.get(java.util.Calendar.HOUR)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    val second = calendar.get(java.util.Calendar.SECOND)
    
    // Calculate rotation angles
    val secondHandAngle = second * 6f
    val minuteHandAngle = minute * 6f + second * 0.1f
    val hourHandAngle = (hour % 12) * 30f + minute * 0.5f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val radius = width / 2
            val center = Offset(width / 2, height / 2)
            
            // 1. Draw Clock Outer Ring (Thin White Border)
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.25.dp.toPx())
            )
            
            // 2. Draw Hour Hand (Shorter, Thicker)
            rotate(hourHandAngle) {
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(center.x, center.y - (radius * 0.5f)),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            // 3. Draw Minute Hand (Longer, Medium)
            rotate(minuteHandAngle) {
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(center.x, center.y - (radius * 0.72f)),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            // 4. Center Pivot Pin (Solid White Core - clean, modern, monochrome!)
            drawCircle(
                color = Color.White,
                radius = 1.75.dp.toPx(),
                center = center
            )
        }
    }
}