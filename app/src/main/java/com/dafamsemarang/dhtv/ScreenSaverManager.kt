package com.dafamsemarang.dhtv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.view.KeyEvent
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*

object ScreenSaverManager {
    var isScreenSaverActive by mutableStateOf(false)
    
    // Screensaver state fetched from Firebase Realtime Database
    var isVideoActive by mutableStateOf(false)
    var videoUrl by mutableStateOf("")
    var activeImages by mutableStateOf<List<String>>(emptyList())
    var guestName by mutableStateOf("")
    
    // Video Caching States
    private var appContext: Context? = null
    private var downloadJob: Job? = null
    var cachedVideoPath by mutableStateOf<String?>(null)
    
    private var idleJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Default idle timeout: 2 minutes (120,000 milliseconds)
    private const val IDLE_TIMEOUT_MS = 120_000L 
    private var isListenerAttached = false

    fun autoConfigureSystemScreensaver(context: Context) {
        try {
            val resolver = context.contentResolver
            
            // 1. Enable screensaver OS feature
            android.provider.Settings.Secure.putInt(resolver, "screensaver_enabled", 1)
            
            // 2. Point OS screensaver directly to our HospitalityDreamService
            android.provider.Settings.Secure.putString(resolver, "screensaver_components", "com.dafamsemarang.dhtv/.HospitalityDreamService")
            
            // 3. Set OS screensaver idle timeout to 2 minutes (120,000 ms)
            android.provider.Settings.Secure.putInt(resolver, "screensaver_timeout", 120000)
            
            Log.d("ScreenSaverManager", "System-level screensaver auto-configured successfully via Secure Settings!")
        } catch (e: SecurityException) {
            Log.w("ScreenSaverManager", "Auto-configuration bypassed: Requires system privilege or WRITE_SECURE_SETTINGS permission.")
        } catch (e: Exception) {
            Log.e("ScreenSaverManager", "Error auto-configuring OS screensaver: ${e.message}", e)
        }
    }

