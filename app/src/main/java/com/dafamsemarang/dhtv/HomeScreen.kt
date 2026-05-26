package com.dafamsemarang.dhtv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.navigation.NavHostController
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import okio.sink
import java.io.File
import java.io.IOException
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.foundation.focusable
import androidx.compose.foundation.indication
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester

// Data class for supported apps
// ADD GLOBAL PERSISTENT HOLDER TO SURVIVE ACTIVITY AND COMPOSE LIFECYCLE DROPS
object GlobalMediaPlayerHolder {
    var sharedMediaPlayer: android.media.MediaPlayer? = null
    var videoWasPaused: Boolean = false
    var videoHasStarted: Boolean = false
    
    // PERSISTENCE PROTOCOL: Save precise playback details to hardware storage
    fun savePlaybackState(context: android.content.Context, index: Int, pos: Int) {
        try {
            val prefs = context.getSharedPreferences("video_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("saved_index", index)
                .putInt("saved_position", pos)
                .putBoolean("was_paused", true)
                .apply()
            android.util.Log.d("GlobalPlayer", "DISK PERSISTENCE - Locked state -> Index: $index, Position: ${pos}ms")
        } catch (e: Exception) {
            android.util.Log.e("GlobalPlayer", "Disk save failed: " + e.message)
        }
    }
}

data class SupportedApp(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable? = null,
    val banner: android.graphics.drawable.Drawable? = null,
    val installerPackageName: String? = null
)

// Helper to fetch installed apps
fun getInstalledApps(context: Context): List<SupportedApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    
    // Also try LEANBACK_LAUNCHER for TV apps
    val tvIntent = Intent(Intent.ACTION_MAIN, null)
    tvIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

    val apps = mutableListOf<SupportedApp>()
    val seenPackages = mutableSetOf<String>()

    val resolveInfos = pm.queryIntentActivities(intent, 0) + pm.queryIntentActivities(tvIntent, 0)

    for (resolveInfo in resolveInfos) {
        val packageName = resolveInfo.activityInfo.packageName
        if (packageName !in seenPackages && packageName != context.packageName) {
            seenPackages.add(packageName) // Fix: Mark package as seen to prevent duplicates
            
            // Try to load banner
            var banner = resolveInfo.activityInfo.loadBanner(pm)
            if (banner == null) {
                banner = resolveInfo.activityInfo.applicationInfo.loadBanner(pm)
            }

            val installer = try {
                pm.getInstallerPackageName(packageName)
            } catch (e: Exception) {
                null
            }

            apps.add(
                SupportedApp(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(pm).toString(),
                    icon = resolveInfo.loadIcon(pm),
                    banner = banner,
                    installerPackageName = installer
                )
            )
        }
    }
    return apps.sortedBy { it.label }
}

//// Helper function to download and cache the video
fun downloadAndCacheVideo(
    videoUrl: String,
    cacheFile: File,
    onSuccess: (File) -> Unit,
    onError: (Exception) -> Unit
) {
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(videoUrl)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                response.body?.let { body ->
                    // Save the video to cache file
                    body.source().use { source ->
                        cacheFile.outputStream().use { outputStream ->
                            source.readAll(outputStream.sink())
                        }
                    }
                    // Video cached successfully
                    onSuccess(cacheFile)
                } ?: onError(Exception("Empty response body"))
            } else {
                onError(Exception("Failed to download video"))
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            onError(e)
        }
    })
}

var currentVideoIndex = 0

fun isVideoUrlValid(videoUrl: String, validVideoUrls: List<String>): Boolean {
    return validVideoUrls.contains(videoUrl)
}

fun deleteInvalidCachedVideo(context: Context, videoUrl: String) {
    val cacheFile = File(context.cacheDir, videoUrl.hashCode().toString() + ".mp4")

    if (cacheFile.exists()) {
        cacheFile.delete()
        Log.d("Cache", "Deleted invalid cache for $videoUrl")
    }
}

fun playNextVideo(
    context: Context,
    videoUrls: List<String>,
    mediaPlayer: MediaPlayer,
    textureView: TextureView,
    forceNext: Boolean = false
) {
    if (videoUrls.isEmpty() || textureView.surfaceTexture == null) return

    // HARDWARE PERSISTENCE DECODER: Check if we returned from process cold-boot!
    val prefs = context.getSharedPreferences("video_prefs", Context.MODE_PRIVATE)
    val savedIndex = prefs.getInt("saved_index", -1)
    val savedPos = prefs.getInt("saved_position", 0)

    if (!forceNext && savedIndex >= 0 && savedIndex < videoUrls.size) {
        // COLD BOOT ESCAPE: Instantly recover state from hardware disk!
        currentVideoIndex = savedIndex
        Log.d("VideoPlayer", "Restored previous cold-boot video index: $currentVideoIndex")
    } else {
        // STANDARD ROTATION
        currentVideoIndex = (currentVideoIndex + 1) % videoUrls.size
    }

    // Wipe disk storage triggers once decoded to restore natural lifecycle
    prefs.edit().remove("saved_index").remove("saved_position").apply()

    val videoUrl = videoUrls[currentVideoIndex]
    val cachedVideoFile = File(context.cacheDir, videoUrl.hashCode().toString() + ".mp4")

    if (!isVideoUrlValid(videoUrl, videoUrls)) {
        deleteInvalidCachedVideo(context, videoUrl)
        playNextVideo(context, videoUrls, mediaPlayer, textureView, forceNext = true)
        return
    }

    val playVideo = { file: File ->
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, Uri.fromFile(file))
            val surface = android.view.Surface(textureView.surfaceTexture)
            mediaPlayer.setSurface(surface)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                if (savedPos > 0) {
                    // INSTANT RECOVERY SEEK: Continue where the hardware decoder died!
                    mediaPlayer.seekTo(savedPos)
                    Log.d("VideoPlayer", "Successfully recovered playback position: ${savedPos}ms")
                }
                mediaPlayer.start()
            }
            mediaPlayer.setOnCompletionListener {
                // Completion always forces rotation to Next!
                playNextVideo(context, videoUrls, mediaPlayer, textureView, forceNext = true)
            }
            mediaPlayer.setOnErrorListener { _, what, extra ->
                Log.e("MediaPlayer", "Playback error: what=$what, extra=$extra")
                true
            }
        } catch (e: Exception) {
            Log.e("VideoCache", "Failed to play video", e)
        }
    }

    if (!cachedVideoFile.exists()) {
        downloadAndCacheVideo(videoUrl, cachedVideoFile, 
            onSuccess = { playVideo(it) },
            onError = { exception ->
                Log.e("VideoCache", "Failed to cache video", exception)
            })
    }
}

fun parseUtcToWib(timeStr: String): Date? {
    if (timeStr.isEmpty()) return null
    var cleanStr = timeStr.trim()
    val formats = listOf(
        "yyyy-MM-dd HH:mm'Z'",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm'Z'"
    )
    for (format in formats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(cleanStr)
            if (date != null) return date
        } catch (e: Exception) {}
    }
    try {
        cleanStr = cleanStr.replace("Z", "")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.parse(cleanStr)
    } catch (e: Exception) {}
    return null
}

fun isSameDayInWib(date: Date): Boolean {
    val todayWib = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("GMT+7")
    }
    val todayStr = todayWib.format(Date())
    val flightStr = todayWib.format(date)
    return todayStr == flightStr
}

@Composable
fun FlightInfoSection(
    title: String,
    airportName: String,
    flights: List<Flight>,
    isArrival: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(top = 6.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, bottom = 2.dp)
            ) {
                // Title on the left
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        painter = painterResource(id = if (isArrival) R.drawable.flight_land else R.drawable.flight_takeoff),
                        contentDescription = null,
                        tint = if (isArrival) Color(0xFF29B6F6) else Color(0xFFFF9800),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isArrival) Color(0xFF29B6F6) else Color(0xFFFF9800),
                        letterSpacing = 1.sp
                    )
                }

                // Airport Name in the center
                Text(
                    text = airportName.uppercase(Locale.US),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Column Header Row (9 columns, with flight number using empty header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MASKAPAI",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    modifier = Modifier.weight(0.15f)
                )
                Text(
                    text = "NOMOR",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    modifier = Modifier.weight(0.08f)
                )
                Text(
                    text = if (isArrival) "DARI" else "KE",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    modifier = Modifier.weight(0.17f)
                )
                Text(
                    text = "BANDARA",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    modifier = Modifier.weight(0.21f)
                )
                Text(
                    text = "IATA",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    modifier = Modifier.weight(0.07f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "GATE",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    modifier = Modifier.weight(0.06f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "TERM",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    modifier = Modifier.weight(0.06f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "JAM",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    modifier = Modifier.weight(0.08f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "STATUS",
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.5.sp,
                    modifier = Modifier.weight(0.12f),
                    textAlign = TextAlign.Center
                )
            }

            if (flights.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No flights scheduled",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 9.sp
                    )
                }
            } else {
                flights.forEach { flight ->
                    FlightRow(flight = flight, isArrival = isArrival)
                }
            }
            
            // Bottom breathing room spacer to avoid dots overlapping
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun FlightRow(flight: Flight, isArrival: Boolean) {
    val cityName = remember(flight.otherAirport) {
        when (flight.otherAirport.uppercase(Locale.US)) {
            "CGK" -> "Jakarta"
            "KUL" -> "Kuala Lumpur"
            "PKN" -> "Pangkalan Bun"
            "SMQ" -> "Sampit"
            "DPS" -> "Bali"
            "PKY" -> "Palangkaraya"
            "PNK" -> "Pontianak"
            "BPN" -> "Balikpapan"
            "BTH" -> "Batam"
            "BDJ" -> "Banjarmasin"
            "UPG" -> "Makassar"
            "SIN" -> "Singapore"
            "SUB" -> "Surabaya"
            "SRG" -> "Semarang"
            "YIA" -> "Yogyakarta"
            else -> flight.otherAirport
        }
    }

    val airportNameLong = remember(flight.otherAirport) {
        when (flight.otherAirport.uppercase(Locale.US)) {
            "CGK" -> "Soekarno-Hatta"
            "KUL" -> "Kuala Lumpur Int'l"
            "PKN" -> "Iskandar"
            "SMQ" -> "H. Asan"
            "DPS" -> "Ngurah Rai"
            "PKY" -> "Tjilik Riwut"
            "PNK" -> "Supadio"
            "BPN" -> "SAMS Sepinggan"
            "BTH" -> "Hang Nadim"
            "BDJ" -> "Syamsudin Noor"
            "UPG" -> "Sultan Hasanuddin"
            "SIN" -> "Changi"
            "SUB" -> "Juanda"
            "SRG" -> "Jenderal Ahmad Yani"
            "YIA" -> "Yogyakarta Int'l"
            else -> flight.otherAirport
        }
    }

    val timeStr = remember(flight.revisedTime, flight.scheduledTime) {
        val rawTime = flight.revisedTime.ifEmpty { flight.scheduledTime }
        val date = parseUtcToWib(rawTime)
        if (date != null) {
            val wibFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT+7")
            }
            wibFormat.format(date)
        } else {
            try {
                val idx = rawTime.indexOf(' ')
                if (idx != -1 && rawTime.length >= idx + 6) {
                    rawTime.substring(idx + 1, idx + 6)
                } else {
                    rawTime.replace("Z", "").takeLast(5)
                }
            } catch (e: Exception) {
                rawTime
            }
        }
    }

    val statusColor = when (flight.status.uppercase(Locale.US)) {
        "EXPECTED", "LANDED", "ARRIVED" -> Color(0xFF81C784)
        "DELAYED" -> Color(0xFFFFB74D)
        "DEPARTED" -> Color.White.copy(alpha = 0.4f)
        else -> Color.White.copy(alpha = 0.7f)
    }

    val statusText = remember(flight.status) {
        if (flight.status.trim().uppercase(Locale.US) == "UNKNOWN" || flight.status.trim().isEmpty()) {
            "-"
        } else {
            flight.status
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(19.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Airline Name (weight = 0.15f)
        Text(
            text = flight.airline,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 8.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.15f)
        )

        // Column 2: Flight Number (weight = 0.08f, soft style, aligned Left)
        Text(
            text = flight.flightNumber,
            color = Color.White.copy(alpha = 0.55f),
            fontWeight = FontWeight.Normal,
            fontSize = 8.sp,
            maxLines = 1,
            textAlign = TextAlign.Left,
            modifier = Modifier.weight(0.08f)
        )

        // Column 3: Destination/Origin City Name (weight = 0.17f, shifted left, increased area)
        Text(
            text = cityName,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 8.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.17f)
        )

        // Column 4: Mapped Airport Long Name (weight = 0.21f)
        Text(
            text = airportNameLong,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 8.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.21f)
        )

        // Column 5: Airport Code (weight = 0.07f, IATA)
        Text(
            text = flight.otherAirport.uppercase(Locale.US),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 8.5.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.07f)
        )

        // Column 6: Gate (weight = 0.06f)
        val gateText = remember(flight.gate) {
            if (flight.gate.trim().isEmpty() || flight.gate.trim() == "-") {
                "-"
            } else {
                flight.gate
            }
        }
        Text(
            text = gateText,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 8.5.sp,
            maxLines = 1,
            modifier = Modifier.weight(0.06f),
            textAlign = TextAlign.Center
        )

        // Column 7: Terminal (weight = 0.06f)
        val terminalText = remember(flight.terminal) {
            if (flight.terminal.trim().isEmpty() || flight.terminal.trim() == "-") {
                "-"
            } else {
                flight.terminal
            }
        }
        Text(
            text = terminalText,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 8.5.sp,
            maxLines = 1,
            modifier = Modifier.weight(0.06f),
            textAlign = TextAlign.Center
        )

        // Column 8: Time (weight = 0.08f)
        Text(
            text = timeStr,
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium,
            fontSize = 8.5.sp,
            modifier = Modifier.weight(0.08f),
            textAlign = TextAlign.Center
        )

        // Column 9: Status (weight = 0.12f)
        Text(
            text = statusText,
            color = statusColor,
            fontWeight = FontWeight.Bold,
            fontSize = 7.5.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.12f)
        )
    }
}

