package com.dafamsemarang.dhtv

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.dafamsemarang.dhtv.ui.components.UpdateProgress
import com.dafamsemarang.dhtv.ui.theme.dhtvTheme
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


class MainActivity : ComponentActivity(), DeviceManager.DeviceStatusListener {
    private var deviceManager: DeviceManager? = null
    private var shouldShowPairing by mutableStateOf(false)
    private lateinit var updateManager: UpdateManager
    private var updateInfo by mutableStateOf<UpdateManager.UpdateInfo?>(null)
    private var showAccessibilityPrompt by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")
        
        // Keep screen on and prevent sleep
        keepScreenAwake()
        
        // Auto-configure the OS screensaver immediately on launch (requires system permissions)
        ScreenSaverManager.autoConfigureSystemScreensaver(this)
        autoEnableAccessibilityService()
        
        handleIntent(intent)
        
        // ULTIMATE OS-LEVEL PERFORMANCE OVERRIDE:
        // This instructs the Android Window Manager that our entire application window is exempt 
        // from accessibility scanning. Instantly forces Compose to dismantle ALL heavy accessibility 
        // calculations, yielding 100% lag-free UI operation even when services are active globally.
        try {
            window.decorView.importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to apply DecorView override", e)
        }
        
        updateManager = UpdateManager(this)

