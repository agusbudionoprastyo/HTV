package com.dafamsemarang.dhtv

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.text.PlatformTextStyle
import androidx.activity.compose.BackHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import java.util.TimeZone
import java.util.Calendar
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Surface
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.media.AudioManager
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.core.graphics.createBitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.scale
import android.content.Intent
import android.widget.Toast
import androidx.core.content.edit
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants

import androidx.compose.ui.geometry.CornerRadius

//// Function to set system volume
//fun setSystemVolume(context: Context, isMuted: Boolean) {
//    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//
//    if (isMuted) {
//        // Mute the system volume (set to 0)
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
//    } else {
//        // Set the system volume to a specific level (e.g., max volume)
//        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
//        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
//    }
//}

fun setSystemVolume(context: Context, isMuted: Boolean) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

    if (isMuted) {
        // Save current volume before muting (only if volume is not already 0)
        if (currentVolume > 0) {
            sharedPreferences.edit().putInt("last_volume", currentVolume).apply()
            Log.d("FooterSection", "Saved volume before muting: $currentVolume")
        }
        
        // Mute the system volume (set to 0)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        Log.d("FooterSection", "Volume muted (set to 0)")
    } else {
        // When unmuting, always use saved volume if available
        val savedVolume = sharedPreferences.getInt("last_volume", -1)
        
        val volumeToSet = when {
            // Priority 1: Use saved volume if available and valid
            savedVolume > 0 && savedVolume <= maxVolume -> {
                Log.d("FooterSection", "Restoring saved volume: $savedVolume")
                savedVolume
            }
            // Priority 2: If current volume is already set (user might have changed it manually), use it
            currentVolume > 0 -> {
                Log.d("FooterSection", "Using current volume (no saved volume): $currentVolume")
                currentVolume
            }
            // Priority 3: Only default to 80% if volume is 0 AND no saved volume exists
            else -> {
                val defaultVolume = (maxVolume * 0.8).toInt()
                Log.d("FooterSection", "No saved volume and current is 0, defaulting to 80%: $defaultVolume")
                defaultVolume
            }
        }
        
        // Only save volume if:
        // 1. We're restoring a saved volume (it's already saved, but ensure it's still there)
        // 2. We're using current volume that's not 0 (user might have set it manually)
        // 3. We're NOT using the default 80% (don't save defaults)
        val defaultVolume80 = (maxVolume * 0.8).toInt()
        if (savedVolume > 0 && savedVolume == volumeToSet) {
            // Volume is already saved, no need to save again
            Log.d("FooterSection", "Volume already saved, no need to save again: $volumeToSet")
        } else if (volumeToSet != defaultVolume80) {
            // Save non-default volume
            sharedPreferences.edit().putInt("last_volume", volumeToSet).apply()
            Log.d("FooterSection", "Saved volume after unmuting: $volumeToSet")
        } else {
            Log.d("FooterSection", "Not saving default 80% volume - waiting for user to set preferred volume")
        }
        
        // Set the system volume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeToSet, 0)
        Log.d("FooterSection", "Volume set to: $volumeToSet (max: $maxVolume)")
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun FooterSection(navController: androidx.navigation.NavHostController? = null)  {
    val navBackStackEntry = navController?.currentBackStackEntryAsState()?.value
    val currentRoute = navBackStackEntry?.destination?.route

    val baseAlpha = 0.15f

    var isNotifFocused by remember { mutableStateOf(false) }
    var isSettingsFocused by remember { mutableStateOf(false) }
    var focusedLabel3 by remember { mutableStateOf<String?>(null) }
    var focusedLabel4 by remember { mutableStateOf<String?>(null) }

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

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val deviceID = sharedPreferences.getString("deviceID", null)
    val branchId = sharedPreferences.getString("branchId", null)
    val guestInfo by DataRepository.guestInfo
    val folioId = guestInfo?.folio

    Log.d("FooterSection", "Initializing FooterSection with deviceID: $deviceID, branchId: $branchId")

    // Proper state management to ensure notification is shown only once
    //    var showPinDialog by remember { mutableStateOf(false) }
    //    var pinInput by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showWaDialog by remember { mutableStateOf(false) }
    var showNotificationButtonDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    val isDndActive by DataRepository.isDndActive
    var notificationCount by remember { mutableIntStateOf(0) }
    var myRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var selectedRequestForDetail by remember { mutableStateOf<Request?>(null) }
    var showRequestDetailDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showReleaseConfirmDialog by remember { mutableStateOf(false) }
    var currentNotification by remember { mutableStateOf<Notification?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showMyRequestsDrawer by remember { mutableStateOf(false) }
    var showCartDrawer by remember { mutableStateOf(false) }
    var showOrderDrawer by remember { mutableStateOf(false) }
    var selectedOrderForDetail by remember { mutableStateOf<Order?>(null) }
    var showOrderDetailDialog by remember { mutableStateOf(false) }

    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    val database: DatabaseReference = Firebase.database.reference

    // Pre-fetch Wi-Fi Data
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiIsWebLogin by remember { mutableStateOf(false) }
    var wifiLoading by remember { mutableStateOf(true) }
    var wifiQrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Pre-fetch WhatsApp/Contact Data
    var waPhone by remember { mutableStateOf("") }
    var waExt by remember { mutableStateOf("") }
    var waTelephone by remember { mutableStateOf("") }
    var waAddress by remember { mutableStateOf("") }
    var waMessage by remember { mutableStateOf("") }
    var waLoading by remember { mutableStateOf(true) }
    var waQrBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(branchId) {
        if (branchId != null) {
            // Fetch Wi-Fi Settings
            database.child("BRANCHES").child(branchId).child("SETTING/WIFI")
                .get()
                .addOnSuccessListener { snapshot ->
                    wifiSsid = snapshot.child("ssid").getValue(String::class.java) ?: ""
                    wifiPassword = snapshot.child("password").getValue(String::class.java) ?: ""
                    wifiIsWebLogin = snapshot.child("isWebLogin").getValue(Boolean::class.java) ?: false
                    wifiLoading = false
                }
                .addOnFailureListener {
                    wifiLoading = false
                }

            // Fetch Contact Settings
            database.child("BRANCHES").child(branchId).child("SETTING/CONTACT")
                .get()
                .addOnSuccessListener { snapshot ->
                    waPhone = snapshot.child("PHONE").getValue(String::class.java) ?: ""
                    val rawMessage = snapshot.child("MESSAGE").getValue(String::class.java) ?: ""
                    try {
                        waMessage = java.net.URLEncoder.encode(rawMessage, java.nio.charset.StandardCharsets.UTF_8.toString())
                    } catch (e: Exception) {
                        waMessage = rawMessage
                    }
                    waExt = snapshot.child("EXT").getValue(String::class.java) ?: ""
                    waTelephone = snapshot.child("TELEPHONE").getValue(String::class.java) ?: ""
                    waAddress = snapshot.child("ADDRESS").getValue(String::class.java) ?: ""
                    waLoading = false
                }
                .addOnFailureListener {
                    waLoading = false
                }
        }
    }

    // Pre-generate Wi-Fi QR Bitmap
    LaunchedEffect(wifiSsid, wifiPassword, wifiIsWebLogin) {
        if (wifiSsid.isNotEmpty()) {
            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                generateWifiQRCode(wifiSsid, wifiPassword, wifiIsWebLogin)
            }
            wifiQrBitmap = bitmap
        }
    }

    // Pre-generate WhatsApp QR Bitmap
    LaunchedEffect(waPhone, waMessage, deviceID) {
        if (waPhone.isNotEmpty() && deviceID != null) {
            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                generateWaQRCode(waPhone, waMessage, deviceID)
            }
            waQrBitmap = bitmap
        }
    }

    DisposableEffect(folioId, branchId) {
        var activeQuery: com.google.firebase.database.Query? = null
        var activeListener: com.google.firebase.database.ValueEventListener? = null

        if (folioId != null && branchId != null) {
            val orderRef = database.child("BRANCHES").child(branchId).child("ORDERS")
            val query = orderRef.orderByChild("folioId").equalTo(folioId.toDouble())
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Order>()
                    for (dataSnapshot in snapshot.children) {
                        val order = dataSnapshot.getValue(Order::class.java)
                        if (order?.branchId == branchId) {
                            order.let { list.add(it) }
                        }
                    }
                    orders = list.sortedByDescending { it.timestamp }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            activeQuery = query
            activeListener = listener
            query.addValueEventListener(listener)
        }

        onDispose {
            if (activeQuery != null && activeListener != null) {
                activeQuery.removeEventListener(activeListener)
            }
        }
    }

    // Floating Cart/My Order state – hoisted here so outer Box can draw them
    var isCartFocused by remember { mutableStateOf(false) }
    var isOrderFocused by remember { mutableStateOf(false) }
    var fnbButtonBoundsInRoot by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var homeButtonBoundsInRoot by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    // Floating My Request state – hoisted here
    var isMyRequestFocused by remember { mutableStateOf(false) }
    var requestButtonBoundsInRoot by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    val cartFocusRequester = remember { FocusRequester() }
    val orderFocusRequester = remember { FocusRequester() }
    LaunchedEffect(cartFocusRequester) {
        GlobalCartState.cartFocusRequester = cartFocusRequester
    }

    val homeFocusRequester = remember { FocusRequester() }
    val foodFocusRequester = remember { FocusRequester() }
    val hotelFocusRequester = remember { FocusRequester() }
    val requestFocusRequester = remember { FocusRequester() }
    val myRequestFocusRequester = remember { FocusRequester() }
    val dndFocusRequester = remember { FocusRequester() }
    val wifiFocusRequester = remember { FocusRequester() }
    val whatsappFocusRequester = remember { FocusRequester() }
    val notificationFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val lastFocused = GlobalFocusTracker.lastFocusedItem
                Log.d("FooterSection", "Lifecycle ON_RESUME - Restoring focus to: $lastFocused")
                if (lastFocused != null && lastFocused.startsWith("footer_")) {
                    scope.launch {
                        delay(400)
                        try {
                            when (lastFocused) {
                                "footer_home" -> homeFocusRequester.requestFocus()
                                "footer_food" -> foodFocusRequester.requestFocus()
                                "footer_hotel" -> hotelFocusRequester.requestFocus()
                                "footer_contact" -> requestFocusRequester.requestFocus()
                                "footer_dnd" -> dndFocusRequester.requestFocus()
                                "footer_wifi" -> wifiFocusRequester.requestFocus()
                                "footer_whatsapp" -> whatsappFocusRequester.requestFocus()
                            }
                            Log.d("FooterSection", "Successfully restored focus to $lastFocused")
                        } catch (e: Exception) {
                            Log.e("FooterSection", "Failed to restore focus to $lastFocused: ${e.message}")
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var wasScreenSaverActive by remember { mutableStateOf(false) }
    LaunchedEffect(ScreenSaverManager.isScreenSaverActive) {
        val isActive = ScreenSaverManager.isScreenSaverActive
        if (wasScreenSaverActive && !isActive) {
            try {
                homeFocusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("FooterSection", "Failed to refocus home button: ${e.message}")
            }
        }
        wasScreenSaverActive = isActive
    }

    var isFooterFocused by remember { mutableStateOf(false) }

    var footerTime by remember { mutableStateOf("") }
    var footerDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
            dateFormat.timeZone = TimeZone.getTimeZone("GMT+7")
            timeFormat.timeZone = TimeZone.getTimeZone("GMT+7")
            footerDate = dateFormat.format(Date())
            footerTime = timeFormat.format(Date())
            delay(1000)
        }
    }

    BackHandler(enabled = true) {
        if (!isFooterFocused) {
            try {
                when (currentRoute) {
                    "home" -> homeFocusRequester.requestFocus()
                    "cantingfood" -> foodFocusRequester.requestFocus()
                    "hotel_guide" -> hotelFocusRequester.requestFocus()
                    "contact" -> requestFocusRequester.requestFocus()
                }
            } catch (e: Exception) {
                Log.e("FooterSection", "Failed to auto-focus footer on back press: ${e.message}")
            }
        } else {
            // User is already focused on footer, do nothing to prevent going back to previous screen
            Log.d("FooterSection", "Back pressed while footer focused - action ignored")
        }
    }



    fun setAudioVolume(isMuted: Boolean) {
        setSystemVolume(context, isMuted)
    }

    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.notif) }

    var storedPin by remember { mutableStateOf<String?>(sharedPreferences.getString("cached_pin", "4646")) }

    DisposableEffect(Unit) {
        var pinRef: com.google.firebase.database.DatabaseReference? = null
        var pinListener: com.google.firebase.database.ValueEventListener? = null
        
        if (branchId != null) {
            pinRef = database.child("BRANCHES").child(branchId).child("SETTING/PIN")
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val pin = snapshot.getValue(String::class.java)
                    if (pin != null) {
                        storedPin = pin
                        sharedPreferences.edit { putString("cached_pin", pin) }
                    } else {
                        storedPin = sharedPreferences.getString("cached_pin", "4646")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    storedPin = sharedPreferences.getString("cached_pin", "4646")
                }
            }
            pinListener = listener
            pinRef.addValueEventListener(listener)
        } else {
            storedPin = sharedPreferences.getString("cached_pin", "4646")
        }
        
        onDispose {
            if (pinRef != null && pinListener != null) {
                pinRef.removeEventListener(pinListener)
            }
        }
    }

    // Guest info is preloaded globally in DataRepository

    DisposableEffect(folioId) {
        var countRef: com.google.firebase.database.DatabaseReference? = null
        var countListener: com.google.firebase.database.ValueEventListener? = null
        
        if (folioId != null && branchId != null) {
            countRef = database.child("BRANCHES").child(branchId).child("NOTIFICATIONS").child(folioId.toString())
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    notificationCount = snapshot.childrenCount.toInt()
                }
                override fun onCancelled(error: DatabaseError) {
                    notificationCount = 0
                }
            }
            countListener = listener
            countRef.addValueEventListener(listener)
        }
        
        onDispose {
            if (countRef != null && countListener != null) {
                countRef?.removeEventListener(countListener!!)
            }
        }
    }

    DisposableEffect(folioId, branchId) {
        var requestsRef: com.google.firebase.database.Query? = null
        var requestsListener: com.google.firebase.database.ValueEventListener? = null

        val currentFolioId = folioId
        if (currentFolioId != null && branchId != null) {
            requestsRef = database.child("BRANCHES").child(branchId).child("REQUEST")
                .orderByChild("folioId").equalTo(currentFolioId.toDouble())
            
            requestsListener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Request>()
                    for (child in snapshot.children) {
                        child.getValue(Request::class.java)?.let { list.add(it) }
                    }
                    myRequests = list.reversed()
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            requestsRef.addValueEventListener(requestsListener)
        }

        onDispose {
            if (requestsRef != null && requestsListener != null) {
                requestsRef?.removeEventListener(requestsListener!!)
            }
        }
    }


    var isFirstDndLoad by remember { mutableStateOf(true) }
    
    LaunchedEffect(isDndActive) {
        if (!isFirstDndLoad || isDndActive) {
            setAudioVolume(isDndActive)
        } else {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentVolume > 0) {
                sharedPreferences.edit().putInt("last_volume", currentVolume).apply()
            }
        }
        isFirstDndLoad = false
    }

    // ── Smart Debounce for Footer Navigation ──
    var pendingNavigationRoute by remember { mutableStateOf<String?>(null) }
    var lastNavigationTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(pendingNavigationRoute) {
        val route = pendingNavigationRoute ?: return@LaunchedEffect
        val currentTime = System.currentTimeMillis()
        val timeSinceLastChange = currentTime - lastNavigationTime
        lastNavigationTime = currentTime

        if (timeSinceLastChange < 300) {
            // Speed scrolling detected! Apply debounce delay so we don't crash/lag loading 5 screens.
            kotlinx.coroutines.delay(250)
        }
        
        if (currentRoute != route) {
            navController?.navigate(route) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    DisposableEffect(folioId) {
        var nRef: com.google.firebase.database.DatabaseReference? = null
        var nListener: com.google.firebase.database.ValueEventListener? = null

        if (folioId != null) {
            nRef = database.child("BRANCHES").child(branchId ?: "").child("NOTIFICATIONS").child(folioId.toString())
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val unreadNotification = snapshot.children
                        .mapNotNull { it.getValue(Notification::class.java) }
                        .firstOrNull { it.status == "unread" }

                    unreadNotification?.let {
                        currentNotification = it
                        showNotificationDialog = true
                        try { mediaPlayer.start() } catch (e: Exception) {}
                    }
                }
                override fun onCancelled(error: DatabaseError) { }
            }
            nListener = listener
            nRef.addValueEventListener(listener)
        }

        onDispose {
            if (nRef != null && nListener != null) {
                nRef.removeEventListener(nListener)
                Log.d("FooterSection", "Notifications monitor disposed.")
            }
        }
    }

    if (showNotificationDialog && currentNotification != null) {
        NotificationDialog(
            context = context,
            notification = currentNotification!!,
            onDismiss = {
                folioId?.let { id ->
                    updateNotificationStatus(context, currentNotification!!, id, "read")
                }
                showNotificationDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .padding(start = 58.dp, bottom = 16.dp)
                .then(
                    if (currentRoute == "home") Modifier.width(448.dp)
                    else Modifier.fillMaxWidth().padding(end = 58.dp)
                )
                .align(Alignment.BottomStart)
                .focusProperties {
                    enter = {
                        when (currentRoute) {
                            "home" -> {
                                val lastFooter = GlobalFocusTracker.lastFocusedFooterItem
                                when (lastFooter) {
                                    "footer_home" -> homeFocusRequester
                                    "footer_food" -> foodFocusRequester
                                    "footer_hotel" -> hotelFocusRequester
                                    "footer_contact" -> requestFocusRequester
                                    "footer_dnd" -> dndFocusRequester
                                    "footer_wifi" -> wifiFocusRequester
                                    "footer_whatsapp" -> whatsappFocusRequester
                                    "footer_notification" -> notificationFocusRequester
                                    "footer_settings" -> settingsFocusRequester
                                    else -> FocusRequester.Default
                                }
                            }
                            "cantingfood" -> foodFocusRequester
                            "hotel_guide" -> hotelFocusRequester
                            "contact" -> requestFocusRequester
                            else -> FocusRequester.Default
                        }
                    }
                }
                .focusGroup()
                .onFocusChanged { isFooterFocused = it.hasFocus },
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Capsule 1 (Notifications)
            Box {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            color = Color(207, 223, 237).copy(alpha = baseAlpha),
                            shape = RoundedCornerShape(50.dp)
                        )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.padding(4.dp)
                ) {
                    SmallServiceButtonWithBadge(
                        iconRes = R.drawable.ic_notifications,
                        badgeCount = notificationCount,
                        onClick = { showNotificationButtonDialog = true },
                        title = "Notifications",
                        isActive = true,
                        focusRequester = notificationFocusRequester,
                        onFocusStateChange = { isFocused ->
                            if (isFocused) {
                                GlobalFocusTracker.lastFocusedItem = "footer_notification"
                                GlobalFocusTracker.lastFocusedFooterItem = "footer_notification"
                            }
                        }
                    )
                }
            }

            // Capsule 2 (Settings)
            Box {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            color = Color(207, 223, 237).copy(alpha = baseAlpha),
                            shape = RoundedCornerShape(50.dp)
                        )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.padding(4.dp)
                ) {
                    SmallServiceButton(
                        iconRes = R.drawable.ic_setting,
                        onClick = { showPinDialog = true },
                        title = "Settings",
                        isActive = true,
                        focusRequester = settingsFocusRequester,
                        onFocusStateChange = { isFocused ->
                            if (isFocused) {
                                GlobalFocusTracker.lastFocusedItem = "footer_settings"
                                GlobalFocusTracker.lastFocusedFooterItem = "footer_settings"
                            }
                        }
                    )
                }
            }


            // Capsule 3 (DND / Wi-Fi / WhatsApp)
            Box {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            color = Color(207, 223, 237).copy(alpha = baseAlpha),
                            shape = RoundedCornerShape(50.dp)
                        )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.padding(4.dp)
                ) {
                    SmallServiceButton(
                        iconRes = R.drawable.ic_dnd,
                        buttonColor = FooterIcon,
                        onClick = {
                            Log.d("FooterSection", "DND button clicked. Current status: $isDndActive, folioId: $folioId")
                            if (isDndActive) {
                                showReleaseConfirmDialog = true
                            } else {
                                showConfirmDialog = true
                            }
                        },
                        title = "DND",
                        isActive = true,
                        focusRequester = dndFocusRequester,
                        onFocusStateChange = { isFocused ->
                            if (isFocused) {
                                GlobalFocusTracker.lastFocusedItem = "footer_dnd"
                                GlobalFocusTracker.lastFocusedFooterItem = "footer_dnd"
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    SmallServiceButton(
                        iconRes = R.drawable.ic_wifi_rounded,
                        onClick = { showDialog = true },
                        title = "Wi-Fi",
                        isActive = true,
                        focusRequester = wifiFocusRequester,
                        onFocusStateChange = { isFocused ->
                            if (isFocused) {
                                GlobalFocusTracker.lastFocusedItem = "footer_wifi"
                                GlobalFocusTracker.lastFocusedFooterItem = "footer_wifi"
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    SmallServiceButton(
                        iconRes = R.drawable.ic_whatsapp,
                        onClick = { showWaDialog = true },
                        title = "WhatsApp",
                        isActive = true,
                        focusRequester = whatsappFocusRequester,
                        onFocusStateChange = { isFocused ->
                            if (isFocused) {
                                GlobalFocusTracker.lastFocusedItem = "footer_whatsapp"
                                GlobalFocusTracker.lastFocusedFooterItem = "footer_whatsapp"
                            }
                        }
                    )
                }
            }

            // Capsule 4 (Home / F&B / Request / Hotel Info)
            Box {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(
                            color = Color(207, 223, 237).copy(alpha = baseAlpha),
                            shape = CircleShape
                        )
                        .animateContentSize(animationSpec = tween(durationMillis = 500))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.padding(4.dp)
                ) {
                    SmallServiceButton(
                        iconRes = null,
                        buttonText = "Home",
                        modifier = Modifier
                            .width(73.dp)
                            .onGloballyPositioned { coords ->
                                homeButtonBoundsInRoot = coords.boundsInRoot()
                            },
                        onClick = {
                            if (currentRoute != "home") navController?.navigate("home") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        title = null, // Tooltip is no longer needed since button has text
                        onFocusAction = {
                            pendingNavigationRoute = "home"
                        },
                        isActive = currentRoute == "home",
                        showBackgroundWhenActive = true,
                        focusRequester = homeFocusRequester,
                        onFocusStateChange = { isFocused ->
                            if (isFocused) {
                                GlobalFocusTracker.lastFocusedItem = "footer_home"
                                GlobalFocusTracker.lastFocusedFooterItem = "footer_home"
                            }
                        },
                        useOriginalTint = true,
                        isHome3D = false
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // F&B button – NOT wrapped, track its position via onGloballyPositioned
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                fnbButtonBoundsInRoot = coords.boundsInRoot()
                            }
                            .wrapContentSize()
                    ) {
                        SmallServiceButton(
                            iconRes = R.drawable.ic_room_service,
                            onClick = {
                                if (currentRoute != "cantingfood") navController?.navigate("cantingfood") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            title = null,
                            onFocusAction = {
                                pendingNavigationRoute = "cantingfood"
                            },
                            isActive = currentRoute == "cantingfood",
                            focusRequester = foodFocusRequester,
                            onFocusStateChange = { isFocused ->
                                if (isFocused) {
                                    GlobalFocusTracker.lastFocusedItem = "footer_food"
                                    GlobalFocusTracker.lastFocusedFooterItem = "footer_food"
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                requestButtonBoundsInRoot = coords.boundsInRoot()
                            }
                            .wrapContentSize()
                    ) {
                        SmallServiceButton(
                            iconRes = R.drawable.ic_request_service,
                            onClick = {
                                if (currentRoute != "contact") navController?.navigate("contact") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            title = null,
                            onFocusAction = {
                                pendingNavigationRoute = "contact"
                            },
                            isActive = currentRoute == "contact",
                            focusRequester = requestFocusRequester,
                            onFocusStateChange = { isFocused ->
                                if (isFocused) {
                                    GlobalFocusTracker.lastFocusedItem = "footer_contact"
                                    GlobalFocusTracker.lastFocusedFooterItem = "footer_contact"
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    SmallServiceButton(
                        iconRes = R.drawable.ic_info_circle,
                        onClick = {
                            if (currentRoute != "hotel_guide") navController?.navigate("hotel_guide") {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        title = null,
                        onFocusAction = {
                            pendingNavigationRoute = "hotel_guide"
                        },
                        isActive = currentRoute == "hotel_guide",
                        focusRequester = hotelFocusRequester,
                        onFocusStateChange = { isFocused ->
                            if (isFocused) {
                                GlobalFocusTracker.lastFocusedItem = "footer_hotel"
                                GlobalFocusTracker.lastFocusedFooterItem = "footer_hotel"
                            }
                        },
                        modifier = Modifier.focusProperties {
                            up = HotelInfoFocus.firstItemRequester
                        }
                    )
                }
            }
        }

        // Clock/Date widget on the footer right
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 58.dp, bottom = 16.dp)
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(start = 0.dp, top = 6.dp, end = 0.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val instagram = DataRepository.instagramHandle.value
            val facebook = DataRepository.facebookHandle.value
            val tiktok = DataRepository.tiktokHandle.value
            val website = DataRepository.websiteUrl.value

            data class SocMedItem(val iconRes: Int, val text: String)
            val socMedItems = remember(instagram, facebook, tiktok, website) {
                val list = mutableListOf<SocMedItem>()
                if (!instagram.isNullOrEmpty()) list.add(SocMedItem(R.drawable.logo_instagram, instagram))
                if (!facebook.isNullOrEmpty()) list.add(SocMedItem(R.drawable.logo_facebook, facebook))
                if (!tiktok.isNullOrEmpty()) list.add(SocMedItem(R.drawable.logo_tiktok, tiktok))
                if (!website.isNullOrEmpty()) list.add(SocMedItem(R.drawable.logo_website, website))
                list
            }

            if (socMedItems.isNotEmpty()) {
                var currentSocMedIndex by remember { mutableIntStateOf(0) }
                LaunchedEffect(socMedItems) {
                    if (socMedItems.size > 1) {
                        while (true) {
                            delay(10000)
                            currentSocMedIndex = (currentSocMedIndex + 1) % socMedItems.size
                        }
                    } else {
                        currentSocMedIndex = 0
                    }
                }

                AnimatedContent(
                    targetState = socMedItems.getOrNull(currentSocMedIndex),
                    transitionSpec = {
                        (slideInVertically(animationSpec = tween(500)) { height -> height } + fadeIn(animationSpec = tween(500)))
                            .togetherWith(slideOutVertically(animationSpec = tween(500)) { height -> -height } + fadeOut(animationSpec = tween(500)))
                    },
                    contentAlignment = Alignment.CenterEnd,
                    label = "SocMedTransition"
                ) { item ->
                    if (item != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.55f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = item.text,
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            // Jam
            Text(
                text = footerTime,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Floating Cart & My Order ──────────────────────────────────────────────
        // Direct children of outer fillMaxSize Box → zero impact on Row layout
        val isFnBActive = currentRoute == "cantingfood"
        val showCartOrder = isFnBActive || isCartFocused || isOrderFocused
        
        var debouncedShowCartOrder by remember { mutableStateOf(showCartOrder) }
        LaunchedEffect(showCartOrder) {
            if (showCartOrder) {
                debouncedShowCartOrder = true
            } else {
                if (currentRoute != "cantingfood") {
                    debouncedShowCartOrder = false
                } else {
                    delay(150) // 150ms debounce delay only when focus changes on the same screen
                    debouncedShowCartOrder = isCartFocused || isOrderFocused || isFnBActive
                }
            }
        }

        val floatingAlpha by animateFloatAsState(
            targetValue = if (debouncedShowCartOrder) 1f else 0f,
            animationSpec = tween(durationMillis = 250),
            label = "floatingBtnAlpha"
        )
        val density = androidx.compose.ui.platform.LocalDensity.current

        // Slide offset: saat hidden Y = 0 (di posisi F&B), saat visible Y = -46dp (naik ke atas)
        val slideOffsetYPx by animateFloatAsState(
            targetValue = if (debouncedShowCartOrder) with(density) { -46.dp.toPx() } else 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "floatingSlideY"
        )

        fnbButtonBoundsInRoot?.let { bounds ->
            val btnCenterX = bounds.left + bounds.width / 2
            val btnTop = bounds.top
            val btnSize = with(density) { 36.dp.toPx() }
            val gap = with(density) { 8.dp.toPx() }

            val homeBounds = homeButtonBoundsInRoot
            val cartX = if (homeBounds != null) {
                (homeBounds.left + homeBounds.width / 2 - btnSize / 2).toInt()
            } else {
                (btnCenterX - btnSize - gap / 2).toInt()
            }

            // Cart – diatas Home
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            x = cartX,
                            y = (btnTop + slideOffsetYPx).toInt()
                        )
                    }
                    .alpha(floatingAlpha)
                    .size(36.dp)
            ) {
                // Button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            color = if (isCartFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.15f)
                        )
                        .focusRequester(cartFocusRequester)
                        .onFocusChanged { isCartFocused = it.isFocused }
                        .focusable(enabled = isFnBActive)
                        .clickable(
                            enabled = isFnBActive,
                            onClick = { showCartDrawer = true },
                            indication = ripple(color = Color(0xFF88B4D4)),
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .onGloballyPositioned { coords ->
                            GlobalCartState.cartBoundsInRoot.value = coords.boundsInRoot()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cart),
                        contentDescription = "Cart",
                        modifier = Modifier.size(20.dp),
                        tint = if (isCartFocused) Color(0xFF1C1D24) else Color.White
                    )
                }

                // Badge jika ada item di cart (angka bertambah realtime setelah dot masuk ke keranjang)
                val actualCartCount = GlobalCartState.selectedItems.size
                var displayedCartCount by remember { mutableStateOf(actualCartCount) }
                val animateTrigger = GlobalCartState.animateTrigger.value

                LaunchedEffect(actualCartCount) {
                    if (actualCartCount < displayedCartCount) {
                        displayedCartCount = actualCartCount
                    } else if (actualCartCount > displayedCartCount) {
                        if (animateTrigger > 0) {
                            delay(650) // Wait exactly for the 650ms flying dot animation to enter the cart
                        }
                        displayedCartCount = actualCartCount
                    }
                }

                DisposableEffect(actualCartCount) {
                    if (animateTrigger == 0) {
                        displayedCartCount = actualCartCount
                    }
                    onDispose {}
                }

                if (displayedCartCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$displayedCartCount",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    }
                }
            }

            val orderX = (btnCenterX - btnSize / 2).toInt()

            // My Order – diatas F&B (Room Service)
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            x = orderX,
                            y = (btnTop + slideOffsetYPx).toInt()
                        )
                    }
                    .alpha(floatingAlpha)
                    .size(36.dp)
            ) {
                // Button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            color = if (isOrderFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.15f)
                        )
                        .focusRequester(orderFocusRequester)
                        .onFocusChanged { isOrderFocused = it.isFocused }
                        .focusable(enabled = isFnBActive)
                        .clickable(
                            enabled = isFnBActive,
                            onClick = { showOrderDrawer = true },
                            indication = ripple(color = Color(0xFF88B4D4)),
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_order),
                        contentDescription = "My Order",
                        modifier = Modifier.size(20.dp),
                        tint = if (isOrderFocused) Color(0xFF1C1D24) else Color.White
                    )
                }

                // Badge jika ada order
                val orderCount = orders.size
                if (orderCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$orderCount",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
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
        // ─────────────────────────────────────────────────────────────────────────

        // ── Floating My Request ──────────────────────────────────────────────────
        val isRequestActive = currentRoute == "contact"
        val showRequestCapsule = isRequestActive || isMyRequestFocused

        // Debounce showRequestCapsule state to smooth out transition animations and prevent stuttering
        var debouncedShowRequestCapsule by remember { mutableStateOf(showRequestCapsule) }
        LaunchedEffect(showRequestCapsule) {
            if (showRequestCapsule) {
                debouncedShowRequestCapsule = true
            } else {
                if (currentRoute != "contact") {
                    debouncedShowRequestCapsule = false
                } else {
                    delay(150) // 150ms debounce delay only when focus changes on the same screen
                    debouncedShowRequestCapsule = isMyRequestFocused || isRequestActive
                }
            }
        }

        val requestAlpha by animateFloatAsState(
            targetValue = if (debouncedShowRequestCapsule) 1f else 0f,
            animationSpec = tween(durationMillis = 250),
            label = "requestBtnAlpha"
        )
        val requestSlideOffsetYPx by animateFloatAsState(
            targetValue = if (debouncedShowRequestCapsule) with(density) { -46.dp.toPx() } else 0f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            label = "requestSlideY"
        )

        requestButtonBoundsInRoot?.let { bounds ->
            val btnCenterX = bounds.left + bounds.width / 2
            val btnTop = bounds.top

            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            x = (btnCenterX - with(density) { 18.dp.toPx() }).toInt(),
                            y = (btnTop + requestSlideOffsetYPx).toInt()
                        )
                    }
                    .alpha(requestAlpha)
                    .size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            color = if (isMyRequestFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .focusRequester(myRequestFocusRequester)
                        .onFocusChanged { isMyRequestFocused = it.isFocused }
                        .focusable(enabled = isRequestActive)
                        .clickable(
                            enabled = isRequestActive,
                            onClick = { showMyRequestsDrawer = true },
                            indication = ripple(color = Color(0xFF88B4D4)),
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_request),
                        contentDescription = "My Request",
                        modifier = Modifier.size(20.dp),
                        tint = if (isMyRequestFocused) Color(0xFF1C1D24) else Color.White
                    )
                }

                // Badge jika ada requests
                if (myRequests.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${myRequests.size}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
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

    if (showReleaseConfirmDialog) {
        DndConfirmDrawer(
            context = context,
            isDndActive = true,
            onDismiss = { showReleaseConfirmDialog = false },
            folioId = folioId ?: 0,
            deviceID = deviceID
        )
    }

    if (showConfirmDialog) {
        DndConfirmDrawer(
            context = context,
            isDndActive = false,
            onDismiss = { showConfirmDialog = false },
            folioId = folioId ?: 0,
            deviceID = deviceID
        )
    }

    if (showNotificationButtonDialog && folioId != null) {
        NotificationButtonDialog(
            context = context,
            showNotificationButtonDialog = showNotificationButtonDialog,
            onDismiss = {
                showNotificationButtonDialog = false
            },
            folioId = folioId!!
        )
    }

    if (showDialog) {
        WifiQRCodeDialog(
            ssid = wifiSsid,
            password = wifiPassword,
            isWebLogin = wifiIsWebLogin,
            roomNumber = DataRepository.guestInfo.value?.room ?: deviceID ?: "",
            loading = wifiLoading,
            qrCodeBitmap = wifiQrBitmap,
            onDismiss = { showDialog = false }
        )
    }

    if (showWaDialog) {
        WaQRCodeDialog(
            phone = waPhone,
            ext = waExt,
            telephone = waTelephone,
            address = waAddress,
            loading = waLoading,
            qrCodeBitmap = waQrBitmap,
            onDismiss = { showWaDialog = false }
        )
    }

    if (showPinDialog) {
        PinDialog(
            pinInput = pinInput,
            onPinChange = { pinInput = it },
            onDismiss = { showPinDialog = false },
            onPinConfirmed = { submittedPin ->
                if (storedPin != null) {
                    if (submittedPin == storedPin) {
                        showPinDialog = false
                        showSettingsMenu = true
                    } else {
                        Toast.makeText(context, "Access Denied.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Error fetching PIN", Toast.LENGTH_SHORT).show()
                }
                pinInput = ""
            },
            confirmEnabled = pinInput.length == 4 && storedPin != null
        )
    }

    if (showSettingsMenu) {
        SettingsOptionsDialog(onDismiss = { showSettingsMenu = false })
    }

    if (showMyRequestsDrawer) {
        MyRequestsDrawer(
            onDismiss = { 
                showMyRequestsDrawer = false 
                try {
                    myRequestFocusRequester.requestFocus()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 
            requests = myRequests,
            onSelectRequest = { req ->
                selectedRequestForDetail = req
                showRequestDetailDialog = true
            }
        )
    }

    if (showCartDrawer) {
        CartDrawer(
            onDismiss = { 
                showCartDrawer = false 
                try {
                    cartFocusRequester.requestFocus()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            context = context
        )
    }

    if (showOrderDrawer) {
        OrderDrawer(
            onDismiss = { 
                showOrderDrawer = false 
                try {
                    orderFocusRequester.requestFocus()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            context = context,
            orders = orders,
            onSelectOrder = { order ->
                selectedOrderForDetail = order
                showOrderDetailDialog = true
            }
        )
    }
    
    if (showOrderDetailDialog && selectedOrderForDetail != null) {
        OrderDetailDialog(
            order = selectedOrderForDetail!!,
            onDismiss = { showOrderDetailDialog = false }
        )
    }
    
    if (showRequestDetailDialog && selectedRequestForDetail != null) {
        RequestDetailDialog(
            request = selectedRequestForDetail!!,
            onDismiss = { showRequestDetailDialog = false }
        )
    }
    
    FlyingDotOverlay()
}
}

@Composable
fun FlyingDotOverlay() {
    val trigger = GlobalCartState.animateTrigger.value
    val startOffset = GlobalCartState.animStartOffset.value
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    if (trigger > 0 && startOffset != androidx.compose.ui.geometry.Offset.Zero) {
        var animProgress by remember(trigger) { androidx.compose.runtime.mutableFloatStateOf(0f) }
        val bounds = GlobalCartState.cartBoundsInRoot.value
        
        if (bounds != null) {
            val destination = remember(bounds) {
                androidx.compose.ui.geometry.Offset(
                    x = bounds.left + bounds.width / 2,
                    y = bounds.top + bounds.height / 2
                )
            }
            
            LaunchedEffect(trigger) {
                androidx.compose.animation.core.animate(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 600, // Sedikit dipercepat untuk feel lemparan yang snappy
                        easing = androidx.compose.animation.core.FastOutLinearInEasing // Mulai pelan, lalu berakselerasi cepat di akhir
                    )
                ) { value, _ ->
                    animProgress = value
                }
            }
            
            if (animProgress < 1f) {
                val currentX = startOffset.x + (destination.x - startOffset.x) * animProgress
                val linearY = startOffset.y + (destination.y - startOffset.y) * animProgress
                val arcHeight = with(density) { -140.dp.toPx() } // Parabola sedikit lebih melengkung agar elegan
                val currentY = linearY + arcHeight * 4 * animProgress * (1f - animProgress)
                
                val scale = 1.1f - animProgress * 0.4f // Scale melengkung yang lebih smooth
                val alpha = if (animProgress > 0.85f) (1f - animProgress) / 0.15f else 1f
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(9999f)
                ) {
                    Box(
                        modifier = Modifier
                            .absoluteOffset {
                                IntOffset(
                                    x = (currentX - with(density) { 4.dp.toPx() }).toInt(), // Offset center 8.dp / 2
                                    y = (currentY - with(density) { 4.dp.toPx() }).toInt()
                                )
                            }
                            .size(8.dp) // Dot diperkecil menjadi 8.dp agar elegan
                            .alpha(alpha)
                            .scale(scale)
                            .background(Color(0xFFE91E63), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun FooterClockIcon(modifier: Modifier = Modifier) {
    var calendar by remember { mutableStateOf(Calendar.getInstance(TimeZone.getTimeZone("GMT+7"))) }
    
    LaunchedEffect(Unit) {
        while (true) {
            calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+7"))
            delay(1000)
        }
    }
    
    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)
    
    val hourHandAngle = (hour % 12) * 30f + minute * 0.5f
    val minuteHandAngle = minute * 6f + second * 0.1f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val radius = width / 2
            val center = Offset(width / 2, height / 2)
            
            // 1. Draw solid filled background circle!
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = radius,
                center = center
            )
            
            // 2. Draw Hour Hand (Shorter, Thicker)
            rotate(hourHandAngle) {
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = center,
                    end = Offset(center.x, center.y - (radius * 0.5f)),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            // 3. Draw Minute Hand (Longer, Medium)
            rotate(minuteHandAngle) {
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = center,
                    end = Offset(center.x, center.y - (radius * 0.72f)),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            
            // 4. Center Pivot Pin (Solid Dark core)
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = 1.75.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
fun MyRequestsDrawer(onDismiss: () -> Unit, requests: List<Request>, onSelectRequest: (Request) -> Unit) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    
    fun closeWithAnimation() {
        scope.launch {
            isVisible = false
            delay(300) // Match exit animation duration
            onDismiss()
        }
    }
    
    // Update time every minute
    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())) }
    
    LaunchedEffect(Unit) {
        isVisible = true
        while(true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            currentDate = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())
            delay(60000)
        }
    }

    Dialog(
        onDismissRequest = {
            closeWithAnimation()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false },
            contentAlignment = Alignment.CenterEnd
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        closeWithAnimation()
                    }
            )

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
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 16.dp, end = 16.dp)
                        .width(360.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1E2026),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .focusGroup()
                    ) {
                        // Header: Date & Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = currentDate,
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = currentTime,
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Text(
                                text = "My Request",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))



                        LaunchedEffect(Unit) {
                            delay(450) // Wait for slide animation
                            if (requests.isNotEmpty()) {
                                try {
                                    firstItemFocusRequester.requestFocus()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        if (requests.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.request))
                                    val progress by animateLottieCompositionAsState(
                                        composition = composition,
                                        iterations = LottieConstants.IterateForever
                                    )
                                    LottieAnimation(
                                        composition = composition,
                                        progress = { progress },
                                        modifier = Modifier.size(180.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No active requests", 
                                        color = Color.White.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { alpha = 0.99f } // Required for DstIn blend mode
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                0.08f to Color.Black,
                                                0.92f to Color.Black,
                                                1f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    },
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 24.dp)
                            ) {
                                items(requests.size) { index ->
                                    val request = requests[index]
                                    var isItemFocused by remember { mutableStateOf(false) }

                                    // Snappy Google TV focus zoom scale transition
                                    val scale by animateFloatAsState(
                                        targetValue = if (isItemFocused) 1.03f else 1.0f,
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                        label = "RequestCardScale"
                                    )

                                    // Smooth fade in/out transition for focus visibility (LED Glow)
                                    val focusFadeAlpha by animateFloatAsState(
                                        targetValue = if (isItemFocused) 1.0f else 0.0f,
                                        animationSpec = tween(durationMillis = 350),
                                        label = "RequestFocusFadeAlpha"
                                    )

                                    val pulseAlpha = remember { androidx.compose.animation.core.Animatable(0.4f) }

                                    LaunchedEffect(isItemFocused) {
                                        if (isItemFocused) {
                                            pulseAlpha.animateTo(
                                                targetValue = 1.0f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                )
                                            )
                                        } else {
                                            pulseAlpha.snapTo(0.4f)
                                        }
                                    }

                                    // White border that pulses and fades in smoothly on focus
                                    val borderModifier = if (isItemFocused) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = Color.White.copy(alpha = pulseAlpha.value * focusFadeAlpha),
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                    } else {
                                        Modifier
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                                            }
                                            .onFocusChanged { isItemFocused = it.isFocused }
                                            .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                                            .clickable { onSelectRequest(request) }
                                            .then(borderModifier)
                                            .padding(6.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = Color.White.copy(alpha = 0.05f),
                                            tonalElevation = 0.dp
                                        ) {
                                            MyRequestItemDrawer(request = request, isFocused = isItemFocused)
                                        }
                                    }
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
fun MyRequestItemDrawer(request: Request, isFocused: Boolean) {
    val timestamp = request.timestamp ?: System.currentTimeMillis()
    val formattedTimestamp = getTimeAgo(timestamp)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Icon / Category Image
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            val guestRequest = request.requests?.firstOrNull()
            val imageUrl = guestRequest?.imageUrl ?: ""
            if (imageUrl.isNotEmpty()) {
                CachedAsyncImage(
                    imageUrl = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = R.drawable.err,
                    error = R.drawable.err
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_request_service),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = request.requests?.firstOrNull()?.request_title ?: "Request",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 16.sp
                )
            )
            Text(
                text = request.status?.uppercase() ?: "PENDING",
                color = getStatusColor(request.status),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 14.sp
                )
            )
            Text(
                text = formattedTimestamp,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeight = 12.sp
                )
            )
        }
    }
}

fun getStatusColor(status: String?): Color {
    return when (status?.lowercase()) {
        "completed", "done" -> Color(0xFF4CAF50) // Green
        "process", "on progress" -> Color(0xFF2196F3) // Blue
        "confirm" -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFFFC107) // Yellow
    }
}

@Composable
private fun FooterPulsingBadge(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE91E63)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnimation"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnimation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Pulsing background
        Box(
            modifier = Modifier
                .size(16.dp)
                .scale(scale)
                .alpha(alpha)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )
        
        // Main badge dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
    }
}

@Composable
fun DndConfirmDrawer(
    context: Context,
    isDndActive: Boolean,
    onDismiss: () -> Unit,
    folioId: Int,
    deviceID: String?
) {
    var animateIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sliderFocusRequester = remember { FocusRequester() }
    val closeButtonFocusRequester = remember { FocusRequester() }
    var isCloseFocused by remember { mutableStateOf(false) }

    var targetProgress by remember(isDndActive) { mutableFloatStateOf(if (isDndActive) 1f else 0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
    )

    LaunchedEffect(Unit) {
        animateIn = true
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    fun dismissWithAnimation() {
        animateIn = false
        scope.launch {
            delay(300)
            onDismiss()
        }
    }

    LaunchedEffect(targetProgress) {
        val currentFolioId = folioId
        if (currentFolioId != null) {
            if (isDndActive && targetProgress == 0f) {
                delay(350)
                Log.d("DndConfirmDrawer", "Confirming DND deactivation via auto-slide for folioId: $folioId")
                setDndStatusInFirebase(context, currentFolioId, false)
                sendDndNotification(context, currentFolioId, release = true, deviceID = deviceID)
                dismissWithAnimation()
            } else if (!isDndActive && targetProgress == 1f) {
                delay(350)
                Log.d("DndConfirmDrawer", "Confirming DND activation via auto-slide for folioId: $folioId")
                setDndStatusInFirebase(context, currentFolioId, true)
                sendDndNotification(context, currentFolioId, release = false, deviceID = deviceID)
                dismissWithAnimation()
            }
        }
    }

    LaunchedEffect(animateIn) {
        if (animateIn) {
            delay(100)
            sliderFocusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { dismissWithAnimation() }
            )

            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(20.dp)
                        .graphicsLayer(clip = false)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1E2026),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .focusGroup(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Do Not Disturb",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .focusRequester(closeButtonFocusRequester)
                                    .clip(CircleShape)
                                    .size(36.dp)
                                    .onFocusChanged { isCloseFocused = it.isFocused }
                                    .background(
                                        color = if (isCloseFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.05f),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        onClick = { dismissWithAnimation() },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                                    .focusable(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "\uF057",
                                    color = if (isCloseFocused) Color(0xFF071434) else Color.White.copy(alpha = 0.55f),
                                    style = TextStyle(fontSize = 18.sp),
                                    fontFamily = FontFamily(Font(R.font.icons))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = rememberAsyncImagePainter(R.drawable.ic_dnd),
                                contentDescription = "DND Icon",
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isDndActive) "Release Do Not Disturb" else "Activate Do Not Disturb",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (folioId == 0) {
                                "Layanan tidak tersedia karena tidak ada tamu aktif."
                            } else if (isDndActive) {
                                "Hold D-pad Left to release 'Do Not Disturb'"
                            } else {
                                "Hold D-pad Right to activate 'Do Not Disturb'"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        var isSliderFocused by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .width(420.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .onFocusChanged { isSliderFocused = it.isFocused }
                                .focusRequester(sliderFocusRequester)
                                .onKeyEvent { keyEvent ->
                                    if (folioId == 0) return@onKeyEvent false
                                    if (isDndActive) {
                                        if (keyEvent.key == Key.DirectionLeft) {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                targetProgress = (targetProgress - 0.08f).coerceAtLeast(0f)
                                                true
                                            } else if (keyEvent.type == KeyEventType.KeyUp) {
                                                if (targetProgress > 0f) {
                                                    targetProgress = 1f
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        } else if (keyEvent.key == Key.DirectionRight) {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                dismissWithAnimation()
                                                true
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }
                                    } else {
                                        if (keyEvent.key == Key.DirectionRight) {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                targetProgress = (targetProgress + 0.08f).coerceAtMost(1f)
                                                true
                                            } else if (keyEvent.type == KeyEventType.KeyUp) {
                                                if (targetProgress < 1f) {
                                                    targetProgress = 0f
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        } else if (keyEvent.key == Key.DirectionLeft) {
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                dismissWithAnimation()
                                                true
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                }
                                .focusable(enabled = folioId != 0),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(if (folioId == 0) 56.dp else (364.dp * animatedProgress + 56.dp))
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.8f))
                                        ),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                            )

                            Text(
                                text = if (folioId == 0) {
                                    "Disabled"
                                } else if (isDndActive) {
                                    if (targetProgress == 0f) "Releasing..." else "Hold D-pad Left"
                                } else {
                                    if (targetProgress == 1f) "Activating..." else "Hold D-pad Right"
                                },
                                color = Color.White.copy(alpha = 0.35f),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            val thumbOffset = with(androidx.compose.ui.platform.LocalDensity.current) {
                                (364.dp * animatedProgress).toPx()
                            }

                            Box(
                                modifier = Modifier
                                    .graphicsLayer { translationX = thumbOffset }
                                    .padding(4.dp)
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )

                            Box(
                                modifier = Modifier
                                    .align(if (isDndActive) Alignment.CenterStart else Alignment.CenterEnd)
                                    .padding(horizontal = 16.dp)
                                    .size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = if (isDndActive) "Left Arrow" else "Right Arrow",
                                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = if (isDndActive) 180f else 0f },
                                    tint = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationButtonDialog(
    context: Context,
    showNotificationButtonDialog: Boolean,
    onDismiss: () -> Unit,
    folioId: Int // Receive folioId to take notification data
) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var selectedNotification by remember { mutableStateOf<Notification?>(null) }
    var globalDpadLeftLocked by remember { mutableStateOf(false) }
    
    // Animation control
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Function to animate out and then dismiss
    fun animateAndDismiss() {
        isVisible = false
        scope.launch {
            delay(300) // Wait for exit animation
            onDismiss()
        }
    }

    // Focus management
    val firstItemFocusRequester = remember { FocusRequester() }
    val closeButtonFocusRequester = remember { FocusRequester() }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    var hasFocusedInitialItem by remember { mutableStateOf(false) }

    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())) }

    // Enter animation, clock tick, & Focus
    LaunchedEffect(Unit) {
        isVisible = true
        while(true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            currentDate = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())
            delay(60000)
        }
    }

    LaunchedEffect(isVisible, notifications.isNotEmpty()) {
        if (isVisible) {
            if (notifications.isNotEmpty() && !hasFocusedInitialItem) {
                delay(350) // Wait for animation
                try {
                    firstItemFocusRequester.requestFocus()
                    hasFocusedInitialItem = true
                } catch (e: Exception) {
                    Log.e("NotificationDialog", "Failed to request focus on first notification", e)
                }
            } else if (notifications.isEmpty()) {
                delay(350) // Wait for animation
                try {
                    closeButtonFocusRequester.requestFocus()
                } catch (e: Exception) {
                    Log.e("NotificationDialog", "Failed to request focus on close button", e)
                }
            }
        }
    }

    // Fetch notification data dynamically
    DisposableEffect(folioId) {
        var fetchRef: com.google.firebase.database.DatabaseReference? = null
        var fetchListener: com.google.firebase.database.ValueEventListener? = null
        
        if (showNotificationButtonDialog) {
            val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val branchId = sharedPreferences.getString("branchId", null)
            val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            
            fetchRef = database.child("BRANCHES").child(branchId ?: "").child("NOTIFICATIONS").child(folioId.toString())
            
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val list = mutableListOf<Notification>()
                    snapshot.children.forEach { data ->
                        val notification = data.getValue(Notification::class.java)
                        notification?.let { list.add(it) }
                    }
                    notifications = list
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    notifications = emptyList()
                }
            }
            fetchListener = listener
            fetchRef.addValueEventListener(listener)
        }
        
        onDispose {
            if (fetchRef != null && fetchListener != null) {
                fetchRef.removeEventListener(fetchListener)
            }
        }
    }

    if (showNotificationButtonDialog) {
        Dialog(
            onDismissRequest = { animateAndDismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusProperties { canFocus = false } // Prevent D-pad from focusing the background
            ) {
                // Dim/scrim background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { animateAndDismiss() } // Close on background click
                )

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInHorizontally(
                        initialOffsetX = { -it }, // Slide in from Left
                        animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { -it }, // Slide out to Left
                        animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
                    ) + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(380.dp) // Side drawer width (exactly like cart!)
                            .padding(top = 16.dp, bottom = 16.dp, start = 16.dp) // Float off the edges (exactly like cart!)
                            .graphicsLayer(clip = false) // Mencegah pemotongan visual konten di luar drawer
                            .clickable(enabled = false) {}, // Intercept clicks
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF1E2026),
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 24.dp, bottom = 24.dp)
                                .graphicsLayer(clip = false) // Mencegah pemotongan visual konten di luar column
                                .focusGroup()
                        ) {
                            // Header (Title left, Close/Clear button right)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notifications",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                var isClearClicked by remember { mutableStateOf(false) }
                                var isCloseFocused by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier
                                        .focusRequester(closeButtonFocusRequester)
                                        .clip(CircleShape)
                                        .let {
                                            if (isClearClicked) {
                                                it.sizeIn(minWidth = 70.dp, minHeight = 36.dp)
                                            } else {
                                                it.size(36.dp)
                                            }
                                        }
                                        .onFocusChanged { isCloseFocused = it.isFocused }
                                        .background(
                                            color = if (isCloseFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.05f),
                                            shape = CircleShape
                                        )
                                        .clickable(
                                            onClick = {
                                                if (notifications.isNotEmpty() && !isClearClicked) {
                                                    isClearClicked = true
                                                } else {
                                                    if (notifications.isNotEmpty() && isClearClicked) {
                                                        deleteAllNotifications(context, folioId)
                                                    }
                                                    animateAndDismiss() // Close dialog
                                                }
                                            },
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        )
                                        .focusable(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isClearClicked) "Clear" else "\uF057", // FontAwesome X icon
                                        color = if (isCloseFocused) Color(0xFF071434) else Color.White.copy(alpha = 0.55f),
                                        style = if (isClearClicked) MaterialTheme.typography.labelLarge else TextStyle(fontSize = 18.sp),
                                        fontFamily = if (isClearClicked) FontFamily.Default else FontFamily(Font(R.font.icons)),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            val notificationListState = rememberLazyListState()
                            val snapBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = notificationListState)
                            var focusedIndex by remember { mutableIntStateOf(-1) }

                            LaunchedEffect(focusedIndex) {
                                if (focusedIndex >= 0) {
                                    notificationListState.animateScrollToItem(focusedIndex)
                                }
                            }

                            // Sort notifications by timestamp in descending order
                            val sortedNotifications = remember(notifications) { 
                                notifications.sortedByDescending { it.timestamp } 
                            }

                            LazyColumn(
                                state = notificationListState,
                                flingBehavior = snapBehavior,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .graphicsLayer { 
                                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                                        clip = false
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        // 1. Kabut Atas - Bawah
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                0.08f to Color.Black,
                                                0.92f to Color.Black,
                                                1f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                        // 2. Kabut Kiri (Statis di area padding 24.dp pertama)
                                        val fadeEndPx = 24.dp.toPx()
                                        val fadeEndRatio = if (size.width > 0) fadeEndPx / size.width else 0.08f
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                0f to Color.Transparent,
                                                fadeEndRatio to Color.Black
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    },
                                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(sortedNotifications) { index, notification ->
                                    val itemFocusRequester = focusRequesters.getOrPut(notification.id) { FocusRequester() }
                                    NotificationItem(
                                        notification = notification,
                                        deleteNotification = {
                                            val nextFocusIndex = if (focusedIndex > 0) {
                                                focusedIndex - 1
                                            } else if (sortedNotifications.size > 1) {
                                                0
                                            } else {
                                                -1
                                            }
                                            deleteNotification(context, notification, folioId)
                                            if (nextFocusIndex >= 0) {
                                                scope.launch {
                                                    delay(100)
                                                    if (nextFocusIndex == 0) {
                                                        firstItemFocusRequester.requestFocus()
                                                    } else {
                                                        val nextNotif = sortedNotifications.getOrNull(nextFocusIndex)
                                                        if (nextNotif != null) {
                                                            focusRequesters[nextNotif.id]?.requestFocus()
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        onNotificationClick = {
                                            selectedNotification = notification
                                        },
                                        onFocused = {
                                            focusedIndex = index
                                        },
                                        globalDpadLeftLocked = globalDpadLeftLocked,
                                        onDpadLeftLockedChange = { globalDpadLeftLocked = it },
                                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier.focusRequester(itemFocusRequester)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Hold Dpad Left to delete notification",
                                color = Color.White.copy(alpha = 0.4f),
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        
        if (selectedNotification != null) {
            NotificationDialog(
                context = context,
                notification = selectedNotification!!,
                onDismiss = { selectedNotification = null }
            )
        }
    }
}

@Composable
 fun NotificationItem(
    notification: Notification,
    deleteNotification: (Notification) -> Unit,
    onNotificationClick: (Notification) -> Unit,
    onFocused: () -> Unit,
    globalDpadLeftLocked: Boolean,
    onDpadLeftLockedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var formattedTimestamp by remember { mutableStateOf(getTimeAgo(notification.timestamp)) }
    var showDeleteButton by remember { mutableStateOf(false) }

    // Hold to delete UX states
    var deleteProgress by remember { mutableStateOf(0f) }
    var isHoldingDpadLeft by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    var isFocused by remember { mutableStateOf(false) }
    val focusPulseAlpha = remember { Animatable(0.4f) }

    // Animasi pergeseran offset card notifikasi
    val offsetX by animateDpAsState(
        targetValue = when {
            isDeleting -> (-1000).dp // Slide out to the left
            showDeleteButton -> (-120).dp
            else -> 0.dp
        },
        animationSpec = tween(
            durationMillis = if (isDeleting) 350 else 250,
            easing = FastOutSlowInEasing
        )
    )

    // Progress counter when holding D-pad Left
    LaunchedEffect(isHoldingDpadLeft) {
        if (isHoldingDpadLeft && !globalDpadLeftLocked) {
            val duration = 1200f // 1.2 seconds hold time
            val startTime = System.currentTimeMillis()
            while (deleteProgress < 1f && isHoldingDpadLeft) {
                val elapsed = System.currentTimeMillis() - startTime
                deleteProgress = (elapsed / duration).coerceIn(0f, 1f)
                if (deleteProgress >= 1f) {
                    isDeleting = true
                    onDpadLeftLockedChange(true) // Lock DPAD Left globally
                    isHoldingDpadLeft = false
                }
                delay(16) // ~60fps loop
            }
        } else if (!isHoldingDpadLeft) {
            deleteProgress = 0f
        }
    }

    // Trigger delete action after sliding off-screen
    LaunchedEffect(isDeleting) {
        if (isDeleting) {
            delay(350) // Wait for slide animation
            deleteNotification(notification)
            isDeleting = false
            showDeleteButton = false
            deleteProgress = 0f
        }
    }

    LaunchedEffect(notification.timestamp) {
        while (true) {
            delay(1000)
            formattedTimestamp = getTimeAgo(notification.timestamp)
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            focusPulseAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            focusPulseAlpha.snapTo(0.4f)
            isHoldingDpadLeft = false
            deleteProgress = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocused()
                } else {
                    showDeleteButton = false
                    isHoldingDpadLeft = false
                    deleteProgress = 0f
                }
            }
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) {
                    if (globalDpadLeftLocked) {
                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                            onDpadLeftLockedChange(false)
                        }
                        true
                    } else {
                        if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                            showDeleteButton = true
                            
                            // Only start holding state if this is the very first event (repeatCount == 0)
                            // when the item is already focused. If focus shifts while holding, the new item
                            // will receive events with repeatCount > 0, preventing accidental deletion.
                            if (event.nativeKeyEvent.repeatCount == 0 && !isDeleting) {
                                isHoldingDpadLeft = true
                            }
                            true
                        } else if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_UP) {
                            isHoldingDpadLeft = false
                            if (!isDeleting) {
                                showDeleteButton = false
                            }
                            true
                        } else {
                            false
                        }
                    }
                } else if (event.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT && showDeleteButton) {
                    if (event.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                        showDeleteButton = false
                        isHoldingDpadLeft = false
                        deleteProgress = 0f
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
            .clickable(
                onClick = {
                    if (showDeleteButton) {
                        if (!isDeleting) {
                            isDeleting = true
                            onDpadLeftLockedChange(true) // Lock DPAD Left globally
                            isHoldingDpadLeft = false
                        }
                    } else {
                        onNotificationClick(notification)
                    }
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .focusable(),
        contentAlignment = Alignment.CenterStart
    ) {
        // Tombol delete dengan background seperti card item di sebelah kanan (CenterEnd)
        androidx.compose.animation.AnimatedVisibility(
            visible = showDeleteButton,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(100.dp)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isDeleting) {
                            isDeleting = true
                            onDpadLeftLockedChange(true) // Lock DPAD Left globally
                            isHoldingDpadLeft = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Circle Progress Indicator Wrapper
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .drawBehind {
                            // Circle Background
                            drawCircle(
                                color = Color.White.copy(alpha = 0.1f),
                                radius = size.minDimension / 2
                            )
                            // Rounded Progress Border
                            if (deleteProgress > 0f) {
                                val strokeWidth = 3.dp.toPx()
                                drawArc(
                                    color = Color.White.copy(alpha = 0.6f),
                                    startAngle = -90f,
                                    sweepAngle = 360f * deleteProgress,
                                    useCenter = false,
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = rememberAsyncImagePainter(R.drawable.ic_trash),
                        contentDescription = "Delete Icon",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
        }

        // Card Notifikasi utama yang bergeser ke kiri (menggunakan offset, background, dan border)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = offsetX)
                .border(
                    width = 3.dp,
                    color = Color.White.copy(alpha = if (isFocused && !showDeleteButton) focusPulseAlpha.value else 0f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconRes = when (notification.type) {
                        "DND" -> R.drawable.ic_dnd
                        "ROOM_SERVICE" -> R.drawable.ic_room_service
                        "GUEST_REQUEST" -> R.drawable.ic_request_service
                        else -> R.drawable.ic_notifications
                    }

                    Icon(
                        painter = rememberAsyncImagePainter(iconRes),
                        contentDescription = "Notification Icon",
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize(),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTimestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationDialog(
    context: Context,
    notification: Notification,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var showOrderDetailDialog by remember { mutableStateOf(false) }
    var orderDetails by remember { mutableStateOf<Order?>(null) }

    var showRequestDetailDialog by remember { mutableStateOf(false) }
    var requestDetails by remember { mutableStateOf<Request?>(null) }

    val scope = rememberCoroutineScope()

    // Fetch order or request details based on notification type using a real-time listener
    DisposableEffect(notification) {
        isLoading = true
        val database = FirebaseDatabase.getInstance().reference
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val branchId = sharedPreferences.getString("branchId", null)

        var queryListener: ValueEventListener? = null
        var queryRef: com.google.firebase.database.Query? = null

        when (notification.type) {
            "ROOM_SERVICE" -> {
                val ordersRef = database.child("BRANCHES").child(branchId ?: "").child("ORDERS")
                val q = ordersRef.orderByChild("orderId").equalTo(notification.id)
                queryRef = q
                queryListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val order = snapshot.children.firstOrNull()?.getValue(Order::class.java)
                            if (orderDetails != null && orderDetails?.status != order?.status) {
                                // If status changes, animate close first, then open with new status details
                                showOrderDetailDialog = false
                                scope.launch {
                                    delay(300)
                                    orderDetails = order
                                    showOrderDetailDialog = true
                                }
                            } else {
                                orderDetails = order
                                isLoading = false
                                showOrderDetailDialog = true
                            }
                        } else {
                            orderDetails = null
                            isLoading = false
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        isLoading = false
                    }
                }
                q.addValueEventListener(queryListener)
            }
            "GUEST_REQUEST" -> {
                val requestsRef = database.child("BRANCHES").child(branchId ?: "").child("REQUEST")
                val q = requestsRef.orderByChild("requestId").equalTo(notification.id)
                queryRef = q
                queryListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val request = snapshot.children.firstOrNull()?.getValue(Request::class.java)
                            if (requestDetails != null && requestDetails?.status != request?.status) {
                                // If status changes, animate close first, then open with new status details
                                showRequestDetailDialog = false
                                scope.launch {
                                    delay(300)
                                    requestDetails = request
                                    showRequestDetailDialog = true
                                }
                            } else {
                                requestDetails = request
                                isLoading = false
                                showRequestDetailDialog = true
                            }
                        } else {
                            requestDetails = null
                            isLoading = false
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        isLoading = false
                    }
                }
                q.addValueEventListener(queryListener)
            }
            else -> {
                isLoading = false
            }
        }

        onDispose {
            queryListener?.let { queryRef?.removeEventListener(it) }
        }
    }

    // Show loading animation while fetching details
    if (isLoading) {
        LottieLoadingIndicator()
    } else {
        // Check for specific notification types and show appropriate dialogs
        if (notification.type == "ROOM_SERVICE" && orderDetails != null) {
            if (showOrderDetailDialog) {
                OrderDetailDialog(
                    order = orderDetails!!,
                    onDismiss = {
                        onDismiss()
                        showOrderDetailDialog = false
                    }
                )
            }
        } else if (notification.type == "GUEST_REQUEST" && requestDetails != null) {
            if (showRequestDetailDialog) {
                RequestDetailDialog(
                    request = requestDetails!!,
                    onDismiss = {
                        onDismiss()
                        showRequestDetailDialog = false
                    }
                )
            }
        } else {
            // Default dialog for other notification types
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    decorFitsSystemWindows = false
                )
            ) {
                var animateIn by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                LaunchedEffect(Unit) {
                    animateIn = true
                }
                val animatedAlpha by animateFloatAsState(
                    targetValue = if (animateIn) 1f else 0f,
                    animationSpec = tween(durationMillis = 300)
                )
                fun dismissWithAnimation() {
                    animateIn = false
                    scope.launch {
                        delay(300)
                        onDismiss()
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusProperties { canFocus = false },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { dismissWithAnimation() }
                    )

                    AnimatedVisibility(
                        visible = animateIn,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300)) + scaleIn(animationSpec = tween(durationMillis = 300)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300)) + scaleOut(animationSpec = tween(durationMillis = 300))
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.6f),
                            color = Color(0xFFF2F7FC),
                            contentColor = Color(0xFF071434),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                            ) {
                                // Handling Do Not Disturb and Dn'D Released notifications
                                if (notification.title == "Do Not Disturb") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DisplayGif(R.drawable.dnd)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        DndInformation(notification)
                                    }
                                }

                                if (notification.title == "Dn'D Released") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        DisplayGif(R.drawable.releasednd)
                                        ReleasedDndInformation(notification)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Press Back to close",
                                    color = Color(0xFF071434).copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DndInformation(notification: Notification) {
    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        Text(notification.message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(getTimeAgo(notification.timestamp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This status indicates that the guest does not wish to be disturbed. " +
                    "Hotel staff should refrain from contacting or visiting the room during this time.",
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Status ini menandakan bahwa tamu tidak ingin diganggu untuk sementara waktu. " +
                    "Staf hotel diharapkan tidak menghubungi atau mengunjungi kamar selama status ini aktif.",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun ReleasedDndInformation( notification: Notification ) {
    Column(
        modifier = Modifier
            .padding(8.dp)
    ) {
        Text(notification.message, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(getTimeAgo(notification.timestamp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This status indicates that the guest has released the 'Do Not Disturb' status. " +
                    "Hotel staff may now contact or visit the room as needed.",
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Status ini menandakan bahwa tamu telah membatalkan status 'Do Not Disturb'. " +
                    "Staf hotel dapat menghubungi atau mengunjungi kamar sesuai kebutuhan.",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun SmallServiceButton(
    iconRes: Int? = null,
    onClick: () -> Unit,
    buttonColor: Color = Color.White, // Force solid white default
    title: String? = null,
    onFocusAction: (() -> Unit)? = null,
    isActive: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusStateChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    useOriginalTint: Boolean = false,
    isHome3D: Boolean = false,
    showBackgroundWhenActive: Boolean = true,
    buttonText: String? = null
) {
    var isClicked by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val titleAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(300),
        label = "titleAlpha"
    )

    val widthModifier = if (buttonText != null) {
        Modifier.widthIn(min = 36.dp)
    } else {
        Modifier.width(36.dp)
    }

    Box(
        modifier = modifier.height(36.dp).then(widthModifier),
        contentAlignment = Alignment.Center
    ) {
        var boxModifier = if (buttonText != null) {
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(18.dp))
        } else {
            Modifier
                .matchParentSize()
                .clip(CircleShape)
        }

        if (focusRequester != null) {
            boxModifier = boxModifier.focusRequester(focusRequester)
        }

        val bgModifier = if (isHome3D) {
            Modifier
                .drawBehind {
                    // Outer Soft Dark Shadow (Bottom-Right)
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.18f),
                        radius = (this.size.height / 2) - 1.dp.toPx(),
                        center = Offset(this.size.width / 2 + 2.dp.toPx(), this.size.height / 2 + 2.dp.toPx())
                    )
                    // Outer Soft Light Highlight Shadow (Top-Left)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = (this.size.height / 2) - 1.dp.toPx(),
                        center = Offset(this.size.width / 2 - 2.dp.toPx(), this.size.height / 2 - 2.dp.toPx())
                    )
                }
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isFocused) {
                            listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0))
                        } else {
                            listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                        }
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .drawBehind {
                    // Inner Embossed Soft Bubble Highlight (Top-Left gloss)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                            center = Offset(this.size.height * 0.28f, this.size.height * 0.28f),
                            radius = this.size.height * 0.35f
                        )
                    )
                    // Bottom-Right Inner Shadow Edge
                    drawArc(
                        color = Color.Black.copy(alpha = 0.12f),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // Top-Left Inner Highlight Edge
                    drawArc(
                        color = Color.White.copy(alpha = 0.9f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
        } else {
            Modifier.background(
                color = if (isFocused) {
                    Color(0xFFCFDFED)
                } else if (isActive && showBackgroundWhenActive) {
                    Color.White.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(18.dp)
            )
        }

        Box(
            modifier = boxModifier
                .onFocusChanged { 
                    isFocused = it.isFocused 
                    onFocusStateChange?.invoke(it.isFocused)
                    if (it.isFocused && !isActive) {
                        onFocusAction?.invoke()
                    }
                }
                .then(bgModifier)
                .clickable(
                    onClick = {
                        onClick()
                        isClicked = !isClicked
                    },
                    indication = ripple(color = FooterRipple),
                    interactionSource = remember { MutableInteractionSource() }
                )
                .padding(horizontal = if (buttonText != null) 8.dp else 0.dp), // Add padding for text
            contentAlignment = Alignment.Center
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp), // Balanced icon size
                    tint = if (useOriginalTint) Color.Unspecified else (if (isFocused) Color(0xFF1C1D24) else buttonColor) // Google TV High-Contrast Dynamic Switch
                )
            } else if (buttonText != null) {
                Text(
                    text = buttonText,
                    color = if (isFocused) Color(0xFF1C1D24) else buttonColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (title != null) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 24.dp)
                    .alpha(titleAlpha)
                    .requiredWidth(160.dp),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun SmallServiceButtonWithBadge(
    iconRes: Int,
    badgeCount: Int,
    onClick: () -> Unit,
    title: String? = null,
    isActive: Boolean = false,
    focusRequester: FocusRequester? = null,
    onFocusStateChange: ((Boolean) -> Unit)? = null
) {
    var isClicked by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnimation"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnimation"
    )

    var isFocused by remember { mutableStateOf(false) }

    val titleAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(300),
        label = "titleAlpha"
    )

    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        // Clickable button
        var boxModifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
        if (focusRequester != null) {
            boxModifier = boxModifier.focusRequester(focusRequester)
        }
        Box(
            modifier = boxModifier
                .onFocusChanged { 
                    isFocused = it.isFocused 
                    onFocusStateChange?.invoke(it.isFocused)
                }
                .background(
                    color = if (isFocused) Color(0xFFCFDFED) else if (isActive) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                    shape = CircleShape
                )
                .clickable(
                    onClick = {
                        onClick()
                        isClicked = !isClicked
                    },
                    indication = ripple(color = FooterRipple),
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isFocused) Color(0xFF1C1D24) else Color.White // Google TV dynamic contrast!
            )
        }

        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(16.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .background(Color(0xFFE91E63), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(16.dp)
                    .background(Color(0xFFE91E63), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeCount.toString(),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
        
        if (title != null) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 24.dp)
                    .alpha(titleAlpha)
                    .requiredWidth(160.dp),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WifiQRCodeDialog(
    ssid: String,
    password: String,
    isWebLogin: Boolean = false,
    roomNumber: String = "",
    loading: Boolean,
    qrCodeBitmap: ImageBitmap?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var animateIn by remember { mutableStateOf(false) }
    val cardFocusRequester = remember { FocusRequester() }
    val closeButtonFocusRequester = remember { FocusRequester() }
    var isCloseFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    LaunchedEffect(animateIn) {
        if (animateIn) {
            delay(100)
            cardFocusRequester.requestFocus()
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    fun dismissWithAnimation() {
        animateIn = false
        scope.launch {
            delay(300)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { dismissWithAnimation() }
            )

            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(20.dp)
                        .graphicsLayer(clip = false)
                        .focusRequester(cardFocusRequester)
                        .focusable()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1E2026),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .focusGroup()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wi-Fi Connection",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .focusRequester(closeButtonFocusRequester)
                                    .clip(CircleShape)
                                    .size(36.dp)
                                    .onFocusChanged { isCloseFocused = it.isFocused }
                                    .background(
                                        color = if (isCloseFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.05f),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        onClick = { dismissWithAnimation() },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                                    .focusable(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "\uF057",
                                    color = if (isCloseFocused) Color(0xFF071434) else Color.White.copy(alpha = 0.55f),
                                    style = TextStyle(fontSize = 18.sp),
                                    fontFamily = FontFamily(Font(R.font.icons))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (loading || qrCodeBitmap == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        } else {
                            val qrCodeBitmap = qrCodeBitmap!!
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1.1f),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.Top
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(20.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .background(Color.White.copy(0.1f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_wifi_rounded),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(28.dp),
                                                        tint = Color.White
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text("Wifi", fontSize = 14.sp, color = Color.White.copy(alpha=0.6f))
                                                    Text(ssid, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            
                                            if (isWebLogin) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(54.dp)
                                                            .background(Color.White.copy(0.1f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.keyboard),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(28.dp),
                                                            tint = Color.White
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Column {
                                                        Text("Username", fontSize = 14.sp, color = Color.White.copy(alpha=0.6f))
                                                        Text(roomNumber, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .background(Color.White.copy(0.1f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.keyboard),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(28.dp),
                                                        tint = Color.White
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text("Password", fontSize = 14.sp, color = Color.White.copy(alpha=0.6f))
                                                    Text(password, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(24.dp))

                                        var linkSpeed by remember { mutableStateOf(0) }
                                        var speedResult by remember { mutableStateOf<Double?>(null) }
                                        var pingResult by remember { mutableStateOf<Int?>(null) }
                                        var isTesting by remember { mutableStateOf(true) }
                                        var testStatus by remember { mutableStateOf("") }

                                        LaunchedEffect(animateIn) {
                                            if (animateIn) {
                                                delay(400)
                                                try {
                                                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                                                    linkSpeed = wifiManager?.connectionInfo?.linkSpeed ?: 0
                                                    testStatus = "Testing ping..."
                                                    pingResult = SpeedTestManager.runPingTest()
                                                    testStatus = "Testing download..."
                                                    speedResult = SpeedTestManager.runDownloadTest()
                                                    testStatus = ""
                                                } catch (t: Throwable) {
                                                    t.printStackTrace()
                                                    testStatus = "Test failed"
                                                } finally {
                                                    isTesting = false
                                                }
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.6f)
                                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                if (isTesting) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(bottom = 12.dp)
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            color = Color.White,
                                                            strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("loading", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                } else {
                                                    val connectionStatus = when {
                                                        pingResult == null || speedResult == null -> "Connection issues detected"
                                                        pingResult!! <= 0 || speedResult!! <= 0 -> "Connection issues detected"
                                                        pingResult!! < 50 && speedResult!! > 20 -> "Connection excellent"
                                                        pingResult!! < 100 && speedResult!! > 10 -> "Connection good"
                                                        pingResult!! < 150 -> "Connection fair"
                                                        else -> "Connection slow"
                                                    }
                                                    val statusColor = when {
                                                        pingResult == null || speedResult == null -> Color.Red
                                                        pingResult!! <= 0 || speedResult!! <= 0 -> Color.Red
                                                        pingResult!! < 50 && speedResult!! > 20 -> Color.Green
                                                        pingResult!! < 100 && speedResult!! > 10 -> Color.Cyan
                                                        else -> Color.Yellow
                                                    }
                                                    Text(
                                                        text = connectionStatus,
                                                        fontSize = 16.sp,
                                                        color = statusColor,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(bottom = 12.dp)
                                                    )
                                                }
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Ping",
                                                        fontSize = 18.sp,
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        modifier = Modifier.width(120.dp)
                                                    )
                                                    val pingVal = pingResult ?: 0
                                                    Text("${pingVal}ms", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Download",
                                                        fontSize = 18.sp,
                                                        color = Color.White.copy(alpha = 0.8f),
                                                        modifier = Modifier.width(120.dp)
                                                    )
                                                    val speedVal = speedResult?.let { String.format(java.util.Locale.US, "%.1f", it).replace('.', ',') } ?: "0,0"
                                                    Text("${speedVal}Mbps", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(32.dp))

                                    Image(
                                        bitmap = qrCodeBitmap, 
                                        contentDescription = "Wi-Fi QR Code",
                                        modifier = Modifier
                                            .size(320.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
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
fun WaQRCodeDialog(
    phone: String,
    ext: String,
    telephone: String,
    address: String,
    loading: Boolean,
    qrCodeBitmap: ImageBitmap?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var animateIn by remember { mutableStateOf(false) }
    val cardFocusRequester = remember { FocusRequester() }
    val closeButtonFocusRequester = remember { FocusRequester() }
    var isCloseFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    LaunchedEffect(animateIn) {
        if (animateIn) {
            delay(100)
            cardFocusRequester.requestFocus()
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    fun dismissWithAnimation() {
        animateIn = false
        scope.launch {
            delay(300)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { dismissWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { dismissWithAnimation() }
            )

            AnimatedVisibility(
                visible = animateIn,
                enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(20.dp)
                        .graphicsLayer(clip = false)
                        .focusRequester(cardFocusRequester)
                        .focusable()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF1E2026),
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .focusGroup()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WhatsApp Receptionist",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .focusRequester(closeButtonFocusRequester)
                                    .clip(CircleShape)
                                    .size(36.dp)
                                    .onFocusChanged { isCloseFocused = it.isFocused }
                                    .background(
                                        color = if (isCloseFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.05f),
                                        shape = CircleShape
                                    )
                                    .clickable(
                                        onClick = { dismissWithAnimation() },
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                                    .focusable(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "\uF057",
                                    color = if (isCloseFocused) Color(0xFF071434) else Color.White.copy(alpha = 0.55f),
                                    style = TextStyle(fontSize = 18.sp),
                                    fontFamily = FontFamily(Font(R.font.icons))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (loading || qrCodeBitmap == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color.White)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Column(
                                    modifier = Modifier.weight(1.1f),
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .background(Color.White.copy(0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_whatsapp),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(28.dp),
                                                    tint = Color.Green
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text("WhatsApp", fontSize = 14.sp, color = Color.White.copy(alpha=0.6f))
                                                Text(phone, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .background(Color.White.copy(0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_phone),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(28.dp),
                                                    tint = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text("Telephone", fontSize = 14.sp, color = Color.White.copy(alpha=0.6f))
                                                Text(telephone, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .background(Color.White.copy(0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "Ext",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text("Extension", fontSize = 14.sp, color = Color.White.copy(alpha=0.6f))
                                                Text(ext, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(32.dp))

                                qrCodeBitmap?.let {
                                    Image(
                                        bitmap = it, 
                                        contentDescription = "Wa QR Code",
                                        modifier = Modifier
                                            .size(320.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = address,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha=0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// Function to listen for guest info updates based on deviceID
fun listenForUpdates(
    database: DatabaseReference,
    deviceID: String?,
    onGuestInfoChange: (GuestInfo?) -> Unit
) {
    deviceID?.let {
        Log.d("FooterSection", "Setting up listener for deviceID: $it")
        
        // First get the room number from DEVICES node
        val deviceRef = database.child("DEVICES").child(it)
        Log.d("FooterSection", "Looking up device info at: $deviceRef")
        
        deviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(deviceSnapshot: DataSnapshot) {
                if (deviceSnapshot.exists()) {
                    val roomNumber = deviceSnapshot.child("room").getValue(String::class.java)
                    val branchId = deviceSnapshot.child("branchId").getValue(String::class.java)
                    Log.d("FooterSection", "Found room number: $roomNumber, branchId: $branchId for device: $it")
                    
                    if (roomNumber != null && branchId != null) {
                        // Look up guest info through BRANCHES node
                        val guestRef = database.child("BRANCHES")
                            .child(branchId)
                            .child("FOGUEST")
                            .child(roomNumber)
                        Log.d("FooterSection", "Looking up guest info at: $guestRef")
                        
                        guestRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(guestSnapshot: DataSnapshot) {
                                Log.d("FooterSection", "Guest data snapshot exists: ${guestSnapshot.exists()}")
                                if (guestSnapshot.exists()) {
                                    val guestInfo = guestSnapshot.getValue(GuestInfo::class.java)
                                    Log.d("FooterSection", "Found guest info: ${guestInfo?.folio} for room: $roomNumber")
                                    onGuestInfoChange(guestInfo)
                                } else {
                                    Log.d("FooterSection", "No guest data found for room: $roomNumber")
                                    onGuestInfoChange(null)
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Log.e("FooterSection", "Error retrieving guest data: ${databaseError.message}")
                                onGuestInfoChange(null)
                            }
                        })
                    } else {
                        Log.e("FooterSection", "No room number or branchId found for device: $it")
                        onGuestInfoChange(null)
                    }
                } else {
                    Log.e("FooterSection", "No device info found for: $it")
                    onGuestInfoChange(null)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FooterSection", "Error retrieving device data: ${databaseError.message}")
                onGuestInfoChange(null)
            }
        })
    } ?: run {
        Log.e("FooterSection", "deviceID is null")
        onGuestInfoChange(null)
    }
}

// Function to get notification count based on folioId (now Int)
private fun getNotificationCount(context: Context, folioId: Int, onCountChange: (Int) -> Unit) {
    val database = Firebase.database.reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    val notificationsRef = database.child("BRANCHES").child(branchId ?: "").child("NOTIFICATIONS").child(folioId.toString())

    notificationsRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Count notifications in folioId
            val count = snapshot.childrenCount.toInt()
            onCountChange(count)
        }

        override fun onCancelled(error: DatabaseError) {
            // Handle errors, such as Firebase issues
            onCountChange(0) // Set count to 0 in case of error
        }
    })
}

private fun getNotifications(context: Context, folioId: Int, onNotificationsReceived: (List<Notification>) -> Unit) {
    val database = Firebase.database.reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    val notificationsRef = database.child("BRANCHES").child(branchId ?: "").child("NOTIFICATIONS").child(folioId.toString())

    notificationsRef.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val notificationList = mutableListOf<Notification>()
            snapshot.children.forEach { data ->
                val notification = data.getValue(Notification::class.java)
                notification?.let { notificationList.add(it) }
            }
            onNotificationsReceived(notificationList) // resend notif
        }

        override fun onCancelled(error: DatabaseError) {
            onNotificationsReceived(emptyList()) // send empty if error
        }
    })
}

//fun getPinFromFirebase(onPinRetrieved: (String?) -> Unit) {
//    val database = Firebase.database.reference
//    database.child("SETTING/PIN").get().addOnSuccessListener { snapshot ->
//        val pin = snapshot.getValue(String::class.java)
//        onPinRetrieved(pin)
//    }.addOnFailureListener {
//        onPinRetrieved(null)
//    }
//}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val activeNetwork = connectivityManager.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnected
}

fun getPin(context: Context, onPinRetrieved: (String?) -> Unit) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val cachedPin = sharedPreferences.getString("cached_pin", null)
    val branchId = sharedPreferences.getString("branchId", null)
    
    if (branchId == null) {
        onPinRetrieved(cachedPin ?: "4646")
        return
    }

    // CEK KONEKSI INTERNET
    if (!isNetworkAvailable(context)) {
        onPinRetrieved(cachedPin ?: "4646")
        return
    }
    
    // Get PIN from Firebase with realtime updates
    val database = com.google.firebase.database.FirebaseDatabase.getInstance().reference
    val pinRef = database.child("BRANCHES").child(branchId).child("SETTING/PIN")
    
    pinRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
            val pin = snapshot.getValue(String::class.java)
            if (pin != null) {
                sharedPreferences.edit { putString("cached_pin", pin) }
                onPinRetrieved(pin)
            } else {
                onPinRetrieved(cachedPin ?: "4646")
            }
        }

        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
            onPinRetrieved(cachedPin ?: "4646")
        }
    })
}

fun setDndStatusInFirebase(context: Context, folioId: Int, isActive: Boolean) {
    val database = Firebase.database.reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    
    Log.d("FooterSection", "Setting DND status to $isActive for folioId: $folioId, branchId: $branchId")
    
    val dndRef = database.child("BRANCHES").child(branchId ?: "").child("DND_STATUS").child(folioId.toString())
    dndRef.setValue(isActive)
        .addOnSuccessListener {
            Log.d("FooterSection", "Successfully set DND status to $isActive")
        }
        .addOnFailureListener { e ->
            Log.e("FooterSection", "Failed to set DND status: ${e.message}")
        }
}

fun sendDndNotification(context: Context, folioId: Int, release: Boolean, deviceID: String?) {
    Log.d("FooterSection", "sendDndNotification called with folioId: $folioId, release: $release, deviceID: $deviceID")
    
    if (deviceID == null) {
        Log.e("sendDndNotification", "DeviceID is null in SharedPreferences")
        return
    }

    val database = Firebase.database.reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    Log.d("FooterSection", "BranchId from SharedPreferences: $branchId")
    
    // Pertama, cari nomor kamar dari deviceID
    val deviceRef = database.child("DEVICES").child(deviceID)
    Log.d("FooterSection", "Looking up device info at: $deviceRef")
    
    deviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(deviceSnapshot: DataSnapshot) {
            if (deviceSnapshot.exists()) {
                val roomNumber = deviceSnapshot.child("room").getValue(String::class.java)
                Log.d("FooterSection", "Found room number: $roomNumber")
                
                if (roomNumber != null) {
                    // Sekarang cari data tamu menggunakan nomor kamar
                    val guestRef = database.child("BRANCHES").child(branchId ?: "").child("FOGUEST").child(roomNumber)
                    Log.d("FooterSection", "Looking up guest info at: $guestRef")
                    
                    guestRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            Log.d("FooterSection", "Guest data snapshot exists: ${snapshot.exists()}")
                            if (snapshot.exists()) {
                                val guest = snapshot.getValue(GuestInfo::class.java)
                                Log.d("FooterSection", "Guest data retrieved: $guest")

                                if (guest != null) {
                                    // Escape HTML special characters
                                    fun escapeHtml(text: String): String {
                                        return text
                                            .replace("&", "&amp;")
                                            .replace("<", "&lt;")
                                            .replace(">", "&gt;")
                                    }
                                    
                                    val escapedRoom = escapeHtml(guest.room)
                                    val escapedFname = escapeHtml(guest.fname)
                                    val escapedEmail = escapeHtml(guest.email.ifEmpty { "-" })
                                    val escapedPhone = escapeHtml(guest.phone.ifEmpty { "-" })
                                    
                                    val message = if (release) {
                                        """
<b>DND Status Dibatalkan</b>

Kamar <b>$escapedRoom</b> tidak lagi dalam status 'Do Not Disturb'.

Status ini menandakan bahwa tamu telah membatalkan status 'Do Not Disturb'. Staf hotel dapat menghubungi atau mengunjungi kamar sesuai kebutuhan.

<i>This status indicates that the guest has released the 'Do Not Disturb' status. Hotel staff may now contact or visit the room as needed.</i>

<b>Detail Tamu</b>
<b>Nama:</b> $escapedFname
<b>Email:</b> $escapedEmail
<b>Telepon:</b> $escapedPhone

<b>Check-in:</b> ${guest.dateci}
<b>Check-out:</b> ${guest.dateco}
                                        """.trimIndent()
                                    } else {
                                        """
<b>DND Status Aktif</b>

Kamar <b>$escapedRoom</b> sekarang dalam status 'Do Not Disturb'.

Status ini menandakan bahwa tamu tidak ingin diganggu untuk sementara waktu. Staf hotel diharapkan tidak menghubungi atau mengunjungi kamar selama status ini aktif.

<i>This status indicates that the guest does not wish to be disturbed. Hotel staff should refrain from contacting or visiting the room during this time.</i>

<b>Detail Tamu</b>
<b>Nama:</b> $escapedFname
<b>Email:</b> $escapedEmail
<b>Telepon:</b> $escapedPhone

<b>Check-in:</b> ${guest.dateci}
<b>Check-out:</b> ${guest.dateco}
                                        """.trimIndent()
                                    }

                                    // Trigger FCM Notification
                                    val dndTitle = if (release) "DND Status Dibatalkan" else "DND Status Aktif"
                                    val dndBody = "Kamar $escapedRoom " + (if (release) "tidak lagi dalam status 'Do Not Disturb'." else "sekarang dalam status 'Do Not Disturb'.")
                                    FcmHelper.sendFcmNotification(
                                        context = context,
                                        type = "DND",
                                        title = dndTitle,
                                        bodyText = dndBody,
                                        additionalData = mapOf(
                                            "room" to escapedRoom,
                                            "guestName" to escapedFname,
                                            "release" to release.toString()
                                        )
                                    )
                                } else {
                                    Log.e("sendFolioDetails", "Data guest tidak ditemukan")
                                }
                            } else {
                                Log.e("sendFolioDetails", "FolioId tidak ditemukan di Firebase")
                            }

                            val notification = Notification(
                                id = "notification_id_${System.currentTimeMillis()}",
                                title = if (release) "Dn'D Released" else "Do Not Disturb",
                                message = if (release) "Your room is no longer set to 'Do Not Disturb'." else "Your room is now set to 'Do Not Disturb'.",
                                timestamp = System.currentTimeMillis(),
                                type = "DND"
                            )

                            val notificationsRef = database.child("BRANCHES").child(branchId ?: "").child("NOTIFICATIONS").child(folioId.toString())
                            notificationsRef.push().setValue(notification)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("sendFolioDetails", "Failed to retrieve guest data", error.toException())
                        }
                    })
                } else {
                    Log.e("FooterSection", "Room number not found for device: $deviceID")
                }
            } else {
                Log.e("FooterSection", "Device info not found for: $deviceID")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("FooterSection", "Failed to retrieve device data", error.toException())
        }
    })
}


fun updateNotificationStatus(context: Context, notification: Notification, folioId: Int, newStatus: String) {
    val database = Firebase.database.reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    val notificationsRef = database.child("BRANCHES").child(branchId ?: "").child("NOTIFICATIONS").child(folioId.toString())

    notificationsRef.orderByChild("id").equalTo(notification.id).get().addOnSuccessListener { snapshot ->
        val updates = hashMapOf<String, Any>()
        snapshot.children.forEach { snapshotChild ->
            val key = snapshotChild.key
            if (key != null) {
                updates["$key/status"] = newStatus
            }
        }
        if (updates.isNotEmpty()) {
            notificationsRef.updateChildren(updates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("Firebase", "Notification status updated to $newStatus")
                    } else {
                        Log.e("Firebase", "Failed to update notification status: ${task.exception?.message}")
                    }
                }
        }
    }
}

fun deleteNotification(context: Context, notification: Notification, folioId: Int) {
    val database = FirebaseDatabase.getInstance().reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    val notificationsRef = database.child("BRANCHES").child(branchId ?: "").child("NOTIFICATIONS").child(folioId.toString())

    notificationsRef.orderByChild("id").equalTo(notification.id).get().addOnSuccessListener { snapshot ->
        val updates = hashMapOf<String, Any?>()
        snapshot.children.forEach { snapshotChild ->
            val key = snapshotChild.key
            if (key != null) {
                updates[key] = null
            }
        }
        if (updates.isNotEmpty()) {
            notificationsRef.updateChildren(updates)
        }
    }
}

fun deleteAllNotifications(context: Context, folioId: Int) {
    val database = FirebaseDatabase.getInstance().reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    val notificationsRef = database.child("BRANCHES").child(branchId ?: "").child("NOTIFICATIONS").child(folioId.toString())

    notificationsRef.removeValue().addOnSuccessListener {
        // Handle success if necessary
    }.addOnFailureListener {
        // Handle failure if necessary
    }
}

fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffInSeconds = (now - timestamp) / 1000

    return when {
        diffInSeconds < 60 -> "Just Now"
        diffInSeconds < 3600 -> "${diffInSeconds / 60} minutes ago"
        diffInSeconds < 86400 -> "${diffInSeconds / 3600} hours ago"
        diffInSeconds < 2592000 -> "${diffInSeconds / 86400} days ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(Date(timestamp)) // Convert Long to Date
        }
    }
}

fun generateWifiQRCode(ssid: String, password: String, isWebLogin: Boolean = false): ImageBitmap {
    val wifiUrl = if (isWebLogin || password.trim().isEmpty()) {
        "WIFI:T:nopass;S:$ssid;;"
    } else {
        "WIFI:T:WPA;S:$ssid;P:$password;;"
    }
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(wifiUrl, BarcodeFormat.QR_CODE, 512, 512)

    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (bitMatrix.get(x, y)) Color.Black.toArgb() else Color.White.toArgb()
        }
    }

    val bitmap = createBitmap(width, height)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    return bitmap.asImageBitmap()
}

fun generateWaQRCode(phone: String, message: String, room: String): ImageBitmap {
    val waUrl = "https://wa.me/$phone?text=$message Room $room"

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(waUrl, BarcodeFormat.QR_CODE, 512, 512)

    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (bitMatrix.get(x, y)) Color.Black.toArgb() else Color.White.toArgb()
        }
    }

    val bitmap = createBitmap(width, height)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    return bitmap.asImageBitmap()
}

@Composable
fun CartDrawer(
    onDismiss: () -> Unit,
    context: Context
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val focusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    var lastFocusedControl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }

    fun closeWithAnimation() {
        scope.launch {
            isVisible = false
            delay(300)
            onDismiss()
        }
    }

    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())) }

    LaunchedEffect(Unit) {
        isVisible = true
        while(true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            currentDate = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())
            delay(60000)
        }
    }

    val cartPreferences = remember { CartPreferences(context) }
    val selectedItems = GlobalCartState.selectedItems

    val showConfirmationDialog = remember { mutableStateOf(false) }
    val dialogMessage = remember { mutableStateOf("") }
    val selectedPaymentMethod = remember { mutableStateOf("Cash") }

    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val roomId = sharedPreferences.getString("room", null)
    val branchId = sharedPreferences.getString("branchId", null)
    var guestInfo by remember { mutableStateOf<GuestInfo?>(null) }
    var folioId by remember { mutableStateOf<Int?>(null) }
    var guestRoom by remember { mutableStateOf<String?>(null) }
    var guestName by remember { mutableStateOf<String?>(null) }
    var guestPhone by remember { mutableStateOf<String?>(null) }

    val database = Firebase.database.reference

    DisposableEffect(roomId, branchId) {
        var activeRef: com.google.firebase.database.DatabaseReference? = null
        var activeListener: com.google.firebase.database.ValueEventListener? = null

        if (roomId != null && branchId != null) {
            activeRef = database.child("BRANCHES").child(branchId).child("FOGUEST").child(roomId)
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val info = snapshot.getValue(GuestInfo::class.java)
                        guestInfo = info
                        folioId = info?.folio
                        guestRoom = info?.room
                        guestName = info?.fname
                        guestPhone = info?.phone
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            activeListener = listener
            activeRef.addValueEventListener(listener)
        }
        onDispose {
            if (activeRef != null && activeListener != null) {
                activeRef.removeEventListener(activeListener)
            }
        }
    }

    val subTotalPrice = selectedItems.sumOf {
        (it.item.price + (it.selectedVariant?.price ?: 0)) * it.quantity
    }
    val taxPrice = selectedItems.sumOf {
        val itemPriceWithVariant = it.item.price + (it.selectedVariant?.price ?: 0)
        itemPriceWithVariant * it.quantity * it.item.tax / 100
    }
    val totalPrice = subTotalPrice + taxPrice

    Dialog(
        onDismissRequest = { closeWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false },
            contentAlignment = Alignment.CenterEnd
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { closeWithAnimation() }
            )

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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .focusGroup()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = currentDate,
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = currentTime,
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Cart",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LaunchedEffect(Unit) {
                            delay(450)
                            if (selectedItems.isNotEmpty()) {
                                try {
                                    firstItemFocusRequester.requestFocus()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        LaunchedEffect(selectedItems.size, selectedItems.map { it.quantity }) {
                            if (selectedItems.isNotEmpty()) {
                                val key = lastFocusedControl
                                if (key != null && focusRequesters.containsKey(key)) {
                                    try {
                                        focusRequesters[key]?.requestFocus()
                                    } catch (e: Exception) {
                                        try {
                                            firstItemFocusRequester.requestFocus()
                                        } catch (e2: Exception) {
                                            e2.printStackTrace()
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.empty_cart))
                                    val progress by animateLottieCompositionAsState(
                                        composition = composition,
                                        iterations = LottieConstants.IterateForever
                                    )
                                    LottieAnimation(
                                        composition = composition,
                                        progress = { progress },
                                        modifier = Modifier.size(180.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Cart empty",
                                        color = Color.White.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                0.08f to Color.Black,
                                                0.92f to Color.Black,
                                                1f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    },
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                items(
                                    count = selectedItems.size,
                                    key = { index -> 
                                        val item = selectedItems[index]
                                        "${item.item.name}_${item.selectedVariant?.name ?: "default"}_${item.specialInstruction}"
                                    }
                                ) { index ->
                                    val selectedItem = selectedItems[index]
                                    val itemPrice = (selectedItem.item.price + (selectedItem.selectedVariant?.price ?: 0)) * selectedItem.quantity

                                    var isMinusFocused by remember { mutableStateOf(false) }
                                    var isPlusFocused by remember { mutableStateOf(false) }

                                    val isAnyFocused = isMinusFocused || isPlusFocused
                                    val pulseAlpha = remember { Animatable(0.0f) }
                                    LaunchedEffect(isAnyFocused) {
                                        if (isAnyFocused) {
                                            pulseAlpha.animateTo(
                                                targetValue = 1.0f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                )
                                            )
                                        } else {
                                            pulseAlpha.snapTo(0.0f)
                                        }
                                    }

                                    val minusFocusFade by animateFloatAsState(
                                        targetValue = if (isMinusFocused) 1.0f else 0.0f,
                                        animationSpec = tween(durationMillis = 350),
                                        label = "MinusFocusFade"
                                    )

                                    val plusFocusFade by animateFloatAsState(
                                        targetValue = if (isPlusFocused) 1.0f else 0.0f,
                                        animationSpec = tween(durationMillis = 350),
                                        label = "PlusFocusFade"
                                    )

                                    val minusBorderModifier = Modifier
                                    val plusBorderModifier = Modifier

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White.copy(alpha = 0.05f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(50.dp)) {
                                                CachedAsyncImage(
                                                    imageUrl = selectedItem.item.imageRes,
                                                    contentDescription = selectedItem.item.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    error = R.drawable.err,
                                                    cachePrefix = "food"
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = selectedItem.item.name,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (selectedItem.selectedVariant != null) {
                                                    Text(
                                                        text = "Variant: ${selectedItem.selectedVariant.name}",
                                                        color = Color.White.copy(alpha = 0.6f),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                                Text(
                                                    text = formatIDR(itemPrice),
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Qty adjustment row
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                val minusKey = "minus_${selectedItem.item.name}_${selectedItem.selectedVariant?.name ?: "default"}_${selectedItem.specialInstruction}"
                                                val minusFocusRequester = focusRequesters.getOrPut(minusKey) { FocusRequester() }

                                                // Minus or Trash Button
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .then(minusBorderModifier)
                                                        .padding(3.5.dp)
                                                        .clip(CircleShape)
                                                        .onFocusChanged { 
                                                            isMinusFocused = it.isFocused 
                                                            if (it.isFocused) {
                                                                lastFocusedControl = minusKey
                                                            }
                                                        }
                                                        .focusRequester(minusFocusRequester)
                                                        .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                                                        .focusable()
                                                        .background(if (isMinusFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.1f))
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            if (selectedItem.quantity > 1) {
                                                                val idx = selectedItems.indexOf(selectedItem)
                                                                if (idx != -1) {
                                                                    selectedItems[idx] = selectedItem.copy(quantity = selectedItem.quantity - 1)
                                                                    cartPreferences.saveCart(selectedItems)
                                                                }
                                                            } else {
                                                                val deletedIdx = selectedItems.indexOf(selectedItem)
                                                                selectedItems.remove(selectedItem)
                                                                cartPreferences.saveCart(selectedItems)
                                                                
                                                                if (selectedItems.isNotEmpty()) {
                                                                    val targetIdx = (deletedIdx - 1).coerceAtLeast(0)
                                                                    val targetItem = selectedItems[targetIdx]
                                                                    val targetKey = "minus_${targetItem.item.name}_${targetItem.selectedVariant?.name ?: "default"}_${targetItem.specialInstruction}"
                                                                    lastFocusedControl = targetKey
                                                                    scope.launch {
                                                                        delay(100)
                                                                        try {
                                                                            focusRequesters[targetKey]?.requestFocus()
                                                                        } catch(e: Exception) {
                                                                            e.printStackTrace()
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (selectedItem.quantity == 1) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = if (isMinusFocused) Color(0xFF071434) else Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "-",
                                                            color = if (isMinusFocused) Color(0xFF071434) else Color.White,
                                                            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                                            modifier = Modifier.offset(y = (-1).dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Text(
                                                    text = "${selectedItem.quantity}",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Spacer(modifier = Modifier.width(8.dp))

                                                val plusKey = "plus_${selectedItem.item.name}_${selectedItem.selectedVariant?.name ?: "default"}_${selectedItem.specialInstruction}"
                                                val plusFocusRequester = focusRequesters.getOrPut(plusKey) { FocusRequester() }

                                                // Plus Button
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .then(plusBorderModifier)
                                                        .padding(3.5.dp)
                                                        .clip(CircleShape)
                                                        .onFocusChanged { 
                                                            isPlusFocused = it.isFocused 
                                                            if (it.isFocused) {
                                                                lastFocusedControl = plusKey
                                                            }
                                                        }
                                                        .focusRequester(plusFocusRequester)
                                                        .focusable()
                                                        .background(if (isPlusFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.1f))
                                                        .clickable(
                                                            interactionSource = remember { MutableInteractionSource() },
                                                            indication = null
                                                        ) {
                                                            val idx = selectedItems.indexOf(selectedItem)
                                                            if (idx != -1) {
                                                                    selectedItems[idx] = selectedItem.copy(quantity = selectedItem.quantity + 1)
                                                                    cartPreferences.saveCart(selectedItems)
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "+",
                                                        color = if (isPlusFocused) Color(0xFF071434) else Color.White,
                                                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                                        modifier = Modifier.offset(y = (-1).dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Total Calculations
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("Subtotal", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(formatIDR(subTotalPrice), color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("Tax & Service", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    Text(formatIDR(taxPrice), color = Color.White, style = MaterialTheme.typography.bodySmall)
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
                                 Row(modifier = Modifier.fillMaxWidth()) {
                                    Text("Total", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(formatIDR(totalPrice), color = Color(0xFFE91E63), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Checkout Button
                            var isCheckoutFocused by remember { mutableStateOf(false) }

                            val checkoutPulseAlpha = remember { Animatable(0.0f) }
                            LaunchedEffect(isCheckoutFocused) {
                                if (isCheckoutFocused) {
                                    checkoutPulseAlpha.animateTo(
                                        targetValue = 1.0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        )
                                    )
                                } else {
                                    checkoutPulseAlpha.snapTo(0.0f)
                                }
                            }

                            val checkoutFocusFade by animateFloatAsState(
                                targetValue = if (isCheckoutFocused) 1.0f else 0.0f,
                                animationSpec = tween(durationMillis = 350),
                                label = "CheckoutFocusFade"
                            )

                            val checkoutBorderModifier = Modifier

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .onFocusChanged { isCheckoutFocused = it.isFocused }
                                    .focusable()
                                    .background(if (isCheckoutFocused) Color(0xFFCFDFED) else Color(0xFF555555))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        dialogMessage.value = "\uF19F Please select payment method to proceed the order"
                                        showConfirmationDialog.value = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Checkout",
                                    color = if (isCheckoutFocused) Color(0xFF071434) else Color.White.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmationDialog.value) {
        var animateIn by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val cashFocusRequester = remember { FocusRequester() }
        val cardFocusRequester = remember { FocusRequester() }
        val confirmFocusRequester = remember { FocusRequester() }
        val cancelFocusRequester = remember { FocusRequester() }
        
        var isCashFocused by remember { mutableStateOf(false) }
        var isCardFocused by remember { mutableStateOf(false) }
        var isConfirmFocused by remember { mutableStateOf(false) }
        var isCancelFocused by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            animateIn = true
            delay(100)
            cashFocusRequester.requestFocus()
        }

        val animatedAlpha by animateFloatAsState(
            targetValue = if (animateIn) 1f else 0f,
            animationSpec = tween(durationMillis = 300)
        )

        fun dismissWithAnimation() {
            animateIn = false
            scope.launch {
                delay(300)
                showConfirmationDialog.value = false
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "borderPulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Dialog(
            onDismissRequest = { dismissWithAnimation() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusProperties { canFocus = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { dismissWithAnimation() }
                )

                AnimatedVisibility(
                    visible = animateIn,
                    enter = fadeIn(animationSpec = tween(durationMillis = 300)) + scaleIn(animationSpec = tween(durationMillis = 300)),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300)) + scaleOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Surface(
                        modifier = Modifier
                            .width(500.dp)
                            .wrapContentHeight()
                            .padding(24.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF1E2026),
                        tonalElevation = 8.dp,
                        shadowElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .focusGroup()
                        ) {
                            Text(
                                text = "Order Confirmation",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Payment Method",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                             Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .focusRequester(cashFocusRequester)
                                        .onFocusChanged { isCashFocused = it.isFocused }
                                        .clickable { selectedPaymentMethod.value = "Cash" }
                                        .focusable(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isCashSelected = selectedPaymentMethod.value == "Cash"
                                    val cashRadioBgColor = if (isCashFocused) {
                                        Color(0xFFCFDFED)
                                    } else if (isCashSelected) {
                                        Color(0xFFCFDFED).copy(alpha = 0.2f)
                                    } else {
                                        Color.White.copy(alpha = 0.05f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(cashRadioBgColor)
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCashSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCashFocused) Color(0xFF071434) else Color(0xFFCFDFED))
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Cash",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .focusRequester(cardFocusRequester)
                                        .onFocusChanged { isCardFocused = it.isFocused }
                                        .clickable { selectedPaymentMethod.value = "Debit/Credit Card" }
                                        .focusable(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isCardSelected = selectedPaymentMethod.value == "Debit/Credit Card"
                                    val cardRadioBgColor = if (isCardFocused) {
                                        Color(0xFFCFDFED)
                                    } else if (isCardSelected) {
                                        Color(0xFFCFDFED).copy(alpha = 0.2f)
                                    } else {
                                        Color.White.copy(alpha = 0.05f)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(cardRadioBgColor)
                                            .padding(3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCardSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCardFocused) Color(0xFF071434) else Color(0xFFCFDFED))
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Debit/Credit Card",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .focusRequester(cancelFocusRequester)
                                        .onFocusChanged { isCancelFocused = it.isFocused }
                                        .clip(CircleShape)
                                        .background(
                                            if (isCancelFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.05f)
                                        )
                                        .clickable {
                                            dismissWithAnimation()
                                        }
                                        .focusable()
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Cancel",
                                        color = if (isCancelFocused) Color(0xFF1C1D24) else Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                val isConfirmEnabled = folioId != null && folioId != 0
                                Box(
                                    modifier = Modifier
                                        .focusRequester(confirmFocusRequester)
                                        .onFocusChanged { isConfirmFocused = it.isFocused }
                                        .clip(CircleShape)
                                        .background(
                                            if (!isConfirmEnabled) {
                                                Color.White.copy(alpha = 0.02f)
                                            } else if (isConfirmFocused) {
                                                Color(0xFFCFDFED)
                                            } else {
                                                Color.White.copy(alpha = 0.05f)
                                            }
                                        )
                                        .clickable(enabled = isConfirmEnabled) {
                                            if (folioId != null) {
                                                val orderId = generateOrderId()
                                                sendOrderNotification(context, folioId!!, selectedPaymentMethod.value, orderId, selectedItems)
                                                sendOrderToDatabase(context, folioId!!, guestName ?: "", guestPhone ?: "", guestRoom ?: "", selectedPaymentMethod.value, selectedItems, "placed", orderId)
                                            } else {
                                                Toast.makeText(context, "Error: No folio ID found", Toast.LENGTH_SHORT).show()
                                            }
                                            selectedItems.clear()
                                            cartPreferences.clearCart()
                                            dismissWithAnimation()
                                            closeWithAnimation()
                                        }
                                        .focusable(enabled = isConfirmEnabled)
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Confirm",
                                        color = if (!isConfirmEnabled) {
                                            Color.White.copy(alpha = 0.3f)
                                        } else if (isConfirmFocused) {
                                            Color(0xFF1C1D24)
                                        } else {
                                            Color.White
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
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
fun OrderDrawer(
    onDismiss: () -> Unit,
    context: Context,
    orders: List<Order>,
    onSelectOrder: (Order) -> Unit = {}
) {
    val firstItemFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }

    fun closeWithAnimation() {
        scope.launch {
            isVisible = false
            delay(300)
            onDismiss()
        }
    }

    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())) }

    LaunchedEffect(Unit) {
        isVisible = true
        while(true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            currentDate = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())
            delay(60000)
        }
    }

    Dialog(
        onDismissRequest = { closeWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false },
            contentAlignment = Alignment.CenterEnd
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { closeWithAnimation() }
            )

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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .focusGroup()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = currentDate,
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = currentTime,
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "My Order",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LaunchedEffect(Unit) {
                            delay(450)
                            if (orders.isNotEmpty()) {
                                try {
                                    firstItemFocusRequester.requestFocus()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        if (orders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.food_carousel))
                                    val progress by animateLottieCompositionAsState(
                                        composition = composition,
                                        iterations = LottieConstants.IterateForever
                                    )
                                    LottieAnimation(
                                        composition = composition,
                                        progress = { progress },
                                        modifier = Modifier.size(180.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "No orders placed",
                                        color = Color.White.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { alpha = 0.99f }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                0.08f to Color.Black,
                                                0.92f to Color.Black,
                                                1f to Color.Transparent
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    },
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp) // Ruang sela samping agar border focus pulsing & zoom scale tidak terpotong
                            ) {
                                items(orders.size) { index ->
                                    val order = orders[index]
                                    var isItemFocused by remember { mutableStateOf(false) }
                                    val itemImages = order.items
                                        ?.filter { it.imageUrl.isNotEmpty() }
                                        ?.map { it.imageUrl }
                                        ?.distinct()
                                        ?: emptyList()
                                    val totalQty = order.items?.sumOf { it.quantity ?: 1 } ?: 0
                                    val statusColor = when (order.status?.lowercase()) {
                                        "placed" -> Color(0xFFFF9800)
                                        "confirmed" -> Color(0xFF2196F3)
                                        "completed" -> Color(0xFF4CAF50)
                                        else -> Color.White.copy(alpha = 0.6f)
                                    }

                                    // Snappy Google TV focus zoom scale transition
                                    val scale by animateFloatAsState(
                                        targetValue = if (isItemFocused) 1.03f else 1.0f,
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                                        label = "OrderCardScale"
                                    )

                                    // Smooth fade in/out transition for focus visibility (LED Glow)
                                    val focusFadeAlpha by animateFloatAsState(
                                        targetValue = if (isItemFocused) 1.0f else 0.0f,
                                        animationSpec = tween(durationMillis = 350),
                                        label = "OrderFocusFadeAlpha"
                                    )

                                    val pulseAlpha = remember { androidx.compose.animation.core.Animatable(0.4f) }

                                    LaunchedEffect(isItemFocused) {
                                        if (isItemFocused) {
                                            pulseAlpha.animateTo(
                                                targetValue = 1.0f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                )
                                            )
                                        } else {
                                            pulseAlpha.snapTo(0.4f)
                                        }
                                    }

                                    // 🚀 PERFORMANCE: Apply elegant white border that pulses and fades in smoothly on focus
                                    val borderModifier = if (isItemFocused) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = Color.White.copy(alpha = pulseAlpha.value * focusFadeAlpha),
                                            shape = RoundedCornerShape(26.dp)
                                        )
                                    } else {
                                        Modifier
                                    }

                                    var lastClickTime by remember { mutableStateOf(0L) }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                                            }
                                            .onFocusChanged { isItemFocused = it.isFocused }
                                            .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier)
                                            .clickable {
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - lastClickTime > 500L) { // 500ms debounce
                                                    lastClickTime = currentTime
                                                    onSelectOrder(order)
                                                }
                                            }
                                    ) {
                                        // Inner Card with Floating border & Air Gap! (Zero background addition on focus)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .then(borderModifier)
                                                .padding(6.dp) // The Floating Air Gap
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Stacked images (mepet ke kiri, offset 12.dp, tanpa border hitam)
                                                Box(
                                                    modifier = Modifier
                                                        .width(if (itemImages.size > 1) (40 + (itemImages.size.coerceAtMost(3) - 1) * 12).dp else 40.dp)
                                                        .height(40.dp)
                                                ) {
                                                    itemImages.take(3).forEachIndexed { i, url ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .offset(x = (i * 12).dp)
                                                                .clip(CircleShape)
                                                        ) {
                                                            CachedAsyncImage(
                                                                imageUrl = url,
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop,
                                                                error = R.drawable.err
                                                            )
                                                        }
                                                    }
                                                    if (itemImages.isEmpty()) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .background(Color.White.copy(alpha = 0.08f), CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(id = R.drawable.ic_room_service),
                                                                contentDescription = null,
                                                                modifier = Modifier.size(20.dp),
                                                                tint = Color.White.copy(alpha = 0.5f)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Order info
                                                Column(modifier = Modifier.weight(1f)) {
                                                    // Baris 1: N items dan Rp Total Price (Sejajar)
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${totalQty} item${if (totalQty > 1) "s" else ""}",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp
                                                        )
                                                        Text(
                                                            text = formatIDR(order.total),
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 15.sp
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(1.5.dp)
                                                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    // Baris 2: Daftar Item Vertikal Berurutan ke bawah
                                                    val itemsList = order.items ?: emptyList()
                                                    itemsList.take(2).forEach { item ->
                                                        Text(
                                                            text = item.itemName ?: "",
                                                            color = Color.White.copy(alpha = 0.9f),
                                                            fontSize = 13.sp,
                                                            modifier = Modifier.padding(vertical = 0.dp),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    if (itemsList.size > 2) {
                                                        Text(
                                                            text = "+${itemsList.size - 2} item lainnya",
                                                            color = Color.White.copy(alpha = 0.45f),
                                                            fontSize = 12.sp,
                                                            modifier = Modifier.padding(vertical = 0.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(2.dp))

                                                    // Baris 3: Status Badge dan Timestamp sejajar
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.Start,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = (order.status ?: "Pending").uppercase(),
                                                            color = statusColor,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 10.sp
                                                        )
                                                        
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        
                                                        Text(
                                                            text = order.timestamp?.let { getTimeAgo(it) } ?: "",
                                                            color = Color.White.copy(alpha = 0.4f),
                                                            fontSize = 12.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private object NoIndication : androidx.compose.foundation.IndicationNodeFactory {
    override fun create(interactionSource: androidx.compose.foundation.interaction.InteractionSource): androidx.compose.ui.node.DelegatableNode {
        return object : androidx.compose.ui.Modifier.Node(), androidx.compose.ui.node.DrawModifierNode {
            override fun androidx.compose.ui.graphics.drawscope.ContentDrawScope.draw() {
                drawContent()
            }
        }
    }

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = System.identityHashCode(this)
}
