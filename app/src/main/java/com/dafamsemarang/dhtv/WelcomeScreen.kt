package com.dafamsemarang.dhtv

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Locale
import java.io.File
import kotlin.coroutines.resume
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.zIndex
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent

fun getDominantColor(bitmap: Bitmap): Int {
    return try {
        val tinyBitmap = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
        val color = tinyBitmap.getPixel(0, 0)
        tinyBitmap.recycle()
        color
    } catch (e: Exception) {
        Log.e("WelcomeScreen", "Error in getDominantColor: ${e.message}", e)
        android.graphics.Color.parseColor("#1E1E1E")
    }
}

fun darkenColor(color: Int, factor: Float = 0.35f): Int {
    val a = android.graphics.Color.alpha(color)
    val r = Math.round(android.graphics.Color.red(color) * factor)
    val g = Math.round(android.graphics.Color.green(color) * factor)
    val b = Math.round(android.graphics.Color.blue(color) * factor)
    return android.graphics.Color.argb(a, r, g, b)
}

fun isValidImageUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val trimmed = url.trim()
    return (trimmed.startsWith("http://", ignoreCase = true) || 
            trimmed.startsWith("https://", ignoreCase = true) || 
            trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("/")) && 
           !trimmed.equals("null", ignoreCase = true) && 
           !trimmed.contains("no image", ignoreCase = true) &&
           !trimmed.contains("no_image", ignoreCase = true)
}

fun formatNameID(fname: String, gender: String? = null): String {
    val cleanName = fname.replace("Mr. ", "", ignoreCase = true)
                         .replace("Mrs. ", "", ignoreCase = true)
                         .replace("Mr ", "", ignoreCase = true)
                         .replace("Mrs ", "", ignoreCase = true)
                         .replace("Bapak ", "", ignoreCase = true)
                         .replace("Ibu ", "", ignoreCase = true)
    val prefix = when (gender?.lowercase()) {
        "male" -> "Bapak "
        "female" -> "Ibu "
        else -> ""
    }
    return prefix + cleanName
}

fun formatNameEN(fname: String, gender: String? = null): String {
    val cleanName = fname.replace("Mr. ", "", ignoreCase = true)
                         .replace("Mrs. ", "", ignoreCase = true)
                         .replace("Mr ", "", ignoreCase = true)
                         .replace("Mrs ", "", ignoreCase = true)
                         .replace("Bapak ", "", ignoreCase = true)
                         .replace("Ibu ", "", ignoreCase = true)
    val prefix = when (gender?.lowercase()) {
        "male" -> "Mr. "
        "female" -> "Mrs. "
        else -> ""
    }
    return prefix + cleanName
}

