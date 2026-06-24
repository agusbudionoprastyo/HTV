package com.dafamsemarang.dhtv

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants

/**
 * Composable untuk menampilkan reminder checkout yang bisa digunakan di semua screen
 */
enum class ReminderState { None, Warning, Expired }

@Composable
fun CheckoutReminder() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val deviceID = sharedPreferences.getString("deviceID", null)
    val branchId = sharedPreferences.getString("branchId", null)
    
    var guestInfo by remember { mutableStateOf<GuestInfo?>(null) }
    var hasLoadedGuestInfo by remember { mutableStateOf(false) }
    var reminderState by remember { mutableStateOf(ReminderState.None) }
    
    // Persist unlock time to handle screen changes
    val unlockTimeKey = "checkout_unlock_time"
    var lastUnlockTime by remember { 
        mutableStateOf(sharedPreferences.getLong(unlockTimeKey, 0L)) 
    }

    var pendingRequestKey by remember { 
        mutableStateOf(sharedPreferences.getString("pending_extend_request_key", "") ?: "") 
    }
    var extendStatus by remember { mutableStateOf<String?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }
    
    // Fetch guest info
    LaunchedEffect(deviceID, branchId) {
        Log.d("CheckoutReminder", "LaunchedEffect: deviceID=$deviceID, branchId=$branchId")
        if (deviceID != null && branchId != null) {
            listenForGuestInfo(context, deviceID, branchId) { info ->
                Log.d("CheckoutReminder", "Received guestInfo: room=${info?.room}, name=${info?.fname}, dateco=${info?.dateco}")
                guestInfo = info
                hasLoadedGuestInfo = true
            }
        } else {
            Log.d("CheckoutReminder", "LaunchedEffect bypassed because deviceID or branchId is null!")
        }
    }

    // Reset checkout reminder status when a new guest (new folio ID) is loaded
    LaunchedEffect(guestInfo) {
        val currentFolio = guestInfo?.folio ?: 0
        if (currentFolio != 0) {
            val savedFolio = sharedPreferences.getInt("disabled_reminder_folio", 0)
            if (currentFolio != savedFolio) {
                sharedPreferences.edit()
                    .putBoolean("checkout_reminder_disabled", false)
                    .putInt("disabled_reminder_folio", currentFolio)
                    .putString("pending_extend_request_key", "")
                    .apply()
                pendingRequestKey = ""
            }
        }
    }

    // Listen to Firebase request status changes
    DisposableEffect(pendingRequestKey, branchId) {
        if (pendingRequestKey.isNotEmpty() && branchId != null) {
            val ref = FirebaseDatabase.getInstance().reference
                .child("BRANCHES").child(branchId).child("REQUEST").child(pendingRequestKey)
            
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val status = snapshot.child("status").getValue(String::class.java) ?: "open"
                        extendStatus = status
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("CheckoutReminder", "Error listening to request: ${error.message}")
                }
            }
            ref.addValueEventListener(listener)
            
            onDispose {
                ref.removeEventListener(listener)
            }
        } else {
            extendStatus = null
            onDispose {}
        }
    }

    // Show status dialog when a request becomes pending, confirmed, or rejected
    LaunchedEffect(pendingRequestKey) {
        if (pendingRequestKey.isNotEmpty()) {
            showStatusDialog = true
        }
    }

    LaunchedEffect(extendStatus) {
        if (extendStatus == "confirm" || extendStatus == "confirmed" || extendStatus == "done" || 
            extendStatus == "reject" || extendStatus == "rejected" || extendStatus == "cancel" || extendStatus == "cancelled") {
            showStatusDialog = true
        }
    }
    
    // Check Status Loop
    LaunchedEffect(hasLoadedGuestInfo, guestInfo, lastUnlockTime) {
        val FORCE_DEV_MODE = true // Ubah ke false jika ingin menggunakan data real-time jam 11:30
        Log.d("CheckoutReminder", "Check Status Loop: hasLoadedGuestInfo=$hasLoadedGuestInfo, guestInfoIsNull=${guestInfo == null}, lastUnlockTime=$lastUnlockTime")
        
        if (hasLoadedGuestInfo) {
            if (guestInfo == null) {
                // Kamar tidak ada tamu / kosong -> Langsung Kunci
                Log.d("CheckoutReminder", "GuestInfo is null, locking room (Expired)")
                reminderState = ReminderState.Expired
            } else {
                while (true) {
                    val reminderDisabled = sharedPreferences.getBoolean("checkout_reminder_disabled", false)
                    if (reminderDisabled) {
                        reminderState = ReminderState.None
                        delay(5000)
                        continue
                    }

                    val now = Calendar.getInstance()
                    
                    val isPast = isCheckoutDatePast(guestInfo!!.dateco)
                    val isToday = isCheckoutDateToday(guestInfo!!.dateco)
                    Log.d("CheckoutReminder", "Loop run - Dateco: ${guestInfo!!.dateco}, isPast: $isPast, isToday: $isToday")
                    
                     if (isPast) {
                          // Tanggal checkout sudah lewat kemarin atau sebelumnya
                          val timeSinceUnlock = now.timeInMillis - lastUnlockTime
                          val ONE_HOUR_MS = 3600000L // 1 Hour
                          Log.d("CheckoutReminder", "isPast = true. timeSinceUnlock = $timeSinceUnlock ms")
                          
                          if (timeSinceUnlock > ONE_HOUR_MS) {
                              reminderState = ReminderState.Expired
                          } else {
                              reminderState = ReminderState.None
                          }
                      } else if (FORCE_DEV_MODE) {
                          // MODE DEV: Langsung bypass ke Warning dengan jeda dismiss 2 menit
                          val lastDismissTime = sharedPreferences.getLong("checkout_warning_dismiss_time", 0L)
                          val TWO_MINUTES_MS = 120000L // 2 Menit (120,000 ms)
                          val timeSinceDismiss = System.currentTimeMillis() - lastDismissTime
                          
                          if (timeSinceDismiss > TWO_MINUTES_MS) {
                              reminderState = ReminderState.Warning
                          } else {
                              reminderState = ReminderState.None
                          }
                      } else {
                          // MODE REAL-TIME (JAM 11:30 & 12:00)
                          if (isToday) {
                             val warningStart = Calendar.getInstance().apply {
                                  set(Calendar.HOUR_OF_DAY, 11)
                                  set(Calendar.MINUTE, 30)
                                  set(Calendar.SECOND, 0)
                             }
                             val checkoutDeadline = Calendar.getInstance().apply {
                                  set(Calendar.HOUR_OF_DAY, 12) // Critical Deadline
                                  set(Calendar.MINUTE, 0)
                                  set(Calendar.SECOND, 0)
                             }
                             
                             if (now.after(checkoutDeadline)) {
                                  // Check if unlocked recently (1 Hour Logic)
                                  val timeSinceUnlock = now.timeInMillis - lastUnlockTime
                                  val ONE_HOUR_MS = 3600000L // 1 Hour
                                  
                                  if (timeSinceUnlock > ONE_HOUR_MS) {
                                      reminderState = ReminderState.Expired
                                  } else {
                                      // Within grace period
                                      reminderState = ReminderState.None
                                  }
                             } else if (now.after(warningStart)) {
                                  val lastDismissTime = sharedPreferences.getLong("checkout_warning_dismiss_time", 0L)
                                  val TWO_MINUTES_MS = 120000L // 2 Menit
                                  val timeSinceDismiss = System.currentTimeMillis() - lastDismissTime
                                  
                                  if (timeSinceDismiss > TWO_MINUTES_MS) {
                                      reminderState = ReminderState.Warning
                                  } else {
                                      reminderState = ReminderState.None
                                  }
                             } else {
                                  reminderState = ReminderState.None
                             }
                         } else {
                             reminderState = ReminderState.None
                         }
                     }
                    
                    delay(5000) // Check status setiap 5 detik agar lebih responsif saat testing
                }
            }
        }
    }

    // UI Rendering
    when (reminderState) {
        ReminderState.Warning -> {
            CheckoutReminderDialog(
                guestName = guestInfo?.fname ?: "Guest",
                roomNumber = guestInfo?.room ?: "",
                checkoutDate = guestInfo?.dateco ?: "Today",
                onDismiss = { 
                    // Simpan waktu dismissal ke SharedPreferences
                    sharedPreferences.edit().putLong("checkout_warning_dismiss_time", System.currentTimeMillis()).apply()
                    reminderState = ReminderState.None
                },
                onExtendSubmitted = { reqKey ->
                    pendingRequestKey = reqKey
                    reminderState = ReminderState.None
                }
            )
        }
        ReminderState.Expired -> {
            CheckoutBlockerDialog(
                guestName = guestInfo?.fname ?: "Dev Guest",
                onUnlock = { 
                    val currentTime = System.currentTimeMillis()
                    lastUnlockTime = currentTime
                    // Save to SharedPreferences
                    sharedPreferences.edit().putLong(unlockTimeKey, currentTime).apply()
                    reminderState = ReminderState.None
                }
            )
        }
        else -> {}
    }

    // Render Extend Status Dialog
    if (showStatusDialog && pendingRequestKey.isNotEmpty()) {
        ExtendStatusDialog(
            status = extendStatus ?: "open",
            onDismiss = {
                showStatusDialog = false
                if (extendStatus != "open" && extendStatus != null) {
                    pendingRequestKey = ""
                    sharedPreferences.edit().putString("pending_extend_request_key", "").apply()
                }
            }
        )
    }
}

