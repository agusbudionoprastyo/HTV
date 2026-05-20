package com.dafamsemarang.dhtv

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Close
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
import kotlinx.coroutines.coroutineScope
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.core.graphics.createBitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.RectangleShape
import androidx.core.content.edit
import androidx.compose.ui.text.input.ImeAction
import io.ktor.client.request.header

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

    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val deviceID = sharedPreferences.getString("deviceID", null)
    val branchId = sharedPreferences.getString("branchId", null)
    var guestInfo by remember { mutableStateOf<GuestInfo?>(null) }
    var folioId by remember { mutableStateOf<Int?>(null) }

    Log.d("FooterSection", "Initializing FooterSection with deviceID: $deviceID, branchId: $branchId")

    // Proper state management to ensure notification is shown only once
    //    var showPinDialog by remember { mutableStateOf(false) }
    //    var pinInput by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var showWaDialog by remember { mutableStateOf(false) }
    var showNotificationButtonDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var isDndActive by remember { mutableStateOf(false) }
    var notificationCount by remember { mutableIntStateOf(0) }
    var myRequests by remember { mutableStateOf<List<Request>>(emptyList()) }
    var selectedRequestForDetail by remember { mutableStateOf<Request?>(null) }
    var showRequestDetailDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showReleaseConfirmDialog by remember { mutableStateOf(false) }
    var currentNotification by remember { mutableStateOf<Notification?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showMyRequestsDrawer by remember { mutableStateOf(false) }
    var showCartDrawer by remember { mutableStateOf(false) }
    var showOrderDrawer by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    val homeFocusRequester = remember { FocusRequester() }
    val foodFocusRequester = remember { FocusRequester() }
    val hotelFocusRequester = remember { FocusRequester() }
    val requestFocusRequester = remember { FocusRequester() }
    val myRequestFocusRequester = remember { FocusRequester() }

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

    val database: DatabaseReference = Firebase.database.reference
    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.notif) }

    var storedPin by remember { mutableStateOf<String?>(null) }

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

    DisposableEffect(deviceID) {
        var activeGuestRef: com.google.firebase.database.DatabaseReference? = null
        var activeGuestListener: com.google.firebase.database.ValueEventListener? = null

        if (deviceID != null) {
            val deviceRef = database.child("DEVICES").child(deviceID)
            deviceRef.addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(deviceSnapshot: DataSnapshot) {
                    if (deviceSnapshot.exists()) {
                        val roomNumber = deviceSnapshot.child("room").getValue(String::class.java)
                        val bId = deviceSnapshot.child("branchId").getValue(String::class.java)
                        
                        if (roomNumber != null && bId != null) {
                            activeGuestRef = database.child("BRANCHES").child(bId).child("FOGUEST").child(roomNumber)
                            val innerListener = object : com.google.firebase.database.ValueEventListener {
                                override fun onDataChange(guestSnapshot: DataSnapshot) {
                                    if (guestSnapshot.exists()) {
                                        val info = guestSnapshot.getValue(GuestInfo::class.java)
                                        guestInfo = info
                                        folioId = info?.folio
                                    } else {
                                        guestInfo = null
                                        folioId = null
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {
                                    guestInfo = null
                                    folioId = null
                                }
                            }
                            activeGuestListener = innerListener
                            activeGuestRef?.addValueEventListener(innerListener)
                        }
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) { }
            })
        }

        onDispose {
            if (activeGuestRef != null && activeGuestListener != null) {
                activeGuestRef?.removeEventListener(activeGuestListener!!)
                Log.d("FooterSection", "Deep guest info listener disconnected successfully.")
            }
        }
    }

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
    
    DisposableEffect(folioId) {
        var dndRef: com.google.firebase.database.DatabaseReference? = null
        var dndListener: com.google.firebase.database.ValueEventListener? = null

        if (folioId != null) {
            Log.d("FooterSection", "Attaching managed DND listener for folioId: $folioId")
            dndRef = database.child("BRANCHES").child(branchId ?: "").child("DND_STATUS").child(folioId.toString())
            
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val dndStatus = dataSnapshot.getValue(Boolean::class.java) == true
                    if (!isFirstDndLoad || dndStatus) {
                        isDndActive = dndStatus
                        setAudioVolume(isDndActive)
                    } else {
                        isDndActive = dndStatus
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        if (currentVolume > 0) {
                            sharedPreferences.edit().putInt("last_volume", currentVolume).apply()
                        }
                    }
                    isFirstDndLoad = false
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("FooterSection", "DND error: ${databaseError.message}")
                }
            }
            dndListener = listener
            dndRef.addValueEventListener(listener)
        }

        onDispose {
            if (dndRef != null && dndListener != null) {
                dndRef.removeEventListener(dndListener)
                Log.d("FooterSection", "DND listener disposed.")
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
                            "home" -> homeFocusRequester
                            "cantingfood" -> foodFocusRequester
                            "hotel_guide" -> hotelFocusRequester
                            "contact" -> requestFocusRequester
                            else -> FocusRequester.Default
                        }
                    }
                }
                .focusGroup()
                .onFocusChanged { isFooterFocused = it.hasFocus },
            horizontalArrangement = Arrangement.spacedBy(17.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color(207, 223, 237).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    SmallServiceButtonWithBadge(
                        iconRes = R.drawable.notifications_svgrepo_com,
                        badgeCount = notificationCount,
                        onClick = { showNotificationButtonDialog = true },
                        title = "Notifications",
                        isActive = true
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = Color(207, 223, 237).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    SmallServiceButton(
                        iconRes = R.drawable.setting_svgrepo_com,
                        onClick = { showPinDialog = true },
                        title = "Settings",
                        isActive = true
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = Color(207, 223, 237).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    SmallServiceButton(
                        iconRes = R.drawable.do_not_disturb_svgrepo_com,
                        buttonColor = FooterIcon,
                        onClick = {
                            Log.d("FooterSection", "DND button clicked. Current status: $isDndActive, folioId: $folioId")
                            val currentFolioId = folioId
                            if (currentFolioId == null) {
                                Log.e("FooterSection", "Cannot toggle DND: folioId is null")
                                return@SmallServiceButton
                            }
                            if (isDndActive) {
                                showReleaseConfirmDialog = true
                            } else {
                                showConfirmDialog = true
                            }
                        },
                        title = "DND",
                        isActive = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    SmallServiceButton(
                        iconRes = R.drawable.wifi_rounded_svgrepo_com,
                        onClick = { showDialog = true },
                        title = "Wi-Fi",
                        isActive = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    SmallServiceButton(
                        iconRes = R.drawable.whatsapp_svgrepo_com,
                        onClick = { showWaDialog = true },
                        title = "WhatsApp",
                        isActive = true
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        color = Color(207, 223, 237).copy(alpha = 0.25f),
                        shape = CircleShape
                    )
                    .animateContentSize(animationSpec = tween(durationMillis = 500))
                    .padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    SmallServiceButton(
                        iconRes = R.drawable.ic_home_launcher_custom,
                        onClick = { if (currentRoute != "home") navController?.navigate("home") },
                        title = "Home",
                        onFocusAction = { if (currentRoute != "home") navController?.navigate("home") },
                        isActive = currentRoute == "home",
                        focusRequester = homeFocusRequester
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    SmallServiceButton(
                        iconRes = R.drawable.room_service_3_svgrepo_com,
                        onClick = { if (currentRoute != "cantingfood") navController?.navigate("cantingfood") },
                        title = "F&B",
                        onFocusAction = { if (currentRoute != "cantingfood") navController?.navigate("cantingfood") },
                        isActive = currentRoute == "cantingfood",
                        focusRequester = foodFocusRequester
                    )

                    val isFnBActive = currentRoute == "cantingfood"
                    var isCartFocused by remember { mutableStateOf(false) }
                    var isOrderFocused by remember { mutableStateOf(false) }
                    val showCartOrderCapsule = isFnBActive || isCartFocused || isOrderFocused

                    val isRequestActive = currentRoute == "contact"
                    var isMyRequestFocused by remember { mutableStateOf(false) }
                    val showRequestCapsule = isRequestActive || isMyRequestFocused

                    val capsuleWidth by animateDpAsState(
                        targetValue = if (showCartOrderCapsule) 165.dp else 0.dp,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        label = "capsuleWidth"
                    )

                    if (capsuleWidth > 0.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                modifier = Modifier.width(
                                    animateDpAsState(
                                        targetValue = if (showCartOrderCapsule) 8.dp else 0.dp,
                                        animationSpec = tween(350)
                                    ).value
                                )
                            )
                            
                            // Combined single capsule container next to F&B
                            CompositionLocalProvider(LocalIndication provides NoIndication) {
                                Box(
                                    modifier = Modifier
                                        .size(width = capsuleWidth, height = 36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            color = Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // 1. Cart Button Segment
                                        Box(
                                            contentAlignment = Alignment.TopEnd
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 64.dp, height = 28.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .onFocusChanged { isCartFocused = it.isFocused }
                                                    .background(
                                                        color = if (isCartFocused) Color(0xFFCFDFED) else Color.Transparent,
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable(
                                                        onClick = { showCartDrawer = true },
                                                        indication = null, // Disable default focus indication box
                                                        interactionSource = remember { MutableInteractionSource() }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Cart",
                                                    color = if (isCartFocused) Color(0xFF1C1D24) else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            if (GlobalCartState.selectedItems.isNotEmpty()) {
                                                FooterPulsingBadge(
                                                    modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                                                )
                                            }
                                        }

                                        // Thin Divider
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(14.dp)
                                                .background(Color.White.copy(alpha = 0.2f))
                                        )

                                        // 2. My Order Button Segment
                                        Box(
                                            modifier = Modifier
                                                .size(width = 88.dp, height = 28.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .onFocusChanged { isOrderFocused = it.isFocused }
                                                .background(
                                                    color = if (isOrderFocused) Color(0xFFCFDFED) else Color.Transparent,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable(
                                                    onClick = { showOrderDrawer = true },
                                                    indication = null, // Disable default focus indication box
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "My Order",
                                                color = if (isOrderFocused) Color(0xFF1C1D24) else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    SmallServiceButton(
                        iconRes = R.drawable.service_request_svgrepo_com,
                        onClick = { if (currentRoute != "contact") navController?.navigate("contact") },
                        title = "Request",
                        onFocusAction = { if (currentRoute != "contact") navController?.navigate("contact") },
                        isActive = currentRoute == "contact",
                        focusRequester = requestFocusRequester
                    )

                    val requestCapsuleWidth by animateDpAsState(
                        targetValue = if (showRequestCapsule) 98.dp else 0.dp,
                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        label = "requestCapsuleWidth"
                    )

                    if (requestCapsuleWidth > 0.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(
                                modifier = Modifier.width(
                                    animateDpAsState(
                                        targetValue = if (showRequestCapsule) 8.dp else 0.dp,
                                        animationSpec = tween(350)
                                    ).value
                                )
                            )

                            CompositionLocalProvider(LocalIndication provides NoIndication) {
                                Box(
                                    modifier = Modifier
                                        .size(width = requestCapsuleWidth, height = 36.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            color = Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (requestCapsuleWidth >= 80.dp) {
                                        Box(
                                            contentAlignment = Alignment.TopEnd
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 90.dp, height = 28.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .focusRequester(myRequestFocusRequester)
                                                    .onFocusChanged { isMyRequestFocused = it.isFocused }
                                                    .background(
                                                        color = if (isMyRequestFocused) Color(0xFFCFDFED) else Color.Transparent,
                                                        shape = RoundedCornerShape(14.dp)
                                                    )
                                                    .clickable(
                                                        onClick = { showMyRequestsDrawer = true },
                                                        indication = null, // Disable default focus indication box
                                                        interactionSource = remember { MutableInteractionSource() }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "My Request",
                                                    color = if (isMyRequestFocused) Color(0xFF1C1D24) else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            if (myRequests.isNotEmpty()) {
                                                FooterPulsingBadge(
                                                    modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    SmallServiceButton(
                        iconRes = R.drawable.info_circle_svgrepo_com,
                        onClick = { if (currentRoute != "hotel_guide") navController?.navigate("hotel_guide") },
                        title = "Hotel Info",
                        onFocusAction = { if (currentRoute != "hotel_guide") navController?.navigate("hotel_guide") },
                        isActive = currentRoute == "hotel_guide",
                        focusRequester = hotelFocusRequester
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
            // Icon (Filled background clock!)
            FooterClockIcon(modifier = Modifier.size(22.dp))
            
            // Jam
            Text(
                text = footerTime,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showConfirmDialog) {
        val sliderFocusRequester = remember { FocusRequester() }
        var targetProgress by remember { mutableFloatStateOf(0f) }
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing) // Faster animation for manual hold follow!
        )

        // Automatically trigger activation when slide completes
        LaunchedEffect(targetProgress) {
            if (targetProgress == 1f) {
                delay(350)
                Log.d("FooterSection", "Confirming DND activation via auto-slide for folioId: $folioId")
                val currentFolioId = folioId
                if (currentFolioId != null) {
                    setDndStatusInFirebase(context, currentFolioId, true)
                    sendDndNotification(context, currentFolioId, release = false, deviceID = deviceID)
                }
                showConfirmDialog = false
            }
        }

        // Request focus on launch
        LaunchedEffect(Unit) {
            delay(100)
            sliderFocusRequester.requestFocus()
        }

        Dialog(
            onDismissRequest = { showConfirmDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1E2026), shape = RoundedCornerShape(28.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE91E63).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = rememberAsyncImagePainter(R.drawable.do_not_disturb_svgrepo_com),
                            contentDescription = "DND Icon",
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFFE91E63)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Set Do Not Disturb",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Hold D-pad Right to activate 'Do Not Disturb'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    var isSliderFocused by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .onFocusChanged { isSliderFocused = it.isFocused }
                            .focusRequester(sliderFocusRequester)
                            .onKeyEvent { keyEvent ->
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
                                        showConfirmDialog = false
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            }
                            .focusable(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Slider fill progress track (nesting the thumb inside the leading edge)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(300.dp * animatedProgress + 56.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFE91E63).copy(alpha = 0.3f), Color(0xFFE91E63))
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                        )

                        // Placeholder Text in center
                        Text(
                            text = if (targetProgress == 1f) "Activating..." else "Hold D-pad Right",
                            color = Color.White.copy(alpha = 0.35f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Circular Slider Thumb (slides based on animatedProgress)
                        val thumbOffset = with(androidx.compose.ui.platform.LocalDensity.current) {
                            (300.dp * animatedProgress).toPx()
                        }

                        Box(
                            modifier = Modifier
                                .graphicsLayer { translationX = thumbOffset }
                                .padding(4.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )

                        // Right Target Arrow to show direction
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "Right Arrow",
                                modifier = Modifier.size(20.dp),
                                tint = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReleaseConfirmDialog) {
        val sliderFocusRequester = remember { FocusRequester() }
        var targetProgress by remember { mutableFloatStateOf(1f) }
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing) // Faster animation for manual hold follow!
        )

        // Automatically trigger release when slide completes
        LaunchedEffect(targetProgress) {
            if (targetProgress == 0f) {
                delay(350)
                Log.d("FooterSection", "Confirming DND deactivation via auto-slide for folioId: $folioId")
                val currentFolioId = folioId
                if (currentFolioId != null) {
                    setDndStatusInFirebase(context, currentFolioId, false)
                    sendDndNotification(context, currentFolioId, release = true, deviceID = deviceID)
                }
                showReleaseConfirmDialog = false
            }
        }

        // Request focus on launch
        LaunchedEffect(Unit) {
            delay(100)
            sliderFocusRequester.requestFocus()
        }

        Dialog(
            onDismissRequest = { showReleaseConfirmDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF1E2026), shape = RoundedCornerShape(28.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = rememberAsyncImagePainter(R.drawable.do_not_disturb_svgrepo_com),
                            contentDescription = "DND Icon",
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Release Do Not Disturb",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Hold D-pad Left to release 'Do Not Disturb'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    var isSliderFocused by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .onFocusChanged { isSliderFocused = it.isFocused }
                            .focusRequester(sliderFocusRequester)
                            .onKeyEvent { keyEvent ->
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
                                        showReleaseConfirmDialog = false
                                        true
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            }
                            .focusable(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Slider fill progress track (nesting the thumb inside the leading edge)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(300.dp * animatedProgress + 56.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.8f))
                                    ),
                                    shape = RoundedCornerShape(28.dp)
                                )
                        )

                        // Placeholder Text in center
                        Text(
                            text = if (targetProgress == 0f) "Releasing..." else "Hold D-pad Left",
                            color = Color.White.copy(alpha = 0.35f),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )

                        // Circular Slider Thumb (slides based on animatedProgress)
                        val thumbOffset = with(androidx.compose.ui.platform.LocalDensity.current) {
                            (300.dp * animatedProgress).toPx()
                        }

                        Box(
                            modifier = Modifier
                                .graphicsLayer { translationX = thumbOffset }
                                .padding(4.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )

                        // Left Arrow to show release direction (placed on the left side of track)
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 16.dp)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "Left Arrow",
                                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f },
                                tint = Color.White.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
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
        WifiQRCodeDialog(onDismiss = { showDialog = false })
    }

    if (showWaDialog) {
        WaQRCodeDialog(onDismiss = { showWaDialog = false })
    }

    if (showPinDialog) {
        PinDialog(
            pinInput = pinInput,
            onPinChange = { pinInput = it },
            onDismiss = { showPinDialog = false },
            onPinConfirmed = { submittedPin ->
                if (storedPin != null) {
                    if (submittedPin == storedPin) {
                        showSettingsMenu = true
                        showPinDialog = false
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
            onDismiss = { showMyRequestsDrawer = false }, 
            requests = myRequests,
            onSelectRequest = { req ->
                selectedRequestForDetail = req
                showRequestDetailDialog = true
            }
        )
    }

    if (showCartDrawer) {
        CartDrawer(
            onDismiss = { showCartDrawer = false },
            context = context
        )
    }

    if (showOrderDrawer) {
        OrderDrawer(
            onDismiss = { showOrderDrawer = false },
            context = context
        )
    }
    
    if (showRequestDetailDialog && selectedRequestForDetail != null) {
        RequestDetailDialog(
            request = selectedRequestForDetail!!,
            onDismiss = { showRequestDetailDialog = false }
        )
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
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { 
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
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    "No active requests", 
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
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
                                contentPadding = PaddingValues(vertical = 24.dp)
                            ) {
                                items(requests.size) { index ->
                                    val request = requests[index]
                                    var isItemFocused by remember { mutableStateOf(false) }
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(if (index == 0) firstItemFocusRequester else FocusRequester())
                                            .onFocusChanged { isItemFocused = it.isFocused }
                                            .clickable { onSelectRequest(request) },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isItemFocused) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                        tonalElevation = if (isItemFocused) 12.dp else 0.dp,
                                        border = if (isItemFocused) BorderStroke(2.dp, Color.White.copy(alpha = 0.2f)) else null
                                    ) {
                                        MyRequestItemDrawer(request = request, isFocused = isItemFocused)
                                    }
                                }
                            }
                        }
                        
                        // Footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DHTV",
                                color = Color.White.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.info_circle_svgrepo_com),
                                contentDescription = "Help",
                                tint = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
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
                    painter = painterResource(id = R.drawable.service_request_svgrepo_com),
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
fun NotificationButtonDialog(
    context: Context,
    showNotificationButtonDialog: Boolean,
    onDismiss: () -> Unit,
    folioId: Int // Receive folioId to take notification data
) {
    var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var selectedNotification by remember { mutableStateOf<Notification?>(null) }
    
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
    val focusRequester = remember { FocusRequester() }

    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }
    var currentDate by remember { mutableStateOf(SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())) }

    // Enter animation, clock tick, & Focus
    LaunchedEffect(Unit) {
        isVisible = true
        scope.launch {
            delay(350) // Wait for animation
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                Log.e("NotificationDialog", "Failed to request focus", e)
            }
        }
        while(true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            currentDate = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())
            delay(60000)
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
                        .background(Color.Black.copy(alpha = 0.4f))
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
                            .clickable(enabled = false) {}, // Intercept clicks
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
                            // Header (Title left, Close/Clear button right)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            indication = ripple(color = Color.White),
                                            interactionSource = remember { MutableInteractionSource() }
                                        )
                                        .focusable(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isClearClicked) "Clear" else "\uF057", // FontAwesome X icon
                                        color = if (isCloseFocused) Color(0xFF1C1D24) else Color.White.copy(alpha = 0.55f),
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
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(sortedNotifications) { index, notification ->
                                    NotificationItem(
                                        notification = notification,
                                        deleteNotification = { deleteNotification(context, notification, folioId) },
                                        onNotificationClick = {
                                            selectedNotification = notification
                                        },
                                        onFocused = {
                                            focusedIndex = index
                                        }
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
fun NotificationItem(
    notification: Notification,
    deleteNotification: (Notification) -> Unit,
    onNotificationClick: (Notification) -> Unit,
    onFocused: () -> Unit
) {
    var formattedTimestamp by remember { mutableStateOf(getTimeAgo(notification.timestamp)) }
    var clickCount by remember { mutableIntStateOf(0) }  // Track the number of clicks

    var isFocused by remember { mutableStateOf(false) }
    val focusPulseAlpha = remember { Animatable(0.0f) }

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
                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            focusPulseAlpha.snapTo(0.0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = if (isFocused) focusPulseAlpha.value else 0f),
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocused()
                } else {
                    clickCount = 0
                }
            }
            .clickable(
                onClick = {
                    onNotificationClick(notification)
                    clickCount += 1
                    if (clickCount == 2) {
                        deleteNotification(notification)
                        clickCount = 0
                    }
                },
                indication = ripple(color = Color.White),
                interactionSource = remember { MutableInteractionSource() }
            )
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp) // Beautiful transparent gap between card and selection border!
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = if (isFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.05f),
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
                            color = if (isFocused) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Show the appropriate icon based on the click count
                    val iconRes = when {
                        clickCount == 1 -> R.drawable.delete_alt_svgrepo_com // Show delete icon after first click
                        notification.type == "DND" -> R.drawable.do_not_disturb_svgrepo_com
                        notification.type == "LAUNDRY" -> R.drawable.hotel_coat_check_svgrepo_com
                        notification.type == "ROOM_SERVICE" -> R.drawable.room_service_3_svgrepo_com
                        notification.type == "GUEST_REQUEST" -> R.drawable.service_request_svgrepo_com
                        else -> R.drawable.notifications_svgrepo_com // Default icon
                    }

                    Icon(
                        painter = rememberAsyncImagePainter(iconRes),
                        contentDescription = "Notification Icon",
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxSize(),
                        tint = if (isFocused) Color(0xFF1C1D24) else Color.White
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
                        color = if (isFocused) Color(0xFF1C1D24) else Color.White
                    )
                    Text(
                        text = notification.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) Color(0xFF1C1D24).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formattedTimestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) Color(0xFF1C1D24).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.5f)
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

    // Fetch order or request details based on notification type
    LaunchedEffect(notification) {
        when (notification.type) {
            "ROOM_SERVICE" -> {
                getOrderDetailsFromFirebase(context, notification.id) { fetchedOrderDetails ->
                    orderDetails = fetchedOrderDetails
                    isLoading = false
                    showOrderDetailDialog = true
                }
            }
            "GUEST_REQUEST" -> {
                getRequestDetailsFromFirebase(context, notification.id) { fetchedRequestDetails ->
                    requestDetails = fetchedRequestDetails
                    isLoading = false
                    showRequestDetailDialog = true
                }
            }
            else -> {
                isLoading = false
            }
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
            Dialog(onDismissRequest = onDismiss) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                            .align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "\uF057", // Close icon
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            fontFamily = FontFamily(Font(R.font.icons)),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Column {
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
    iconRes: Int,
    onClick: () -> Unit,
    buttonColor: Color = Color.White, // Force solid white default
    title: String? = null,
    onFocusAction: (() -> Unit)? = null,
    isActive: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    var isClicked by remember { mutableStateOf(false) }
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
                    if (it.isFocused && !isActive) {
                        onFocusAction?.invoke()
                    }
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
                modifier = Modifier.size(20.dp), // Balanced icon size
                tint = if (isFocused) Color(0xFF1C1D24) else buttonColor // Google TV High-Contrast Dynamic Switch
            )
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
                    .wrapContentWidth(unbounded = true),
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
    isActive: Boolean = false
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .onFocusChanged { isFocused = it.isFocused }
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
                    .wrapContentWidth(unbounded = true),
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun WifiQRCodeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val database: DatabaseReference = Firebase.database.reference
        database.child("BRANCHES").child(branchId ?: "").child("SETTING/WIFI")
            .get()
            .addOnSuccessListener { snapshot ->
                ssid = snapshot.child("ssid").getValue(String::class.java) ?: ""
                password = snapshot.child("password").getValue(String::class.java) ?: ""
                loading = false
            }
            .addOnFailureListener {
                loading = false
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            if (!loading) {
                val qrCodeBitmap = generateWifiQRCode(ssid, password)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {}, 
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Left Side: Text Info
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Scan to connect\nto Wi-Fi",
                            fontSize = 32.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        // SSID
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.wifi_rounded_svgrepo_com),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Network",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha=0.6f)
                                )
                                Text(
                                    ssid,
                                    fontSize = 22.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Password
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.keyboard),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Password",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha=0.6f)
                                )
                                Text(
                                    password,
                                    fontSize = 22.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Speed Test Section - Auto-run on dialog open
                        var linkSpeed by remember { mutableStateOf(0) }
                        var speedResult by remember { mutableStateOf<Double?>(null) }
                        var pingResult by remember { mutableStateOf<Int?>(null) }
                        var isTesting by remember { mutableStateOf(true) }
                        var testStatus by remember { mutableStateOf("") }

                        // Auto-run tests when dialog opens
                        LaunchedEffect(Unit) {
                            try {
                                // Get WiFi link speed
                                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
                                linkSpeed = wifiManager?.connectionInfo?.linkSpeed ?: 0
                                
                                // Run ping test
                                testStatus = "Testing ping..."
                                pingResult = SpeedTestManager.runPingTest()
                                
                                // Run download test
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

                        // Display results
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column {
                                // Connection Status Summary
                                val connectionStatus = when {
                                    isTesting -> "Testing connection..."
                                    pingResult == null || speedResult == null -> "Running tests..."
                                    pingResult!! <= 0 || speedResult!! <= 0 -> "Connection issues detected"
                                    pingResult!! < 50 && speedResult!! > 20 -> "Connection excellent"
                                    pingResult!! < 100 && speedResult!! > 10 -> "Connection good"
                                    pingResult!! < 150 -> "Connection fair"
                                    else -> "Connection slow"
                                }
                                
                                val statusColor = when {
                                    isTesting -> Color.White.copy(alpha = 0.7f)
                                    pingResult == null || speedResult == null -> Color.White.copy(alpha = 0.7f)
                                    pingResult!! <= 0 || speedResult!! <= 0 -> Color.Red
                                    pingResult!! < 50 && speedResult!! > 20 -> Color.Green
                                    pingResult!! < 100 && speedResult!! > 10 -> Color.Cyan
                                    else -> Color.Yellow
                                }
                                
                                // Status row with icon
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isTesting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        connectionStatus,
                                        fontSize = 13.sp,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                // Only show results if tests have started
                                if (pingResult != null || speedResult != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                                
                                // Ping result
                                if (pingResult != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Ping",
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            if (pingResult!! > 0) "${pingResult}ms" else "Failed",
                                            fontSize = 14.sp,
                                            color = if (pingResult!! > 0) Color.White.copy(alpha = 0.8f) else Color.Red,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                // Download result
                                if (speedResult != null) {
                                    if (pingResult != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Download",
                                            fontSize = 14.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        if (speedResult!! > 0) {
                                            Text(
                                                "${String.format("%.1f", speedResult)} Mbps",
                                                fontSize = 14.sp,
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Text(
                                                "Failed",
                                                fontSize = 14.sp,
                                                color = Color.Red,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(80.dp))

                    // Right Side: QR Code
                    Image(
                        bitmap = qrCodeBitmap, 
                        contentDescription = "Wi-Fi QR Code",
                        modifier = Modifier
                            .size(400.dp)
                            .clip(RoundedCornerShape(32.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun WaQRCodeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val room = sharedPreferences.getString("deviceID", null)
    val branchId = sharedPreferences.getString("branchId", null)

    var phone by remember { mutableStateOf("") }
    var ext by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val database: DatabaseReference = Firebase.database.reference
        database.child("BRANCHES").child(branchId ?: "").child("SETTING/CONTACT")
            .get()
            .addOnSuccessListener { snapshot ->
                phone = snapshot.child("PHONE").getValue(String::class.java) ?: ""
                val rawMessage = snapshot.child("MESSAGE").getValue(String::class.java) ?: ""
                message = URLEncoder.encode(rawMessage, StandardCharsets.UTF_8.toString())
                ext = snapshot.child("EXT").getValue(String::class.java) ?: ""
                telephone = snapshot.child("TELEPHONE").getValue(String::class.java) ?: ""
                address = snapshot.child("ADDRESS").getValue(String::class.java) ?: ""
                loading = false
            }
            .addOnFailureListener {
                loading = false
            }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            if (!loading) {
                val qrCodeBitmap = room?.let { generateWaQRCode(phone, message, it) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {}, 
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Left Side: Info
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Scan to contact\nreceptionist",
                            fontSize = 32.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp,
                            fontFamily = FontFamily(Font(R.font.ionicons)) // Assuming using icon font or regular? User code used icon before.
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Phone Info Rows
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Whatsapp
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.White.copy(0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.whatsapp_svgrepo_com),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.Green
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("WhatsApp", fontSize=12.sp, color=Color.White.copy(0.6f))
                                    Text(phone, fontSize=20.sp, color=Color.White, fontWeight=FontWeight.Bold)
                                }
                            }

                            // Telephone
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.White.copy(0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.phone_rounded_svgrepo_com),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Telephone", fontSize=12.sp, color=Color.White.copy(0.6f))
                                    Text(telephone, fontSize=20.sp, color=Color.White, fontWeight=FontWeight.Bold)
                                }
                            }
                            
                            // Ext
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.White.copy(0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Ext",
                                        fontSize=12.sp,
                                        fontWeight=FontWeight.Bold,
                                        color=Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Extension", fontSize=12.sp, color=Color.White.copy(0.6f))
                                    Text(ext, fontSize=20.sp, color=Color.White, fontWeight=FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(60.dp))

                    // Right Side: QR
                    qrCodeBitmap?.let {
                        Image(
                            bitmap = it, 
                            contentDescription = "Wa QR Code",
                            modifier = Modifier
                                .size(400.dp)
                                .clip(RoundedCornerShape(32.dp))
                        )
                    }
                }
                
                // Footer Address
                Text(
                    text = address,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha=0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(0.8f)
                )
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

                                    Log.d("FooterSection", "Step 3: Calling sendPostDnDToApi with message")
                                    sendPostDnDToApi(context, message)
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

fun sendPostDnDToApi(context: Context, message: String) {
    val database = Firebase.database.reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    
    Log.d("TelegramGatewayHandler", "Starting Telegram API call with branchId: $branchId")
    
    val telegramGatewayRef = database.child("BRANCHES").child(branchId ?: "").child("SETTING").child("TELEGRAM_REQUEST")
    Log.d("TelegramGatewayHandler", "Firebase path: ${telegramGatewayRef.toString()}")

    telegramGatewayRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d("TelegramGatewayHandler", "Received Firebase data: ${snapshot.exists()}")
            
            // Firebase structure: chatId = chat ID (group ID), tokenBot = bot token
            val chatId = snapshot.child("chatId").getValue(Any::class.java) // Can be Long or Number
            val botToken = snapshot.child("tokenBot").getValue(String::class.java)

            Log.d("TelegramGatewayHandler", "Configuration values:")
            Log.d("TelegramGatewayHandler", "chatId: $chatId")
            Log.d("TelegramGatewayHandler", "botToken: $botToken")

            if (chatId != null && botToken != null) {
                // Build Telegram Bot API endpoint URL
                val endpointUrl = "https://api.telegram.org/bot$botToken/sendMessage"
                
                Log.d("TelegramGatewayHandler", "Endpoint URL: $endpointUrl")
                Log.d("TelegramGatewayHandler", "Message to API: $message")

                // Convert chat_id to Long (Telegram group IDs are always numbers)
                val chatIdNumber: Long = when (chatId) {
                    is Long -> chatId
                    is Number -> chatId.toLong()
                    is String -> {
                        try {
                            chatId.toLong()
                        } catch (e: NumberFormatException) {
                            Log.e("TelegramGatewayHandler", "chatId is not a valid number: $chatId")
                            throw IllegalArgumentException("chatId must be a number, got: $chatId")
                        }
                    }
                    else -> {
                        Log.e("TelegramGatewayHandler", "chatId has unsupported type: ${chatId.javaClass}")
                        throw IllegalArgumentException("chatId must be a number, got: ${chatId.javaClass}")
                    }
                }
                
                // Create request body using data class for proper serialization
                val requestBody = TelegramMessageRequest(
                    chat_id = chatIdNumber,  // Telegram group chat ID (Long)
                    text = message,         // Message text with HTML formatting
                    parse_mode = "HTML"     // Enable HTML parsing for <b>bold</b> and <i>italic</i>
                )

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val client = HttpClient(Android) {
                            install(ContentNegotiation) {
                                json(Json { prettyPrint = true; isLenient = true })
                            }
                        }

                        Log.d("TelegramGatewayHandler", "Making API call to: $endpointUrl")
                        Log.d("TelegramGatewayHandler", "Request body: $requestBody")
                        
                        // Serialize to JSON to verify the actual JSON being sent
                        val jsonSerializer = Json { prettyPrint = false; isLenient = true }
                        val jsonString = jsonSerializer.encodeToString(TelegramMessageRequest.serializer(), requestBody)
                        Log.d("TelegramGatewayHandler", "JSON being sent: $jsonString")
                        
                        val response = client.post(endpointUrl) {
                            contentType(ContentType.Application.Json)
                            setBody(requestBody)
                        }

                        val responseBody = response.bodyAsText()
                        Log.d("TelegramGatewayHandler", "Response status: ${response.status}")
                        Log.d("TelegramGatewayHandler", "Response body: $responseBody")

                        if (response.status == HttpStatusCode.OK) {
                            Log.d("TelegramGatewayHandler", "Message sent successfully to Telegram")
                        } else {
                            Log.e("TelegramGatewayHandler", "Error: ${response.status}")
                            Log.e("TelegramGatewayHandler", "Response Body: $responseBody")
                        }

                        client.close()

                    } catch (e: Exception) {
                        Log.e("TelegramGatewayHandler", "Request failed: ${e.message}")
                        Log.e("TelegramGatewayHandler", "Exception type: ${e.javaClass.simpleName}")
                        e.printStackTrace()
                    }
                }
            } else {
                Log.e("TelegramGatewayHandler", "Incomplete TELEGRAM_REQUEST settings in branch $branchId")
                Log.e("TelegramGatewayHandler", "Missing values: ${listOfNotNull(
                    if (chatId == null) "chatId (chat ID)" else null,
                    if (botToken == null) "tokenBot (bot token)" else null
                ).joinToString(", ")}")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("TelegramGatewayHandler", "Failed to retrieve TELEGRAM_REQUEST settings from branch $branchId", error.toException())
        }
    })
}

fun updateNotificationStatus(context: Context, notification: Notification, folioId: Int, newStatus: String) {
    val database = Firebase.database.reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    val notificationsRef = database.child("BRANCHES").child(branchId ?: "").child("NOTIFICATIONS").child(folioId.toString())

    notificationsRef.orderByChild("id").equalTo(notification.id).get().addOnSuccessListener { snapshot ->
        snapshot.children.forEach { snapshotChild ->
            snapshotChild.ref.child("status").setValue(newStatus)
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

    notificationsRef.orderByChild("id").equalTo(notification.id).get().addOnSuccessListener {
        it.children.forEach { snapshot ->
            snapshot.ref.removeValue()
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

fun generateWifiQRCode(ssid: String, password: String): ImageBitmap {
    val wifiUrl = "WIFI:T:WPA;S:$ssid;P:$password;;"

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(wifiUrl, BarcodeFormat.QR_CODE, 512, 512)

    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[y * width + x] = if (bitMatrix.get(x, y)) FooterText.toArgb() else Color.Transparent.toArgb()
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
            pixels[y * width + x] = if (bitMatrix.get(x, y)) FooterText.toArgb() else Color.Transparent.toArgb()
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
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { closeWithAnimation() }
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
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    "Cart empty",
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
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

                                    val minusBorderModifier = if (isMinusFocused) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            color = Color.White.copy(alpha = pulseAlpha.value * minusFocusFade),
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }

                                    val plusBorderModifier = if (isPlusFocused) {
                                        Modifier.border(
                                            width = 1.5.dp,
                                            color = Color.White.copy(alpha = pulseAlpha.value * plusFocusFade),
                                            shape = CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }

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
                                                        .padding(3.dp)
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
                                                        .background(if (isMinusFocused) Color(0xFFE91E63) else Color.White.copy(alpha = 0.1f))
                                                        .clickable {
                                                            if (selectedItem.quantity > 1) {
                                                                val idx = selectedItems.indexOf(selectedItem)
                                                                if (idx != -1) {
                                                                    selectedItems[idx] = selectedItem.copy(quantity = selectedItem.quantity - 1)
                                                                    cartPreferences.saveCart(selectedItems)
                                                                }
                                                            } else {
                                                                selectedItems.remove(selectedItem)
                                                                cartPreferences.saveCart(selectedItems)
                                                                Toast.makeText(context, "${selectedItem.item.name} removed from cart.", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (selectedItem.quantity == 1) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "-",
                                                            color = Color.White,
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
                                                        .padding(3.dp)
                                                        .clip(CircleShape)
                                                        .onFocusChanged { 
                                                            isPlusFocused = it.isFocused 
                                                            if (it.isFocused) {
                                                                lastFocusedControl = plusKey
                                                            }
                                                        }
                                                        .focusRequester(plusFocusRequester)
                                                        .focusable()
                                                        .background(if (isPlusFocused) Color(0xFFE91E63) else Color.White.copy(alpha = 0.1f))
                                                        .clickable {
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
                                                        color = Color.White,
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

                            val checkoutBorderModifier = if (isCheckoutFocused) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = Color.White.copy(alpha = checkoutPulseAlpha.value * checkoutFocusFade),
                                    shape = RoundedCornerShape(25.dp)
                                )
                            } else {
                                Modifier
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .then(checkoutBorderModifier)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .onFocusChanged { isCheckoutFocused = it.isFocused }
                                    .focusable()
                                    .background(if (isCheckoutFocused) Color(0xFFE91E63) else Color(0xFF555555))
                                    .clickable {
                                        dialogMessage.value = "\uF19F Please select payment method to proceed the order"
                                        showConfirmationDialog.value = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Checkout",
                                    color = if (isCheckoutFocused) Color.White else Color.White.copy(alpha = 0.6f),
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
        AlertDialog(
            onDismissRequest = { showConfirmationDialog.value = false },
            title = { Text("Order Confirmation") },
            text = {
                Column {
                    Text("Payment Method", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RadioButton(
                            selected = selectedPaymentMethod.value == "Cash",
                            onClick = { selectedPaymentMethod.value = "Cash" }
                        )
                        Text("Cash", color = Color.Black, modifier = Modifier.clickable { selectedPaymentMethod.value = "Cash" })

                        Spacer(modifier = Modifier.width(24.dp))

                        RadioButton(
                            selected = selectedPaymentMethod.value == "Debit/Credit Card",
                            onClick = { selectedPaymentMethod.value = "Debit/Credit Card" }
                        )
                        Text("Debit/Credit Card", color = Color.Black, modifier = Modifier.clickable { selectedPaymentMethod.value = "Debit/Credit Card" })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folioId != null) {
                            val orderId = generateOrderId()
                            sendOrderNotification(context, folioId!!, selectedPaymentMethod.value, orderId, selectedItems)
                            sendOrderToDatabase(context, folioId!!, guestName ?: "", guestPhone ?: "", guestRoom ?: "", selectedPaymentMethod.value, selectedItems, "placed", orderId)
                            sendPostOrderToApi(
                                context,
                                folioId!!,
                                guestName ?: "",
                                guestPhone ?: "",
                                guestRoom ?: "",
                                selectedPaymentMethod.value,
                                selectedItems,
                                orderId
                            )
                        } else {
                            Toast.makeText(context, "Error: No folio ID found", Toast.LENGTH_SHORT).show()
                        }
                        selectedItems.clear()
                        cartPreferences.clearCart()
                        showConfirmationDialog.value = false
                        closeWithAnimation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                ) {
                    Text("Confirm", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog.value = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun OrderDrawer(
    onDismiss: () -> Unit,
    context: Context
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

    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val roomId = sharedPreferences.getString("room", null)
    val branchId = sharedPreferences.getString("branchId", null)
    var guestInfo by remember { mutableStateOf<GuestInfo?>(null) }
    var folioId by remember { mutableStateOf<Int?>(null) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }

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

    DisposableEffect(folioId, branchId) {
        var activeQuery: com.google.firebase.database.Query? = null
        var activeListener: com.google.firebase.database.ValueEventListener? = null

        if (folioId != null && branchId != null) {
            val orderRef = database.child("BRANCHES").child(branchId).child("ORDERS")
            val query = orderRef.orderByChild("folioId").equalTo(folioId!!.toDouble())
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

    Dialog(
        onDismissRequest = { closeWithAnimation() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { closeWithAnimation() }
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
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(
                                    "No orders placed",
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
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
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                items(orders.size) { index ->
                                    val order = orders[index]
                                    var isItemFocused by remember { mutableStateOf(false) }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { isItemFocused = it.isFocused }
                                            .then(if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier),
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isItemFocused) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                        border = if (isItemFocused) BorderStroke(1.5.dp, Color(0xFFE91E63)) else null
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "ID: ${order.orderId ?: "N/A"}",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                
                                                val statusColor = when (order.status?.lowercase()) {
                                                    "placed" -> Color(0xFFFF9800)
                                                    "confirmed" -> Color(0xFF2196F3)
                                                    "completed" -> Color(0xFF4CAF50)
                                                    else -> Color.White.copy(alpha = 0.6f)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .background(statusColor.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = (order.status ?: "Pending").uppercase(),
                                                        color = statusColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            order.items?.forEach { item ->
                                                Text(
                                                    text = "- ${item.itemName} x${item.quantity ?: 1}${if (!item.variant.isNullOrEmpty()) " (${item.variant})" else ""}",
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Total:",
                                                    color = Color.White.copy(alpha = 0.6f),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                Text(
                                                    text = formatIDR(order.total),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall
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
