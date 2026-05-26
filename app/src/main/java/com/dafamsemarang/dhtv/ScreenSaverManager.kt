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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.HorizontalDivider

object ScreenSaverManager {
    var isScreenSaverActive by mutableStateOf(false)
    
    // Screensaver state fetched from Firebase Realtime Database
    var isVideoActive by mutableStateOf(false)
    var videoUrl by mutableStateOf("")
    var activeImages by mutableStateOf<List<String>>(emptyList())
    var guestName by mutableStateOf("")
    var guestImageUrl by mutableStateOf("")
    var isWelcomeScreenActive by mutableStateOf(false)
    var welcomeMessage by mutableStateOf("")
    var signUrl by mutableStateOf("")
    var gmName by mutableStateOf("")
    var companyIconUrl by mutableStateOf<String?>(null)
    
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

                            val rawGuestUrl = snapshot.child("guestImageUrl").getValue(String::class.java) ?: ""
                            if (rawGuestUrl.isNotEmpty()) {
                                guestImageUrl = rawGuestUrl
                                val ctx = appContext
                                if (ctx != null) {
                                    val cacheFileName = getImageCacheFileName(rawGuestUrl)
                                    downloadAndCacheImage(ctx, rawGuestUrl, cacheFileName, { path ->
                                        Log.d("ScreenSaverManager", "Guest image pre-cached: $path")
                                    }, { e ->
                                        Log.e("ScreenSaverManager", "Failed to cache guest image: ${e.message}")
                                    })
                                }
                            }