/**
 * Fungsi untuk mendengarkan perubahan guest info dari Firebase
 */
private fun listenForGuestInfo(
    context: Context,
    deviceID: String,
    branchId: String,
    onGuestInfoChange: (GuestInfo?) -> Unit
) {
    val database: DatabaseReference = Firebase.database.reference
    
    // First get the room number from DEVICES node
    val deviceRef = database.child("DEVICES").child(deviceID)
    
    deviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(deviceSnapshot: DataSnapshot) {
            if (deviceSnapshot.exists()) {
                val roomNumber = deviceSnapshot.child("room").getValue(String::class.java)
                
                if (roomNumber != null) {
                    // Look up guest info through BRANCHES node
                    val guestRef = database.child("BRANCHES")
                        .child(branchId)
                        .child("FOGUEST")
                        .child(roomNumber)
                    
                    guestRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(guestSnapshot: DataSnapshot) {
                            if (guestSnapshot.exists()) {
                                val guestInfo = guestSnapshot.getValue(GuestInfo::class.java)
                                onGuestInfoChange(guestInfo)
                            } else {
                                onGuestInfoChange(null)
                            }
                        }
                        
                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("CheckoutReminder", "Error retrieving guest data: ${databaseError.message}")
                            onGuestInfoChange(null)
                        }
                    })
                } else {
                    onGuestInfoChange(null)
                }
            } else {
                onGuestInfoChange(null)
            }
        }
        
        override fun onCancelled(databaseError: DatabaseError) {
            Log.e("CheckoutReminder", "Error retrieving device data: ${databaseError.message}")
            onGuestInfoChange(null)
        }
    })
}

