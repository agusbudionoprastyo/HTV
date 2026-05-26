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

                    // Check if device is paired
                    LaunchedEffect(Unit) {
                        Log.d("MainActivity", "Checking device pairing status")
                        val tempDeviceManager = DeviceManager(this@MainActivity)
                        tempDeviceManager.setDeviceStatusListener(this@MainActivity)
                        isPaired = tempDeviceManager.isDevicePaired()
                        if (isPaired) {
                            deviceManager = tempDeviceManager
                            Log.d("MainActivity", "Device is paired, DeviceManager initialized")
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

                    // Use shouldShowPairing state to control navigation
                    if (shouldShowPairing) {
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
                }
            }
        }
    }

    override fun onPairingModeRequired() {
        Log.d("MainActivity", "Pairing mode required, navigating to pairing screen")
        shouldShowPairing = true
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