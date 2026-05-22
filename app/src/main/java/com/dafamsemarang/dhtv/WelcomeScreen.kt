package com.dafamsemarang.dhtv

import android.speech.tts.TextToSpeech
import android.content.Context
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
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

fun formatNameID(fname: String): String {
    val words = fname.split(", ")

    return if (words.size == 2) {
        val title = words[1]
        val name = words[0]

        // Replace Mr with Bapak, Mrs with Ibu
        val formattedTitle = when (title) {
            "Mr" -> "bapak"
            "Mrs" -> "ibu"
            else -> title // If title is not Mr or Mrs, use it as is
        }

        // Combine formatted title and name
        "$formattedTitle $name"
    } else {
        fname // Return as is if no comma is found
    }
}

fun formatNameEN(fname: String): String {
    val words = fname.split(", ")
    return if (words.size == 2) {
        "${words[1]} ${words[0]}"
    } else {
        fname // Return as is if no comma is found
    }
}

@Composable
fun WelcomeScreen(onNavigateToHome: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val roomId = sharedPreferences.getString("room", null)
    val deviceId = sharedPreferences.getString("deviceID", null)
    val branchId = sharedPreferences.getString("branchId", null)
    val coroutineScope = rememberCoroutineScope()
    
    // Create a unique key for this pairing session to ensure state reset after unpair/pair
    // This key changes when any pairing data changes, ensuring fresh state
    val pairingSessionKey = remember(roomId, deviceId, branchId) { 
        "${roomId ?: "null"}_${deviceId ?: "null"}_${branchId ?: "null"}"
    }
    
    var iconUrl by remember(pairingSessionKey) { mutableStateOf<String?>(null) }
    val database = Firebase.database.reference

    var guestInfo by remember(pairingSessionKey) { mutableStateOf<GuestInfo?>(null) }
    var guestImageUrl by remember(pairingSessionKey) { mutableStateOf("") }

    var welcomeData by remember(pairingSessionKey) { mutableStateOf(WelcomeData()) }

    // Initialize Text-to-Speech
    var textToSpeech by remember(pairingSessionKey) { mutableStateOf<TextToSpeech?>(null) }
    var isTTSInitialized by remember(pairingSessionKey) { mutableStateOf(false) }
    // Flag to completely disable TTS operations - checked in all TTS callbacks
    var ttsDisabled by remember(pairingSessionKey) { mutableStateOf(false) }

    var showPinDialog by remember(pairingSessionKey) { mutableStateOf(false) }
    var pinInput by remember(pairingSessionKey) { mutableStateOf("") }
    var storedPin by remember(pairingSessionKey) { mutableStateOf<String?>(null) }
    var isExitDialog by remember(pairingSessionKey) { mutableStateOf(false) }
    var hasNavigated by remember(pairingSessionKey) { mutableStateOf(false) }
    
    // Use rememberUpdatedState to ensure callbacks always see the latest hasNavigated value
    val hasNavigatedState = rememberUpdatedState(hasNavigated)
    val ttsDisabledState = rememberUpdatedState(ttsDisabled)
    
    // Focus requester to ensure Box always has focus for key events
    val focusRequester = remember { FocusRequester() }

    // Handle back press
    BackHandler {
        // Tidak melakukan apa-apa, jadi tombol back diabaikan di WelcomeScreen
        // Jika ingin info ke user, bisa aktifkan baris di bawah:
        // Toast.makeText(context, "Gunakan menu untuk keluar", Toast.LENGTH_SHORT).show()
    }

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
        ttsDisabled = false
        guestInfo = null
        guestImageUrl = ""
        welcomeData = WelcomeData()
        iconUrl = null
        isTTSInitialized = false
        // Cleanup old TTS instance if exists
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    // Fetch company icon URL from Firebase and check data validity
    // Use pairingSessionKey to ensure cleanup when unpair/pair happens
    // Track if listeners are already set up to prevent duplication
    var listenersSetup by remember(pairingSessionKey) { mutableStateOf(false) }
    
    DisposableEffect(pairingSessionKey) {
        // If critical data is missing, unpair and clear data
        if (roomId == null || branchId == null || deviceId == null) {
            Log.e("WelcomeScreen", "Critical data missing - roomId: $roomId, branchId: $branchId, deviceId: $deviceId")
            Log.d("WelcomeScreen", "Unpairing device due to missing data")
            
            // Use DeviceManager to properly unpair
            val deviceManager = DeviceManager(context)
            deviceManager.returnToPairingMode()
            
            // Clear SharedPreferences
            sharedPreferences.edit().apply {
                remove("deviceID")
                remove("branchId")
                remove("room")
                apply()
            }
            
            // Restart activity to go back to pairing screen
            val intent = (context as? Activity)?.intent
            (context as? Activity)?.finish()
            intent?.let {
                context.startActivity(it)
            }
            
            onDispose { }
        } else if (roomId != null && branchId != null) {
            // Check if already set up to prevent duplication
            if (listenersSetup) {
                Log.d("WelcomeScreen", "Firebase listeners already set up - skipping duplicate setup")
                onDispose { }
            } else {
                // Mark as set up BEFORE setting up listeners to prevent race condition
                listenersSetup = true
                Log.d("WelcomeScreen", "Setting up Firebase listeners - BranchId: $branchId, RoomId: $roomId")
            
            // Store listeners for cleanup
            val welcomeListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
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
                        // Ambil data guest dari FOGUEST
                        val guest = dataSnapshot.getValue(GuestInfo::class.java)
                        guestInfo = guest
                        Log.d("WelcomeScreen", "Guest data received: ${guest?.fname}, Folio: ${guest?.folio}")

                        // Setelah mendapatkan guestInfo, ambil data dari GUESTIMAGE
                        guestInfo?.folio?.let { folio ->
                            // Menggunakan folio untuk membangun path yang benar
                            val imageRef = database.child("BRANCHES").child(branchId).child("GUESTIMAGE").child(folio.toString()).child("imageUrl")
                            Log.d("WelcomeScreen", "Fetching guest image from path: BRANCHES/$branchId/GUESTIMAGE/$folio/imageUrl")
                            
                        imageListener = object : ValueEventListener {
                                override fun onDataChange(imageSnapshot: DataSnapshot) {
                                    // Ambil URL gambar dari child imageUrl
                                    val imageUrl = imageSnapshot.getValue(String::class.java)
                                    // Simpan URL gambar ke dalam state guestImageUrl
                                    guestImageUrl = imageUrl ?: ""
                                    Log.d("WelcomeScreen", "Guest image URL received: $guestImageUrl")
                                }

                                override fun onCancelled(imageError: DatabaseError) {
                                    Log.e("WelcomeScreen", "Error fetching guest image: ${imageError.message}")
                                    guestImageUrl = "" // fallback jika gagal mengambil imageUrl
                                }
                        }
                        
                        imageRef.addValueEventListener(imageListener!!)
                        } ?: run {
                            Log.w("WelcomeScreen", "No folio found for guest")
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("WelcomeScreen", "Error fetching guest data: ${databaseError.message}")
                        guestInfo = null
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
                        val imageRef = database.child("BRANCHES").child(branchId).child("GUESTIMAGE").child(folio.toString()).child("imageUrl")
                        imageRef.removeEventListener(listener)
                    }
                }
            }
            }
            } else {
            if (branchId == null) {
                Log.e("WelcomeScreen", "No branchId found in SharedPreferences")
            }
            if (roomId == null) {
            Log.e("WelcomeScreen", "No roomId found in SharedPreferences")
            }
            
            onDispose { }
        }
    }

    // TTS Initialization - reset when pairing session changes
    // Track if TTS is being initialized to prevent duplication
    var ttsInitializing by remember(pairingSessionKey) { mutableStateOf(false) }
    
    LaunchedEffect(pairingSessionKey) {
        // Cleanup old TTS instance first
        val oldTts = textToSpeech
        if (oldTts != null) {
            try {
                oldTts.setOnUtteranceProgressListener(null) // Remove listener first
                oldTts.stop()
                oldTts.shutdown()
                Log.d("WelcomeScreen", "Old TTS instance cleaned up")
            } catch (e: Exception) {
                Log.e("WelcomeScreen", "Error cleaning up old TTS: ${e.message}")
            }
        }
        textToSpeech = null
        isTTSInitialized = false
        ttsInitializing = false
        
        // Only initialize TTS if we haven't navigated yet and TTS is not disabled
        if (!hasNavigated && !ttsDisabled) {
            // Check if already initializing to prevent duplication
            if (ttsInitializing) {
                Log.d("WelcomeScreen", "TTS already initializing - skipping duplicate initialization")
                return@LaunchedEffect
            }
            
            // Mark as initializing BEFORE initializing to prevent race condition
            ttsInitializing = true
            Log.d("WelcomeScreen", "Initializing Text-to-Speech for session: $pairingSessionKey")
        textToSpeech = TextToSpeech(context) { status ->
                // Use updated state to check if navigation happened during initialization
                if (status == TextToSpeech.SUCCESS && !hasNavigatedState.value && !ttsDisabledState.value) {
                Log.d("WelcomeScreen", "TTS initialized successfully")
                // Set the initial language to English (or any default language)
                val result = textToSpeech?.setLanguage(Locale("en", "US"))
                when (result) {
                    TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                        Log.e("WelcomeScreen", "Language not supported for TTS")
                    }
                }

                    // Mark TTS as initialized only if still not disabled
                    if (!ttsDisabledState.value) {
                textToSpeech?.setSpeechRate(1f)
                textToSpeech?.setPitch(1.2f)
                isTTSInitialized = true
                Log.d("WelcomeScreen", "TTS settings configured")

                // Set the UtteranceProgressListener
                        // Store reference to TTS instance for callback
                        val currentTTS = textToSpeech
                        currentTTS?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                                // Check navigation status before logging
                                if (hasNavigatedState.value || ttsDisabledState.value) {
                                    Log.d("WelcomeScreen", "TTS started but navigation already occurred - ignoring")
                                    return
                                }
                        Log.d("WelcomeScreen", "Started speaking: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d("WelcomeScreen", "Finished speaking: $utteranceId")
                                // Use updated state to check current navigation status - check FIRST before any operations
                                if (hasNavigatedState.value || ttsDisabledState.value || currentTTS == null) {
                                    Log.d("WelcomeScreen", "Skipping next TTS message - hasNavigated: ${hasNavigatedState.value}, TTS disabled: ${ttsDisabledState.value}, TTS null: ${currentTTS == null}")
                                    return
                                }
                        // Check if we finished speaking the English message
                                if (utteranceId == "english" && currentTTS != null && !hasNavigatedState.value && !ttsDisabledState.value) {
                                    try {
                            // After English, switch to Indonesian language and speak
                                        currentTTS.language = Locale("id", "ID")
                            val indonesianMessage = "Halo! ${formatNameID(guestInfo?.fname ?: "Nama Tamu")}. ${welcomeData.voId.replace("\\n", "")}"
                            Log.d("WelcomeScreen", "Speaking Indonesian message: $indonesianMessage")
                                        currentTTS.speak(indonesianMessage, TextToSpeech.QUEUE_FLUSH, null, "indonesian")
                                    } catch (e: Exception) {
                                        Log.e("WelcomeScreen", "Error speaking Indonesian TTS: ${e.message}")
                                    }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e("WelcomeScreen", "Error in TTS for utterance: $utteranceId")
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        Log.d("WelcomeScreen", "TTS stopped for utterance: $utteranceId, interrupted: $interrupted")
                    }
                })
                        ttsInitializing = false
                    } else {
                        Log.d("WelcomeScreen", "TTS initialization completed but TTS was disabled during init")
                    }
                } else {
                    if (hasNavigatedState.value || ttsDisabledState.value) {
                        Log.d("WelcomeScreen", "TTS initialization skipped - already navigated or disabled")
            } else {
                Log.e("WelcomeScreen", "Failed to initialize TTS, status: $status")
            }
                }
            }
        } else {
            Log.d("WelcomeScreen", "Skipping TTS initialization - already navigated or disabled")
        }
    }

    // Speak guest's name twice (English first, then Indonesian)
    // Include isTTSInitialized as dependency to trigger when TTS becomes ready
    LaunchedEffect(guestInfo, isTTSInitialized, welcomeData.voEn, hasNavigated, ttsDisabled, pairingSessionKey) {
        // Don't speak if user already navigated, TTS is disabled, or TTS is null
        if (hasNavigated || ttsDisabled || textToSpeech == null) {
            Log.d("WelcomeScreen", "Skipping TTS - hasNavigated: $hasNavigated, TTS disabled: $ttsDisabled, TTS null: ${textToSpeech == null}")
            return@LaunchedEffect
        }
        
        // Wait for both TTS to be initialized AND guestInfo to be available
        if (isTTSInitialized && !guestInfo?.fname.isNullOrBlank() && textToSpeech != null && !ttsDisabled && welcomeData.voEn.isNotEmpty()) {
            val messageEn = welcomeData.voEn.replace("\\n", "")
            // Prepare the message for English
            val englishMessage = "Hello! ${formatNameEN(guestInfo?.fname ?: "Guest Name")}. $messageEn"
            Log.d("WelcomeScreen", "Speaking English message: $englishMessage")
            // Speak the message in English and tag it with "english" as an utterance ID
            try {
            textToSpeech?.speak(englishMessage, TextToSpeech.QUEUE_FLUSH, null, "english")
            } catch (e: Exception) {
                Log.e("WelcomeScreen", "Error speaking TTS: ${e.message}")
            }
        } else {
            Log.w("WelcomeScreen", "Cannot speak message: TTS initialized: $isTTSInitialized, Guest name: ${guestInfo?.fname}, TTS: ${textToSpeech != null}, TTS disabled: $ttsDisabled, Welcome message: ${welcomeData.voEn.isNotEmpty()}")
        }
    }

    // Remember to clean up TTS when composable is disposed
    DisposableEffect(context) {
        onDispose {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        }
    }

    // Background Image (Custom background from Firebase WELCOME_LETTER or fallback to Cached Wallpaper)
    val backgroundUrl = welcomeData.backgroundUrl.ifEmpty {
        sharedPreferences.getString("cached_wallpaper_url", "") ?: ""
    }
    val backgroundImage = rememberCachedPainter(backgroundUrl)

    // Room Image
    val roomImage = rememberCachedPainter(welcomeData.roomImageUrl, R.drawable.err)
    
    // Sign Image
    val signImage = rememberCachedPainter(welcomeData.signUrl, R.drawable.err)

    // Guest Image
    val guestImage = rememberCachedPainter(guestImageUrl, R.drawable.err)

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
            .background(Color.Black)
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
                        // Set flags FIRST to prevent any TTS operations
                        hasNavigated = true
                        ttsDisabled = true
                        Log.d("WelcomeScreen", "OK pressed - disabling TTS and navigating immediately")
                        
                        // Navigate IMMEDIATELY - don't wait for TTS to stop
                        val navStartTime = System.currentTimeMillis()
                        onNavigateToHome()
                        val navDuration = System.currentTimeMillis() - navStartTime
                        Log.d("WelcomeScreen", "Navigation call completed in ${navDuration}ms")
                        
                        // Stop TTS immediately and aggressively (don't block navigation)
                        val tts = textToSpeech
                        textToSpeech = null // Clear reference FIRST
                        // Remove listener before stopping to prevent callbacks
                        try {
                            tts?.setOnUtteranceProgressListener(null)
                        } catch (e: Exception) {
                            Log.e("WelcomeScreen", "Error removing TTS listener: ${e.message}")
                        }
                        // Stop TTS in background thread
                        Handler(Looper.getMainLooper()).post {
                            try {
                                if (tts != null) {
                                    tts.stop()
                                    tts.shutdown()
                                    Log.d("WelcomeScreen", "TTS stopped and shutdown in background")
                                }
                            } catch (e: Exception) {
                                Log.e("WelcomeScreen", "Error stopping TTS: ${e.message}")
                            }
                        }
                        
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
                        // Set flags FIRST to prevent any TTS operations
                        hasNavigated = true
                        ttsDisabled = true
                        Log.d("WelcomeScreen", "OK pressed - disabling TTS and navigating immediately")
                        
                        // Navigate IMMEDIATELY - don't wait for TTS to stop
                        val navStartTime = System.currentTimeMillis()
                        onNavigateToHome()
                        val navDuration = System.currentTimeMillis() - navStartTime
                        Log.d("WelcomeScreen", "Navigation call completed in ${navDuration}ms")
                        
                        // Stop TTS immediately and aggressively (don't block navigation)
                        val tts = textToSpeech
                        textToSpeech = null // Clear reference FIRST
                        // Stop TTS immediately on main thread to prevent callbacks
                        Handler(Looper.getMainLooper()).post {
                            try {
                                if (tts != null) {
                                    // Stop TTS FIRST to interrupt any ongoing speech
                                    tts.stop()
                                    // Remove listener AFTER stopping to prevent any queued callbacks
                                    tts.setOnUtteranceProgressListener(null)
                                    // Shutdown TTS
                                    tts.shutdown()
                                    Log.d("WelcomeScreen", "TTS stopped and shutdown - listener removed")
                                }
                            } catch (e: Exception) {
                                Log.e("WelcomeScreen", "Error stopping TTS: ${e.message}")
                            }
                        }
                        
                        val totalDuration = System.currentTimeMillis() - startTime
                        Log.d("WelcomeScreen", "Total click handling time: ${totalDuration}ms")
                    }
                },
                indication = null, // Remove ripple effect
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        // 1. Draw the Background Image first (vibrant, no dark overlay)
        Image(
            painter = backgroundImage,
            contentDescription = "Welcome Screen Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Draw the Guest Image (or Room Image fallback) on top of the background (aligned to the right)
        val guestImageState = guestImage.state
        Image(
            painter = if (guestImageState is AsyncImagePainter.State.Success) guestImage else roomImage,
            contentDescription = "Guest Image",
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.CenterEnd) // Centering guest image
                .fillMaxSize(.5f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(.6f)
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
                var isIconLoadError by remember(iconUrl) { mutableStateOf(false) }
                if (iconUrl != null && iconUrl!!.isNotEmpty()) {
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
                            model = iconUrl,
                            imageLoader = svgAwareImageLoader,
                            contentDescription = "Company Logo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            contentScale = ContentScale.Fit,
                            onError = { isIconLoadError = true }
                        )
                    }
                } else {
                    Text(
                        text = "Your Company Logo",
                        color = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(64.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(.5f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Dear ${formatName(guestInfo?.fname ?: "Guest Name")}",
                        modifier = Modifier
                            .padding(0.dp),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 0.dp),
                        color = Color.White,
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
                        color = Color.White,
                        maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Visible,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Best Regards",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "General Manager",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(
                                text = welcomeData.gm,
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        // Signature image with higher z-index
                        Image(
                            painter = signImage,
                            contentDescription = "sign",
                            modifier = Modifier
                                .width(200.dp)
                                .height(144.dp)
                                .align(Alignment.Center)
                                .zIndex(1f),
                            contentScale = ContentScale.Fit
                        )
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
                style = MaterialTheme.typography.titleSmall
            )
    }
}