/**
 * Fungsi helper untuk mengecek apakah checkout date adalah hari ini
 */
private fun isCheckoutDateToday(checkoutDate: String): Boolean {
    if (checkoutDate.isEmpty()) {
        return false
    }
    
    // Parse checkout date - coba beberapa format yang mungkin digunakan
    val dateFormats = listOf(
        "dd/MM/yyyy",
        "yyyy-MM-dd",
        "dd-MM-yyyy",
        "MM/dd/yyyy",
        "dd MMM yyyy",
        "EEEE, dd MMMM yyyy"
    )
    
    var checkoutCalendar: Calendar? = null
    for (format in dateFormats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            val date = sdf.parse(checkoutDate)
            if (date != null) {
                checkoutCalendar = Calendar.getInstance().apply {
                    time = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                break
            }
        } catch (e: Exception) {
            // Try next format
        }
    }
    
    if (checkoutCalendar == null) {
        Log.w("CheckoutReminder", "Could not parse checkout date: $checkoutDate")
        return false
    }
    
    // Cek apakah checkout date adalah hari ini
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val isCheckoutToday = checkoutCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            checkoutCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    
    return isCheckoutToday
}

/**
 * Fungsi helper untuk mengecek apakah checkout date sudah terlewat (kemarin atau sebelumnya)
 */