    fun startListening(context: Context) {
        appContext = context.applicationContext
        if (isListenerAttached) return
        
        // Auto-configure the Google TV / Android TV OS daydream settings in the background
        autoConfigureSystemScreensaver(context)
        
        // Trigger initial cache check if there's an existing videoUrl
        if (videoUrl.isNotEmpty()) {
            cacheVideo(videoUrl)
        }
        
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val branchId = sharedPreferences.getString("branchId", null)
        val roomId = sharedPreferences.getString("room", null)
        
        if (branchId == null) {
            Log.w("ScreenSaverManager", "Cannot start listening: branchId is null")
            return
        }
        
        val database = FirebaseDatabase.getInstance().reference
        
        // Listen to FOGUEST node to dynamically retrieve guest's name for glassmorphic card
        if (roomId != null) {
            val guestRef = database.child("BRANCHES").child(branchId).child("FOGUEST").child(roomId)
            Log.d("ScreenSaverManager", "Attaching FOGUEST listener for screensaver: BRANCHES/$branchId/FOGUEST/$roomId")
            guestRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        if (snapshot.exists()) {
                            val rawFname = snapshot.child("fname").getValue(String::class.java) ?: ""
                            if (rawFname.isNotEmpty()) {
                                val words = rawFname.split(", ")
                                guestName = if (words.size == 2) {
                                    "${words[1]} ${words[0]}"
                                } else {
                                    rawFname
                                }
                                Log.d("ScreenSaverManager", "Guest name loaded for screensaver: $guestName")
                            }
                        } else {
                            guestName = ""
                        }
                    } catch (e: Exception) {
                        Log.e("ScreenSaverManager", "Error parsing guest name for screensaver: ${e.message}")
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
        val screensaverRef = database.child("BRANCHES").child(branchId).child("SETTING").child("SCREEN_SAVER")
        
        Log.d("ScreenSaverManager", "Attaching Firebase listener to path: BRANCHES/$branchId/SETTING/SCREEN_SAVER")
        screensaverRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (!snapshot.exists()) {
                        Log.w("ScreenSaverManager", "Screensaver setting path does not exist in Firebase")
                        return
                    }
                    
                    // Parse Video Setting
                    val videoSnapshot = snapshot.child("VIDEO")
                    val newVideoActive = videoSnapshot.child("ACTIVE").getValue(Boolean::class.java) ?: false
                    val newVideoUrl = videoSnapshot.child("VIDEO_URL").getValue(String::class.java) ?: ""
                    
                    // Parse Images Setting
                    val imageSnapshot = snapshot.child("IMAGE")
                    val imagesList = mutableListOf<String>()
                    if (imageSnapshot.exists()) {
                        for (child in imageSnapshot.children) {
                            val active = child.child("ACTIVE").getValue(Boolean::class.java) ?: false
                            val url = child.child("IMAGE_URL").getValue(String::class.java) ?: ""
                            if (active && url.isNotEmpty()) {
                                imagesList.add(url)
                            }
                        }
                    }
                    
                    // CRITICAL: Only update states if values have ACTUALLY changed.
                    // This prevents infinite recomposition/rendering loops!
                    if (isVideoActive != newVideoActive) {
                        isVideoActive = newVideoActive
                    }
                    if (videoUrl != newVideoUrl) {
                        videoUrl = newVideoUrl
                        cacheVideo(newVideoUrl)
                    }
                    if (activeImages != imagesList) {
                        activeImages = imagesList
                    }
                    
                    Log.d("ScreenSaverManager", "Screensaver settings loaded successfully: isVideoActive=$isVideoActive, videoUrl=$videoUrl, activeImagesCount=${activeImages.size}")
                } catch (e: Exception) {
                    Log.e("ScreenSaverManager", "Error parsing Firebase screensaver settings: ${e.message}", e)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ScreenSaverManager", "Firebase screensaver listener cancelled: ${error.message}")
            }
        })
        isListenerAttached = true
    }

    fun triggerInactivity(context: Context) {
        // Reset the idle timer
        idleJob?.cancel()
        
        // Any user action dismisses the screensaver
        if (isScreenSaverActive) {
            isScreenSaverActive = false
            Log.d("ScreenSaverManager", "Screensaver dismissed by user interaction")
        }
        
        idleJob = scope.launch {
            delay(IDLE_TIMEOUT_MS)
            
            val hasVideo = isVideoActive && videoUrl.isNotEmpty()
            val hasImages = activeImages.isNotEmpty()
            
            if (hasVideo || hasImages) {
                isScreenSaverActive = true
                Log.d("ScreenSaverManager", "Screensaver triggered! isVideoActive=$isVideoActive")
            } else {
                Log.d("ScreenSaverManager", "Idle timeout reached, but no screensaver content is active.")
            }
        }
    }

    private fun cacheVideo(url: String) {
        val context = appContext ?: return
        if (url.isEmpty()) {
            cachedVideoPath = null
            return
        }

        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            try {
                // Generate a unique safe filename based on URL hash
                val fileName = "screensaver_" + url.hashCode().toString() + ".mp4"
                val cacheFile = java.io.File(context.filesDir, fileName)

                if (cacheFile.exists() && cacheFile.length() > 1024) {
                    Log.d("ScreenSaverManager", "Video already cached locally: ${cacheFile.absolutePath}")
                    withContext(Dispatchers.Main) {
                        cachedVideoPath = cacheFile.absolutePath
                    }
                    return@launch
                }

                Log.d("ScreenSaverManager", "Starting download of screensaver video to cache: $url")
                
                // Clear any other old cached screensaver videos to save space on TV storage
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("screensaver_") && file.name.endsWith(".mp4") && file.name != fileName) {
                        file.delete()
                        Log.d("ScreenSaverManager", "Deleted old cached video: ${file.name}")
                    }
                }

                // Download to a temporary file first to prevent playing incomplete files
                val tempFile = java.io.File(context.filesDir, fileName + ".tmp")
                java.net.URL(url).openStream().use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempFile.renameTo(cacheFile)) {
                    Log.d("ScreenSaverManager", "Video downloaded and cached successfully: ${cacheFile.absolutePath}")
                    withContext(Dispatchers.Main) {
                        cachedVideoPath = cacheFile.absolutePath
                    }
                } else {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Log.e("ScreenSaverManager", "Failed to cache video: ${e.message}", e)
            }
        }
    }
}