        setContent {
            dhtvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isPaired by remember { mutableStateOf(false) }

                    // Check for updates
                    LaunchedEffect(Unit) {
                        Log.d("MainActivity", "Starting update check flow")
                        updateManager.checkForUpdate().collect { info ->
                            Log.d("MainActivity", "Received update info: $info")
                            updateInfo = info
                        }
                    }

                    // Collect download progress
                    val downloadProgress by updateManager.downloadProgress.collectAsState()
                    val isDownloading by updateManager.isDownloading.collectAsState()

                    // Show update progress dialog
                    UpdateProgress(
                        progress = downloadProgress,
                        isDownloading = isDownloading,
                        onDismiss = { /* Progress dialog cannot be dismissed */ }
                    )

                    // Show update dialog if update is available
                    updateInfo?.let { info ->
                        Log.d("MainActivity", "Showing update dialog for version ${info.versionName}")
                        AlertDialog(
                            onDismissRequest = { 
                                Log.d("MainActivity", "Update dialog dismissed")
                                updateInfo = null 
                            },
                            title = { Text("Update Available") },
                            text = { Text("New version ${info.versionName} is available.\n\n${info.releaseNotes}") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        Log.d("MainActivity", "Update button clicked")
                                        lifecycleScope.launch {
                                            try {
                                                updateManager.downloadAndInstallUpdate(info, this@MainActivity)
                                            } catch (e: Exception) {
                                                Log.e("MainActivity", "Error during update: ${e.message}", e)
                                                Toast.makeText(this@MainActivity, "Error during update: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Update Now")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { 
                                        Log.d("MainActivity", "Later button clicked")
                                        updateInfo = null 
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Later")
                                }
                            }
                        )
                    }

                    LaunchedEffect(Unit) {
                        Log.d("MainActivity", "Checking device pairing status")
                        val tempDeviceManager = DeviceManager(this@MainActivity)
                        tempDeviceManager.setDeviceStatusListener(this@MainActivity)
                        isPaired = tempDeviceManager.isDevicePaired()
                        if (isPaired) {
                            deviceManager = tempDeviceManager
                            Log.d("MainActivity", "Device is paired, DeviceManager initialized")
                            // STB BOOT ONBOARDING: Open screensaver immediately on first boot/launch!
                            ScreenSaverManager.isScreenSaverActive = true
                        } else {
                            Log.d("MainActivity", "Device is not paired yet")
                            // Log current device information
                            val deviceInfo = tempDeviceManager.getDeviceInformation()
                            Log.d("MainActivity", "Current device information: $deviceInfo")
                        }
                    }

                    LaunchedEffect(isPaired) {
                        if (isPaired) {
                            Log.d("MainActivity", "Device paired. Starting screensaver listener.")
                            ScreenSaverManager.startListening(this@MainActivity)
                        }
                    }

                    val isAppLocked by remember { DataRepository.isAppLocked }
                    val lockMessage by remember { DataRepository.lockMessage }

                    if (isPaired && isAppLocked) {
                        LockOverlay(message = lockMessage)
                    } else if (shouldShowPairing) {
                        Log.d("MainActivity", "Showing pairing screen")
                        PairingScreen(
                            onDeviceIdSaved = { deviceId ->
                                Log.d("MainActivity", "Device ID saved: $deviceId")
                                // Handle device ID saved
                                shouldShowPairing = false
                                // Initialize new DeviceManager with paired device
                                deviceManager = DeviceManager(this@MainActivity).apply {
                                    setDeviceStatusListener(this@MainActivity)
                                }
                                Log.d("MainActivity", "New DeviceManager initialized after pairing")
                            },
                            sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE),
                            deviceManager = DeviceManager(this@MainActivity)
                        )
                    } else {
                        AppNavigation()
                    }

                    // Render screensaver inside the app overlay when active
                    val isScreenSaverActive = ScreenSaverManager.isScreenSaverActive && !isAppLocked
                    LaunchedEffect(isScreenSaverActive) {
                        val window = this@MainActivity.window
                        try {
                            val decorView = window.decorView
                            val controller = androidx.core.view.WindowCompat.getInsetsController(window, decorView)
                            if (isScreenSaverActive) {
                                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            } else {
                                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
                                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error setting fullscreen flags: ${e.message}")
                        }
                    }

                    if (isScreenSaverActive) {
                        ScreenSaverOverlay()
                    }
                }
            }
        }
    }

    override fun onPairingModeRequired() {
        Log.d("MainActivity", "Pairing mode required, clearing data and restarting to pairing screen")
        
        // Clear pairing information from SharedPreferences
        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        sharedPrefs.edit().run {
            remove("deviceID")
            remove("branchId")
            remove("room")
            apply()
        }
        
        // Clear all cached files (images, audio, videos) to completely clean up local storage
        try {
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            Log.d("MainActivity", "Successfully cleared application cache")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to clear cache: ${e.message}")
        }
        
        // Restart the activity to completely reset navigation and clear memory references
        val intent = intent
        finish()
        startActivity(intent)
    }

    override fun onPairingSuccess() {
        Log.d("MainActivity", "Device paired successfully")
        shouldShowPairing = false
        // Initialize new DeviceManager with paired device
        deviceManager = DeviceManager(this).apply {
            setDeviceStatusListener(this@MainActivity)
        }
    }

    override fun onPairingFailed(error: String) {
        Log.e("MainActivity", "Pairing failed: $error")
        // You might want to show an error message to the user here
    }

    private fun keepScreenAwake() {
        try {
            // Ensure screenshot is allowed by removing FLAG_SECURE if it exists
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            Log.d("MainActivity", "Screenshot enabled, WakeLock disabled to let OS Daydream trigger naturally")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error enabling screenshot: ${e.message}")
        }
    }

    private fun autoEnableAccessibilityService() {
        try {
            val serviceComponent = ComponentName(this, LauncherAccessibilityService::class.java).flattenToString()
            val enabledServicesSetting = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            if (!enabledServicesSetting.contains(serviceComponent)) {
                val newEnabledServices = if (enabledServicesSetting.isEmpty()) {
                    serviceComponent
                } else {
                    "$enabledServicesSetting:$serviceComponent"
                }
                Settings.Secure.putString(
                    contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    newEnabledServices
                )
                Settings.Secure.putInt(
                    contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
                )
                Log.d("MainActivity", "Successfully auto-enabled accessibility service via Secure Settings!")
            } else {
                Log.d("MainActivity", "Accessibility service is already enabled.")
            }
        } catch (e: SecurityException) {
            Log.w("MainActivity", "Auto-enable accessibility bypassed: Requires system privilege or WRITE_SECURE_SETTINGS permission.")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error auto-enabling accessibility service: ${e.message}", e)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, LauncherAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Could not open accessibility settings", e)
            Toast.makeText(this, "Could not open accessibility settings.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openHomeSettings() {
        try {
            // Direct attempt to open Default Home Settings (Android 10+)
            val intent = Intent(Settings.ACTION_HOME_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback for Android TV general settings if home intent fails
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                Toast.makeText(this, "Find 'Home App' or 'Default Apps' in Settings", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Log.e("MainActivity", "Could not open settings", e2)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Ensure screenshot is always enabled
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Ensure screenshot is enabled when window gains focus
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val navigateTo = intent?.getStringExtra("navigate_to")
        if (navigateTo != null) {
            Log.d("MainActivity", "Handling screen saver external navigation: $navigateTo")
            NavigationTrigger.pendingRoute = navigateTo
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (DataRepository.isAppLocked.value) {
            // Block all input actions (like D-pad navigation, back button, home triggers) when app is locked due to expired subscription
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d("MainActivity", "Global BACK key pressed - Forcing native Screensaver via Somnambulator")
            try {
                val intent = Intent().apply {
                    setClassName("com.android.systemui", "com.android.systemui.Somnambulator")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                return true // Consume key event globally
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to launch native screensaver from global key listener: ${e.message}")
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Update device status to offline before destroying
        deviceManager?.handleDeviceShutdown()
        deviceManager = null
    }

    override fun onStop() {
        super.onStop()
        // Update device status to offline when app goes to background
        deviceManager?.handleDeviceShutdown()
    }
}

@androidx.compose.runtime.Composable
fun LockOverlay(message: String) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val deviceId = sharedPrefs.getString("deviceID", "Unknown") ?: "Unknown"
    val branchId = sharedPrefs.getString("branchId", "Unknown") ?: "Unknown"
    val room = sharedPrefs.getString("room", "Unknown") ?: "Unknown"
    
    val branchNameState by remember { DataRepository.branchName }
    val displayBranchName = branchNameState ?: branchId

    val deviceName = remember { "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}" }
    val ipAddress = remember { DeviceManager(context).getIpAddress() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1013)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Circle block with DND icon as a lock/no-entry representation
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = "Locked",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(44.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Text(
                text = "SUBSCRIPTION EXPIRED",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Device Information Box
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "INFORMASI PERANGKAT",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "$room", color = Color.White.copy(alpha = 0.9f), fontSize = 23.sp)
                Text(text = displayBranchName, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                Text(text = "$deviceName • $ipAddress", color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp)
            }
        }
    }
}