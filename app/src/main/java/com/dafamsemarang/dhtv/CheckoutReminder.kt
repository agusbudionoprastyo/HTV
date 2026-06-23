package com.dafamsemarang.dhtv

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
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
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.delay
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
    
    // Fetch guest info
    LaunchedEffect(deviceID, branchId) {
        if (deviceID != null && branchId != null) {
            listenForGuestInfo(context, deviceID, branchId) { info ->
                guestInfo = info
                hasLoadedGuestInfo = true
            }
        }
    }
    
    // Check Status Loop
    LaunchedEffect(hasLoadedGuestInfo, guestInfo, lastUnlockTime) {
        val FORCE_DEV_MODE = true // Ubah ke false jika ingin menggunakan data real-time jam 11:30
        
        if (hasLoadedGuestInfo) {
            if (guestInfo == null) {
                // Kamar tidak ada tamu / kosong -> Langsung Kunci
                reminderState = ReminderState.Expired
            } else {
                while (true) {
                    val now = Calendar.getInstance()
                    
                    val isPast = isCheckoutDatePast(guestInfo!!.dateco)
                    val isToday = isCheckoutDateToday(guestInfo!!.dateco)
                    
                    if (FORCE_DEV_MODE) {
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
                        if (isPast) {
                            // Tanggal checkout sudah lewat kemarin atau sebelumnya
                            reminderState = ReminderState.Expired
                        } else if (isToday) {
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
 * Dialog untuk menampilkan reminder checkout
 */
@Composable
fun CheckoutReminderDialog(
    guestName: String,
    roomNumber: String,
    checkoutDate: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(540.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1E2026), shape = RoundedCornerShape(28.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Checkout Reminder",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Start
                        )

                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = ParagraphStyle(textAlign = TextAlign.Start)) {
                                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)) {
                                        append("This is a reminder that your checkout time is at 12:00 PM today. Please ensure all your belongings are packed and ready for checkout.\n\n")
                                    }
                                }
                                withStyle(style = ParagraphStyle(textAlign = TextAlign.Start)) {
                                    append("Ini adalah pengingat bahwa waktu checkout Anda adalah pukul 12:00 siang hari ini. Mohon pastikan semua barang Anda sudah dikemas dan siap untuk checkout.")
                                }
                            },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }

                    // Header Warning Icon (Lottie) di Kanan
                    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.check_out))
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever
                    )
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Confirm button
                var isButtonFocused by remember { mutableStateOf(false) }
                val focusRequester = remember { FocusRequester() }
                
                LaunchedEffect(Unit) {
                    delay(100)
                    try {
                        focusRequester.requestFocus()
                    } catch (e: Exception) {}
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isButtonFocused) Color.White else Color.White.copy(alpha = 0.05f))
                            .onFocusChanged { isButtonFocused = it.isFocused }
                            .focusRequester(focusRequester)
                            .clickable { onDismiss() }
                            .focusable(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "OK",
                            color = if (isButtonFocused) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
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