@Composable
fun ScreenSaverOverlay() {
    val context = LocalContext.current
    var showWelcomeCard by remember { mutableStateOf(false) }
    val mainFocusRequester = remember { FocusRequester() }
    
    // Smoothly reveal the welcome navigation card after 10 seconds of uninterrupted screensaver playback
    LaunchedEffect(Unit) {
        delay(10000)
        showWelcomeCard = true
    }
    
    // Request focus on the main Box to ensure key events are captured correctly on TV remote controls
    LaunchedEffect(Unit) {
        try {
            mainFocusRequester.requestFocus()
        } catch (e: Exception) {}
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(mainFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val nativeCode = keyEvent.nativeKeyEvent.keyCode
                    if (nativeCode == KeyEvent.KEYCODE_BACK || keyEvent.key == Key.Back) {
                        Log.d("ScreenSaverOverlay", "BACK button pressed - exiting screensaver")
                        if (context is android.service.dreams.DreamService) {
                            context.finish()
                        } else {
                            ScreenSaverManager.isScreenSaverActive = false
                        }
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
    ) {
        if (ScreenSaverManager.isVideoActive && ScreenSaverManager.videoUrl.isNotEmpty()) {
            VideoScreenSaver(url = ScreenSaverManager.videoUrl)
        } else if (ScreenSaverManager.activeImages.isNotEmpty()) {
            ImageSlideshowScreenSaver(images = ScreenSaverManager.activeImages)
        }
        
        // Premium glassmorphic card layered perfectly on top, fading in after 10 seconds
        AnimatedVisibility(
            visible = showWelcomeCard,
            enter = fadeIn(animationSpec = tween(1200)),
            exit = fadeOut(animationSpec = tween(800)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            GlassmorphicWelcomeCard(
                guestName = ScreenSaverManager.guestName,
                onNavigate = { destination ->
                    if (context is android.service.dreams.DreamService) {
                        // Native DreamService flow: launch activity with explicit navigation extras and finish service
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("navigate_to", destination)
                        }
                        context.startActivity(intent)
                        context.finish()
                    } else {
                        // In-app overlay flow: close overlay and navigate via shared trigger
                        ScreenSaverManager.isScreenSaverActive = false
                        NavigationTrigger.pendingRoute = destination
                    }
                }
            )
        }
    }
}

@Composable
fun GlassmorphicWelcomeCard(guestName: String, onNavigate: (String) -> Unit) {
    var isWelcomeFocused by remember { mutableStateOf(false) }
    var isHomeFocused by remember { mutableStateOf(false) }
    
    val focusRequesterWelcome = remember { FocusRequester() }
    val focusRequesterHome = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Automatically request D-pad focus on the primary action button to yield flawless TV remote usability
        try {
            focusRequesterWelcome.requestFocus()
        } catch (e: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp) // Much more compact width
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.1f)) // Bright white glass with alpha 0.1
                .padding(20.dp) // Smaller interior padding
        ) {
            // Elegant Welcome Title
            Text(
                text = if (guestName.isNotEmpty()) "Welcome, $guestName" else "Welcome Guest",
                color = Color.White,
                fontSize = 20.sp, // Compact font size
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Enjoy your premium hospitality stay with us. Feel free to explore our services or continue to home.",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp, // Compact details
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.height(18.dp))
            
            // Buttons Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Button 1: Welcome Screen (Pill shaped, no border, compact height)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp) // Compact button height
                        .clip(RoundedCornerShape(20.dp)) // Perfect pill shape (height is 40.dp)
                        .background(if (isWelcomeFocused) Color.White else Color.White.copy(alpha = 0.15f))
                        .focusRequester(focusRequesterWelcome)
                        .onFocusChanged { isWelcomeFocused = it.isFocused }
                        .focusable()
                        .clickable { onNavigate("welcome") }
                ) {
                    Text(
                        text = "Welcome",
                        color = if (isWelcomeFocused) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp // Smaller font
                    )
                }
                
                // Button 2: Home Screen (Pill shaped, no border, compact height)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp) // Compact button height
                        .clip(RoundedCornerShape(20.dp)) // Perfect pill shape (height is 40.dp)
                        .background(if (isHomeFocused) Color.White else Color.White.copy(alpha = 0.15f))
                        .focusRequester(focusRequesterHome)
                        .onFocusChanged { isHomeFocused = it.isFocused }
                        .focusable()
                        .clickable { onNavigate("home") }
                ) {
                    Text(
                        text = "Home",
                        color = if (isHomeFocused) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp // Smaller font
                    )
                }
            }
        }
    }
}

