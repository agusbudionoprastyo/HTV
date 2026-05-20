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
    var reminderState by remember { mutableStateOf(ReminderState.None) }
    
    // Persist unlock time to handle screen changes
    val unlockTimeKey = "checkout_unlock_time"
    var lastUnlockTime by remember { 
        mutableStateOf(sharedPreferences.getLong(unlockTimeKey, 0L)) 
    }
    
    var warningDismissed by remember { mutableStateOf(false) } // Track if warning was dismissed
    
    // Fetch guest info
    LaunchedEffect(deviceID, branchId) {
        if (deviceID != null && branchId != null) {
            listenForGuestInfo(context, deviceID, branchId) { info ->
                guestInfo = info
            }
        }
    }
    
    // Check Status Loop
    LaunchedEffect(guestInfo, lastUnlockTime, warningDismissed) {
        if (guestInfo != null) {
            while (true) {
                val now = Calendar.getInstance()
                
                // Parse date relative to Today
                val isToday = isCheckoutDateToday(guestInfo!!.dateco)
                
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
                            warningDismissed = false 
                        } else {
                            // Within grace period
                            reminderState = ReminderState.None
                        }
                    } else if (now.after(warningStart)) {
                        if (!warningDismissed) {
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
                
                delay(10000) // Check every 10 seconds
            }
        }
    }

    // UI Rendering
    when (reminderState) {
        ReminderState.Warning -> {
            CheckoutReminderDialog(
                guestName = guestInfo!!.fname,
                roomNumber = guestInfo!!.room,
                checkoutDate = guestInfo!!.dateco,
                onDismiss = { 
                    warningDismissed = true 
                    reminderState = ReminderState.None
                }
            )
        }
        ReminderState.Expired -> {
            CheckoutBlockerDialog(
                guestName = guestInfo!!.fname,
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
            dismissOnClickOutside = false
        )
    ) {
        // Glassmorphism container dengan backdrop blur effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .liquidGlass(
                    cornerRadius = 24.dp,
                    glassColor = Color.White,
                    alphaInitial = 0.3f,
                    alphaFinal = 0f,
                    hasTopRimLight = true,
                    isFullBorder = false,
                    borderWidth = 1.dp
                )
        ) {
            // (Inner glow removed - handled by liquidGlass)
            
            // Close button di pojok kanan atas
            // Close button (Liquid Glass Focused)
            var isCloseFocused by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp) // Smaller size
                    .clip(RoundedCornerShape(18.dp))
                    .onFocusChanged { isCloseFocused = it.isFocused }
                    .focusable() 
                    .clickable { onDismiss() }
                    .liquidGlass(
                        cornerRadius = 18.dp,
                        glassColor = if (isCloseFocused) Color.White else Color.Transparent, // Water Style
                        alphaInitial = if (isCloseFocused) 0.3f else 0f,
                        alphaFinal = if (isCloseFocused) 0.1f else 0f,
                        hasTopRimLight = true, // Droplet Lighting
                        isFullBorder = false,
                        borderAlpha = if (isCloseFocused) 0.8f else 0f
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\uF057", // Close icon
                    fontSize = 20.sp, // Smaller icon
                    color = if (isCloseFocused) Color.White else Color.White.copy(alpha = 0.5f), // Black Icon
                    fontFamily = FontFamily(Font(R.font.icons))
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) { 
                // Title dengan glassmorphism text style
                Text(
                    text = "Checkout Reminder",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                
                // Message dengan glassmorphism text style
                Text(
                    text = buildAnnotatedString {
                        // Nama tamu center aligned dan bold
                        withStyle(style = ParagraphStyle(textAlign = TextAlign.Left)) {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)) {
                                append("This is a reminder that your checkout time is at 12:00 PM today. Please ensure all your belongings are packed and ready for checkout.\n")
                            }
                        }
                        // Teks di bawah left aligned
                        withStyle(style = ParagraphStyle(textAlign = TextAlign.Left)) {
                            append("Ini adalah pengingat bahwa waktu checkout Anda adalah pukul 12:00 siang hari ini. Mohon pastikan semua barang Anda sudah dikemas dan siap untuk checkout.")
                        }
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 20.sp,
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Countdown checkout timer
                CheckoutCountdown()
            }
        }
    }
}

/**
 * Composable untuk menampilkan countdown checkout timer
 */
@Composable
fun CheckoutCountdown() {
    // Fungsi untuk menghitung waktu tersisa
    fun calculateTimeRemaining(): String {
        val now = Calendar.getInstance()
        val checkoutTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12) // Jam checkout 12:00 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Jika sudah lewat jam 12:00 hari ini, set ke 18:00 (6:00 PM) hari ini
        // Atau jika sudah lewat jam 18:00, set ke 12:00 esok hari
        if (now.after(checkoutTime)) {
            val checkoutTime18 = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 12) // Jam checkout 18:00 (6:00 PM)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            if (now.after(checkoutTime18)) {
                // Sudah lewat jam 18:00, set ke 12:00 esok hari
                checkoutTime.add(Calendar.DAY_OF_YEAR, 1)
            } else {
                // Masih sebelum jam 18:00, gunakan jam 18:00 hari ini
                checkoutTime.set(Calendar.HOUR_OF_DAY, 12)
            }
        }
        
        val diff = checkoutTime.timeInMillis - now.timeInMillis
        
        if (diff > 0) {
            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
            val seconds = (diff % (1000 * 60)) / 1000
            
            return String.format(
                "%02d:%02d:%02d",
                hours,
                minutes,
                seconds
            )
        } else {
            return "00:00:00"
        }
    }
    
    // Initialize dengan nilai awal
    var timeRemaining by remember { mutableStateOf(calculateTimeRemaining()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            timeRemaining = calculateTimeRemaining()
            delay(1000) // Update setiap detik
        }
    }
    
    if (timeRemaining.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
//                .liquidGlass(
//                    cornerRadius = 12.dp,
//                    glassColor = Color.White,
//                    alphaInitial = 0.05f,
//                    hasTopRimLight = true,
//                    isFullBorder = false,
//                    borderAlpha = 0.3f
//                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Time until checkout",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Waktu hingga checkout",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = timeRemaining,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
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