private fun isCheckoutDatePast(checkoutDate: String): Boolean {
    if (checkoutDate.isEmpty()) {
        return false
    }
    
    val dateFormats = listOf(
        "dd/MM/yyyy",
        "yyyy-MM-dd",
        "dd-MM-yyyy",
        "MM/dd/yyyy",
        "dd MMM yyyy",
        "EEEE, dd MMMM yyyy"
    )
    
    var checkoutCalendar: Calendar? = null
    for (format in dateFormats) {
        try {
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            val date = sdf.parse(checkoutDate)
            if (date != null) {
                checkoutCalendar = Calendar.getInstance().apply {
                    time = date
                    // Set ke akhir hari checkout tersebut (23:59:59)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                break
            }
        } catch (e: Exception) {
            // Try next format
        }
    }
    
    if (checkoutCalendar == null) {
        return false
    }
    
    val now = Calendar.getInstance()
    return now.after(checkoutCalendar)
}


/**
 * Fungsi untuk mengecek apakah perlu menampilkan reminder checkout
 * Reminder akan muncul mulai jam 11:30 sampai jam checkout lewat (15:00)
 * Hanya untuk tamu yang checkout hari ini
 */
private fun shouldShowCheckoutReminder(
    context: Context,
    checkoutDate: String
): Boolean {
    // Cek apakah checkout date adalah hari ini
    val isCheckoutToday = isCheckoutDateToday(checkoutDate)
    
    if (!isCheckoutToday) {
        Log.d("CheckoutReminder", "Checkout is not today, skipping reminder")
        return false
    }
    
    val now = Calendar.getInstance()
    
    // Reminder mulai muncul setelah jam 11:30
    val reminderStartTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 11)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    // Reminder berhenti setelah jam checkout lewat (15:00 / 3:00 PM)
    val checkoutTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 18) // Jam checkout 18:00 (6:00 PM)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    // Tampilkan reminder jika sudah jam 11:30 atau lebih, dan masih sebelum jam checkout lewat
    val shouldShow = now.after(reminderStartTime) && now.before(checkoutTime)
    
    // Logging untuk debugging
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    Log.d("CheckoutReminder", "=== Checkout Reminder Check ===")
    Log.d("CheckoutReminder", "Checkout date: $checkoutDate")
    Log.d("CheckoutReminder", "Is checkout today: $isCheckoutToday")
    Log.d("CheckoutReminder", "Current time: ${timeFormat.format(now.time)}")
    Log.d("CheckoutReminder", "Reminder start time (11:30): ${timeFormat.format(reminderStartTime.time)}")
    Log.d("CheckoutReminder", "Checkout time (15:00): ${timeFormat.format(checkoutTime.time)}")
    Log.d("CheckoutReminder", "After start time (11:30): ${now.after(reminderStartTime)}")
    Log.d("CheckoutReminder", "Before checkout time (15:00): ${now.before(checkoutTime)}")
    Log.d("CheckoutReminder", "Should show: $shouldShow")
    Log.d("CheckoutReminder", "===============================")
    
    return shouldShow
}

/**
 * Dialog untuk menampilkan reminder checkout dengan pilihan Check-Out atau Extend
 */