@Composable
fun WelcomeScreen(onNavigateToHome: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val roomId = sharedPreferences.getString("room", null)
    val deviceId = sharedPreferences.getString("deviceID", null)
    val branchId = sharedPreferences.getString("branchId", null)
    val coroutineScope = rememberCoroutineScope()
    
    val welcomeTextShadow = Shadow(
        color = Color.Black.copy(alpha = 0.50f),
        offset = Offset(0f, 1.5f),
        blurRadius = 3f
    )
    
    // Create a unique key for this pairing session to ensure state reset after unpair/pair
    // This key changes when any pairing data changes, ensuring fresh state
    val pairingSessionKey = remember(roomId, deviceId, branchId) { 
        "${roomId ?: "null"}_${deviceId ?: "null"}_${branchId ?: "null"}"
    }
    
    var iconUrl by remember(pairingSessionKey) { mutableStateOf<String?>(null) }
    val database = Firebase.database.reference

    var guestInfo by remember(pairingSessionKey) { mutableStateOf<GuestInfo?>(null) }
    var guestImageUrl by remember(pairingSessionKey) { mutableStateOf("") }
    var guestImageZoom by remember(pairingSessionKey) { mutableStateOf(1.0f) }
    var guestImageOffsetX by remember(pairingSessionKey) { mutableStateOf(0.0f) }
    var guestImageOffsetY by remember(pairingSessionKey) { mutableStateOf(0.0f) }
    var isGuestDataLoaded by remember(pairingSessionKey) { mutableStateOf(false) }

    var welcomeData by remember(pairingSessionKey) { mutableStateOf(WelcomeData()) }

    // MediaPlayer for vocal greeting and static welcome audio playback
    var mediaPlayer by remember(pairingSessionKey) { mutableStateOf<android.media.MediaPlayer?>(null) }
    var audioDisabled by remember(pairingSessionKey) { mutableStateOf(false) }

    var showPinDialog by remember(pairingSessionKey) { mutableStateOf(false) }
    var pinInput by remember(pairingSessionKey) { mutableStateOf("") }
    var storedPin by remember(pairingSessionKey) { mutableStateOf<String?>(null) }
    var isExitDialog by remember(pairingSessionKey) { mutableStateOf(false) }
    var hasNavigated by remember(pairingSessionKey) { mutableStateOf(false) }
    
    // Use rememberUpdatedState to ensure callbacks always see the latest values
    val hasNavigatedState = rememberUpdatedState(hasNavigated)
    val audioDisabledState = rememberUpdatedState(audioDisabled)

    fun stopAndReleaseAudio() {
        audioDisabled = true
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                player.stop()
                player.release()
                Log.d("WelcomeScreen", "MediaPlayer stopped and released successfully")
            } catch (e: Exception) {
                Log.e("WelcomeScreen", "Error releasing MediaPlayer: ${e.message}")
            }
        }
    }
    
    // Focus requester to ensure Box always has focus for key events
    val focusRequester = remember { FocusRequester() }


    // Get PIN
    LaunchedEffect(Unit) {
        getPin(context) { pin ->
            storedPin = pin
        }
    }

    // Reset state when pairing session changes (e.g., after unpair/pair)
    LaunchedEffect(pairingSessionKey) {
        Log.d("WelcomeScreen", "New pairing session: $pairingSessionKey - Resetting all state")
        hasNavigated = false
        audioDisabled = false
        guestInfo = null
        guestImageUrl = ""
        isGuestDataLoaded = false
        welcomeData = WelcomeData()
        iconUrl = null
        
        // Cleanup old MediaPlayer instance if exists
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
    }

    // Safety fallback timer to prevent infinite loading state if Firebase is slow/offline
    LaunchedEffect(pairingSessionKey) {
        delay(2000)
        if (!isGuestDataLoaded) {
            Log.w("WelcomeScreen", "Firebase guest data load timed out - falling back to default view")
            isGuestDataLoaded = true
        }
    }

    // Fetch company icon URL from Firebase and check data validity
    // Use pairingSessionKey to ensure cleanup when unpair/pair happens
    // Track if listeners are already set up to prevent duplication
    var listenersSetup by remember(roomId, branchId, pairingSessionKey) { mutableStateOf(false) }
    
    DisposableEffect(roomId, branchId, pairingSessionKey) {
        if (roomId == null || branchId == null || deviceId == null) {
            Log.e("WelcomeScreen", "Critical data missing in WelcomeScreen - roomId: $roomId, branchId: $branchId, deviceId: $deviceId. Doing nothing.")
            return@DisposableEffect onDispose { }
        }

        // Check if already set up to prevent duplication
        if (listenersSetup) {
            Log.d("WelcomeScreen", "Firebase listeners already set up - skipping duplicate setup")
            return@DisposableEffect onDispose { }
        }

        // Mark as set up BEFORE setting up listeners to prevent race condition
        listenersSetup = true
        Log.d("WelcomeScreen", "Setting up Firebase listeners - BranchId: $branchId, RoomId: $roomId")
        
        // Store listeners for cleanup
        val welcomeListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                android.util.Log.d("WelcomeScreen", "WELCOME_LETTER raw snapshot: ${dataSnapshot.value}")
                val data = dataSnapshot.getValue(WelcomeData::class.java)
                welcomeData = data ?: WelcomeData() // Use default if data is null
                Log.d("WelcomeScreen", "Welcome data updated: ${welcomeData.welcomeMessage}")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("WelcomeScreen", "Error fetching welcome data: ${databaseError.message}")
            }
        }

        var imageListener: ValueEventListener? = null
            
        val guestListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    guestInfo = null
                    guestImageUrl = ""
                    isGuestDataLoaded = true
                    return
                }

                // Try parsing GuestInfo automatically, fallback to robust manual parsing on error
                val guest = try {
                    dataSnapshot.getValue(GuestInfo::class.java)
                } catch (e: Exception) {
                    Log.e("WelcomeScreen", "Failed to deserialize GuestInfo automatically, falling back to manual parsing", e)
                    try {
                        val folioVal = try {
                            dataSnapshot.child("folio").getValue(Int::class.java) ?: 0
                        } catch (e2: Exception) {
                            try {
                                dataSnapshot.child("folio").getValue(String::class.java)?.toIntOrNull() ?: 0
                            } catch (e3: Exception) { 0 }
                        }
                        val roomnightVal = try {
                            dataSnapshot.child("roomnight").getValue(Int::class.java) ?: 0
                        } catch (e2: Exception) {
                            try {
                                dataSnapshot.child("roomnight").getValue(String::class.java)?.toIntOrNull() ?: 0
                            } catch (e3: Exception) { 0 }
                        }
                        GuestInfo(
                            folio = folioVal,
                            dateci = dataSnapshot.child("dateci").getValue(String::class.java) ?: "",
                            dateco = dataSnapshot.child("dateco").getValue(String::class.java) ?: "",
                            datecreate = dataSnapshot.child("datecreate").getValue(String::class.java) ?: "",
                            fname = dataSnapshot.child("fname").getValue(String::class.java) ?: "",
                            foliostatus = dataSnapshot.child("foliostatus").getValue(String::class.java) ?: "",
                            email = dataSnapshot.child("email").getValue(String::class.java) ?: "",
                            phone = dataSnapshot.child("phone").getValue(String::class.java) ?: "",
                            room = dataSnapshot.child("room").getValue(String::class.java) ?: "",
                            roomnight = roomnightVal,
                            roomtype = dataSnapshot.child("roomtype").getValue(String::class.java) ?: "",
                            guestImageUrl = dataSnapshot.child("guestImageUrl").getValue(String::class.java) ?: "",
                            isSmoking = dataSnapshot.child("isSmoking").getValue(Boolean::class.java) == true,
                            gender = dataSnapshot.child("gender").getValue(String::class.java) ?: ""
                        )
                    } catch (manualEx: Exception) {
                        Log.e("WelcomeScreen", "Failed manual parse of GuestInfo", manualEx)
                        null
                    }
                }

                guestInfo = guest
                // Initialize guestImageUrl with the direct guestImageUrl property from FOGUEST first
                val rawGuestUrl = guest?.guestImageUrl ?: ""
                guestImageUrl = if (isValidImageUrl(rawGuestUrl)) rawGuestUrl else ""
                Log.d("WelcomeScreen", "Guest data received: ${guest?.fname}, Folio: ${guest?.folio}, guestImageUrl: $guestImageUrl")

                // Setelah mendapatkan guestInfo, ambil data dari GUESTIMAGE
                guestInfo?.folio?.let { folio ->
                    // Menggunakan folio untuk membangun path yang benar
                    val imageRef = database.child("BRANCHES").child(branchId).child("GUESTIMAGE").child(folio.toString())
                    Log.d("WelcomeScreen", "Fetching guest image data from path: BRANCHES/$branchId/GUESTIMAGE/$folio")
                    
                    val imageListenerObj = object : ValueEventListener {
                        override fun onDataChange(imageSnapshot: DataSnapshot) {
                            // Ambil URL gambar dari child imageUrl
                            val imageUrl = imageSnapshot.child("imageUrl").getValue(String::class.java)
                            
                            // Ambil zoom, offsetX, dan offsetY dari data tamu spesifik ini
                            val zoomVal = try {
                                imageSnapshot.child("zoom").getValue(Float::class.java)
                            } catch (e: Exception) {
                                try {
                                    imageSnapshot.child("zoom").getValue(String::class.java)?.toFloatOrNull()
                                } catch (e2: Exception) { null }
                            }
                            val offsetXVal = try {
                                imageSnapshot.child("offsetX").getValue(Float::class.java)
                            } catch (e: Exception) {
                                try {
                                    imageSnapshot.child("offsetX").getValue(String::class.java)?.toFloatOrNull()
                                } catch (e2: Exception) { null }
                            }
                            val offsetYVal = try {
                                imageSnapshot.child("offsetY").getValue(Float::class.java)
                            } catch (e: Exception) {
                                try {
                                    imageSnapshot.child("offsetY").getValue(String::class.java)?.toFloatOrNull()
                                } catch (e2: Exception) { null }
                            }

                            guestImageZoom = zoomVal ?: 1.0f
                            guestImageOffsetX = offsetXVal ?: 0.0f
                            guestImageOffsetY = offsetYVal ?: 0.0f

                            // Simpan URL gambar ke dalam state guestImageUrl, falling back to direct guestImageUrl from FOGUEST if null/empty
                            val resolvedUrl = if (!imageUrl.isNullOrBlank()) imageUrl else (guest?.guestImageUrl ?: "")
                            guestImageUrl = if (isValidImageUrl(resolvedUrl)) resolvedUrl else ""
                            Log.d("WelcomeScreen", "Guest image data received: $guestImageUrl, zoom: $guestImageZoom, offsetX: $guestImageOffsetX, offsetY: $guestImageOffsetY")
                            isGuestDataLoaded = true
                        }

                        override fun onCancelled(imageError: DatabaseError) {
                            Log.e("WelcomeScreen", "Error fetching guest image: ${imageError.message}")
                            val rawFallback = guest?.guestImageUrl ?: ""
                            guestImageUrl = if (isValidImageUrl(rawFallback)) rawFallback else ""
                            guestImageZoom = 1.0f
                            guestImageOffsetX = 0.0f
                            guestImageOffsetY = 0.0f
                            isGuestDataLoaded = true
                        }
                    }
                    imageListener = imageListenerObj
                    imageRef.addValueEventListener(imageListenerObj)
                } ?: run {
                    Log.w("WelcomeScreen", "No folio found for guest")
                    isGuestDataLoaded = true
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("WelcomeScreen", "Error fetching guest data: ${databaseError.message}")
                guestInfo = null
                isGuestDataLoaded = true
            }
        }

        val iconListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                iconUrl = snapshot.child("iconUrl").getValue(String::class.java)
                Log.d("DHTV_WELCOME", "Company icon URL updated: $iconUrl")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DHTV_WELCOME", "Error loading company icon: ${error.message}")
            }
        }
        
        // Use branchId in the path for WELCOME_LETTER
        val myRef = database.child("BRANCHES").child(branchId).child("WELCOME_LETTER")
        Log.d("WelcomeScreen", "Fetching welcome data from path: BRANCHES/$branchId/WELCOME_LETTER")
        myRef.addValueEventListener(welcomeListener)

        // Use branchId in the path for FOGUEST with BRANCHES node
        val guestRef = database.child("BRANCHES").child(branchId).child("FOGUEST").child(roomId)
        Log.d("WelcomeScreen", "Fetching guest data from path: BRANCHES/$branchId/FOGUEST/$roomId")
        guestRef.addValueEventListener(guestListener)

        // Fetch company icon URL from Firebase
        val iconRef = database.child("BRANCHES").child(branchId).child("SETTING").child("COMPANY_ICON")
        iconRef.addValueEventListener(iconListener)
        
        // Cleanup all listeners when composable is disposed or roomId/branchId changes
        onDispose {
            Log.d("WelcomeScreen", "Cleaning up Firebase listeners for roomId: $roomId, branchId: $branchId")
            listenersSetup = false
            myRef.removeEventListener(welcomeListener)
            guestRef.removeEventListener(guestListener)
            iconRef.removeEventListener(iconListener)
            // Cleanup image listener if it was created
            imageListener?.let { listener ->
                guestInfo?.folio?.let { folio ->
                    val imageRef = database.child("BRANCHES").child(branchId).child("GUESTIMAGE").child(folio.toString())
                    imageRef.removeEventListener(listener)
                }
            }
        }
    }

    // Suspend function to play a single audio file and wait until completion
    suspend fun playAudioFile(context: Context, file: File) = kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
        val player = android.media.MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
        }
        mediaPlayer = player
        player.setOnPreparedListener {
            player.start()
        }
        player.setOnCompletionListener {
            player.release()
            if (mediaPlayer == player) {
                mediaPlayer = null
            }
            continuation.resume(Unit)
        }
        player.setOnErrorListener { _, _, _ ->
            player.release()
            if (mediaPlayer == player) {
                mediaPlayer = null
            }
            continuation.resume(Unit)
            true
        }
        continuation.invokeOnCancellation {
            try {
                player.stop()
                player.release()
            } catch (e: Exception) {}
            if (mediaPlayer == player) {
                mediaPlayer = null
            }
        }
    }

    suspend fun playAudioUrl(context: Context, url: String) {
        val file = AudioCacheHelper.getAudioCacheFile(context, url)
        if (file.exists() && file.length() > 0) {
            playAudioFile(context, file)
        } else {
            val downloadedFile = AudioCacheHelper.downloadAndCacheAudio(context, url)
            if (downloadedFile != null) {
                playAudioFile(context, downloadedFile)
            }
        }
    }

    // Sequence of audio playback for both English and Indonesian greetings + static messages
    LaunchedEffect(guestInfo, welcomeData, hasNavigated, audioDisabled, pairingSessionKey) {
        if (hasNavigated || audioDisabled || guestInfo == null) return@LaunchedEffect
        
        val name = guestInfo?.fname ?: ""
        if (name.isEmpty()) return@LaunchedEffect

        // English sequence
        if (welcomeData.voEn.isNotEmpty()) {
            val greetingEn = "Hello! ${formatNameEN(name, guestInfo?.gender)}."
            val voiceNameEn = if (welcomeData.voEnVoiceName.isNotEmpty()) welcomeData.voEnVoiceName else "en-US-Neural2-F"
            val languageCodeEn = if (voiceNameEn.contains("-")) {
                voiceNameEn.split("-").take(2).joinToString("-")
            } else {
                "en-US"
            }
            Log.d("WelcomeScreen", "Synthesizing dynamic English greeting: $greetingEn with voice: $voiceNameEn, lang: $languageCodeEn")
            val enGreetingFile = GoogleTtsHelper.synthesizeSpeech(
                context = context,
                text = greetingEn,
                languageCode = languageCodeEn,
                voiceName = voiceNameEn
            )
            if (enGreetingFile != null && !hasNavigatedState.value && !audioDisabledState.value) {
                playAudioFile(context, enGreetingFile)
                try { enGreetingFile.delete() } catch (e: Exception) {}
            }
        }
        
        if (welcomeData.voEnAudioUrl.isNotEmpty() && !hasNavigatedState.value && !audioDisabledState.value) {
            Log.d("WelcomeScreen", "Playing static English welcome audio: ${welcomeData.voEnAudioUrl}")
            playAudioUrl(context, welcomeData.voEnAudioUrl)
        }

        // Indonesian sequence
        if (welcomeData.voId.isNotEmpty()) {
            val greetingId = "Halo! ${formatNameID(name, guestInfo?.gender)}."
            val voiceNameId = if (welcomeData.voIdVoiceName.isNotEmpty()) welcomeData.voIdVoiceName else "id-ID-Wavenet-B"
            val languageCodeId = if (voiceNameId.contains("-")) {
                voiceNameId.split("-").take(2).joinToString("-")
            } else {
                "id-ID"
            }
            Log.d("WelcomeScreen", "Synthesizing dynamic Indonesian greeting: $greetingId with voice: $voiceNameId, lang: $languageCodeId")
            val idGreetingFile = GoogleTtsHelper.synthesizeSpeech(
                context = context,
                text = greetingId,
                languageCode = languageCodeId,
                voiceName = voiceNameId
            )
            if (idGreetingFile != null && !hasNavigatedState.value && !audioDisabledState.value) {
                playAudioFile(context, idGreetingFile)
                try { idGreetingFile.delete() } catch (e: Exception) {}
            }
        }

        if (welcomeData.voIdAudioUrl.isNotEmpty() && !hasNavigatedState.value && !audioDisabledState.value) {
            Log.d("WelcomeScreen", "Playing static Indonesian welcome audio: ${welcomeData.voIdAudioUrl}")
            playAudioUrl(context, welcomeData.voIdAudioUrl)
        }
    }

    DisposableEffect(context) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null
        }
    }

    // Room Image (No error placeholder to prevent generic error images)
    val roomImage = rememberCachedPainter(welcomeData.roomImageUrl, null)
    
    // Welcome Background Image from CMS (No error placeholder to prevent generic error images)
    val welcomeBackground = rememberCachedPainter(welcomeData.backgroundUrl, null)
    val hasBackgroundImage = remember(welcomeData.backgroundUrl) { isValidImageUrl(welcomeData.backgroundUrl) }

    // Local path state for guest image
    var localGuestImagePath by remember(guestImageUrl) { 
        mutableStateOf(if (guestImageUrl.isNotEmpty()) getCachedImagePath(context, getImageCacheFileName(guestImageUrl)) else null) 
    }

    // Download and cache guest image on-the-fly if not cached
    LaunchedEffect(guestImageUrl) {
        if (guestImageUrl.isNotEmpty()) {
            val cacheFileName = getImageCacheFileName(guestImageUrl)
            val cachedPath = getCachedImagePath(context, cacheFileName)
            if (cachedPath == null) {
                Log.d("WelcomeScreen", "Guest image not cached - starting on-the-fly caching: $guestImageUrl")
                downloadAndCacheImage(
                    context = context,
                    imageUrl = guestImageUrl,
                    cacheFileName = cacheFileName,
                    onSuccess = { path ->
                        localGuestImagePath = path
                        Log.d("WelcomeScreen", "Guest image cached on-the-fly: $path")
                    },
                    onError = { e ->
                        Log.e("WelcomeScreen", "Failed to cache guest image on-the-fly: ${e.message}")
                    }
                )
            } else {
                localGuestImagePath = cachedPath
            }
        } else {
            localGuestImagePath = null
        }
    }

    // Guest Image with hardware bitmaps disabled to allow dominant color extraction
    val guestImageModel = remember(guestImageUrl, localGuestImagePath) {
        if (localGuestImagePath != null) {
            java.io.File(localGuestImagePath!!)
        } else if (guestImageUrl.isNotEmpty()) {
            guestImageUrl
        } else {
            ""
        }
    }

    val guestImageRequest = remember(guestImageModel) {
        ImageRequest.Builder(context)
            .data(guestImageModel)
            .allowHardware(false) // CRITICAL: Disable hardware bitmaps to allow CPU pixel extraction!
            .listener(
                onStart = { request -> Log.d("WelcomeScreen", "Coil: Start loading $guestImageModel") },
                onSuccess = { request, metadata -> Log.d("WelcomeScreen", "Coil: Success loading $guestImageModel") },
                onError = { request, result -> Log.e("WelcomeScreen", "Coil: Error loading $guestImageModel", result.throwable) }
            )
            // No error placeholder to prevent generic error image overlays
            .crossfade(false)
            .build()
    }

    val guestImage = rememberAsyncImagePainter(model = guestImageRequest)

    val showRoomImage = (isGuestDataLoaded && guestImageUrl.isEmpty()) || 
            (guestImageUrl.isNotEmpty() && guestImage.state is AsyncImagePainter.State.Error)



    val roomImageAlpha by animateFloatAsState(
        targetValue = if (showRoomImage) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "RoomImageAlpha"
    )

    val guestImageAlpha by animateFloatAsState(
        targetValue = if (guestImage.state is AsyncImagePainter.State.Success) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "GuestImageAlpha"
    )

    // Dynamic color matching the guest image nuance
    var dominantColor by remember(pairingSessionKey) { mutableStateOf(Color(0xFF1E1E1E)) }

    LaunchedEffect(guestImage, guestImageUrl) {
        snapshotFlow { guestImage.state }
            .collect { state ->
                Log.d("WelcomeScreen", "snapshotFlow state change: $state")
                if (guestImageUrl.isBlank()) {
                    dominantColor = Color(0xFF1E1E1E)
                } else if (state is AsyncImagePainter.State.Success) {
                    try {
                        val bitmap = state.result.drawable.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888)
                        val averageColor = getDominantColor(bitmap)
                        val darkened = darkenColor(averageColor, factor = 0.35f)
                        dominantColor = Color(darkened)
                        Log.d("WelcomeScreen", "Successfully extracted dominant nuance: #%06X".format(darkened and 0xFFFFFF))
                    } catch (e: Exception) {
                        Log.e("WelcomeScreen", "Failed to extract dominant color from guest photo: ${e.message}", e)
                        dominantColor = Color(0xFF1E1E1E)
                    }
                }
            }
    }

    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "BackgroundColorTransition"
    )

    // Request focus when composable is first displayed and after pairing session changes
    LaunchedEffect(pairingSessionKey) {
        // Small delay to ensure composable is fully laid out
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
        Log.d("WelcomeScreen", "Focus requested for Box")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedDominantColor)
            .focusRequester(focusRequester)
            .focusable() // Make the Box focusable to handle remote events
            .onKeyEvent { keyEvent ->
                // Log all key events for debugging
                val nativeKeyCode = keyEvent.nativeKeyEvent?.keyCode
                Log.d("WelcomeScreen", "Key event received: key=${keyEvent.key}, nativeKeyCode=$nativeKeyCode")
                // Capture key events from remote control FIRST (before clickable)
                // nativeKeyCode 23 is KEYCODE_DPAD_CENTER (OK button on remote)
                // Also check for Key.Enter for compatibility
                if (keyEvent.key == Key.Enter || nativeKeyCode == 23) { // If the Enter key (OK button) is pressed
                    val startTime = System.currentTimeMillis()
                    Log.d("WelcomeScreen", "OK button pressed from remote - hasNavigated: $hasNavigated")
                    if (!hasNavigated) {
                        // Set flags FIRST to prevent any audio operations
                        hasNavigated = true
                        stopAndReleaseAudio()
                        Log.d("WelcomeScreen", "OK pressed - disabling audio and navigating immediately")
                        
                        // Navigate IMMEDIATELY - don't wait for MediaPlayer to stop
                        val navStartTime = System.currentTimeMillis()
                        onNavigateToHome()
                        val navDuration = System.currentTimeMillis() - navStartTime
                        Log.d("WelcomeScreen", "Navigation call completed in ${navDuration}ms")
                        
                        val totalDuration = System.currentTimeMillis() - startTime
                        Log.d("WelcomeScreen", "Total OK button handling time: ${totalDuration}ms")
                    } else {
                        Log.d("WelcomeScreen", "OK button pressed but already navigated - ignoring")
                    }
                    true // Indicate that the event was handled
                } else {
                    false // Pass the event to other handlers if not Enter
                }
            }
            .clickable(
                onClick = { // This makes the entire screen clickable using OK button (Enter)
                    val startTime = System.currentTimeMillis()
                    Log.d("WelcomeScreen", "Screen clicked - hasNavigated: $hasNavigated")
                    if (!hasNavigated) {
                        // Set flags FIRST to prevent any audio operations
                        hasNavigated = true
                        stopAndReleaseAudio()
                        Log.d("WelcomeScreen", "Screen clicked - disabling audio and navigating immediately")
                        
                        // Navigate IMMEDIATELY - don't wait for MediaPlayer to stop
                        val navStartTime = System.currentTimeMillis()
                        onNavigateToHome()
                        val navDuration = System.currentTimeMillis() - navStartTime
                        Log.d("WelcomeScreen", "Navigation call completed in ${navDuration}ms")
                        
                        val totalDuration = System.currentTimeMillis() - startTime
                        Log.d("WelcomeScreen", "Total click handling time: ${totalDuration}ms")
                    }
                },
                indication = null, // Remove ripple effect
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        val roomImageAlignment = remember(welcomeData.roomImageAlignX, welcomeData.roomImageAlignY) {
            androidx.compose.ui.BiasAlignment(welcomeData.roomImageAlignX, welcomeData.roomImageAlignY)
        }
        val density = androidx.compose.ui.platform.LocalDensity.current
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

        val guestImageTransX = remember(guestImageOffsetX, screenWidthPx) {
            (guestImageOffsetX / 1920f) * screenWidthPx
        }
        val guestImageTransY = remember(guestImageOffsetY, screenHeightPx) {
            (guestImageOffsetY / 1080f) * screenHeightPx
        }
        val roomImageTransX = remember(welcomeData.offsetX, screenWidthPx) {
            (welcomeData.offsetX / 1920f) * screenWidthPx
        }
        val roomImageTransY = remember(welcomeData.offsetY, screenHeightPx) {
            (welcomeData.offsetY / 1080f) * screenHeightPx
        }

        // 1. Draw the Room Image fallback as the base underlay (occupies full screen)
        Image(
            painter = roomImage,
            contentDescription = "Room Image fallback",
            modifier = Modifier
                .fillMaxSize()
                .alpha(roomImageAlpha)
                .graphicsLayer(
                    scaleX = welcomeData.zoom,
                    scaleY = welcomeData.zoom,
                    translationX = roomImageTransX,
                    translationY = roomImageTransY
                ),
            contentScale = ContentScale.Crop,
            alignment = roomImageAlignment
        )

        // 2. Draw the Guest Image on top if guestImageUrl is not empty (occupies right 45% screen)
        if (guestImageUrl.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.50f)
                    .align(Alignment.CenterEnd)
            ) {
                Image(
                    painter = guestImage,
                    contentDescription = "Guest Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(guestImageAlpha)
                        .graphicsLayer(
                            scaleX = guestImageZoom,
                            scaleY = guestImageZoom,
                            translationX = guestImageTransX,
                            translationY = guestImageTransY
                        ),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center
                )
            }
        }

        // 3. Background Layer (CMS Background Image or Solid Fallback Color) drawn ON TOP of the images.
        // We apply a PorterDuff DstOut mask on the right-hand path to make it transparent, letting the images show through!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    // 1. Draw the background content (image or color)
                    drawContent()
                    
                    // 2. Apply the reversed S-curve mask by clearing the right side using BlendMode.DstOut
                    val w = size.width
                    val h = size.height
                    
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w, 0f)
                        lineTo(w * 0.65f, 0f)
                        cubicTo(
                            w * 0.78f, h * 0.3f, // control point 1 (pulls curve right)
                            w * 0.42f, h * 0.7f, // control point 2 (pulls curve left)
                            w * 0.58f, h        // end point
                        )
                        lineTo(w, h)
                        close()
                    }
                    
                    drawPath(
                        path = path,
                        color = Color.Black,
                        blendMode = BlendMode.DstOut
                    )
                }
        ) {
            // Render the Background Content inside the masked Box
            if (hasBackgroundImage) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = welcomeBackground,
                        contentDescription = "CMS Background Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Dark semi-transparent overlay to ensure white text is highly readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(animatedDominantColor)
                ) {
                    // Apply dark overlay over dominant color background for text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(.5f)
                .fillMaxHeight()
                .padding(16.dp)
                .wrapContentSize(Alignment.TopStart),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val context = LocalContext.current
                val svgAwareImageLoader = remember(context) {
                    ImageLoader.Builder(context)
                        .components { add(SvgDecoder.Factory()) }
                        .build()
                }
                val sanitizedIconUrl = remember(iconUrl) { iconUrl?.replace(" ", "%20") ?: "" }
                var isIconLoadError by remember(sanitizedIconUrl) { mutableStateOf(false) }
                if (sanitizedIconUrl.isNotEmpty()) {
                    if (isIconLoadError) {
                        Text(
                            text = "Your Company Logo",
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        AsyncImage(
                            model = sanitizedIconUrl,
                            imageLoader = svgAwareImageLoader,
                            contentDescription = "Company Logo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(Color.White),
                            onError = { state ->
                                Log.e("WelcomeScreen", "Failed to load company logo. URL: $sanitizedIconUrl, Error: ${state.result.throwable.message}", state.result.throwable)
                                isIconLoadError = true
                            }
                        )
                    }
                } else {
                    Text(
                        text = "Your Company Logo",
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(64.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (guestInfo == null || guestInfo?.fname.isNullOrEmpty()) "No Guest" else "Dear ${formatNameEN(guestInfo?.fname ?: "", guestInfo?.gender)}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(0.dp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 0.dp),
                        color = Color.White.copy(alpha = 0.5f),
                        thickness = .5.dp
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = welcomeData.welcomeMessage.replace("\\n", "\n"),
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
 
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .wrapContentHeight()
                            .padding(top = 24.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                             Text(
                                 text = "Warm Regards,",
                                 color = Color.White.copy(alpha = 0.85f),
                                 fontSize = 11.sp,
                                 fontWeight = FontWeight.Bold,
                                 textAlign = TextAlign.Center,
                                 modifier = Modifier.offset(y = 16.dp)
                             )
                             if (welcomeData.signUrl.isNotEmpty()) {
                                 val signImage = rememberCachedPainter(welcomeData.signUrl, null)
                                 android.util.Log.d("WelcomeScreen", "Signature image state: URL='${welcomeData.signUrl}', State=${signImage.state}")
                                 Image(
                                     painter = signImage,
                                     contentDescription = "sign",
                                     modifier = Modifier
                                         .width(150.dp)
                                         .height(80.dp)
                                         .offset(y = 8.dp),
                                     contentScale = ContentScale.Fit,
                                     colorFilter = ColorFilter.tint(Color.White)
                                 )
                             } else {
                                 Spacer(modifier = Modifier.height(24.dp))
                             }
 
                             if (welcomeData.gm.isNotEmpty()) {
                                 Text(
                                     text = welcomeData.gm,
                                     color = Color.White,
                                     fontSize = 12.sp,
                                     fontWeight = FontWeight.Bold,
                                     textAlign = TextAlign.Center
                                 )
                             }
                             Text(
                                 text = if (welcomeData.gmTitle.isNullOrEmpty()) "General Manager" else welcomeData.gmTitle,
                                 color = Color.White.copy(alpha = 0.85f),
                                 fontSize = 11.sp,
                                 fontWeight = FontWeight.Bold,
                                 textAlign = TextAlign.Center,
                                 modifier = Modifier.offset(y = (-8).dp)
                             )
                        }
                    }
                }
            }
        }



        // Text instruction at the bottom - not blocking clicks
            Text(
                text = "Press the OK button to continue.",
                modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .zIndex(1f), // Lower z-index so it doesn't block clicks
                color = Color.White,
                style = MaterialTheme.typography.titleSmall.copy(shadow = welcomeTextShadow)
            )
    }
}