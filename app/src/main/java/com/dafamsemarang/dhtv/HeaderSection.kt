

package com.dafamsemarang.dhtv

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.input.key.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.SvgDecoder
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint

@Composable
fun HeaderSection(currentRoute: String? = "home", hazeState: HazeState? = null) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    var iconUrl by remember { mutableStateOf<String?>(null) }
    val database = Firebase.database.reference

    var configuredCity by remember { mutableStateOf<String?>(null) }
    var liveWeather by remember { mutableStateOf<LiveWeather?>(null) }
    var forecastData by remember { mutableStateOf<ForecastData?>(null) }

    val weatherData = remember(configuredCity, liveWeather, forecastData) {
        if (configuredCity != null && (liveWeather != null || forecastData != null)) {
            FirebaseWeatherData(
                config = FirebaseWeatherConfig(city = configuredCity),
                liveWeather = liveWeather,
                forecast = forecastData
            )
        } else {
            null
        }
    }
    var isFocused by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(0) } // 0 = Current Weather, 1 = Hourly Forecast, 2 = 7 Days Forecast
    var currentTimeString by remember { mutableStateOf("") }

    var redrawTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(500)
        redrawTrigger++
        delay(1000)
        redrawTrigger++
        delay(2000)
        redrawTrigger++
        delay(4000)
        redrawTrigger++
    }

    LaunchedEffect(Unit) {
        val sdfDate = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        sdfDate.timeZone = TimeZone.getTimeZone("GMT+7")
        currentTimeString = sdfDate.format(Date())
        while (isActive) {
            delay(60000)
            currentTimeString = sdfDate.format(Date())
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

    // Fetch configured city from Firebase Realtime Database
    DisposableEffect(branchId) {
        var cityRef: com.google.firebase.database.DatabaseReference? = null
        var cityListener: ValueEventListener? = null

        if (branchId != null) {
            cityRef = database.child("BRANCHES").child(branchId).child("SETTING").child("WEATHER")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    configuredCity = snapshot.child("CITY").getValue(String::class.java)
                        ?: snapshot.child("city").getValue(String::class.java)
                    Log.d("HeaderSection", "Configured city updated to: $configuredCity")
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("HeaderSection", "City config listener cancelled: ${error.message}")
                }
            }
            cityListener = listener
            cityRef.addValueEventListener(listener)
        }

        onDispose {
            if (cityRef != null && cityListener != null) {
                cityRef.removeEventListener(cityListener)
            }
        }
    }

    // Fetch live weather and forecast when configured city changes
    DisposableEffect(configuredCity) {
        var liveRef: com.google.firebase.database.DatabaseReference? = null
        var liveListener: ValueEventListener? = null
        var forecastRef: com.google.firebase.database.DatabaseReference? = null
        var forecastListener: ValueEventListener? = null

        val currentCity = configuredCity
        if (!currentCity.isNullOrEmpty()) {
            // 1. Live Weather Listener
            liveRef = database.child("weather").child("liveWeather")
            val lListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val matchingNode = if (snapshot.child(currentCity).exists()) {
                            snapshot.child(currentCity)
                        } else {
                            snapshot.children.find { 
                                it.child("city").getValue(String::class.java)?.equals(currentCity, ignoreCase = true) == true 
                            }
                        }

                        if (matchingNode != null) {
                            liveWeather = FirebaseWeatherData.parseLiveWeather(matchingNode)
                            Log.d("HeaderSection", "Parsed live weather for $currentCity")
                        } else {
                            if (snapshot.child("city").getValue(String::class.java)?.equals(currentCity, ignoreCase = true) == true) {
                                liveWeather = FirebaseWeatherData.parseLiveWeather(snapshot)
                                Log.d("HeaderSection", "Parsed single live weather node for $currentCity")
                            } else {
                                Log.w("HeaderSection", "No live weather data found matching city: $currentCity")
                                liveWeather = null
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HeaderSection", "Error parsing live weather: ${e.message}", e)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("HeaderSection", "Live weather listener cancelled: ${error.message}")
                }
            }
            liveListener = lListener
            liveRef.addValueEventListener(lListener)

            // 2. Forecast Listener
            forecastRef = database.child("weather").child("forecast")
            val fListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val matchingNode = if (snapshot.child(currentCity).exists()) {
                            snapshot.child(currentCity)
                        } else {
                            snapshot.children.find { 
                                it.child("city").getValue(String::class.java)?.equals(currentCity, ignoreCase = true) == true 
                            }
                        }

                        if (matchingNode != null) {
                            forecastData = FirebaseWeatherData.parseForecastData(matchingNode)
                            Log.d("HeaderSection", "Parsed forecast data for $currentCity")
                        } else {
                            if (snapshot.child("city").getValue(String::class.java)?.equals(currentCity, ignoreCase = true) == true) {
                                forecastData = FirebaseWeatherData.parseForecastData(snapshot)
                                Log.d("HeaderSection", "Parsed single forecast node for $currentCity")
                            } else {
                                Log.w("HeaderSection", "No forecast data found matching city: $currentCity")
                                forecastData = null
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HeaderSection", "Error parsing forecast data: ${e.message}", e)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("HeaderSection", "Forecast listener cancelled: ${error.message}")
                }
            }
            forecastListener = fListener
            forecastRef.addValueEventListener(fListener)
        } else {
            liveWeather = null
            forecastData = null
        }

        onDispose {
            if (liveRef != null && liveListener != null) {
                liveRef.removeEventListener(liveListener)
            }
            if (forecastRef != null && forecastListener != null) {
                forecastRef.removeEventListener(forecastListener)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .padding(top = 24.dp, end = 58.dp, bottom = 16.dp), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing border alpha — same as shortcut icon focus
            val focusPulseAlpha = remember { Animatable(0.4f) }
            LaunchedEffect(isFocused) {
                if (isFocused) {
                    focusPulseAlpha.animateTo(
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                } else {
                    focusPulseAlpha.snapTo(0.4f)
                }
            }

            val mainScreenOrder = listOf("home", "cantingfood", "contact", "hotel_guide")
            val getSlideDirection = { from: String?, to: String? ->
                val fromIndex = mainScreenOrder.indexOf(from ?: "home")
                val toIndex = mainScreenOrder.indexOf(to ?: "home")
                if (toIndex >= fromIndex) 1 else -1
            }

            val headerSlideDuration = 800
            val googleTvEasing = CubicBezierEasing(0.18f, 0.85f, 0.18f, 1.00f)
            val density = androidx.compose.ui.platform.LocalDensity.current
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
            val slideDistance = (screenWidthPx * 0.15f).toInt()

            AnimatedContent(
                targetState = currentRoute ?: "home",
                transitionSpec = {
                    val dir = getSlideDirection(initialState, targetState)
                    if (dir > 0) {
                        (slideInHorizontally(animationSpec = tween(headerSlideDuration, easing = googleTvEasing)) { slideDistance } + 
                         fadeIn(animationSpec = tween(headerSlideDuration, easing = googleTvEasing)))
                            .togetherWith(
                         slideOutHorizontally(animationSpec = tween(headerSlideDuration, easing = googleTvEasing)) { -slideDistance } + 
                         fadeOut(animationSpec = tween(headerSlideDuration, easing = googleTvEasing)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(headerSlideDuration, easing = googleTvEasing)) { -slideDistance } + 
                         fadeIn(animationSpec = tween(headerSlideDuration, easing = googleTvEasing)))
                            .togetherWith(
                         slideOutHorizontally(animationSpec = tween(headerSlideDuration, easing = googleTvEasing)) { slideDistance } + 
                         fadeOut(animationSpec = tween(headerSlideDuration, easing = googleTvEasing)))
                    }
                },
                label = "left_header_transition"
            ) { targetRoute ->
                if (targetRoute == "home") {
                    // Left-aligned Interactive Weather Widget (448.dp wide, matches banner)
                if (weatherData != null && weatherData?.liveWeather != null) {
                val live = weatherData!!.liveWeather!!                // Outer container: handles focus state and click
                Box(
                    modifier = Modifier
                        .padding(start = 58.dp)
                        .width(456.dp)
                        .height(88.dp)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .onFocusChanged { 
                            isFocused = it.isFocused 
                            // Preserve last page on focus loss as explicitly requested!
                        }
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown) {
                                when (keyEvent.key) {
                                    Key.DirectionRight -> {
                                        if (viewMode == 0) {
                                            viewMode = 1
                                        } else if (viewMode == 1) {
                                            viewMode = 2
                                        } else {
                                            viewMode = 0
                                        }
                                        true // Intercept event and wrap around
                                    }
                                    Key.DirectionLeft -> {
                                        if (viewMode == 2) {
                                            viewMode = 1
                                        } else if (viewMode == 1) {
                                            viewMode = 0
                                        } else {
                                            viewMode = 2
                                        }
                                        true // Intercept event and wrap around
                                    }
                                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                        true // Intercept event to do absolutely nothing (disable OK key)
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
                        }
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing Outer Focus Border (drawn outside the card with a 4.dp gap)
                    if (isFocused) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = focusPulseAlpha.value),
                                    shape = RoundedCornerShape(20.dp) // Concentric shape: 16.dp (card corner) + 4.dp (gap) = 20.dp
                                )
                        )
                    }

                    val baseAlpha = 0.40f

                    // Card Body (Always exactly 448.dp x 80.dp, no border)
                    Box(
                        modifier = Modifier
                            .width(448.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (hazeState != null) {
                                    val trigger = redrawTrigger
                                    Modifier.hazeChild(
                                        state = hazeState,
                                        shape = RoundedCornerShape(16.dp),
                                        style = HazeStyle(
                                            tint = HazeTint(Color.Transparent),
                                            blurRadius = 24.dp
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                color = Color(207, 223, 237).copy(alpha = baseAlpha),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .drawBehind {
                                // 2. Shiny Bevel & Highlights (Kaca 3D Bevel Edge)
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.03f),
                                            Color.White.copy(alpha = 0.20f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, size.height)
                                     ),
                                    cornerRadius = CornerRadius(16.dp.toPx()),
                                    style = Stroke(width = 1.2.dp.toPx())
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = viewMode,
                            transitionSpec = {
                                val isSlidingRight = targetState == (initialState + 1) % 3
                                if (isSlidingRight) {
                                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300)))
                                        .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300)))
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(300)))
                                        .togetherWith(slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(300)))
                                }
                            },
                            label = "weather_transition"
                        ) { mode ->
                            if (mode == 0) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left Column: City Info & Date
                                    Column(
                                        modifier = Modifier.weight(0.45f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Weather",
                                            fontSize = 8.sp,
                                            lineHeight = 8.sp,
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Normal,
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(
                                                    includeFontPadding = false
                                                )
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = live.city ?: "Semarang",
                                            fontSize = 16.sp,
                                            lineHeight = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(
                                                    includeFontPadding = false
                                                )
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentTimeString,
                                            fontSize = 8.sp,
                                            lineHeight = 8.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = Color.White.copy(alpha = 0.8f),
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(
                                                    includeFontPadding = false
                                                )
                                            )
                                        )
                                    }
                                    
                                    // Right Column: Temp, Label & Bottom Metrics
                                    Column(
                                        modifier = Modifier.weight(0.55f),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        val tempVal = live.temperature ?: 30.8
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            GoogleWeatherIcon(
                                                wmoCode = live.weather_code,
                                                isDay = live.is_day == 1,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", tempVal)}°C",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                style = TextStyle(
                                                    platformStyle = PlatformTextStyle(
                                                        includeFontPadding = false
                                                    )
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(1.dp))

                                        // Weather label (right-aligned)
                                        Text(
                                            text = live.weather_label ?: "Gerimis Ringan",
                                            fontSize = 8.sp,
                                            lineHeight = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                            modifier = Modifier.fillMaxWidth(),
                                            style = TextStyle(
                                                platformStyle = PlatformTextStyle(
                                                    includeFontPadding = false
                                                )
                                            )
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Wind Speed & Humidity (horizontal layout)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            WindIcon(
                                                modifier = Modifier
                                                    .size(width = 11.dp, height = 7.dp),
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            val windSpeedVal = live.wind_speed ?: 10.6
                                            Text(
                                                text = "Wind speed: ${String.format(Locale.US, "%.1f", windSpeedVal)} km/h",
                                                fontSize = 8.sp,
                                                lineHeight = 8.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                style = TextStyle(
                                                    platformStyle = PlatformTextStyle(
                                                        includeFontPadding = false
                                                    )
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            WaterDropIcon(
                                                modifier = Modifier
                                                    .size(width = 6.dp, height = 9.dp),
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "Humidity: ${live.humidity ?: 63}%",
                                                fontSize = 8.sp,
                                                lineHeight = 8.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                style = TextStyle(
                                                    platformStyle = PlatformTextStyle(
                                                        includeFontPadding = false
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            } else if (mode == 1) {
                                // Hourly Forecast Layout (9 columns)
                                val hourlyList = forecastData?.hourly ?: emptyList()
                                val startIndex = remember(hourlyList) {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.US)
                                    sdf.timeZone = TimeZone.getTimeZone("GMT+7")
                                    val nowStr = sdf.format(Date())
                                    val foundIndex = hourlyList.indexOfFirst { it.time?.startsWith(nowStr) == true }
                                    if (foundIndex != -1) {
                                        foundIndex
                                    } else {
                                        val hourSdf = SimpleDateFormat("HH", Locale.US)
                                        hourSdf.timeZone = TimeZone.getTimeZone("GMT+7")
                                        val currentHourInt = try { hourSdf.format(Date()).toInt() } catch(e: Exception) { 12 }
                                        if (currentHourInt in hourlyList.indices) currentHourInt else 0
                                    }
                                }
                                val displayHours = remember(hourlyList, startIndex) {
                                    if (hourlyList.isEmpty()) {
                                        emptyList()
                                    } else {
                                        hourlyList.subList(startIndex, (startIndex + 9).coerceAtMost(hourlyList.size))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (displayHours.isEmpty()) {
                                        Text(
                                            text = "Hourly forecast data unavailable",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        displayHours.forEach { hour ->
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                val timeText = hour.time?.substringAfter("T")?.take(5) ?: "00:00"
                                                Text(
                                                    text = timeText,
                                                    fontSize = 8.sp,
                                                    lineHeight = 8.sp,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontWeight = FontWeight.Medium,
                                                    style = TextStyle(
                                                        platformStyle = PlatformTextStyle(
                                                            includeFontPadding = false
                                                        )
                                                    )
                                                )
                                                
                                                Spacer(modifier = Modifier.height(2.dp))
                                                
                                                val hourStr = hour.time?.substringAfter("T")?.take(2)?.toIntOrNull() ?: 12
                                                val isHourDay = hourStr in 6..18
                                                GoogleWeatherIcon(
                                                    wmoCode = hour.weather_code,
                                                    isDay = isHourDay,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                
                                                Spacer(modifier = Modifier.height(1.dp))
                                                
                                                val isWarmOrSunny = hour.weather_label?.contains("Cerah", ignoreCase = true) == true || (hour.temperature ?: 0.0) >= 32.5
                                                val tempColor = if (isWarmOrSunny) Color(0xFFFFD54F) else Color.White
                                                val labelColor = if (isWarmOrSunny) Color(0xFFFFD54F).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.7f)
                                                
                                                Text(
                                                    text = hour.weather_label ?: "Cerah",
                                                    fontSize = 7.sp,
                                                    lineHeight = 7.sp,
                                                    color = labelColor,
                                                    fontWeight = if (isWarmOrSunny) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    modifier = Modifier.width(46.dp),
                                                    style = TextStyle(
                                                        platformStyle = PlatformTextStyle(
                                                            includeFontPadding = false
                                                        )
                                                    )
                                                )
                                                
                                                Spacer(modifier = Modifier.height(2.dp))
                                                
                                                Text(
                                                    text = "${String.format(Locale.US, "%.1f", hour.temperature ?: 30.0)}°C",
                                                    fontSize = 9.sp,
                                                    lineHeight = 9.sp,
                                                    color = tempColor,
                                                    fontWeight = FontWeight.Bold,
                                                    style = TextStyle(
                                                        platformStyle = PlatformTextStyle(
                                                            includeFontPadding = false
                                                        )
                                                    )
                                                )
                                                
                                                Spacer(modifier = Modifier.height(2.dp))
                                                
                                                val prob = hour.precipitation_probability ?: 0
                                                Box(
                                                    modifier = Modifier
                                                        .width(42.dp)
                                                        .height(11.dp)
                                                        .clip(RoundedCornerShape(5.5.dp))
                                                        .background(Color.White.copy(alpha = 0.12f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val progress = prob / 100f
                                                    if (progress > 0f) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxHeight()
                                                                .fillMaxWidth(progress)
                                                                .align(Alignment.CenterStart)
                                                                .background(Color(0xFF29B6F6))
                                                        )
                                                    }
                                                    Text(
                                                        text = "$prob%",
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        style = TextStyle(
                                                            platformStyle = PlatformTextStyle(
                                                                includeFontPadding = false
                                                            )
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // 7-Day Forecast Layout (7 cards)
                                val dailyList = forecastData?.daily ?: emptyList()
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (dailyList.isEmpty()) {
                                        Text(
                                            text = "7-Day forecast data unavailable",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        dailyList.take(7).forEachIndexed { index, day ->
                                            val parsedDay = try {
                                                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                                val outputFormat = SimpleDateFormat("EEE, d MMM", Locale("id", "ID"))
                                                val parsedDate = inputFormat.parse(day.date ?: "")
                                                outputFormat.format(parsedDate ?: Date())
                                            } catch (e: Exception) {
                                                day.date ?: ""
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .width(56.dp)
                                                    .fillMaxHeight()
                                                    .padding(horizontal = 2.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    // Day & Date (e.g. Jum, 22 Mei)
                                                    Text(
                                                        text = parsedDay,
                                                        fontSize = 7.sp,
                                                        lineHeight = 7.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                    )

                                                    // Condition (e.g. Gerimis Ringan)
                                                    Text(
                                                        text = day.weather_label ?: "Cerah",
                                                        fontSize = 6.sp,
                                                        lineHeight = 6.sp,
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                    )

                                                    // Weather Icon & Temperatures
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        GoogleWeatherIcon(
                                                            wmoCode = day.weather_code,
                                                            isDay = true,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        
                                                        val maxTemp = day.temp_max ?: 30.0
                                                        val minTemp = day.temp_min ?: 24.0
                                                        val isWarm = maxTemp >= 32.5 || (day.weather_label?.contains("Cerah", ignoreCase = true) == true)
                                                        
                                                        Text(
                                                            text = "${String.format(Locale.US, "%.0f", maxTemp)}°/${String.format(Locale.US, "%.0f", minTemp)}°",
                                                            fontSize = 7.sp,
                                                            lineHeight = 7.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isWarm) Color(0xFFFFD54F) else Color.White,
                                                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                        )
                                                    }

                                                    // Sunrise & Sunset with Icons
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        // Sunrise info
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                painter = painterResource(id = R.drawable.ic_sunny),
                                                                contentDescription = "Sunrise",
                                                                modifier = Modifier.size(5.dp),
                                                                tint = Color(0xFFFFD54F)
                                                            )
                                                            Spacer(modifier = Modifier.width(1.dp))
                                                            val sunriseTime = day.sunrise?.substringAfter("T")?.take(5) ?: "05:41"
                                                            Text(
                                                                text = sunriseTime,
                                                                fontSize = 5.5.sp,
                                                                lineHeight = 5.5.sp,
                                                                color = Color.White.copy(alpha = 0.8f),
                                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                            )
                                                        }
                                                        
                                                        // Sunset info
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                painter = painterResource(id = R.drawable.moon),
                                                                contentDescription = "Sunset",
                                                                modifier = Modifier.size(5.dp),
                                                                tint = Color(0xFF424242)
                                                            )
                                                            Spacer(modifier = Modifier.width(1.dp))
                                                            val sunsetTime = day.sunset?.substringAfter("T")?.take(5) ?: "17:28"
                                                            Text(
                                                                text = sunsetTime,
                                                                fontSize = 5.5.sp,
                                                                lineHeight = 5.5.sp,
                                                                color = Color.White.copy(alpha = 0.8f),
                                                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }   }
                    }

            } else {
                // Sleek loading shimmer/placeholder matching widget size
                Box(
                    modifier = Modifier
                        .padding(start = 58.dp)
                        .width(456.dp)
                        .height(88.dp)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .onFocusChanged { isFocused = it.isFocused }
                        .focusable(),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing Outer Focus Border (drawn outside the card with a 4.dp gap)
                    if (isFocused) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = focusPulseAlpha.value),
                                    shape = RoundedCornerShape(20.dp) // Concentric shape: 16.dp (card corner) + 4.dp (gap) = 20.dp
                                )
                        )
                    }

                    val baseAlpha = 0.40f

                    // Card Body (Always exactly 448.dp x 80.dp, no border)
                    Box(
                        modifier = Modifier
                            .width(448.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .then(
                                if (hazeState != null) {
                                    val trigger = redrawTrigger
                                    Modifier.hazeChild(
                                        state = hazeState,
                                        shape = RoundedCornerShape(16.dp),
                                        style = HazeStyle(
                                            tint = HazeTint(Color.Transparent),
                                            blurRadius = 24.dp
                                        )
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                color = Color(207, 223, 237).copy(alpha = baseAlpha),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .drawBehind {
                                // 2. Shiny Bevel & Highlights (Kaca 3D Bevel Edge)
                                drawRoundRect(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.03f),
                                            Color.White.copy(alpha = 0.20f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, size.height)
                                    ),
                                    cornerRadius = CornerRadius(16.dp.toPx()),
                                    style = Stroke(width = 1.2.dp.toPx())
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Column Placeholder (City Info)
                            Column(
                                modifier = Modifier.weight(0.45f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(8.dp)
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(16.dp)
                                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(100.dp)
                                        .height(8.dp)
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                                )
                            }

                            // Right Column Placeholder (Temp, Label, Metrics)
                            Column(
                                modifier = Modifier.weight(0.55f),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(16.dp)
                                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(8.dp)
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(8.dp)
                                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
                                )
                            }
                        }
                }
            }
        }
    } else {
            // Replacement container for non-home screens
            val headerReplacement = when (targetRoute) {
                    "cantingfood" -> Pair(R.drawable.room_service_3_svgrepo_com, "Room Service")
                    "contact" -> Pair(R.drawable.service_request_svgrepo_com, "Request Service")
                    "hotel_guide" -> Pair(R.drawable.info_circle_svgrepo_com, "Hotel Info")
                    else -> Pair(R.drawable.info_circle_svgrepo_com, "Hotel Info")
                }
                val (iconRes, headerText) = headerReplacement

                Box(
                    modifier = Modifier
                        .padding(start = 58.dp)
                        .width(448.dp)
                        .height(80.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 0.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = headerText,
                                modifier = Modifier.size(36.dp),
                                tint = Color.White.copy(alpha = 0.3f)
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "GUEST COMPANION",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 1.5.sp,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = headerText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE3F2FD),
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                )
                            )
                        }
                    }
                    }
                }
            }
        }

            // 2. Company Logo on the right side of the header in a beautiful square glassmorphic container
            var isIconLoadError by remember(iconUrl) { mutableStateOf(false) }
            val logoContext = LocalContext.current
            val svgAwareImageLoader = remember(logoContext) {
                ImageLoader.Builder(logoContext)
                    .components { add(SvgDecoder.Factory()) }
                    .build()
            }
            
            val showPlaceholder = iconUrl.isNullOrEmpty() || isIconLoadError

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (showPlaceholder) {
                    Text(
                        text = "DAFAM",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    AsyncImage(
                        model = iconUrl,
                        imageLoader = svgAwareImageLoader,
                        contentDescription = "Company Logo",
                        modifier = Modifier
                            .fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(HeaderIcon),
                        onError = { isIconLoadError = true }
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleWeatherIcon(wmoCode: Int?, isDay: Boolean, modifier: Modifier = Modifier) {
    val iconUrl = mapWmoCodeToGoogleIconUrl(wmoCode, isDay)
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
    AsyncImage(
        model = iconUrl,
        imageLoader = imageLoader,
        contentDescription = "Weather Icon",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

fun mapWmoCodeToGoogleIconUrl(wmoCode: Int?, isDay: Boolean): String {
    val baseName = if (wmoCode == null) {
        if (isDay) "sunny" else "clear"
    } else {
        when (wmoCode) {
            0 -> if (isDay) "sunny" else "clear"
            1 -> if (isDay) "mostly_sunny" else "mostly_clear"
            2 -> if (isDay) "partly_cloudy" else "partly_clear"
            3 -> if (isDay) "mostly_cloudy" else "mostly_cloudy_night"
            45, 48 -> "mist"
            51, 53, 55 -> "drizzle"
            56, 57 -> "wintry_mix"
            61, 63 -> "showers"
            65 -> "heavy"
            66, 67 -> "wintry_mix"
            71, 73 -> "snow_showers"
            75 -> "heavy_snow"
            77 -> "flurries"
            80, 81 -> "showers"
            82 -> "heavy"
            85 -> "scattered_snow"
            86 -> "heavy_snow"
            95 -> "strong_tstorms"
            96, 99 -> "strong_tstorms"
            else -> if (isDay) "sunny" else "clear"
        }
    }
    // Since our TV app is dark themed, always append _dark suffix for beautiful contrast.
    // SVG format is used for absolute crispness and sharpness on high-resolution screens.
    return "https://maps.gstatic.com/weather/v1/${baseName}_dark.svg"
}

@Composable
fun AnimatedClockIcon(modifier: Modifier = Modifier) {
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
            
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.25.dp.toPx())
            )
            
            rotate(hourHandAngle) {
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(center.x, center.y - (radius * 0.5f)),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            rotate(minuteHandAngle) {
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(center.x, center.y - (radius * 0.72f)),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            drawCircle(
                color = Color.White,
                radius = 1.75.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
fun WindIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Icon(
        painter = painterResource(id = R.drawable.ic_wind_custom),
        contentDescription = "Wind Speed",
        modifier = modifier,
        tint = color
    )
}

@Composable
fun WaterDropIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Icon(
        painter = painterResource(id = R.drawable.ic_humidity_custom),
        contentDescription = "Humidity",
        modifier = modifier,
        tint = color
    )
}