@Composable
fun CheckoutReminderDialog(
    guestName: String,
    roomNumber: String,
    checkoutDate: String,
    onDismiss: () -> Unit,
    onExtendSubmitted: (String) -> Unit
) {
    val context = LocalContext.current
    var animateIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cardFocusRequester = remember { FocusRequester() }
    val closeButtonFocusRequester = remember { FocusRequester() }
    var isCloseFocused by remember { mutableStateOf(false) }

    var isCheckOutFocused by remember { mutableStateOf(false) }
    var isExtendFocused by remember { mutableStateOf(false) }
    var extraDays by remember { mutableStateOf(1) }
    var isMinusFocused by remember { mutableStateOf(false) }
    var isPlusFocused by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    val checkOutFocusRequester = remember { FocusRequester() }
    val extendFocusRequester = remember { FocusRequester() }
    val minusFocusRequester = remember { FocusRequester() }
    val plusFocusRequester = remember { FocusRequester() }
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        animateIn = true
    }

    LaunchedEffect(animateIn) {
        if (animateIn) {
            delay(100)
            checkOutFocusRequester.requestFocus()
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
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusProperties { canFocus = false }
        ) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f * animatedAlpha))
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
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Checkout Reminder",
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
                                    text = "",
                                    color = if (isCloseFocused) Color(0xFF071434) else Color.White.copy(alpha = 0.55f),
                                    style = TextStyle(fontSize = 18.sp),
                                    fontFamily = FontFamily(Font(R.font.icons))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Body Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(
                                modifier = Modifier.weight(1.1f),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = ParagraphStyle(textAlign = TextAlign.Start)) {
                                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)) {
                                                append("This is a reminder that your checkout time is at 12:00 PM today. Please ensure all your belongings are packed and ready for checkout.\n")
                                            }
                                            withStyle(style = SpanStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)) {
                                                append("Ini adalah pengingat bahwa waktu checkout Anda adalah pukul 12:00 siang hari ini. Mohon pastikan semua barang Anda sudah dikemas dan siap untuk checkout.")
                                            }
                                        }
                                    },
                                    color = Color.White,
                                    lineHeight = 28.sp
                                )

                                Spacer(modifier = Modifier.height(28.dp))

                                // ── Action Row: Check-Out | − 1 + | Extend ──
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Check-Out Button
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(
                                                if (isCheckOutFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.08f)
                                            )
                                            .onFocusChanged { isCheckOutFocused = it.isFocused }
                                            .focusRequester(checkOutFocusRequester)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                dismissWithAnimation()
                                            }
                                            .focusable()
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Check-Out",
                                            color = if (isCheckOutFocused) Color(0xFF071434) else Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Minus Button
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isMinusFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.1f)
                                            )
                                            .onFocusChanged { isMinusFocused = it.isFocused }
                                            .focusRequester(minusFocusRequester)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { if (extraDays > 1) extraDays-- }
                                            .focusable(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "−",
                                            color = if (isMinusFocused) Color(0xFF071434) else Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Day Count Display
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "$extraDays",
                                            color = Color.White,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Hari",
                                            color = Color.White.copy(alpha = 0.55f),
                                            fontSize = 12.sp
                                        )
                                    }

                                    // Plus Button
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPlusFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.1f)
                                            )
                                            .onFocusChanged { isPlusFocused = it.isFocused }
                                            .focusRequester(plusFocusRequester)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { if (extraDays < 30) extraDays++ }
                                            .focusable(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+",
                                            color = if (isPlusFocused) Color(0xFF071434) else Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Extend Button
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(
                                                if (isExtendFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.08f)
                                            )
                                            .onFocusChanged { isExtendFocused = it.isFocused }
                                            .focusRequester(extendFocusRequester)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                if (!isSending) {
                                                    isSending = true
                                                    sendExtendRequest(
                                                        context = context,
                                                        guestName = guestName,
                                                        roomNumber = roomNumber,
                                                        extraDays = extraDays,
                                                        checkoutDate = checkoutDate,
                                                        onSuccess = { reqKey ->
                                                            // Save to SharedPreferences that extend was requested and disable reminder
                                                            sharedPreferences.edit()
                                                                .putBoolean("checkout_reminder_disabled", true)
                                                                .apply()
                                                            onExtendSubmitted(reqKey)
                                                        }
                                                    )
                                                    dismissWithAnimation()
                                                }
                                            }
                                            .focusable()
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isSending) "Sending..." else "Extend",
                                            color = if (isExtendFocused) Color(0xFF071434) else Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(32.dp))

                            // Warning Lottie Icon
                            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.check_out))
                            val progress by animateLottieCompositionAsState(
                                composition = composition,
                                iterations = LottieConstants.IterateForever
                            )
                            Box(
                                modifier = Modifier.size(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LottieAnimation(
                                    composition = composition,
                                    progress = { progress },
                                    modifier = Modifier.fillMaxSize()
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
fun ExtendStatusDialog(
    status: String,
    onDismiss: () -> Unit
) {
    var animateIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val closeFocusRequester = remember { FocusRequester() }
    var isCloseFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animateIn = true
    }

    LaunchedEffect(animateIn) {
        if (animateIn) {
            delay(100)
            closeFocusRequester.requestFocus()
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
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
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
                    .background(Color.Black.copy(alpha = 0.5f * animatedAlpha))
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
                        .height(300.dp)
                        .padding(20.dp),
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val (title, message) = when (status) {
                            "open" -> Pair(
                                "Permintaan Diproses",
                                "Permintaan perpanjangan menginap Anda sedang diproses oleh receptionist. Mohon tunggu sebentar."
                            )
                            "confirm", "confirmed", "done" -> Pair(
                                "Permintaan Disetujui",
                                "Permintaan perpanjangan menginap Anda telah DISETUJUI. Terima kasih."
                            )
                            else -> Pair(
                                "Permintaan Ditolak",
                                "Mohon maaf, permintaan perpanjangan menginap Anda ditolak oleh receptionist. Silakan hubungi receptionist."
                            )
                        }

                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = message,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .width(180.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (isCloseFocused) Color(0xFFCFDFED) else Color.White.copy(alpha = 0.08f))
                                .onFocusChanged { isCloseFocused = it.isFocused }
                                .focusRequester(closeFocusRequester)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { dismissWithAnimation() }
                                .focusable(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (status == "open") "Tutup" else "OK",
                                color = if (isCloseFocused) Color(0xFF071434) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutBlockerDialog(
    guestName: String,
    onUnlock: () -> Unit
) {
    val CORRECT_PIN = "1234" // Hardcoded for blocking override
    var pinInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = { /* Blocked */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(enabled = false) { }
                ,
            contentAlignment = Alignment.Center
        ) {
            // Invisible TextField to capture input
            OutlinedTextField(
                value = pinInput,
                onValueChange = { },
                readOnly = true,
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            val keyCode = event.nativeKeyEvent.keyCode
                            when (keyCode) {
                                in android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9 -> {
                                    if (pinInput.length < 4) {
                                        val digit = (keyCode - android.view.KeyEvent.KEYCODE_0).toString()
                                        val newPin = pinInput + digit
                                        pinInput = newPin
                                        showError = false
                                        if (newPin.length == 4) {
                                            if (newPin == CORRECT_PIN) {
                                                onUnlock()
                                            } else {
                                                pinInput = ""
                                                showError = true
                                            }
                                        }
                                    }
                                    true
                                }
                                in android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9 -> {
                                    if (pinInput.length < 4) {
                                        val digit = (keyCode - android.view.KeyEvent.KEYCODE_NUMPAD_0).toString()
                                        val newPin = pinInput + digit
                                        pinInput = newPin
                                        showError = false
                                        if (newPin.length == 4) {
                                            if (newPin == CORRECT_PIN) {
                                                onUnlock()
                                            } else {
                                                pinInput = ""
                                                showError = true
                                            }
                                        }
                                    }
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_DEL -> {
                                    if (pinInput.isNotEmpty()) {
                                        pinInput = pinInput.dropLast(1)
                                        showError = false
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            )

            // UI Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Lock Icon or similar
                Text(
                    text = "\uf023", // Lock icon
                    fontSize = 32.sp,
                    color = Color.White,
                    fontFamily = FontFamily(Font(R.font.icons))
                )

                Text(
                    text = "Checkout",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Please contact reception to extend your stay.",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(50)) // Circle
                                .background(
                                    if (index < pinInput.length) Color.White else Color.White.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                if (showError) {
                    Text(
                        text = "Incorrect PIN",
                        color = Color.Red,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Text(
                    text = "Enter PIN to Unlock",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

/**
 * Kirim permintaan extend stay ke Firebase REQUEST, NOTIFICATIONS, dan FCM.
 * Telegram diteruskan oleh backend via FCM topic.
 */
fun sendExtendRequest(
    context: Context,
    guestName: String,
    roomNumber: String,
    extraDays: Int,
    checkoutDate: String,
    onSuccess: (String) -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null) ?: run {
        Log.e("CheckoutReminder", "branchId is null, cannot send extend request")
        return
    }
    val deviceID = sharedPreferences.getString("deviceID", null) ?: run {
        Log.e("CheckoutReminder", "deviceID is null, cannot send extend request")
        return
    }

    database.child("DEVICES").child(deviceID)
        .addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(deviceSnapshot: DataSnapshot) {
                val folioIdRaw = deviceSnapshot.child("folio").getValue(String::class.java)
                    ?: deviceSnapshot.child("folioId").getValue(String::class.java)
                val folioId = folioIdRaw?.toIntOrNull()

                val requestId = "${System.currentTimeMillis()}"
                val timeStamp = System.currentTimeMillis()

                // Date time request pakai datenow
                val now = Date()
                val currentDateNow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
                val currentTimeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)

                // Hitung until date co (new check-out date)
                val dateFormats = listOf(
                    "dd/MM/yyyy",
                    "yyyy-MM-dd",
                    "dd-MM-yyyy",
                    "MM/dd/yyyy",
                    "dd MMM yyyy",
                    "EEEE, dd MMMM yyyy"
                )
                var calendar = Calendar.getInstance()
                var parsedFormat = "dd/MM/yyyy"
                for (format in dateFormats) {
                    try {
                        val sdf = SimpleDateFormat(format, Locale.getDefault())
                        val parsedDate = sdf.parse(checkoutDate)
                        if (parsedDate != null) {
                            calendar.time = parsedDate
                            parsedFormat = format
                            break
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                calendar.add(Calendar.DAY_OF_YEAR, extraDays)
                val untilDateCo = SimpleDateFormat(parsedFormat, Locale.getDefault()).format(calendar.time)

                // Catatan instruksi receptionist + detail deskripsi until date co
                val extendNote = "Permintaan perpanjangan menginap $extraDays hari - Kamar $roomNumber ($guestName) s/d $untilDateCo. Catatan untuk staff receptionist: ganti kartu dan antarkan ke kamar. dan update reservasi di pms."

                // Push to REQUEST node
                val extendRequest = Request(
                    folioId = folioId,
                    guestName = guestName,
                    guestPhone = "",
                    guestRoom = roomNumber,
                    status = "open",
                    timestamp = timeStamp,
                    requestId = requestId,
                    selectedDate = currentDateNow,
                    selectedTime = currentTimeNow,
                    date = currentDateNow,
                    time = currentTimeNow,
                    requests = listOf(
                        GuestRequest(
                            request_title = "Extend Stay $extraDays Hari",
                            category = "EXTEND_STAY",
                            description = extendNote
                        )
                    ),
                    note = extendNote
                )

                val requestRef = database.child("BRANCHES").child(branchId).child("REQUEST").push()
                val requestKey = requestRef.key ?: requestId

                requestRef.setValue(extendRequest)
                    .addOnSuccessListener {
                        Log.d("CheckoutReminder", "Extend request saved to Firebase")
                        sharedPreferences.edit().putString("pending_extend_request_key", requestKey).apply()
                        onSuccess(requestKey)
                    }
                    .addOnFailureListener { e ->
                        Log.e("CheckoutReminder", "Failed to save extend request: ${e.message}")
                    }

                // Push NOTIFICATION to guest folio
                if (folioId != null) {
                    val notification = Notification(
                        id = requestId,
                        title = "Extend Stay",
                        message = "Your request to extend stay by $extraDays day(s) has been submitted.",
                        timestamp = timeStamp,
                        type = "GUEST_REQUEST"
                    )
                    database.child("BRANCHES").child(branchId)
                        .child("NOTIFICATIONS").child(folioId.toString())
                        .push().setValue(notification)
                }

                // FCM push — backend handles Telegram forwarding
                FcmHelper.sendFcmNotification(
                    context = context,
                    type = "REQUEST",
                    title = "Extend Stay Request",
                    bodyText = "Kamar $roomNumber - $guestName minta perpanjangan $extraDays hari s/d $untilDateCo",
                    additionalData = mapOf(
                        "requestId" to requestId,
                        "room" to roomNumber,
                        "guestName" to guestName,
                        "requestTitle" to "Extend Stay $extraDays Hari",
                        "note" to extendNote
                    )
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CheckoutReminder", "Failed to read device data: ${error.message}")
            }
        })
}