                            val folioVal = snapshot.child("folio").getValue(Int::class.java) ?: 0
                            if (folioVal != 0) {
                                val imageRef = database.child("BRANCHES").child(branchId).child("GUESTIMAGE").child(folioVal.toString()).child("imageUrl")
                                imageRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(imgSnapshot: DataSnapshot) {
                                        val url = imgSnapshot.getValue(String::class.java)
                                        if (!url.isNullOrBlank()) {
                                            guestImageUrl = url
                                            val ctx = appContext
                                            if (ctx != null) {
                                                val cacheFileName = getImageCacheFileName(url)
                                                downloadAndCacheImage(ctx, url, cacheFileName, { path ->
                                                    Log.d("ScreenSaverManager", "Guest image from GUESTIMAGE pre-cached: $path")
                                                }, { e ->
                                                    Log.e("ScreenSaverManager", "Failed to cache guest image from GUESTIMAGE: ${e.message}")
                                                })
                                            }
                                        }
                                        Log.d("ScreenSaverManager", "Guest image loaded for screensaver from GUESTIMAGE: $guestImageUrl")
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
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
        
        val welcomeRef = database.child("BRANCHES").child(branchId).child("WELCOME_LETTER")
        Log.d("ScreenSaverManager", "Attaching WELCOME_LETTER listener: BRANCHES/$branchId/WELCOME_LETTER")
        welcomeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    if (snapshot.exists()) {
                        welcomeMessage = snapshot.child("welcomeMessage").getValue(String::class.java) ?: ""
                        signUrl = snapshot.child("signUrl").getValue(String::class.java) ?: ""
                        gmName = snapshot.child("gm").getValue(String::class.java) ?: ""
                        Log.d("ScreenSaverManager", "Welcome letter loaded for screensaver. Message: $welcomeMessage, Sign: $signUrl")
                    } else {
                        welcomeMessage = ""
                        signUrl = ""
                        gmName = ""
                    }
                } catch (e: Exception) {
                    Log.e("ScreenSaverManager", "Error parsing WELCOME_LETTER: ${e.message}")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        val iconRef = database.child("BRANCHES").child(branchId).child("SETTING").child("COMPANY_ICON")
        Log.d("ScreenSaverManager", "Attaching COMPANY_ICON listener: BRANCHES/$branchId/SETTING/COMPANY_ICON")
        iconRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    companyIconUrl = snapshot.child("iconUrl").getValue(String::class.java)
                    Log.d("ScreenSaverManager", "Company icon loaded for screensaver: $companyIconUrl")
                } catch (e: Exception) {
                    Log.e("ScreenSaverManager", "Error parsing COMPANY_ICON: ${e.message}")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

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
                    
                    // Parse Welcome Screen Setting
                    val welcomeSnapshot = snapshot.child("WELCOME_SCREEN")
                    val newWelcomeActive = welcomeSnapshot.child("ACTIVE").getValue(Boolean::class.java) ?: false
                    
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
                    if (isWelcomeScreenActive != newWelcomeActive) {
                        isWelcomeScreenActive = newWelcomeActive
                    }
                    
                    Log.d("ScreenSaverManager", "Screensaver settings loaded: isVideoActive=$isVideoActive, isWelcomeScreenActive=$isWelcomeScreenActive, activeImagesCount=${activeImages.size}")
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
            val hasWelcome = isWelcomeScreenActive
            
            if (hasVideo || hasImages || hasWelcome) {
                isScreenSaverActive = true
                Log.d("ScreenSaverManager", "Screensaver triggered! isVideoActive=$isVideoActive, isWelcomeActive=$isWelcomeScreenActive")
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
    val focusRequester = remember { FocusRequester() }
    
    // Automatically request focus on screensaver launch to intercept key events cleanly
    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {}
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val nativeCode = keyEvent.nativeKeyEvent.keyCode
                    Log.d("ScreenSaverOverlay", "Screensaver: Key pressed ($nativeCode) - exiting screensaver instantly!")
                    if (context is android.service.dreams.DreamService) {
                        context.finish()
                    } else {
                        ScreenSaverManager.isScreenSaverActive = false
                    }
                    return@onPreviewKeyEvent true
                }
                false
            }
    ) {
        var showWelcomeCard by remember { mutableStateOf(false) }

        // Start 5 seconds delay timer when screensaver media starts
        LaunchedEffect(Unit) {
            delay(5000)
            showWelcomeCard = true
        }

        if (ScreenSaverManager.isWelcomeScreenActive) {
            // Option 3: Full-screen Personalized Welcome Screen as Screensaver!
            WelcomeScreen(
                onNavigateToHome = {
                    if (context is android.service.dreams.DreamService) {
                        // Native DreamService flow: launch activity with home navigation extra and close service
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("navigate_to", "home")
                        }
                        context.startActivity(intent)
                        context.finish()
                    } else {
                        // In-app screensaver flow: close screensaver overlay and navigate to Home Screen
                        ScreenSaverManager.isScreenSaverActive = false
                        NavigationTrigger.pendingRoute = "home"
                    }
                }
            )
        } else {
            // Options 1 & 2: Pristine, full-screen Media Screensaver (No overlays!)
            if (ScreenSaverManager.isVideoActive && ScreenSaverManager.videoUrl.isNotEmpty()) {
                VideoScreenSaver(url = ScreenSaverManager.videoUrl)
            } else if (ScreenSaverManager.activeImages.isNotEmpty()) {
                ImageSlideshowScreenSaver(images = ScreenSaverManager.activeImages)
            }

            // Overlay elegant Glassmorphic Welcome Card on top of video or slideshow after 5 seconds delay
            if (ScreenSaverManager.guestName.isNotEmpty() || ScreenSaverManager.welcomeMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedVisibility(
                        visible = showWelcomeCard,
                        enter = fadeIn(animationSpec = tween(1500)),
                        exit = fadeOut(animationSpec = tween(800))
                    ) {
                        GlassmorphicWelcomeCard(
                            guestName = ScreenSaverManager.guestName,
                            welcomeMessage = ScreenSaverManager.welcomeMessage,
                            signUrl = ScreenSaverManager.signUrl,
                            gmName = ScreenSaverManager.gmName,
                            guestImageUrl = ScreenSaverManager.guestImageUrl
                        )
                    }
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

@Composable
fun GlassmorphicWelcomeCard(
    guestName: String,
    welcomeMessage: String,
    signUrl: String,
    gmName: String,
    guestImageUrl: String
) {
    Box(
        modifier = Modifier
            .width(480.dp)
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.1f)), // Removed padding from parent Box to allow true overflow!
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.Start, // Set card contents alignment start
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // Applied padding internally to Column to keep text aligned safely!
        ) {
            Text(
                text = "Welcome, ${formatName(guestName)}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // Row containing left-aligned welcome message and circular guest image on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = welcomeMessage.replace("\\n", "\n"),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Start, // Left aligned
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                if (guestImageUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(20.dp))
                    Image(
                        painter = rememberCachedPainter(url = guestImageUrl),
                        contentDescription = "Guest Photo",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Clean left-aligned closing column containing the vertical space gap for the overlay signature
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Warm Regards,",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Start
                )
                
                Spacer(modifier = Modifier.height(44.dp)) // The signature gap

                Text(
                    text = "General Manager",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start
                )
                if (gmName.isNotEmpty()) {
                    Text(
                        text = gmName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "Press any key to continue",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Render signature image OUTSIDE the main column, as a direct child of the card Box.
        // This allows it to ignore card padding boundaries and overflow beautifully while staying centered/aligned!
        if (signUrl.isNotEmpty()) {
            Image(
                painter = rememberCachedPainter(url = signUrl),
                contentDescription = "Signature",
                modifier = Modifier
                    .width(130.dp)
                    .height(75.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-10).dp, y = (-67).dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
    }
}
