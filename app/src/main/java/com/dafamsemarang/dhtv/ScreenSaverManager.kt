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
import androidx.compose.ui.layout.layout
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.zIndex

object ScreenSaverManager {
    var isScreenSaverActive by mutableStateOf(false)
    var lastDismissedTime by mutableStateOf(0L)
    
    // Screensaver state fetched from Firebase Realtime Database
    var isVideoActive by mutableStateOf(false)
    var videoUrl by mutableStateOf("")
    var activeImages by mutableStateOf<List<String>>(emptyList())
    var guestName by mutableStateOf("")
    var guestGender by mutableStateOf("")
    var guestImageUrl by mutableStateOf("")
    var isWelcomeScreenActive by mutableStateOf(false)
    var welcomeMessage by mutableStateOf("")
    var signUrl by mutableStateOf("")
    var gmName by mutableStateOf("")
    var gmTitle by mutableStateOf("General Manager")
    var companyIconUrl by mutableStateOf<String?>(null)
    var voEn by mutableStateOf("")
    var voEnAudioUrl by mutableStateOf("")
    var voEnVoiceName by mutableStateOf("")
    var voId by mutableStateOf("")
    var voIdAudioUrl by mutableStateOf("")
    var voIdVoiceName by mutableStateOf("")
    
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
                            val genderVal = snapshot.child("gender").getValue(String::class.java) ?: ""
                            if (rawFname.isNotEmpty()) {
                                guestGender = genderVal
                                // Store with English format (Mr./Mrs.) for welcome card display.
                                // TTS functions (formatNameID / formatNameEN) strip prefixes before adding their own,
                                // so they will still work correctly regardless of what prefix is stored here.
                                guestName = formatNameEN(rawFname, genderVal)
                                Log.d("ScreenSaverManager", "Guest name loaded for screensaver: $guestName, gender: $guestGender")
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
                            Log.d("ScreenSaverManager", "FOGUEST data is null or does not exist for room: $roomId")
                            guestName = ""
                            guestGender = ""
                            guestImageUrl = ""
                        }
                    } catch (e: Exception) {
                        Log.e("ScreenSaverManager", "Error parsing guest name for screensaver: ${e.message}")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("ScreenSaverManager", "FOGUEST listener cancelled: ${error.message}")
                }
            })
        }
        
        val welcomeRef = database.child("BRANCHES").child(branchId).child("WELCOME_LETTER")
        Log.d("ScreenSaverManager", "Attaching WELCOME_LETTER listener: BRANCHES/$branchId/WELCOME_LETTER")
        welcomeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    android.util.Log.d("ScreenSaverManager", "WELCOME_LETTER raw snapshot: ${snapshot.value}")
                    if (snapshot.exists()) {
                        welcomeMessage = snapshot.child("welcomeMessage").getValue(String::class.java) ?: ""
                        signUrl = snapshot.child("signUrl").getValue(String::class.java) ?: ""
                        gmName = snapshot.child("gm").getValue(String::class.java) ?: ""
                        gmTitle = snapshot.child("gmTitle").getValue(String::class.java) ?: "General Manager"
                        voEn = snapshot.child("voEn").getValue(String::class.java) ?: ""
                        voEnAudioUrl = snapshot.child("voEnAudioUrl").getValue(String::class.java) ?: ""
                        voEnVoiceName = snapshot.child("voEnVoiceName").getValue(String::class.java) ?: ""
                        voId = snapshot.child("voId").getValue(String::class.java) ?: ""
                        voIdAudioUrl = snapshot.child("voIdAudioUrl").getValue(String::class.java) ?: ""
                        voIdVoiceName = snapshot.child("voIdVoiceName").getValue(String::class.java) ?: ""
                        Log.d("ScreenSaverManager", "Welcome letter loaded for screensaver. Message: $welcomeMessage, Sign: $signUrl")
                    } else {
                        welcomeMessage = ""
                        signUrl = ""
                        gmName = ""
                        gmTitle = "General Manager"
                        voEn = ""
                        voEnAudioUrl = ""
                        voEnVoiceName = ""
                        voId = ""
                        voIdAudioUrl = ""
                        voIdVoiceName = ""
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
                    android.util.Log.d("ScreenSaverManager", "SCREEN_SAVER raw snapshot: ${snapshot.value}")
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
                    
                    // Parse Welcome Screen Setting (with fallbacks for key variations)
                    val welcomeSnapshot = snapshot.child("WELCOME_SCREEN")
                    var newWelcomeActive = welcomeSnapshot.child("ACTIVE").getValue(Boolean::class.java) ?: false
                    
                    if (!newWelcomeActive) {
                        val welcomeLetterSnapshot = snapshot.child("WELCOME_LETTER")
                        newWelcomeActive = welcomeLetterSnapshot.child("ACTIVE").getValue(Boolean::class.java) ?: false
                    }
                    if (!newWelcomeActive) {
                        val welcomeOnlySnapshot = snapshot.child("WELCOME")
                        newWelcomeActive = welcomeOnlySnapshot.child("ACTIVE").getValue(Boolean::class.java) ?: false
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
            // Retry focus request with small delays to ensure Box is attached
            for (i in 1..3) {
                delay(100L)
                focusRequester.requestFocus()
            }
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
                        ScreenSaverManager.lastDismissedTime = System.currentTimeMillis()
                        ScreenSaverManager.isScreenSaverActive = false
                    }
                }
                // ALWAYS consume both KeyDown and KeyUp events inside screensaver to prevent event leaking/propagation to underlying buttons on dismissal
                true
            }
    ) {
        var showWelcomeCard by remember { mutableStateOf(false) }
        var isPlayingAudio by remember { mutableStateOf(false) }

        // Start 5 seconds delay timer when screensaver media starts, play audio, and fade card out on completion
        LaunchedEffect(Unit) {
            if (ScreenSaverManager.isWelcomeScreenActive) return@LaunchedEffect
            delay(5000)
            showWelcomeCard = true
            
            // Audio synthesis logic:
            val name = ScreenSaverManager.guestName
            val gender = ScreenSaverManager.guestGender
            
            if (name.isNotEmpty()) {
                isPlayingAudio = true
                try {
                    // English TTS
                    if (ScreenSaverManager.voEn.isNotEmpty()) {
                        val voiceNameEn = if (ScreenSaverManager.voEnVoiceName.isNotEmpty()) ScreenSaverManager.voEnVoiceName else "en-US-Neural2-F"
                        val languageCodeEn = if (voiceNameEn.contains("-")) {
                            voiceNameEn.split("-").take(2).joinToString("-")
                        } else {
                            "en-US"
                        }
                        val greetingEn = "Hello! ${formatNameEN(name, gender)}."
                        val enFile = GoogleTtsHelper.synthesizeSpeech(context, greetingEn, languageCodeEn, voiceNameEn)
                        if (enFile != null) {
                            val player = android.media.MediaPlayer().apply {
                                setDataSource(enFile.absolutePath)
                                prepare()
                                start()
                            }
                            // Wait for playback
                            while (player.isPlaying) { delay(100) }
                            player.release()
                            try { enFile.delete() } catch (e: Exception) {}
                        }
                    }
                    
                    // English Static Welcome Audio URL
                    if (ScreenSaverManager.voEnAudioUrl.isNotEmpty()) {
                        val cacheFile = AudioCacheHelper.getAudioCacheFile(context, ScreenSaverManager.voEnAudioUrl)
                        val file = if (cacheFile.exists() && cacheFile.length() > 0) cacheFile else AudioCacheHelper.downloadAndCacheAudio(context, ScreenSaverManager.voEnAudioUrl)
                        if (file != null) {
                            val player = android.media.MediaPlayer().apply {
                                setDataSource(file.absolutePath)
                                prepare()
                                start()
                            }
                            while (player.isPlaying) { delay(100) }
                            player.release()
                        }
                    }

                    // Indonesian TTS
                    if (ScreenSaverManager.voId.isNotEmpty()) {
                        val voiceNameId = if (ScreenSaverManager.voIdVoiceName.isNotEmpty()) ScreenSaverManager.voIdVoiceName else "id-ID-Wavenet-B"
                        val languageCodeId = if (voiceNameId.contains("-")) {
                            voiceNameId.split("-").take(2).joinToString("-")
                        } else {
                            "id-ID"
                        }
                        val greetingId = "Halo! ${formatNameID(name, gender)}."
                        val idFile = GoogleTtsHelper.synthesizeSpeech(context, greetingId, languageCodeId, voiceNameId)
                        if (idFile != null) {
                            val player = android.media.MediaPlayer().apply {
                                setDataSource(idFile.absolutePath)
                                prepare()
                                start()
                            }
                            while (player.isPlaying) { delay(100) }
                            player.release()
                            try { idFile.delete() } catch (e: Exception) {}
                        }
                    }

                    // Indonesian Static Welcome Audio URL
                    if (ScreenSaverManager.voIdAudioUrl.isNotEmpty()) {
                        val cacheFile = AudioCacheHelper.getAudioCacheFile(context, ScreenSaverManager.voIdAudioUrl)
                        val file = if (cacheFile.exists() && cacheFile.length() > 0) cacheFile else AudioCacheHelper.downloadAndCacheAudio(context, ScreenSaverManager.voIdAudioUrl)
                        if (file != null) {
                            val player = android.media.MediaPlayer().apply {
                                setDataSource(file.absolutePath)
                                prepare()
                                start()
                            }
                            while (player.isPlaying) { delay(100) }
                            player.release()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ScreenSaverOverlay", "TTS Audio sequence error: ${e.message}", e)
                } finally {
                    isPlayingAudio = false
                    // Hide the Glassmorphic card after audio completes
                    showWelcomeCard = false
                }
            }
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
            // Options 1 & 2: Full-screen Media Screensaver with company logo overlay
            if (ScreenSaverManager.isVideoActive && ScreenSaverManager.videoUrl.isNotEmpty()) {
                VideoScreenSaver(url = ScreenSaverManager.videoUrl, isPlayingAudio = isPlayingAudio)
            } else if (ScreenSaverManager.activeImages.isNotEmpty()) {
                ImageSlideshowScreenSaver(images = ScreenSaverManager.activeImages)
            }

            // Company logo overlay — top-right corner
            val logoUrl = ScreenSaverManager.companyIconUrl
            val logoContext = LocalContext.current
            val svgAwareImageLoader = remember(logoContext) {
                coil.ImageLoader.Builder(logoContext)
                    .components { add(coil.decode.SvgDecoder.Factory()) }
                    .build()
            }
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(1500)),
                exit = fadeOut(animationSpec = tween(800)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 28.dp)
            ) {
                if (logoUrl.isNullOrEmpty()) {
                    Text(
                        text = "Your Logo Company",
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    coil.compose.AsyncImage(
                        model = logoUrl,
                        imageLoader = svgAwareImageLoader,
                        contentDescription = "Company Logo",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                }
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
                            gmTitle = ScreenSaverManager.gmTitle,
                            guestImageUrl = ScreenSaverManager.guestImageUrl
                        )
                    }
                }

                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(1500)),
                    exit = fadeOut(animationSpec = tween(800)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = "Press any key to continue.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp),
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 3f
                            )
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun VideoScreenSaver(url: String, isPlayingAudio: Boolean) {
    val context = LocalContext.current
    var mediaPlayerInstance by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var currentSurface by remember { mutableStateOf<android.view.Surface?>(null) }
    var activePath by remember { mutableStateOf("") }
    
    // Reactively adjust volume smoothly when isPlayingAudio changes
    LaunchedEffect(isPlayingAudio, mediaPlayerInstance) {
        val mp = mediaPlayerInstance ?: return@LaunchedEffect
        try {
            val targetVol = if (isPlayingAudio) 0.3f else 1.0f
            val startVol  = if (isPlayingAudio) 1.0f else 0.3f
            val steps = 20
            val stepDelayMs = 50L // total fade duration = 20 * 50ms = 1 second
            for (i in 1..steps) {
                val vol = startVol + (targetVol - startVol) * (i.toFloat() / steps)
                mp.setVolume(vol, vol)
                delay(stepDelayMs)
            }
            mp.setVolume(targetVol, targetVol) // ensure exact target at end
            Log.d("VideoScreenSaver", "Smooth volume fade complete → $targetVol")
        } catch (e: Exception) {
            Log.e("VideoScreenSaver", "Error during smooth volume fade: ${e.message}")
        }
    }
    
    // Manage MediaPlayer lifecycle
    DisposableEffect(url) {
        onDispose {
            try {
                mediaPlayerInstance?.stop()
                mediaPlayerInstance?.release()
            } catch (e: Exception) {
                Log.e("ScreenSaver", "Error releasing MediaPlayer: ${e.message}")
            }
            mediaPlayerInstance = null
            currentSurface?.release()
            currentSurface = null
        }
    }

    AndroidView(
        factory = { ctx ->
            android.view.TextureView(ctx).apply {
                surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
                        val surface = android.view.Surface(surfaceTexture)
                        currentSurface = surface
                        
                        try {
                            val cachedPath = ScreenSaverManager.cachedVideoPath
                            val targetPath = if (cachedPath != null && java.io.File(cachedPath).exists()) cachedPath else url
                            activePath = targetPath
                            
                            val mp = android.media.MediaPlayer().apply {
                                setSurface(surface)
                                isLooping = true
                                
                                if (targetPath == cachedPath) {
                                    Log.d("VideoScreenSaver", "Playing screensaver from local cache: $cachedPath")
                                    setDataSource(cachedPath)
                                } else {
                                    Log.d("VideoScreenSaver", "Local cache not ready. Streaming from remote URL: $url")
                                    setDataSource(ctx, android.net.Uri.parse(url))
                                }
                                
                                val initialVol = if (isPlayingAudio) 0.3f else 1.0f
                                setVolume(initialVol, initialVol)
                                setOnPreparedListener { 
                                    start() 
                                }
                                setOnErrorListener { _, _, _ -> 
                                    true 
                                }
                                prepareAsync()
                            }
                            mediaPlayerInstance = mp
                        } catch (e: Exception) {
                            Log.e("VideoScreenSaver", "Error preparing MediaPlayer: ${e.message}")
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surfaceTexture: android.graphics.SurfaceTexture): Boolean {
                        try {
                            mediaPlayerInstance?.stop()
                            mediaPlayerInstance?.release()
                        } catch (e: Exception) {}
                        mediaPlayerInstance = null
                        currentSurface?.release()
                        currentSurface = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surfaceTexture: android.graphics.SurfaceTexture) {}
                }
            }
        },
        update = { textureView ->
            // If the local cache becomes ready while the screensaver is active, seamlessly reload video source
            val cachedPath = ScreenSaverManager.cachedVideoPath
            val targetPath = if (cachedPath != null && java.io.File(cachedPath).exists()) cachedPath else url
            val mp = mediaPlayerInstance
            
            if (mp != null && activePath != targetPath) {
                activePath = targetPath
                try {
                    mp.reset()
                    mp.setSurface(currentSurface)
                    mp.isLooping = true
                    if (targetPath == cachedPath) {
                        Log.d("VideoScreenSaver", "Seamlessly switching playback to local cached file: $cachedPath")
                        mp.setDataSource(cachedPath)
                    } else {
                        mp.setDataSource(context, android.net.Uri.parse(url))
                    }
                    mp.prepareAsync()
                } catch (e: Exception) {
                    Log.e("VideoScreenSaver", "Error updating MediaPlayer source: ${e.message}")
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
    gmTitle: String,
    guestImageUrl: String
) {
    Box(
        modifier = Modifier
            .width(480.dp)
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(207, 223, 237).copy(alpha = 0.30f))
            .drawBehind {
                // Shiny Bevel & Highlights (Kaca 3D Bevel Edge)
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
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            },
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
                text = if (guestName.isBlank()) "No Guest" else "Welcome, $guestName",
                color = Color(0xFF292A2C),
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
                    color = Color(0xFF292A2C).copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
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

            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Warm Regards,",
                    color = Color(0xFF292A2C).copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                
                if (signUrl.isNotEmpty()) {
                    val signPainter = rememberCachedPainter(url = signUrl)
                    android.util.Log.d("ScreenSaverManager", "Signature image state: URL='$signUrl', State=${signPainter.state}")
                    Image(
                        painter = signPainter,
                        contentDescription = "Signature",
                        modifier = Modifier
                            .width(130.dp)
                            .height(75.dp)
                            .offset(y = (16).dp)
                            .offset(x = (-16).dp),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0xFF292A2C))
                    )
                } else {
                    Spacer(modifier = Modifier.height(44.dp))
                }

                if (gmName.isNotEmpty()) {
                    Text(
                        text = gmName,
                        color = Color(0xFF292A2C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start
                    )
                }
                Text(
                    text = if (gmTitle.isNullOrEmpty()) "General Manager" else gmTitle,
                    color = Color(0xFF292A2C).copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