@Composable
fun VideoAndSlideshowSection(
    context: Context,
    videoUrls: List<String>,
    imageList: List<String>,
    currentImageIndex: Int,
    wasPausedState: MutableState<Boolean>? = null,
    videoStartedState: MutableState<Boolean>? = null,
    sharedMediaPlayer: MutableState<MediaPlayer?>? = null,
    isLoadingVideos: Boolean = false,
    isLoadingSlideshow: Boolean = false,
    guestInfo: GuestInfo?,
    roomId: String?,
    roomTypeText: String,
    currentTime: String,
    onImageIndexChanged: (Int) -> Unit = {},
    flightArrivals: List<Flight> = emptyList(),
    flightDepartures: List<Flight> = emptyList(),
    flightAirportName: String = "",
    fidsActive: Boolean = true,
    onBannerFocusChanged: (Boolean) -> Unit = {},
    onNavigateDown: () -> Unit = {}
) {
    // Use shared MediaPlayer if provided, otherwise create local one
    var localMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val mediaPlayer = sharedMediaPlayer?.value ?: localMediaPlayer
    val setMediaPlayer: (MediaPlayer?) -> Unit = { player ->
        if (sharedMediaPlayer != null) {
            sharedMediaPlayer.value = player
        } else {
            localMediaPlayer = player
        }
    }
    
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Track if video was paused (not stopped) so we can resume it
    // Use provided state if available, otherwise use local state
    var wasPausedLocal by remember { mutableStateOf(GlobalMediaPlayerHolder.videoWasPaused) }
    val wasPaused = wasPausedState?.value ?: wasPausedLocal
    val setWasPaused: (Boolean) -> Unit = { value ->
        GlobalMediaPlayerHolder.videoWasPaused = value
        if (wasPausedState != null) {
            wasPausedState.value = value
        } else {
            wasPausedLocal = value
        }
    }
    
    // Track if video has been started at least once (to prevent restart on recomposition)
    // Use provided state if available, otherwise use local state
    var videoStartedLocal by remember { mutableStateOf(GlobalMediaPlayerHolder.videoHasStarted) }
    val videoStarted = videoStartedState?.value ?: videoStartedLocal
    val setVideoStarted: (Boolean) -> Unit = { value ->
        GlobalMediaPlayerHolder.videoHasStarted = value
        if (videoStartedState != null) {
            videoStartedState.value = value
        } else {
            videoStartedLocal = value
        }
    }
    
    // Use rememberUpdatedState to always get the latest mediaPlayer reference
    val currentMediaPlayer by rememberUpdatedState(mediaPlayer)
    val currentTextureViewRef by rememberUpdatedState(textureViewRef)

    // Helper function to stop video
    val stopVideo: () -> Unit = remember {
        {
            currentMediaPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                        setWasPaused(false) // Reset flag when stopped
                        Log.d("VideoPlayer", "Video stopped")
                }
            } catch (e: Exception) {
                    Log.e("VideoPlayer", "Error stopping video: ${e.message}")
                }
            }
        }
    }

    // Helper function to pause video
    val pauseVideo: () -> Unit = remember {
        {
            currentMediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.pause()
                        setWasPaused(true) // Mark that video was paused (not stopped)
                        Log.d("VideoPlayer", "Video paused - wasPaused set to true")
                    }
                } catch (e: Exception) {
                    Log.e("VideoPlayer", "Error pausing video: ${e.message}")
                }
            }
        }
    }

    // Helper function to resume video
    val resumeVideo: () -> Unit = remember {
        {
            currentMediaPlayer?.let { player ->
                try {
                    // Check if player is paused (not stopped) and can be resumed
                    if (!player.isPlaying && currentTextureViewRef != null && currentTextureViewRef?.surfaceTexture != null) {
                        // Try to get current position to check if player is paused or stopped
                        val currentPosition = try {
                            player.currentPosition
                        } catch (e: Exception) {
                            -1 // If we can't get position, player might be stopped
                        }
                        
                        if (currentPosition >= 0 || wasPaused) {
                            // Player is paused (or was marked as paused), can resume
                            try {
                                player.start()
                                setWasPaused(false) // Reset flag after successful resume
                                Log.d("VideoPlayer", "Video resumed from position: $currentPosition ms")
                            } catch (e: Exception) {
                                Log.e("VideoPlayer", "Error starting video: ${e.message}", e)
                                // If start fails, player might be in wrong state, try to reset
                                setWasPaused(false)
                            }
                        } else {
                            // Player was stopped, cannot resume
                            Log.d("VideoPlayer", "Video was stopped (not paused), cannot resume - position: $currentPosition")
                            setWasPaused(false)
                        }
                    } else {
                        Log.d("VideoPlayer", "Cannot resume - isPlaying: ${player.isPlaying}, texture: ${currentTextureViewRef != null}, surface: ${currentTextureViewRef?.surfaceTexture != null}")
                    }
                } catch (e: Exception) {
                    Log.e("VideoPlayer", "Error resuming video: ${e.message}", e)
                    setWasPaused(false)
                }
            }
        }
    }

    // IMPORTANT: If sharedMediaPlayer is provided, use it and NEVER release it
    // Only pause when navigating away
    DisposableEffect(sharedMediaPlayer) {
        // If sharedMediaPlayer is provided, we don't need to create or manage local MediaPlayer
        // Just use the shared one
        if (sharedMediaPlayer != null) {
            Log.d("VideoPlayer", "Using shared MediaPlayer - will not release on dispose")
        }
        
        onDispose {
            // IMPORTANT: NEVER release MediaPlayer when navigating to other screens
            // Only pause it so it can resume when coming back
            // MediaPlayer will be released only when HomeScreen is completely disposed
            val player = currentMediaPlayer
            if (player != null) {
            try {
                    // Just pause, don't stop or release
                if (player.isPlaying) {
                        player.pause()
                        setWasPaused(true)
                        wasPausedState?.value = true
                        videoStartedState?.value = true
                        Log.d("VideoPlayer", "MediaPlayer paused on dispose (NOT released) - state saved for resume")
                    } else {
                        // Still save state even if not playing
                        setWasPaused(true)
                        wasPausedState?.value = true
                        videoStartedState?.value = true
                        Log.d("VideoPlayer", "MediaPlayer state saved on dispose (NOT released)")
                    }
                } catch (e: Exception) {
                    Log.e("VideoPlayer", "Error pausing MediaPlayer on dispose: ${e.message}", e)
                }
            }
            // CRITICAL: Don't release or set to null, keep MediaPlayer for resume
            Log.d("VideoPlayer", "VideoAndSlideshowSection disposed - MediaPlayer kept alive for resume")
        }
    }

    // Listen to lifecycle events to pause/resume video when app loses/gains focus
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // ANTI-PAUSE SHIELD: Do NOT pause the video for momentary system focus shifts (like swallowed Home keys)!
                    Log.d("VideoPlayer", "Lifecycle ON_PAUSE - Shield active: Ignoring pause to keep video playing on Home click!")
                }
                Lifecycle.Event.ON_STOP -> {
                    // HARD EXIT: The user has actually launched another app (e.g. YouTube). Pause and Lock to Disk!
                    Log.d("VideoPlayer", "Lifecycle ON_STOP - App went to background. Pausing safely and locking to disk.")
                    currentMediaPlayer?.let { player ->
                        try {
                            if (player.isPlaying) {
                                val currentPosition = player.currentPosition
                                player.pause()
                                setWasPaused(true)
                                // Lock data to absolute hardware storage to withstand YouTube RAM purges!
                                GlobalMediaPlayerHolder.savePlaybackState(context, currentVideoIndex, currentPosition)
                                Log.d("VideoPlayer", "Video safely paused on ON_STOP & PERSISTED TO DISK (pos: ${currentPosition}ms)")
                            }
                        } catch (e: Exception) {
                            Log.e("VideoPlayer", "Error handling exit in ON_STOP: " + e.message)
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("VideoPlayer", "Lifecycle ON_RESUME - check wasPaused: $wasPaused")
                    if (currentMediaPlayer != null && videoUrls.isNotEmpty()) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                val player = currentMediaPlayer
                                val textureView = currentTextureViewRef
                                
                                if (player != null && textureView != null && textureView.isAvailable && textureView.surfaceTexture != null) {
                                    if (wasPaused) {
                                        // Video was only paused, just resume it smoothly
                                        Log.d("VideoPlayer", "ON_RESUME: Resuming video smoothly")
                                        resumeVideo()
                                    } else {
                                        // Video was fully stopped, restart from beginning
                                        if (player.isPlaying) {
                                            player.stop()
                                        }
                                        player.reset()
                                        setWasPaused(false)
                                        setVideoStarted(false)
                                        Log.d("VideoPlayer", "ON_RESUME: Restarting video from beginning")
                                        playNextVideo(context, videoUrls, player, textureView)
                                    }
                                } else {
                                    Log.d("VideoPlayer", "ON_RESUME: Texture not ready yet")
                                }
                            } catch (e: Exception) {
                                Log.e("VideoPlayer", "Error during ON_RESUME: ${e.message}", e)
                            }
                        }, 200)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Also listen to window focus changes for better handling when opening other apps (launcher scenario)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        var windowFocusListener: ((Boolean) -> Unit)? = null
        
        // Set up window focus listener using Activity's onWindowFocusChanged
        if (activity != null) {
            windowFocusListener = { hasFocus: Boolean ->
                Log.d("VideoPlayer", "Window focus changed: hasFocus=$hasFocus")
                if (!hasFocus) {
                    // ANTI-FOCUS SHIELD: Swallowed Home keys trigger a momentary focus loss. Ignore it to keep playing!
                    Log.d("VideoPlayer", "Window focus lost - Shield active: Ignoring pause to prevent freezing on Home press!")
                } else {
                    // App gained focus - resume if wasPaused, or restart if fully stopped
                    Log.d("VideoPlayer", "Window gained focus - checking resume scenario")
                    if (currentMediaPlayer != null && videoUrls.isNotEmpty()) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                val player = currentMediaPlayer
                                val textureView = currentTextureViewRef
                                
                                if (player != null && textureView != null && textureView.isAvailable && textureView.surfaceTexture != null) {
                                    if (wasPaused) {
                                        // Video was paused transiently (e.g. Home button click), resume instantly!
                                        Log.d("VideoPlayer", "Window focus: Resuming video smoothly")
                                        resumeVideo()
                                    } else {
                                        // Only force full restart if it was NOT in a paused state
                                        if (player.isPlaying) {
                                            player.stop()
                                        }
                                        player.reset()
                                        setWasPaused(false)
                                        setVideoStarted(false)
                                        Log.d("VideoPlayer", "Window focus: Restarting video from beginning")
                                        playNextVideo(context, videoUrls, player, textureView)
                                    }
                                } else {
                                    // Texture not ready block - check if we can still attempt safe resume
                                    if (wasPaused) {
                                        Log.d("VideoPlayer", "Window focus: wasPaused but texture looks unready, safe-resuming player")
                                        resumeVideo()
                                    } else {
                                        // Fresh retry loop
                                        Log.d("VideoPlayer", "Window focus: Texture not ready for fresh play, retrying...")
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            if (textureView != null && textureView.isAvailable && textureView.surfaceTexture != null) {
                                                val p2 = currentMediaPlayer
                                                if (p2 != null) {
                                                    if (p2.isPlaying) p2.stop()
                                                    p2.reset()
                                                    setWasPaused(false)
                                                    setVideoStarted(false)
                                                    playNextVideo(context, videoUrls, p2, textureView)
                                                }
                                            }
                                        }, 300)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("VideoPlayer", "Error recovering video on focus: ${e.message}", e)
                            }
                        }, 200)
                    }
                }
            }
            
            // Store listener in activity for access from onWindowFocusChanged
            // Note: This requires modifying MainActivity to call this listener
            // For now, we'll rely on lifecycle events which should work for launcher apps
        }
        
        onDispose {
            windowFocusListener = null
        }
    }

    // Track lifecycle state to trigger video restart when returning from other apps
    var lifecycleState by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    
    // Update lifecycle state when it changes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            lifecycleState = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // LaunchedEffect to handle video playback/resume when videoUrls change or when returning to this composable
    LaunchedEffect(videoUrls, textureViewRef?.isAvailable, lifecycleState) {
        // Always use currentMediaPlayer to get the latest reference (shared or local)
        val player = currentMediaPlayer
        val textureView = textureViewRef
        
        // Stop video if videoUrls is empty
        if (videoUrls.isEmpty()) {
            stopVideo()
            setWasPaused(false)
            setVideoStarted(false)
            return@LaunchedEffect
        }
        
        // Wait for texture view to be available
        if (textureView == null || !textureView.isAvailable) {
            Log.d("VideoPlayer", "Texture view not available yet, waiting...")
            return@LaunchedEffect
        }
        
        // Only start/restart video if lifecycle is RESUMED (app is active)
        if (lifecycleState != Lifecycle.State.RESUMED) {
            Log.d("VideoPlayer", "Lifecycle not RESUMED (current: $lifecycleState), skipping video start")
            return@LaunchedEffect
        }
        
        // Always restart video from beginning when returning (no resume)
        if (videoUrls.isNotEmpty() && player != null) {
            try {
                // Check if video is already playing
                if (player.isPlaying) {
                    // Video is already playing, do nothing
                    Log.d("VideoPlayer", "Video already playing, skipping restart")
                    setVideoStarted(true)
                    wasPausedState?.value = false
                    videoStartedState?.value = true
                    return@LaunchedEffect
                }
                
                // Always restart video from beginning when returning (no resume)
                Log.d("VideoPlayer", "Starting video playback from beginning (LaunchedEffect) - lifecycle: $lifecycleState")
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                setWasPaused(false)
                setVideoStarted(true)
                // Update parent state immediately when video starts
                wasPausedState?.value = false
                videoStartedState?.value = true
                Log.d("VideoPlayer", "Video started - parent state updated (wasPaused: false, videoStarted: true)")
                playNextVideo(context, videoUrls, player, textureView)
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Error playing video", e)
                setWasPaused(false)
                setVideoStarted(false)
            }
        }
    }
    
    // Resume functionality removed - video will restart from beginning when returning
    
    // Track if composable is currently active (not paused/stopped)
    var isComposableActive by remember { mutableStateOf(true) }
    
    // Listen to lifecycle to track if composable is active
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    isComposableActive = false
                    Log.d("VideoPlayer", "Composable became inactive")
                }
                Lifecycle.Event.ON_RESUME -> {
                    isComposableActive = true
                    Log.d("VideoPlayer", "Composable became active")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    
    // Periodic check removed - video will restart from beginning when returning, no resume
    

    

    // Tracks last manual navigation direction – hoisted so the auto-scroll timer can reset it
    var isNavigatingLeft by remember { mutableStateOf(false) }

    // Slideshow Images with shimmer (Widescreen landscape banner on the left)
    if (isLoadingSlideshow || imageList.isNotEmpty()) {
        val arrivalsPagesCount = if (flightArrivals.isEmpty()) 1 else ((flightArrivals.size + 3) / 4)
        val departuresPagesCount = if (flightDepartures.isEmpty()) 1 else ((flightDepartures.size + 3) / 4)
        val fidsSlidesCount = if (fidsActive) (arrivalsPagesCount + departuresPagesCount) else 0
        val totalSlidesCount = imageList.size + fidsSlidesCount
        var isBannerFocusedInternal by remember { mutableStateOf(false) }
        val isBannerFocused = isBannerFocusedInternal

        // Reset arah navigasi ke kanan setelah index berubah (hanya saat TIDAK sedang di-focus/auto-scroll)
        LaunchedEffect(currentImageIndex) {
            delay(400) // Beri waktu animasi transisi selesai dulu
            if (!isBannerFocusedInternal) {
                isNavigatingLeft = false
            }
        }

        val bannerFocusPulseAlpha = remember { Animatable(0.4f) }
        LaunchedEffect(isBannerFocusedInternal) {
            if (!isBannerFocusedInternal) {
                delay(150) // Filter out temporary focus changes during recomposition/transitions
                if (!isBannerFocusedInternal) {
                    isNavigatingLeft = false // Kembalikan arah pesawat ke kanan saat banner benar-benar kehilangan fokus
                }
            }
            if (isBannerFocusedInternal) {
                bannerFocusPulseAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            } else {
                bannerFocusPulseAlpha.snapTo(0.4f)
            }
        }

        Box(
            modifier = Modifier
                .requiredWidth(456.dp)
                .requiredHeight(168.dp)
                .offset(x = (-4).dp, y = 0.dp)
                .onFocusChanged { 
                    isBannerFocusedInternal = it.isFocused 
                    onBannerFocusChanged(it.isFocused)
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionRight -> {
                                if (imageList.isNotEmpty() && totalSlidesCount > 0) {
                                    isNavigatingLeft = false
                                    val nextIndex = (currentImageIndex + 1) % totalSlidesCount
                                    onImageIndexChanged(nextIndex)
                                    true
                                } else false
                            }
                            Key.DirectionLeft -> {
                                if (imageList.isNotEmpty() && totalSlidesCount > 0) {
                                    isNavigatingLeft = true
                                    val prevIndex = (currentImageIndex - 1 + totalSlidesCount) % totalSlidesCount
                                    onImageIndexChanged(prevIndex)
                                    true
                                } else false
                            }
                            Key.DirectionDown -> {
                                onNavigateDown()
                                true
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                true // Intercept OK click doing nothing
                            }
                            else -> false
                        }
                    } else false
                }
                .focusable(),
            contentAlignment = Alignment.Center
        ) {
            // Pulsing Outer Focus Border (drawn outside the card with a 4.dp gap)
            if (isBannerFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = bannerFocusPulseAlpha.value),
                            shape = RoundedCornerShape(24.dp) // Concentric shape: 20.dp (card corner) + 4.dp (gap) = 24.dp
                        )
                )
            }

            // Card Body (Always exactly 448.dp x 160.dp)
            Box(
                modifier = Modifier
                    .width(448.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (isLoadingSlideshow) {
                    // Show shimmer while loading
                    SlideshowShimmer(
                        modifier = Modifier
                            .fillMaxSize()
                    )
                } else if (imageList.isNotEmpty()) {
                    // 1. Sliding Content (Images and Flight Rows)
                    AnimatedContent(
                        targetState = currentImageIndex,
                        transitionSpec = {
                            val lastIndex = totalSlidesCount - 1
                            // Always use standard horizontal slide in/out
                            val isForward = if (lastIndex > 0 && initialState >= lastIndex && targetState == 0) {
                                true
                            } else if (lastIndex > 0 && initialState == 0 && targetState >= lastIndex) {
                                false
                            } else {
                                targetState > initialState
                            }
                            
                            if (isForward) {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith
                                        slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
                            } else {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) togetherWith
                                        slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
                            }
                        }, label = ""
                    ) { targetIndex ->
                        if (fidsActive && targetIndex >= imageList.size) {
                            val isArrival = targetIndex < imageList.size + arrivalsPagesCount
                            val flightsList = if (isArrival) flightArrivals else flightDepartures
                            val pageIndex = if (isArrival) (targetIndex - imageList.size) else (targetIndex - imageList.size - arrivalsPagesCount)
                            val startIdx = pageIndex * 4
                            val endIdx = minOf(startIdx + 4, flightsList.size)
                            val pageFlights = if (startIdx in flightsList.indices) {
                                flightsList.subList(startIdx, endIdx)
                            } else {
                                emptyList()
                            }

                            // Flight data slide layout (with invisible header placeholders for perfect layout alignment)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(top = 6.dp, bottom = 4.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // INVISIBLE HEADERS (Alpha = 0f) to reserve pixel-perfect layout height!
                                    Column(modifier = Modifier.fillMaxWidth().alpha(0f)) {
                                        // Header Row Placeholder
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 10.dp, end = 10.dp, bottom = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.align(Alignment.CenterStart)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = if (isArrival) R.drawable.flight_land else R.drawable.flight_takeoff),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isArrival) "Arrivals" else "Departures",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                            }

                                            Text(
                                                text = flightAirportName.uppercase(Locale.US),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.9f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Column Headers Row Placeholder
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White.copy(alpha = 0.12f))
                                                .padding(horizontal = 10.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "MASKAPAI", fontSize = 7.sp, modifier = Modifier.weight(0.15f))
                                            Text(text = "NOMOR", fontSize = 7.sp, modifier = Modifier.weight(0.08f))
                                            Text(text = if (isArrival) "DARI" else "KE", fontSize = 7.sp, modifier = Modifier.weight(0.17f))
                                            Text(text = "BANDARA", fontSize = 7.sp, modifier = Modifier.weight(0.21f))
                                            Text(text = "IATA", fontSize = 7.sp, modifier = Modifier.weight(0.07f), textAlign = TextAlign.Center)
                                            Text(text = "GATE", fontSize = 7.sp, modifier = Modifier.weight(0.06f), textAlign = TextAlign.Center)
                                            Text(text = "TERM", fontSize = 7.sp, modifier = Modifier.weight(0.06f), textAlign = TextAlign.Center)
                                            Text(text = "JAM", fontSize = 7.sp, modifier = Modifier.weight(0.08f), textAlign = TextAlign.Center)
                                            Text(text = "STATUS", fontSize = 7.5.sp, modifier = Modifier.weight(0.12f), textAlign = TextAlign.Center)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Flight rows that SLIDE!
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        if (pageFlights.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No flights scheduled",
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        } else {
                                            pageFlights.forEach { flight ->
                                                FlightRow(flight = flight, isArrival = isArrival)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            val imageUrl = imageList.getOrNull(targetIndex) ?: ""
                            Image(
                                painter = rememberCachedPainter(imageUrl),
                                contentDescription = "Slideshow Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // 2. Dynamic FIDS Header (Slides in/out on category change, stays completely still on page change!)
                    val fidsCategory = when {
                        !fidsActive -> null
                        currentImageIndex < imageList.size -> null
                        currentImageIndex < imageList.size + arrivalsPagesCount -> "ARRIVALS"
                        else -> "DEPARTURES"
                    }

                    AnimatedContent(
                        targetState = fidsCategory,
                        transitionSpec = {
                            val initialRank = when (initialState) {
                                null -> 0
                                "ARRIVALS" -> 1
                                "DEPARTURES" -> 2
                                else -> 0
                            }
                            val targetRank = when (targetState) {
                                null -> 0
                                "ARRIVALS" -> 1
                                "DEPARTURES" -> 2
                                else -> 0
                            }
                            
                            val isForward = if (initialState == "DEPARTURES" && targetState == null) {
                                true
                            } else if (initialState == null && targetState == "DEPARTURES") {
                                false
                            } else {
                                targetRank > initialRank
                            }
                            
                            if (isForward) {
                                slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith
                                        slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })
                            } else {
                                slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) togetherWith
                                        slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = ""
                    ) { targetCategory ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (targetCategory != null) {
                                val isArrival = targetCategory == "ARRIVALS"
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 6.dp, bottom = 4.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Title & Airport Header Row
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 10.dp, end = 10.dp, bottom = 2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.align(Alignment.CenterStart)
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = if (isArrival) R.drawable.flight_land else R.drawable.flight_takeoff),
                                                    contentDescription = null,
                                                    tint = if (isArrival) Color(0xFF29B6F6) else Color(0xFFFF9800),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isArrival) "Arrivals" else "Departures",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isArrival) Color(0xFF29B6F6) else Color(0xFFFF9800),
                                                    letterSpacing = 1.sp
                                                )
                                            }

                                            Text(
                                                text = flightAirportName.uppercase(Locale.US),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White.copy(alpha = 0.9f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Table Column Headers Row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.White.copy(alpha = 0.12f))
                                                .padding(horizontal = 10.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "MASKAPAI", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 7.sp, modifier = Modifier.weight(0.15f))
                                            Text(text = "NOMOR", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 7.sp, modifier = Modifier.weight(0.08f))
                                            Text(text = if (isArrival) "DARI" else "KE", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 7.sp, modifier = Modifier.weight(0.17f))
                                            Text(text = "BANDARA", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 7.sp, modifier = Modifier.weight(0.21f))
                                            Text(text = "IATA", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 7.sp, modifier = Modifier.weight(0.07f), textAlign = TextAlign.Center)
                                            Text(text = "GATE", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 7.sp, modifier = Modifier.weight(0.06f), textAlign = TextAlign.Center)
                                            Text(text = "TERM", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 7.sp, modifier = Modifier.weight(0.06f), textAlign = TextAlign.Center)
                                            Text(text = "JAM", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 7.sp, modifier = Modifier.weight(0.08f), textAlign = TextAlign.Center)
                                            Text(text = "STATUS", color = Color.White.copy(alpha = 0.75f), fontWeight = FontWeight.Bold, fontSize = 7.5.sp, modifier = Modifier.weight(0.12f), textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } // Closes Card Body Box

                // Dynamic dot and flight/dash carousel indicators (unwrapped Row outside Card Body to go lower!)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Slideshow Image DOT Indicators
                    repeat(imageList.size) { idx ->
                        val isActive = idx == currentImageIndex
                        val widthCoeff by animateDpAsState(
                            targetValue = if (isActive) 14.dp else 6.dp,
                            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            label = "dot_width"
                        )
                        val alphaCoeff by animateFloatAsState(
                            targetValue = if (isActive) 1.0f else 0.4f,
                            animationSpec = tween(durationMillis = 300),
                            label = "dot_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(width = widthCoeff, height = 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = alphaCoeff))
                        )
                    }

                    // 2. Separator Label "FLIGHT INFO" between slideshow and flights (simplified, smaller, tighter bottom margin)
                    if (fidsActive && imageList.isNotEmpty() && fidsSlidesCount > 0) {
                        Spacer(modifier = Modifier.width(1.dp))
                        Text(
                            text = "FLIGHT INFO",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 6.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // 3. Flight/FIDS Dash/Icon Indicators
                    if (fidsActive) {
                        repeat(fidsSlidesCount) { fidsIdx ->
                            val actualIdx = imageList.size + fidsIdx
                            val isActive = actualIdx == currentImageIndex
                            
                            if (isActive) {
                                // Active Flight Page: Show flight icon, flip horizontally when navigating left
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_flight),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(8.dp)
                                        .graphicsLayer { scaleX = if (isNavigatingLeft) -1f else 1f }
                                )
                            } else {
                                // Inactive Flight Page: Show dash (-)
                                val alphaCoeff by animateFloatAsState(
                                    targetValue = 0.4f,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "flight_dash_alpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(width = 6.dp, height = 2.dp)
                                        .background(Color.White.copy(alpha = alphaCoeff), shape = RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Composable function for the Home screen
@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val initStartTime = System.currentTimeMillis()
    Log.d("HomeScreen", "HomeScreen composition started")
    
    val imageList by DataRepository.slideshowImages
    var currentImageIndex by DataRepository.currentImageIndex
    val videoUrlsState by DataRepository.videoUrls
    val slideDurations by DataRepository.slideshowDurations
    val isSlideshowActive by DataRepository.isSlideshowActive
    var isBannerFocused by remember { mutableStateOf(false) }
    
    // Customizable Shortcuts State
    val shortcutsPrefs = remember { context.getSharedPreferences("app_shortcuts", Context.MODE_PRIVATE) }
    var shortcutSlots by remember {
        mutableStateOf(
            listOf(
                shortcutsPrefs.getString("slot_0", "com.dh.iptv") ?: "com.dh.iptv",
                shortcutsPrefs.getString("slot_1", "com.spotify.tv.android") ?: "com.spotify.tv.android",
                shortcutsPrefs.getString("slot_2", "com.amazon.amazonvideo.livingroom") ?: "com.amazon.amazonvideo.livingroom",
                shortcutsPrefs.getString("slot_3", "com.netflix.ninja") ?: "com.netflix.ninja",
                shortcutsPrefs.getString("slot_4", "com.google.android.youtube.tv") ?: "com.google.android.youtube.tv",
                shortcutsPrefs.getString("slot_5", "EMPTY_SLOT") ?: "EMPTY_SLOT"
            )
        )
    }
    var showDrawer by remember { mutableStateOf(false) }
    var selectedSlotIndex by remember { mutableIntStateOf(-1) }
    var installedApps by remember { mutableStateOf<List<SupportedApp>>(ShortcutIconCache.getInstalledAppsList()) }
    
    var allowedPackagesFromFirebase by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) {
        val dbRef = FirebaseDatabase.getInstance().reference.child("allowedApps")
        dbRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val packages = mutableSetOf<String>()
                for (child in snapshot.children) {
                    val allowed = child.child("allowed").getValue(Boolean::class.java) ?: false
                    val pkgName = child.child("packageName").getValue(String::class.java)
                    if (allowed && !pkgName.isNullOrEmpty()) {
                        packages.add(pkgName)
                    }
                }
                allowedPackagesFromFirebase = packages
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // Ignore or log error
            }
        })
    }
    
    val filteredInstalledApps = remember(installedApps, allowedPackagesFromFirebase) {
        installedApps.filter { app ->
            app.packageName == "com.dh.iptv" ||
                    app.installerPackageName == "com.android.vending" ||
                    app.packageName == "com.android.vending" ||
                    app.packageName.startsWith("com.google.android.") ||
                    app.packageName in allowedPackagesFromFirebase
        }
    }
    
    // Reorder & Action Dialog State
    var isReorderMode by remember { mutableStateOf(false) }
    var actionTargetIndex by remember { mutableIntStateOf(-1) }
    var optionMenuIndex by remember { mutableIntStateOf(-1) } // Added: Inline Option Menu State
    
    // Focus Requesters for Shortcuts (to move focus during reorder)
    val shortcutFocusRequesters = remember { List(6) { FocusRequester() } }

    fun swapShortcuts(fromIndex: Int, toIndex: Int) {
        if (toIndex in shortcutSlots.indices) {
            val list = shortcutSlots.toMutableList()
            java.util.Collections.swap(list, fromIndex, toIndex)
            shortcutSlots = list
            
            // Persist Order
            val editor = shortcutsPrefs.edit()
            list.forEachIndexed { index, pkg ->
                editor.putString("slot_$index", pkg)
            }
            editor.apply()
            
            actionTargetIndex = toIndex // Update selection to follow the moved item
            try {
                shortcutFocusRequesters[toIndex].requestFocus() // Move Focus
            } catch (e: Exception) {
               // Ignore
            }
        }
    }

    LaunchedEffect(Unit) {
        if (installedApps.isEmpty()) {
            withContext(Dispatchers.IO) {
                val apps = getInstalledApps(context)
                withContext(Dispatchers.Main) {
                    installedApps = apps
                    ShortcutIconCache.setInstalledAppsList(apps)
                }
            }
        }
    }
    
    // Loading states for shimmer read from global repository
    val isLoadingVideos by DataRepository.isLoadingVideos
    val isLoadingSlideshow by DataRepository.isLoadingSlideshow
    
    // Track if video was paused (persistent across navigation)
    // Sync with static GlobalMediaPlayerHolder
    var videoWasPaused by rememberSaveable { mutableStateOf(GlobalMediaPlayerHolder.videoWasPaused) }
    // Track if video has been started (persistent across navigation)
    // Sync with static GlobalMediaPlayerHolder
    var videoHasStarted by rememberSaveable { mutableStateOf(GlobalMediaPlayerHolder.videoHasStarted) }
    
    // Shared MediaPlayer that persists across navigation & activity drops
    var sharedMediaPlayer by remember { mutableStateOf<MediaPlayer?>(GlobalMediaPlayerHolder.sharedMediaPlayer) }
    
    // Initialize MediaPlayer once at HomeScreen level
    DisposableEffect(Unit) {
        if (GlobalMediaPlayerHolder.sharedMediaPlayer == null) {
            val player = MediaPlayer().apply {
                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPlayer", "Shared Error: what=$what, extra=$extra")
                    true
                }
            }
            GlobalMediaPlayerHolder.sharedMediaPlayer = player
            Log.d("VideoPlayer", "Brand NEW Static Shared MediaPlayer created at Global level")
        } else {
            Log.d("VideoPlayer", "RE-USING existing Static Shared MediaPlayer from Global level")
        }
        
        // Pull the verified instance into local state
        sharedMediaPlayer = GlobalMediaPlayerHolder.sharedMediaPlayer
        
        onDispose {
            // CRITICAL: Don't release MediaPlayer here!
            Log.d("VideoPlayer", "HomeScreen disposed - Static MediaPlayer safely retained in Process RAM")
        }
    }

    // Setup Firebase Realtime Database references
    val database = FirebaseDatabase.getInstance().reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    
    val roomId = sharedPreferences.getString("room", null)
    val guestInfo by DataRepository.guestInfo
    val roomTypeText = "${guestInfo?.roomtype ?: "Unavailable"}  "
    var currentTime by remember { mutableStateOf(getCurrentTime()) }

    val fidsActive by DataRepository.fidsActive
    val fidsIcaoCode by DataRepository.fidsIcaoCode

    val flightArrivals by DataRepository.flightArrivals
    val flightDepartures by DataRepository.flightDepartures
    val flightAirportName by DataRepository.flightAirportName
    val isDndActive by DataRepository.isDndActive

    // Set up clock updates
    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = getCurrentTime()
            delay(60000)
        }
    }
    
    Log.d("HomeScreen", "HomeScreen initial state setup completed in ${System.currentTimeMillis() - initStartTime}ms")

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var storedPin by remember { mutableStateOf<String?>(null) }
    var isExitDialog by remember { mutableStateOf(false) }
    var pendingPinAction by remember { mutableStateOf<(() -> Unit)?>(null) } // For PIN-protected actions
    var showSettingsMenu by remember { mutableStateOf(false) }

    // Handle back press to launch the system screensaver (Native OS Screensaver)
    BackHandler {
        Log.d("HomeScreen", "Back pressed - Launching native OS Screensaver via Somnambulator")
        try {
            // Method 1: Use Somnambulator to launch standard AOSP/Google TV Screensaver (100% Native & Safe)
            val intent = Intent().apply {
                setClassName("com.android.systemui", "com.android.systemui.Somnambulator")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d("HomeScreen", "Somnambulator daydream launched successfully!")
        } catch (e: Exception) {
            Log.e("HomeScreen", "Somnambulator failed, trying reflection fallback: ${e.message}")
            try {
                // Fallback 1: IDreamManager reflection
                val dreamManagerClass = Class.forName("android.service.dreams.IDreamManager\$Stub")
                val serviceManagerClass = Class.forName("android.os.ServiceManager")
                val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
                val binder = getServiceMethod.invoke(null, "dreams")
                val asInterfaceMethod = dreamManagerClass.getMethod("asInterface", android.os.IBinder::class.java)
                val dreamManager = asInterfaceMethod.invoke(null, binder)
                val dreamMethod = dreamManager.javaClass.getMethod("dream")
                dreamMethod.invoke(dreamManager)
                Log.d("HomeScreen", "System daydream triggered via reflection fallback")
            } catch (e2: Exception) {
                Log.e("HomeScreen", "All daydream triggers failed: ${e2.message}")
            }
        }
    }

    // Memanggil getPin() saat composable pertama kali dijalankan
    LaunchedEffect(Unit) {
        getPin(context) { pin ->
            storedPin = pin
        }
    }
    
    // Ensure screenshot is allowed whenever HomeScreen is displayed
    val activity = context as? Activity
    
    // Continuously ensure screenshot is enabled
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.let {
            // Remove FLAG_SECURE to allow screenshots
            it.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            // Enable drawing cache for screenshots
            it.decorView.isDrawingCacheEnabled = true
            Log.d("HomeScreen", "Screenshot enabled for HomeScreen")
        }
        
        // Set up a periodic check to ensure screenshot stays enabled
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                handler.postDelayed(this, 1000) // Check every second
            }
        }
        handler.post(runnable)
        
        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    // Firebase listeners are now handled globally in DataRepository during preload

    // Dynamic FIDS page counts calculation
    val arrivalsPagesCount = if (flightArrivals.isEmpty()) 1 else ((flightArrivals.size + 3) / 4)
    val departuresPagesCount = if (flightDepartures.isEmpty()) 1 else ((flightDepartures.size + 3) / 4)
    val totalSlidesCount = imageList.size + (if (fidsActive) (arrivalsPagesCount + departuresPagesCount) else 0)

    // Safe index clamping to prevent out-of-bounds errors on FIDS configuration changes
    LaunchedEffect(fidsActive, imageList.size, arrivalsPagesCount, departuresPagesCount) {
        if (totalSlidesCount > 0 && currentImageIndex >= totalSlidesCount) {
            currentImageIndex = 0
        }
    }

    // Timer for changing image based on slide duration (restarts on manual D-pad changes for jank-free transition timing)
    LaunchedEffect(isSlideshowActive, imageList, slideDurations, currentImageIndex, fidsActive, arrivalsPagesCount, departuresPagesCount, isBannerFocused) {
        if (isSlideshowActive && imageList.isNotEmpty() && !isBannerFocused) {
            if (totalSlidesCount > 0) {
                try {
                    // Flight info total = 30 detik, dibagi rata ke semua halaman (arr + dept)
                    val flightPagesCount = if (fidsActive) (arrivalsPagesCount + departuresPagesCount) else 0
                    val duration = if (fidsActive && currentImageIndex >= imageList.size) {
                        if (flightPagesCount > 0) maxOf(1, 30 / flightPagesCount) else 30
                    } else {
                        slideDurations.getOrNull(currentImageIndex) ?: 5
                    }
                    delay(duration * 1000L)
                    if (isSlideshowActive && imageList.isNotEmpty() && !isBannerFocused) {
                        currentImageIndex = (currentImageIndex + 1) % totalSlidesCount
                    }
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Error in slideshow timer: ${e.message}")
                }
            }
        }
    }



    Box(
        modifier = Modifier.fillMaxSize()
            // Removed padding here so Drawer can be full screen
    ) {

        // Inner Content Box with Padding (Header, Body, Footer)
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Content like buttons, text, etc.

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 125.dp)
                .padding(start = 58.dp, end = 58.dp), // Unified TV safety margins
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Use key to prevent recreation when returning from other screens
            // Pass state directly (not wrapped in remember) so it's always in sync
            key("video_section") {
                // Use MutableState directly from parent to ensure state persists across navigation
                // IMPORTANT: Initialize with current parent state values
                val wasPausedState = remember { mutableStateOf<Boolean>(videoWasPaused) }
                val videoStartedState = remember { mutableStateOf<Boolean>(videoHasStarted) }
                val sharedPlayerState = remember { mutableStateOf<MediaPlayer?>(sharedMediaPlayer) }
                
                // Sync state from parent whenever it changes
                // This ensures child component always has latest state from parent
                // IMPORTANT: Only update child state if parent state is true (preserve true values)
                LaunchedEffect(videoWasPaused, videoHasStarted, sharedMediaPlayer) {
                    // Only update child state if parent state is true (preserve true values)
                    // This prevents resetting state to false when parent is recreated
                    if (videoWasPaused) {
                        wasPausedState.value = true
                    }
                    if (videoHasStarted) {
                        videoStartedState.value = true
                    }
                    sharedPlayerState.value = sharedMediaPlayer
                    Log.d("VideoPlayer", "State synced from parent - wasPaused: $videoWasPaused, videoStarted: $videoHasStarted")
                }
                
                // Resume functionality removed - video will restart from beginning when returning from other screens
                
                
                // Row to align DND indicator next to the banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp), // Perfect alignment matching the height of the banner!
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VideoAndSlideshowSection(
                        context = context, 
                        videoUrls = videoUrlsState, 
                        imageList = imageList, 
                        currentImageIndex = currentImageIndex,
                        wasPausedState = wasPausedState,
                        videoStartedState = videoStartedState,
                        sharedMediaPlayer = sharedPlayerState,
                        isLoadingVideos = isLoadingVideos,
                        isLoadingSlideshow = isLoadingSlideshow,
                        guestInfo = guestInfo,
                        roomId = roomId,
                        roomTypeText = roomTypeText,
                        currentTime = currentTime,
                        onImageIndexChanged = { currentImageIndex = it },
                        flightArrivals = flightArrivals,
                        flightDepartures = flightDepartures,
                        flightAirportName = flightAirportName,
                        fidsActive = fidsActive,
                        onBannerFocusChanged = { isBannerFocused = it },
                        onNavigateDown = {
                            try {
                                shortcutFocusRequesters[0].requestFocus()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )

                    // Large white 20% opacity DND Active Indicator (Icon Only)
                    AnimatedVisibility(
                        visible = isDndActive,
                        modifier = Modifier
                    ) {
                        Icon(
                            painter = rememberAsyncImagePainter(R.drawable.ic_dnd),
                            contentDescription = "DND Active Indicator",
                            modifier = Modifier.size(96.dp),
                            tint = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp)) // Close vertical spacing to align closely with the banner
                
                // 5. Bottom Service & Shortcut Buttons and Guest Greeting Row (Bottom Aligned)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    ServiceButtonsSection(
                        context = context,
                        navController = navController,
                        shortcutSlots = shortcutSlots,
                        installedApps = filteredInstalledApps,
                        onSlotClick = { appPackage -> 
                            if (isReorderMode) {
                                 isReorderMode = false
                            } else {
                                val pm = context.packageManager
                                try {
                                    // Unified TV Launch: Check Leanback Launcher first, fall back to standard Launcher
                                    var intent = pm.getLeanbackLaunchIntentForPackage(appPackage)
                                    if (intent == null) {
                                        intent = pm.getLaunchIntentForPackage(appPackage)
                                    }
                                    
                                    if (intent != null) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } else {
                                       Log.e("HomeScreen", "App not found: $appPackage")
                                       Toast.makeText(context, "Cannot launch app: $appPackage", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        onSlotLongClick = { index ->
                            if (!isReorderMode) {
                                optionMenuIndex = index // Open Inline Options
                                actionTargetIndex = index
                            }
                        },
                        isReorderMode = isReorderMode,
                        reorderSelectedIndex = actionTargetIndex,
                        activeOptionIndex = optionMenuIndex, // Pass active option
                        onReorder = ::swapShortcuts,
                        onExitReorder = { 
                            isReorderMode = false 
                            optionMenuIndex = -1
                        },
                        onDismissOption = { optionMenuIndex = -1 },
                        onMoveOption = { index ->
                            optionMenuIndex = -1
                            actionTargetIndex = index
                            isReorderMode = true
                        },
                        onChangeOption = { index ->
                            val action = {
                                optionMenuIndex = -1 // Close options
                                actionTargetIndex = index
                                selectedSlotIndex = index
                                showDrawer = true // Open App Drawer
                            }
                            
                            if (index == 0) {
                                pendingPinAction = action
                                showPinDialog = true
                            } else {
                                action()
                            }
                        },
                        onDeleteOption = { index ->
                            val action = {
                                optionMenuIndex = -1 // Close options
                                // Remove item logic: Replace with EMPTY_SLOT
                                val newSlots = shortcutSlots.toMutableList()
                                if (index in newSlots.indices) {
                                    newSlots[index] = "EMPTY_SLOT"
                                    shortcutSlots = newSlots
                                    
                                    shortcutsPrefs.edit().putString("slot_$index", "EMPTY_SLOT").apply()
                                    
                                    // Restore Focus to the current slot (Wait for recomposition)
                                CoroutineScope(Dispatchers.Main).launch {
                                         try {
                                             delay(350) // Allow UI to switch to "Add" button (Increased for safety)
                                             shortcutFocusRequesters[index].requestFocus()
                                         } catch (e: Exception) {
                                             e.printStackTrace()
                                         }
                                    }
                                }
                            }

                            if (index == 0) {
                                pendingPinAction = action
                                showPinDialog = true
                            } else {
                                action()
                            }
                        },
                        focusRequesters = shortcutFocusRequesters
                    )

                    // Guest Greeting & Room Details Column (Bottom-aligned with the shortcut container)
                    Column(
                        modifier = Modifier.width(320.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Enlarged Guest Name (Size 36.sp, Bold, left aligned)
                        Text(
                            text = formatName(guestInfo?.fname ?: "Guest Name"),
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        )
                        
                        Text(
                            text = "Its our pleasure to welcome you to our hotel. We will do everything in our power to make your stay most convenient and enjoyable.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 19.sp
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        )

                        // Room Number, Type, Non-Smoking Group (Distributed evenly across the same 320.dp width)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            // Room Number Group (Door icon + Number)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.nest_doorbell_visitor_24dp_e8eaed_fill0_wght400_grad0_opsz24),
                                    contentDescription = "Room Icon",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = roomId ?: "-",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Text(
                                text = "|",
                                color = Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            // Room Type Group (Just roomtype label)
                            Text(
                                text = roomTypeText.trim(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Text(
                                text = "|",
                                color = Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            // Smoking Preference Group
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isSmoking = guestInfo?.isSmoking ?: false
                                val smokeIconRes = if (isSmoking) {
                                    R.drawable.cigarette_with_smoke_svgrepo_com
                                } else {
                                    R.drawable.i_no_smoking_svgrepo_com
                                }
                                val smokeLabel = if (isSmoking) "Smoking" else "Non-Smoking"

                                Icon(
                                    painter = painterResource(id = smokeIconRes),
                                    contentDescription = "Smoking Preference",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = smokeLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
}



    } // End of Inner Content Box

        // Action Dialog for Shortcuts


        // Drawer Overlay (Global / Full Screen)
        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ), 
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .zIndex(20f) // Highest zIndex
        ) {
            AppDrawer(
                installedApps = remember(filteredInstalledApps, shortcutSlots) {
                    filteredInstalledApps.filter { it.packageName !in shortcutSlots }
                },
                onDismiss = { showDrawer = false },
                onAppSelected = { app ->
                    if (selectedSlotIndex in shortcutSlots.indices) {
                        val newSlots = shortcutSlots.toMutableList()
                        newSlots[selectedSlotIndex] = app.packageName
                        shortcutSlots = newSlots
                        shortcutsPrefs.edit().putString("slot_$selectedSlotIndex", app.packageName).apply()
                        
                        // Restore Focus to the newly added shortcut (Wait for recomposition)
                        CoroutineScope(Dispatchers.Main).launch {
                             try {
                                 // Preload new shortcut banner into cache in background
                                 ShortcutIconCache.preloadSingle(context, app.packageName)
                                 delay(350) 
                                 shortcutFocusRequesters[selectedSlotIndex].requestFocus()
                             } catch (e: Exception) {
                                 e.printStackTrace()
                             }
                        }
                    }
                    showDrawer = false
                }
            )
        }
        
        BackHandler(enabled = showDrawer) {
            showDrawer = false
        }
    }
        if (showPinDialog) {
        PinDialog(
            pinInput = pinInput,
            onPinChange = { pinInput = it },
            onDismiss = { 
                showPinDialog = false
                pinInput = ""
                isExitDialog = false
                pendingPinAction = null
            },
            onPinConfirmed = { submittedPin ->
                if (storedPin != null) {
                    if (submittedPin == storedPin) {
                        if (pendingPinAction != null) {
                            pendingPinAction?.invoke()
                            pendingPinAction = null
                        } else if (isExitDialog) {
                            (context as? Activity)?.finish()
                        } else {
                            showSettingsMenu = true
                        }
                        showPinDialog = false
                        pinInput = ""
                        isExitDialog = false
                    } else {
                        Toast.makeText(context, "Access Denied.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error fetching PIN", Toast.LENGTH_SHORT).show()
                }
            },
            confirmEnabled = pinInput.length == 4 && storedPin != null
        )
    }

    if (showSettingsMenu) {
        SettingsOptionsDialog(onDismiss = { showSettingsMenu = false })
    }
}


@Composable
fun PinKeypadButton(
    text: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val isAction = text == "⌫" || text == "OK"
    val defaultBg = if (isAction) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.1f)
    val bgColor = if (isFocused) Color.White else defaultBg
    val textColor = if (isFocused) Color.Black else Color.White
    val scaleAmount = if (isFocused) 1.1f else 1.0f
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scaleAmount)
            .size(68.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isEnabled) bgColor else bgColor.copy(alpha = 0.05f))
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(enabled = isEnabled, interactionSource = interactionSource)
    ) {
        if (text == "⌫") {
            Icon(
                painter = painterResource(id = R.drawable.backspace),
                contentDescription = "Delete",
                tint = if (isEnabled) textColor else textColor.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = text,
                color = if (isEnabled) textColor else textColor.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }
    }
}

@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onConfirmClick: () -> Unit,
    confirmEnabled: Boolean,
    focusRequester: FocusRequester
) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "OK")
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        buttons.forEachIndexed { rIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { key ->
                    val isConfirm = key == "OK"
                    val isEnabled = if (isConfirm) confirmEnabled else true
                    
                    val isFirstKey = rIndex == 0 && key == "1"
                    
                    PinKeypadButton(
                        text = key,
                        isEnabled = isEnabled,
                        modifier = if (isFirstKey) Modifier.focusRequester(focusRequester) else Modifier,
                        onClick = {
                            when (key) {
                                "⌫" -> onDeleteClick()
                                "OK" -> onConfirmClick()
                                else -> onDigitClick(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PinDialog(
    pinInput: String,
    onPinChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPinConfirmed: (String) -> Unit,
    confirmEnabled: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        try {
            kotlinx.coroutines.delay(150)
            focusRequester.requestFocus()
        } catch (e: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .onKeyEvent { event ->
                    // Catch physical remote keyboard numbers globally on this screen
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                        val keyCode = event.nativeKeyEvent.keyCode
                        when (keyCode) {
                            in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 -> {
                                if (pinInput.length < 4) {
                                    val digit = (keyCode - android.view.KeyEvent.KEYCODE_0).toString()
                                    val newPin = pinInput + digit
                                    onPinChange(newPin)
                                    if (newPin.length == 4) {
                                        onPinConfirmed(newPin)
                                    }
                                }
                                true
                            }
                            in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 -> {
                                if (pinInput.length < 4) {
                                    val digit = (keyCode - android.view.KeyEvent.KEYCODE_NUMPAD_0).toString()
                                    val newPin = pinInput + digit
                                    onPinChange(newPin)
                                    if (newPin.length == 4) {
                                        onPinConfirmed(newPin)
                                    }
                                }
                                true
                            }
                            android.view.KeyEvent.KEYCODE_DEL -> {
                                if (pinInput.isNotEmpty()) {
                                    onPinChange(pinInput.dropLast(1))
                                }
                                true
                            }
                            android.view.KeyEvent.KEYCODE_ENTER,
                            android.view.KeyEvent.KEYCODE_DPAD_CENTER -> {
                                // Let the button focus system handle OK/Center click normally 
                                // to prevent collision with focused OK button
                                false
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .clickable(enabled = false) { }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(320.dp)
                    .wrapContentHeight()
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.keyboard), // Restored previous icon
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Autentikasi PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                // PIN Stars Preview Text Field
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textStyle = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = true,
                    maxLines = 1,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                
                // Virtual Keypad
                PinKeypad(
                    onDigitClick = { digit ->
                        if (pinInput.length < 4) {
                            val newPin = pinInput + digit
                            onPinChange(newPin)
                            if (newPin.length == 4) {
                                onPinConfirmed(newPin)
                            }
                        }
                    },
                    onDeleteClick = {
                        if (pinInput.isNotEmpty()) {
                            onPinChange(pinInput.dropLast(1))
                        }
                    },
                    onConfirmClick = {
                        if (confirmEnabled) {
                            onPinConfirmed(pinInput)
                        }
                    },
                    confirmEnabled = confirmEnabled,
                    focusRequester = focusRequester
                )
            }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServiceButtonsSection(
    context: Context, 
    navController: NavHostController,
    shortcutSlots: List<String>,
    installedApps: List<SupportedApp>,
    onSlotClick: (String) -> Unit,
    onSlotLongClick: (Int) -> Unit,
    // Reorder Params
    isReorderMode: Boolean,
    reorderSelectedIndex: Int,
    activeOptionIndex: Int, // Added: Which option is active
    onReorder: (Int, Int) -> Unit,
    onExitReorder: () -> Unit,
    onDismissOption: () -> Unit,
    onMoveOption: (Int) -> Unit,
    onChangeOption: (Int) -> Unit,
    onDeleteOption: (Int) -> Unit,
    focusRequesters: List<FocusRequester> = emptyList()
) {
    // 1. Main Content: Service Buttons Column Grid (2 rows of 3 columns, matching slideshow width)
    Column(
        modifier = Modifier
            .offset(x = (-4).dp)
            .width(456.dp)
            .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Row 1 (Slots 0, 1, 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0..2) {
                    if (index in shortcutSlots.indices) {
                        val packageName = shortcutSlots[index]
                        val appData = installedApps.find { it.packageName == packageName }
                        val effectivePackageName = if (appData != null) packageName else "EMPTY_SLOT"
                        val label = appData?.label ?: "Add App"
                        
                        ServiceButtonWithPackageBanner(
                            packageName = effectivePackageName,
                            label = label,
                            fallbackIconRes = R.drawable.circle_information_svgrepo_com,
                            onClick = {
                                if (effectivePackageName == "EMPTY_SLOT") {
                                    onChangeOption(index)
                                } else {
                                    onSlotClick(effectivePackageName)
                                }
                            },
                            onLongClick = { 
                                 if (effectivePackageName != "EMPTY_SLOT") {
                                     onSlotLongClick(index) 
                                 }
                            },
                            isReordering = isReorderMode && reorderSelectedIndex == index,
                            isOptionsActive = (activeOptionIndex == index),
                            onReorderLeft = {
                                if (index > 1) { // Prevent moving into Slot 1 (index 0)
                                    onReorder(index, index - 1)
                                }
                            },
                            onReorderRight = {
                                if (index < shortcutSlots.lastIndex) {
                                    onReorder(index, index + 1)
                                }
                            },
                            onReorderDown = {
                                if (index + 3 < shortcutSlots.size) {
                                    onReorder(index, index + 3)
                                }
                            },
                            onExitReorder = onExitReorder,
                            onDismissOptions = onDismissOption,
                            onMoveRequest = { 
                                if (index != 0) onMoveOption(index) 
                            },
                            onChangeRequest = { onChangeOption(index) },
                            onDeleteRequest = { onDeleteOption(index) },
                            focusRequester = focusRequesters.getOrNull(index),
                            installedApps = installedApps.filter { !shortcutSlots.contains(it.packageName) },
                            canMove = index != 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Row 2 (Slots 3, 4, 5)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 3..5) {
                    if (index in shortcutSlots.indices) {
                        val packageName = shortcutSlots[index]
                        val appData = installedApps.find { it.packageName == packageName }
                        val effectivePackageName = if (appData != null) packageName else "EMPTY_SLOT"
                        val label = appData?.label ?: "Add App"
                        
                        ServiceButtonWithPackageBanner(
                            packageName = effectivePackageName,
                            label = label,
                            fallbackIconRes = R.drawable.circle_information_svgrepo_com,
                            onClick = {
                                if (effectivePackageName == "EMPTY_SLOT") {
                                    onChangeOption(index)
                                } else {
                                    onSlotClick(effectivePackageName)
                                }
                            },
                            onLongClick = { 
                                 if (effectivePackageName != "EMPTY_SLOT") {
                                     onSlotLongClick(index) 
                                 }
                            },
                            isReordering = isReorderMode && reorderSelectedIndex == index,
                            isOptionsActive = (activeOptionIndex == index),
                            onReorderLeft = {
                                if (index > 1) { // Prevent moving into Slot 1 (index 0)
                                    onReorder(index, index - 1)
                                }
                            },
                            onReorderRight = {
                                if (index < shortcutSlots.lastIndex) {
                                    onReorder(index, index + 1)
                                }
                            },
                            onReorderUp = {
                                if (index - 3 > 0) { // Prevent moving into Slot 1 (index 0)
                                    onReorder(index, index - 3)
                                }
                            },
                            onExitReorder = onExitReorder,
                            onDismissOptions = onDismissOption,
                            onMoveRequest = { 
                                if (index != 0) onMoveOption(index) 
                            },
                            onChangeRequest = { onChangeOption(index) },
                            onDeleteRequest = { onDeleteOption(index) },
                            focusRequester = focusRequesters.getOrNull(index),
                            installedApps = installedApps.filter { !shortcutSlots.contains(it.packageName) },
                            canMove = index != 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
            }
        }
    }
}


@Composable
fun AppDrawer(
    installedApps: List<SupportedApp>,
    onDismiss: () -> Unit,
    onAppSelected: (SupportedApp) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }



    fun closeWithAnimation() {
        scope.launch {
            isVisible = false
            delay(300)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Wrap in Dialog to ensure it renders on top of everything
    Dialog(
        onDismissRequest = { closeWithAnimation() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // Root Wrapper to manage alignment
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd 
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)) // Dim background
                    .clickable { closeWithAnimation() }
            ) {} // Empty content for Scrim
        
            // Safety Delay state & Focus Request
            var isInputReady by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(450)
                isInputReady = true
                try {
                    focusRequester.requestFocus()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
                ) + fadeIn(),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
                ) + fadeOut()
            ) {
                // Drawer Content Styled like CartDrawer
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp, end = 16.dp)
                        .width(380.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1E2026),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (installedApps.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        } else {
                            val appRows = installedApps.chunked(2)
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
                            ) {
                                item {
                                    Text(
                                        text = "Add Application",
                                        style = TextStyle(
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                items(appRows.size) { rowIndex ->
                                    val rowApps = appRows[rowIndex]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowApps.forEachIndexed { colIndex, app ->
                                            val flatIndex = rowIndex * 2 + colIndex
                                            var isFocused by remember { mutableStateOf(false) }
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
                                            
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(103.dp)
                                                    .then(if (flatIndex == 0) Modifier.focusRequester(focusRequester) else Modifier)
                                                    // FOCUS TRAPS (Top/Bottom)
                                                    .onKeyEvent { event ->
                                                        if (event.type == KeyEventType.KeyDown) {
                                                            if (flatIndex == 0 && event.key == Key.DirectionUp) {
                                                                return@onKeyEvent true // Stop going up
                                                            }
                                                            if (flatIndex == installedApps.lastIndex && event.key == Key.DirectionDown) {
                                                                return@onKeyEvent true // Stop going down
                                                            }
                                                        }
                                                        false
                                                    }
                                                    .onFocusChanged { isFocused = it.isFocused }
                                                    .focusable()
                                                    .clickable { 
                                                        if (isInputReady) {
                                                            onAppSelected(app) 
                                                            closeWithAnimation()
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // 1. Pulsing Outer Focus Border (drawn with a 4.dp gap)
                                                if (isFocused) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .border(
                                                                width = 2.dp,
                                                                color = Color.White.copy(alpha = focusPulseAlpha.value),
                                                                shape = RoundedCornerShape(20.dp) // Concentric shape: 16.dp inner + 4.dp gap = 20.dp
                                                            )
                                                    )
                                                }

                                                // 2. Card Body (always padded inside by 4.dp to create the focus gap)
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(4.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(
                                                            if (isFocused) Color.White.copy(alpha = 0.2f) 
                                                            else Color.White.copy(alpha = 0.05f)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (app.banner != null) {
                                                        // Android TV Widescreen Banner
                                                        Image(
                                                            painter = rememberAsyncImagePainter(model = app.banner),
                                                            contentDescription = app.label,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.FillBounds
                                                        )
                                                    } else {
                                                        // Fallback UI for apps without a banner
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center,
                                                            modifier = Modifier.fillMaxSize().padding(8.dp)
                                                        ) {
                                                            if (app.icon != null) {
                                                                Image(
                                                                    painter = rememberAsyncImagePainter(model = app.icon),
                                                                    contentDescription = app.label,
                                                                    modifier = Modifier
                                                                        .size(36.dp)
                                                                        .clip(RoundedCornerShape(8.dp)),
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = app.label,
                                                                style = TextStyle(
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = if (isFocused) Color.White else Color(0xFFDDDDDD)
                                                                ),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (rowApps.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }

                        // Top fog/fade overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF1E2026), Color.Transparent)
                                    )
                                )
                        )

                        // Bottom fog/fade overlay
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFF1E2026))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServiceButtonWithPackageBanner(
    packageName: String,
    label: String,
    fallbackIconRes: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    // Reorder Params
    isReordering: Boolean = false,
    isOptionsActive: Boolean = false,
    onReorderLeft: () -> Unit = {},
    onReorderRight: () -> Unit = {},
    onReorderUp: () -> Unit = {},
    onReorderDown: () -> Unit = {},
    onExitReorder: () -> Unit = {},
    onDismissOptions: () -> Unit = {},
    onMoveRequest: () -> Unit = {},
    onChangeRequest: () -> Unit = {},
    onDeleteRequest: () -> Unit = {},
    focusRequester: FocusRequester? = null,
    installedApps: List<SupportedApp> = emptyList(), // Added for random banner pulse
    canMove: Boolean = true, // Added to disable move for Slot 1
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var bannerDrawable by remember(packageName) { mutableStateOf<android.graphics.drawable.Drawable?>(ShortcutIconCache.get(packageName)) }
    
    val isEmptySlot = packageName == "EMPTY_SLOT"

    // Remote Control Handling
    val scope = rememberCoroutineScope()
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    var isLongPressHandled by remember { mutableStateOf(false) } // Track long press
    var isFocused by remember { mutableStateOf(false) } // Focus State
    var isPressed by remember { mutableStateOf(false) } // Pressed State
    
    // Internal Option Navigation State (0 = Move, 1 = Change, 2 = Delete)
    var selectedOptionIndex by remember { mutableIntStateOf(0) }
    
    // Track if we should ignore the first KeyUp (passed from the Long Press)
    var ignoreFirstKeyUp by remember { mutableStateOf(false) }

    // Reset internal option selection when options become active
    LaunchedEffect(isOptionsActive) {
        if (isOptionsActive) {
            selectedOptionIndex = 0
            ignoreFirstKeyUp = true // Start by ignoring the release of the long-press key
        }
    }

    // Scale Animation (Zoom or Pulsate) - reduced scale on focus
    val targetScale = if (isEmptySlot) 1f else if (isReordering) 1f else if (isPressed) 1.04f else if (isFocused) 1.08f else 1f
    val scale by animateFloatAsState(targetScale, label = "scale")
    
    // Dedicated focus pulse alpha animation for the outer border
    val focusPulseAlpha = remember { androidx.compose.animation.core.Animatable(0.4f) }
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

    // Visual Highlight for Reordering (Pulsating Border) OR Add Button Focus
    val pulseAlpha = remember { androidx.compose.animation.core.Animatable(0.2f) }
    val isPulseActive = isReordering || (isEmptySlot && isFocused)
    LaunchedEffect(isPulseActive) {
        if (isPulseActive) {
            pulseAlpha.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAlpha.snapTo(0.2f)
        }
    }

    LaunchedEffect(packageName) {
        if (!isEmptySlot) {
            if (bannerDrawable == null) {
                withContext(Dispatchers.IO) {
                    try {
                        val pm = context.packageManager
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        // Try to load banner, fallback to icon
                        val banner = appInfo.loadBanner(pm) ?: appInfo.loadIcon(pm)
                        if (banner != null) {
                            bannerDrawable = banner
                            ShortcutIconCache.put(packageName, banner)
                        }
                    } catch (e: Exception) {
                        // e.printStackTrace()
                    }
                }
            }
        } else {
            bannerDrawable = null // Ensure banner is cleared if slot becomes empty
        }
    }

    // Random Banner Pulse State for "Add" Button
    var randomBanner by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }
    LaunchedEffect(isEmptySlot, isFocused) {
        if (isEmptySlot && isFocused && installedApps.isNotEmpty()) {
            // Load a single random banner from preloaded in-memory apps (Instant & zero CPU overhead!)
            val randomApp = installedApps.random()
            randomBanner = randomApp.banner ?: randomApp.icon
        } else {
            randomBanner = null
        }
    }

    Box(
        modifier = modifier
            .height(70.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .scale(scale)
            .zIndex(if (isFocused || isReordering || isOptionsActive) 10f else 0f) // High Z-Index for overlay
            .onKeyEvent { event ->
                if (isEmptySlot) {
                    // Simplified Key Handling for Empty Slot ("Add" Button)
                     if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                         // Consumer Down events to prevent propagation
                         when (event.nativeKeyEvent.keyCode) {
                             android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                             android.view.KeyEvent.KEYCODE_ENTER,
                             android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                 isPressed = true
                                 return@onKeyEvent true
                             }
                         }
                     } else if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                         when (event.nativeKeyEvent.keyCode) {
                             android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                             android.view.KeyEvent.KEYCODE_ENTER,
                             android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                 isPressed = false
                                 onChangeRequest() // Trigger "Add" action (Open Drawer)
                                 return@onKeyEvent true
                             }
                         }
                     }
                     return@onKeyEvent false
                }

                // 1. Handle Inline Options Navigation
                if (isOptionsActive) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (event.type == KeyEventType.KeyDown) {
                                  if (selectedOptionIndex > 0) selectedOptionIndex--
                            }
                            return@onKeyEvent true
                        }
                        Key.DirectionRight -> {
                            if (event.type == KeyEventType.KeyDown) {
                                  val maxIndex = if (canMove) 2 else 1
                                  if (selectedOptionIndex < maxIndex) selectedOptionIndex++
                            }
                            return@onKeyEvent true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            return@onKeyEvent true // Block D-Pad up/down navigation completely when menu is open!
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (event.type == KeyEventType.KeyUp) {
                                if (ignoreFirstKeyUp) {
                                    ignoreFirstKeyUp = false
                                    return@onKeyEvent true
                                }
                                if (canMove) {
                                    when (selectedOptionIndex) {
                                        0 -> onMoveRequest()
                                        1 -> onChangeRequest()
                                        2 -> onDeleteRequest()
                                    }
                                } else {
                                    when (selectedOptionIndex) {
                                        0 -> onChangeRequest()
                                        1 -> onDeleteRequest()
                                    }
                                }
                            }
                            return@onKeyEvent true
                        }
                        Key.Back -> {
                            if (event.type == KeyEventType.KeyUp) {
                                onDismissOptions()
                            }
                            return@onKeyEvent true
                        }
                    }
                }

                // 2. Handle Reordering
                if (isReordering && event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            onReorderLeft()
                            return@onKeyEvent true
                        }
                        Key.DirectionRight -> {
                            onReorderRight()
                            return@onKeyEvent true
                        }
                        Key.DirectionUp -> {
                            onReorderUp()
                            return@onKeyEvent true
                        }
                        Key.DirectionDown -> {
                            onReorderDown()
                            return@onKeyEvent true
                        }
                        Key.Back, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (event.nativeKeyEvent.repeatCount == 0) {
                                onExitReorder()
                            }
                            return@onKeyEvent true
                        }
                    }
                }
                
                // 3. Normal Interaction
                if (event.type == KeyEventType.KeyDown) {
                     if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                        if (event.nativeKeyEvent.repeatCount == 0) {
                            isPressed = true
                            
                            // Start Long Press Detection
                            if (longPressJob == null && onLongClick != null) {
                                isLongPressHandled = false
                                longPressJob = scope.launch {
                                    delay(600) // Long Press Threshold
                                    isLongPressHandled = true
                                    onLongClick()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isPressed = false 
                                }
                            }
                        }
                        return@onKeyEvent true
                    }
                } else if (event.type == KeyEventType.KeyUp) {
                     if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                        isPressed = false
                        longPressJob?.cancel()
                        longPressJob = null
                        
                        if (!isLongPressHandled) {
                            onClick()
                        }
                        return@onKeyEvent true
                     }
                }
                false
            }
            .onFocusChanged { 
                isFocused = it.isFocused 
                if (!it.isFocused) {
                     isPressed = false
                     longPressJob?.cancel()
                     longPressJob = null
                }
            }
            .focusable() // Enable focus for D-Pad
            .pointerInput(Unit) {
                  detectTapGestures(
                    onLongPress = {
                        if (!isEmptySlot) onLongClick?.invoke()
                    },
                    onTap = {
                         if (isEmptySlot) onChangeRequest() else onClick()
                    },
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 1. Pulsing Outer Focus Border
        // 1. Outer Focus Border: LED Strip for empty slots, White Pulsing for normal shortcuts
        if (isFocused) {
            if (isEmptySlot) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .liquidGlass(
                            cornerRadius = 16.dp,
                            glassColor = Color.Transparent,
                            alphaInitial = 0f,
                            alphaFinal = 0f,
                            isLedStrip = true,
                            borderAlpha = 1f,
                            borderWidth = 2.dp
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = focusPulseAlpha.value),
                            shape = RoundedCornerShape(16.dp)
                        )
                )
            }
        }

        // 2. Inner card Box with liquidGlass background & a beautiful gap
        val isPressedAndHeld = isReordering || isOptionsActive
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp) // Gap exactly as thick as the 2.dp border (giving a 2.dp transparent space)
                .then(
                    if (isPressedAndHeld) {
                        Modifier.background(
                            color = Color(207, 223, 237).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        Modifier.liquidGlass(
                            cornerRadius = 12.dp,
                            glassColor = if (isEmptySlot) Color.Black else Color.White,
                            alphaInitial = if (isEmptySlot) 0.1f else if (isFocused) 1f else 0.22f,
                            alphaFinal = if (isFocused) (if (isEmptySlot) 0.15f else 0.5f) else 0.05f,
                            hasTopRimLight = !isEmptySlot, 
                            isLedStrip = false, // No LED strip inside normal card
                            isHorizontalRim = false,
                            isFullBorder = false,
                            borderAlpha = 1f,
                            borderWidth = 1.dp
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
         if (isEmptySlot) {
             // --- Empty Slot Content ("Add" Button) ---
             // Setup manual ripple for D-Pad/Remote support
             val innerInteractionSource = remember { MutableInteractionSource() }
             var currentPress by remember { mutableStateOf<PressInteraction.Press?>(null) }
             
             // Bridge the parent's 'isPressed' state to the inner ripple
             LaunchedEffect(isPressed) {
                 if (isPressed) {
                     val press = PressInteraction.Press(Offset.Zero)
                     currentPress = press
                     innerInteractionSource.emit(press)
                 } else {
                     currentPress?.let {
                         innerInteractionSource.emit(PressInteraction.Release(it))
                         currentPress = null
                     }
                 }
             }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Transparent) // Background handled inside Crossfade
                    // Apply visual ripple driven by innerInteractionSource
                    .indication(
                        interactionSource = innerInteractionSource,
                        indication = ripple(color = Color.White)
                    )
            ) {
                // Random Banner Pulse Overlay with Smooth Fade
                // Handles both the "White Focus" state (null) and "Banner" state
                Crossfade(
                    targetState = randomBanner,
                    animationSpec = tween(durationMillis = 1000),
                    label = "RandomBannerFade"
                ) { banner ->
                    if (banner != null) {
                        Image(
                            painter = rememberAsyncImagePainter(model = banner),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.3f) // Transparent pulse effect
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Default Focus Highlight (White Transparent)
                        // Shows when banner is null (initial delay or between pulses)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    color = Color.Transparent, // No white focus background, handled by ring
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }
                
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_custom),
                        contentDescription = "Add App",
                        modifier = Modifier
                            .size(24.dp)
                            .alpha(if (isFocused) 1f else 0.5f), // Translucent plus sign when not focused, fully bright when focused
                        tint = Color.White
                    )
                }
            }
         } else {
             // --- App Content (Banner/Icon) ---
             if (bannerDrawable != null) {
                  Image(
                      painter = rememberAsyncImagePainter(model = bannerDrawable),
                      contentDescription = label,
                      modifier = Modifier
                          .fillMaxSize()
                          .padding(if (isReordering) 4.dp else 0.dp) // Shrink inside rim when moving
                          .alpha(if (isOptionsActive) 0.05f else if (isReordering) 0.5f else 1f) // Transparent banner when moving or menu active
                          .clip(RoundedCornerShape(if (isReordering) 8.dp else 12.dp)),
                      contentScale = ContentScale.Crop
                  )
             } else {
                  // Uniform loading shimmer matching the slideshow banner shimmer style!
                  val infiniteTransition = rememberInfiniteTransition(label = "shortcutShimmer")
                  val shimmerTranslateAnim by infiniteTransition.animateFloat(
                      initialValue = 0f,
                      targetValue = 1000f,
                      animationSpec = infiniteRepeatable(
                          animation = tween(
                              durationMillis = 1200,
                              easing = LinearEasing
                          ),
                          repeatMode = RepeatMode.Restart
                      ),
                      label = "shimmerTranslate"
                  )
                  
                  val shimmerColors = listOf(
                      Color.Gray.copy(alpha = 0.2f),
                      Color.Gray.copy(alpha = 0.4f),
                      Color.Gray.copy(alpha = 0.2f)
                  )
                  
                  Box(
                      modifier = Modifier
                          .fillMaxSize()
                          .background(
                              brush = Brush.linearGradient(
                                  colors = shimmerColors,
                                  start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                                  end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                              ),
                              shape = RoundedCornerShape(12.dp)
                          )
                  )
             }
             
             // --- Move Mode Overlay Icon ---
             if (isReordering) {
                 Icon(
                     painter = painterResource(id = R.drawable.ic_move_custom),
                     contentDescription = "Move",
                     modifier = Modifier
                         .align(Alignment.Center)
                         .size(32.dp) // Center icon size
                         .alpha(1f), // Static, no pulse
                     tint = Color.White
                 )
             }
    
             // Inline Option Overlay (Badges)
             if (isOptionsActive) {
                 Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .clip(RoundedCornerShape(12.dp)),
                     contentAlignment = Alignment.Center
                 ) {
                     Row(
                         modifier = Modifier
                             .fillMaxSize()
                             .padding(4.dp),
                         verticalAlignment = Alignment.CenterVertically,
                         horizontalArrangement = Arrangement.spacedBy(4.dp)
                     ) {
                         // Move Option Badge (Only if allowed)
                         if (canMove) {
                             val isSelected = selectedOptionIndex == 0
                             Box(
                                 modifier = Modifier
                                     .weight(1f)
                                     .fillMaxHeight()
                                     .background(
                                         color = if (isSelected) Color(0xFFCFDFED) else Color.Transparent,
                                         shape = RoundedCornerShape(8.dp)
                                     )
                                     .clip(RoundedCornerShape(8.dp)),
                                 contentAlignment = Alignment.Center
                             ) {
                                 Column(
                                     horizontalAlignment = Alignment.CenterHorizontally
                                 ) {
                                     Icon(
                                         painter = painterResource(id = R.drawable.ic_move_custom),
                                         contentDescription = "Move",
                                         tint = if (isSelected) Color(0xFF1C1D24) else Color.White.copy(alpha = 0.9f),
                                         modifier = Modifier.size(20.dp)
                                     )
                                     Text("Move", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF1C1D24) else Color.White.copy(alpha = 0.9f))
                                 }
                             }
                         }

                         // Change Option Badge
                         val swapIndex = if (canMove) 1 else 0
                         val isSwapSelected = selectedOptionIndex == swapIndex
                         Box(
                             modifier = Modifier
                                 .weight(1f)
                                 .fillMaxHeight()
                                 .background(
                                     color = if (isSwapSelected) Color(0xFFCFDFED) else Color.Transparent,
                                     shape = RoundedCornerShape(8.dp)
                                 )
                                 .clip(RoundedCornerShape(8.dp)),
                             contentAlignment = Alignment.Center
                         ) {
                             Column(
                                 horizontalAlignment = Alignment.CenterHorizontally
                             ) {
                                 Icon(
                                     painter = painterResource(id = R.drawable.ic_swap_custom),
                                     contentDescription = "Change",
                                     tint = if (isSwapSelected) Color(0xFF1C1D24) else Color.White.copy(alpha = 0.9f),
                                     modifier = Modifier.size(20.dp)
                                 )
                                 Text("Swap", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSwapSelected) Color(0xFF1C1D24) else Color.White.copy(alpha = 0.9f))
                             }
                         }

                         // Delete Option Badge
                         val deleteIndex = if (canMove) 2 else 1
                         val isDeleteSelected = selectedOptionIndex == deleteIndex
                         Box(
                             modifier = Modifier
                                 .weight(1f)
                                 .fillMaxHeight()
                                 .background(
                                     color = if (isDeleteSelected) Color(0xFFCFDFED) else Color.Transparent,
                                     shape = RoundedCornerShape(8.dp)
                                 )
                                 .clip(RoundedCornerShape(8.dp)),
                             contentAlignment = Alignment.Center
                         ) {
                             Column(
                                 horizontalAlignment = Alignment.CenterHorizontally
                             ) {
                                 Icon(
                                     painter = painterResource(id = R.drawable.ic_delete_custom),
                                     contentDescription = "Delete",
                                     tint = if (isDeleteSelected) Color(0xFF1C1D24) else Color.White.copy(alpha = 0.9f),
                                     modifier = Modifier.size(20.dp)
                                 )
                                 Text("Del", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isDeleteSelected) Color(0xFF1C1D24) else Color.White.copy(alpha = 0.9f))
                             }
                         }
                     }
                 }
             }
          }
      }
  }
}

@Composable
fun ServiceButtonWithIcon(
    iconRes: Int, 
    label: String, 
    modifier: Modifier = Modifier, // Changed from width to modifier
    onClick: () -> Unit
) {
    var isClicked by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val rippleIndication = ripple(color = HomeRipple)
    
    var isFocused by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 1.05f else if (isFocused) 1.25f else 1f) // Standardized Zoom

    Box(
        modifier = modifier // Apply passed modifier (e.g. weight)
            .onFocusChanged { 
                isFocused = it.isFocused 
                if (!isFocused) isPressed = false
            }
            .zIndex(if (isFocused) 1f else 0f) // Draw on top when focused
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .height(70.dp) // Height fixed, width flexible via modifier
            .onKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || 
                    keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                         if (event.nativeKeyEvent.repeatCount == 0) {
                             isPressed = true
                             onClick()
                             return@onKeyEvent true
                         }
                    } else if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                        isPressed = false
                        return@onKeyEvent true
                    }
                }
                false
            }
            .clickable(
                onClick = {
                    onClick() // Notify parent to show item details
                    isClicked = !isClicked
                },
                indication = null, // Disable Ripple
                interactionSource = interactionSource
            ), // Removed padding(4.dp)
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .liquidGlass(
                     cornerRadius = 12.dp,
                     alphaInitial = if (isFocused) 1f else 0.22f, // Much less transparent when focused
                     alphaFinal = if (isFocused) 0.5f else 0.05f
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = HomeIcon

                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    color = HomeText,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

//fun openApp(context: Context, packageName: String) {
//    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
//    if (intent != null) {
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        context.startActivity(intent)
//    } else {
//        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
//        context.startActivity(marketIntent)
//    }
//}

//fun openApp(context: Context, packageName: String) {
//    try {
//        val intent = when (packageName) {
//            "com.google.android.youtube.tv" -> {
//                // YouTube TV menggunakan ShellActivity
//                Intent().apply {
//                    component = ComponentName(
//                        "com.google.android.youtube.tv",
//                        "com.google.android.apps.youtube.tv.activity.ShellActivity"
//                    )
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
//            }
//            "in.startv.hotstar.dplus.tv" -> {
//                // Disney+ menggunakan MainActivity
//                Intent().apply {
//                    component = ComponentName(packageName, "com.hotstar.MainActivity")
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
//            }
//            "com.dh.iptv" -> {
//                Intent().apply {
//                    component = ComponentName(packageName, "com.dh.iptv.SplashActivity")  // Menggunakan SplashActivity
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
//            }
//
//            "com.spotify.tv.android" -> {
//                // Spotify TV menggunakan SpotifyTVActivity
//                Intent().apply {
//                    component = ComponentName(packageName, "com.spotify.tv.android.SpotifyTVActivity")
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
//            }
//            "com.netflix.ninja" -> {
//                // Netflix menggunakan MainActivity (sesuaikan jika perlu)
//                Intent().apply {
//                    component = ComponentName(packageName, "$packageName.MainActivity")
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
//            }
//            "com.amazon.amazonvideo.livingroom" -> {
//                // Amazon menggunakan IgnitionActivity (sesuaikan jika perlu)
//                Intent().apply {
//                    component = ComponentName(packageName, "com.amazon.ignition.IgnitionActivity")
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
//            }
//            else -> {
//                // Untuk aplikasi lainnya, menggunakan package manager untuk mencari intent
//                context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
//            }
//        }
//
//        // Jika intent ditemukan, jalankan
//        if (intent != null) {
//            context.startActivity(intent)
//        } else {
//            // Jika aplikasi tidak ditemukan, beri feedback
//            Toast.makeText(context, "Aplikasi tidak ditemukan!", Toast.LENGTH_SHORT).show()
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//        // Menampilkan pesan kesalahan jika ada exception
//        Toast.makeText(context, "Terjadi kesalahan saat membuka aplikasi", Toast.LENGTH_SHORT).show()
//    }
//}

// Add this data class at the top of the file, after the imports
data class SlideData(
    val url: String,
    val duration: Int,
    val title: String?
)

// Shimmer components for loading states
@Composable
fun VideoShimmer(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "videoShimmer")
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.2f),
        Color.Gray.copy(alpha = 0.4f),
        Color.Gray.copy(alpha = 0.2f)
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                    end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                ),
                shape = RoundedCornerShape(20.dp)
            )
    )
}