@Composable
fun VideoScreenSaver(url: String) {
    var videoViewInstance: VideoView? by remember { mutableStateOf(null) }

    // CRITICAL: Safely stop playback and release resources when the screensaver is closed/disposed
    DisposableEffect(url) {
        onDispose {
            try {
                videoViewInstance?.stopPlayback()
            } catch (e: Exception) {
                Log.e("ScreenSaver", "Error releasing VideoView: ${e.message}")
            }
            videoViewInstance = null
        }
    }

    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                videoViewInstance = this
                
                // Determine whether to play from the local cache file or fall back to the remote URL
                val cachedPath = ScreenSaverManager.cachedVideoPath
                if (cachedPath != null && java.io.File(cachedPath).exists()) {
                    Log.d("VideoScreenSaver", "Playing screensaver from local cache: $cachedPath")
                    setVideoPath(cachedPath)
                } else {
                    Log.d("VideoScreenSaver", "Local cache not ready. Streaming from remote URL: $url")
                    setVideoURI(Uri.parse(url))
                }
                
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    mediaPlayer.start()
                }
                setOnErrorListener { _, _, _ ->
                    true // Prevents error dialog from displaying
                }
            }
        },
        update = { videoView ->
            // If the local cache becomes ready while the screensaver is displayed, switch to it seamlessly
            val cachedPath = ScreenSaverManager.cachedVideoPath
            val currentUriStr = videoView.tag as? String
            val targetPath = if (cachedPath != null && java.io.File(cachedPath).exists()) cachedPath else url
            
            if (currentUriStr != targetPath) {
                videoView.tag = targetPath
                try {
                    videoView.stopPlayback()
                    if (targetPath == cachedPath) {
                        Log.d("VideoScreenSaver", "Seamlessly switching playback to local cached file: $cachedPath")
                        videoView.setVideoPath(cachedPath)
                    } else {
                        videoView.setVideoURI(Uri.parse(url))
                    }
                    videoView.start()
                } catch (e: Exception) {
                    Log.e("VideoScreenSaver", "Error updating VideoView source: ${e.message}")
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ImageSlideshowScreenSaver(images: List<String>) {
    var currentIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(images) {
        while (true) {
            delay(7000) // Change image every 7 seconds
            currentIndex = (currentIndex + 1) % images.size
        }
    }
    
    val currentImageUrl = images.getOrNull(currentIndex) ?: ""
    
    Crossfade(
        targetState = currentImageUrl,
        animationSpec = tween(durationMillis = 1500),
        label = "ScreensaverSlideshow"
    ) { url ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = url),
                contentDescription = "Screensaver Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