@Composable
fun SlideshowShimmer(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "slideshowShimmer")
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.2f),
        Color.Gray.copy(alpha = 0.4f),
        Color.Gray.copy(alpha = 0.2f)
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                    end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                ),
                shape = RoundedCornerShape(20.dp)
            )
    )
}

@Composable
fun ServiceButtonAdd(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    
    // Match HomeScreen Zoom: 1.3x on focus, 1.05x on press
    val scale by animateFloatAsState(if (isPressed) 1.05f else if (isFocused) 1.25f else 1f, label = "scale")

    Box(
        modifier = modifier
            .width(120.dp)
            .height(70.dp)
            .scale(scale)
            .zIndex(if (isFocused) 1f else 0f)
            .onFocusChanged { 
                isFocused = it.isFocused 
                if (!isFocused) isPressed = false
            }
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                    // Consumer Down events to prevent propagation
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            isPressed = true
                            return@onKeyEvent true
                        }
                    }
                } else if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER,
                        android.view.KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            isPressed = false
                            onClick()
                            return@onKeyEvent true
                        }
                    }
                }
                false
            }
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .liquidGlass(
                cornerRadius = 12.dp,
                glassColor = Color.White,
                alphaInitial = if (isFocused) 1f else 0.22f, // Match Hotel Menu
                alphaFinal = if (isFocused) 0.5f else 0.05f
            ),
        contentAlignment = Alignment.Center
    ) {
         // Content: White Box with Add Icon
         Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                // .background(Color.White.copy(alpha = if (isFocused) 0.9f else 0.5f)) // Removed, handled by liquidGlass
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_add_custom),
                contentDescription = "Add App",
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center),
                tint = if (isFocused) HomeText else Color.White // Match Hotel Menu icon tint behavior if possible, or keep simple
            )
        }
    }
}

fun getCurrentTime(): String {
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("en", "ID"))
    val timeFormat = SimpleDateFormat("HH:mm", Locale("en", "ID"))

    dateFormat.timeZone = TimeZone.getTimeZone("GMT+7")
    timeFormat.timeZone = TimeZone.getTimeZone("GMT+7")

    val currentDate = dateFormat.format(Date())
    val currentTime = timeFormat.format(Date())

    return "$currentDate $currentTime"
}

fun formatName(fname: String): String {
    val words = fname.split(", ")
    return if (words.size == 2) {
        "${words[1]}. ${words[0]}"
    } else {
        fname
    }
}

