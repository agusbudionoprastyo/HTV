package com.dafamsemarang.dhtv

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.net.NetworkInterface
import com.google.firebase.database.GenericTypeIndicator

class DeviceManager(private val context: Context) {
    interface DeviceStatusListener {
        fun onPairingModeRequired()
        fun onPairingSuccess()
        fun onPairingFailed(error: String)
    }

    private var deviceStatusListener: DeviceStatusListener? = null

    fun setDeviceStatusListener(listener: DeviceStatusListener) {
        deviceStatusListener = listener
    }

    fun getIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress!!.indexOf(':') < 0) {
                        return address.hostAddress!!.toString()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DeviceManager", "Error getting IP address: ${e.message}")
        }
        return "Unknown"
    }

    private val database: FirebaseDatabase = Firebase.database
    private val deviceRef = database.getReference("DEVICES")
    private val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isOnline = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        Log.d("DeviceManager", "Initializing DeviceManager")
        startStatusUpdates()
        startNetworkMonitoring()
        monitorDeviceExistence()
        Log.d("DeviceManager", "DeviceManager initialization completed")
    }

    private fun startNetworkMonitoring() {
        Log.d("DeviceManager", "Starting network monitoring")
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("DeviceManager", "Network connection available")
                isOnline = true
                scope.launch {
                    Log.d("DeviceManager", "Updating device status after network available")
                    updateDeviceStatus()
                }
            }

            override fun onLost(network: Network) {
                Log.d("DeviceManager", "Network connection lost")
                isOnline = false
                scope.launch {
                    Log.d("DeviceManager", "Updating device status after network lost")
                    updateDeviceStatus()
                }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                Log.d("DeviceManager", "Network capabilities changed. Has internet: $hasInternet")
                isOnline = hasInternet
                scope.launch {
                    Log.d("DeviceManager", "Updating device status after network capabilities changed")
                    updateDeviceStatus()
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        Log.d("DeviceManager", "Network monitoring started successfully")
    }

    private fun stopNetworkMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
            networkCallback = null
        }
    }

    private fun startStatusUpdates() {
        Log.d("DeviceManager", "Starting periodic status updates")
        scope.launch {
            while (true) {
                Log.d("DeviceManager", "Running periodic status update")
                updateDeviceStatus()
                kotlinx.coroutines.delay(30000) // Update every 30 seconds
            }
        }
    }

    private suspend fun updateDeviceStatus() {
        try {
            val deviceId = sharedPreferences.getString("deviceID", null)
            val branchId = sharedPreferences.getString("branchId", null)
            val room = sharedPreferences.getString("room", null)

            Log.d("DeviceManager", "Updating device status - DeviceID: $deviceId, BranchID: $branchId, Room: $room")

            if (deviceId != null && branchId != null && room != null) {
                try {
                    // First check if device still exists in Firebase
                    val deviceSnapshot = deviceRef.child(deviceId).get().await()
                    if (!deviceSnapshot.exists()) {
                        Log.d("DeviceManager", "Device no longer exists in Firebase, unpairing...")
                        returnToPairingMode()
                        deviceStatusListener?.onPairingModeRequired()
                        return
                    }

                    // Validate device data matches with SharedPreferences
                    val typeIndicator = object : GenericTypeIndicator<Map<String, Any>>() {}
                    val deviceData = deviceSnapshot.getValue(typeIndicator)
                    if (deviceData == null || 
                        deviceData["branchId"] != branchId ||
                        deviceData["room"] != room) {
                        Log.d("DeviceManager", "Device data mismatch, unpairing...")
                        returnToPairingMode()
                        deviceStatusListener?.onPairingModeRequired()
                        return
                    }

                    val (versionName, versionCode) = getAppVersion()
                    val updateMap = mapOf(
                        "status" to (if (isOnline) "online" else "offline"),
                        "lastSeen" to System.currentTimeMillis(),
                        "appVersion" to versionName,
                        "appVersionCode" to versionCode
                    )
                    Log.d("DeviceManager", "Sending status update to Firebase: $updateMap")
                    deviceRef.child(deviceId).updateChildren(updateMap).await()
                    Log.d("DeviceManager", "Device status updated successfully")
                } catch (e: Exception) {
                    Log.e("DeviceManager", "Failed to update device status: ${e.message}")
                    e.printStackTrace()
                    // If update fails, mark as offline
                    isOnline = false
                }
            } else {
                val missingInfo = mutableListOf<String>()
                if (deviceId == null) missingInfo.add("deviceID")
                if (branchId == null) missingInfo.add("branchId")
                if (room == null) missingInfo.add("room")
                
                Log.e("DeviceManager", "Missing required device information: ${missingInfo.joinToString(", ")}")
            }
        } catch (e: Exception) {
            Log.e("DeviceManager", "Error updating device status: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getDeviceName(): String {
        return Build.MANUFACTURER + " " + Build.MODEL
    }

    // Get app version name and code
    fun getAppVersion(): Pair<String, Int> {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val versionName = packageInfo.versionName ?: "Unknown"
            @Suppress("DEPRECATION")
            val versionCode = packageInfo.versionCode
            Pair(versionName, versionCode)
        } catch (e: Exception) {
            Log.e("DeviceManager", "Error getting app version: ${e.message}")
            Pair("Unknown", 0)
        }
    }

    // Add method to check if device is paired
    fun isDevicePaired(): Boolean {
        val deviceId = sharedPreferences.getString("deviceID", null)
        val branchId = sharedPreferences.getString("branchId", null)
        val room = sharedPreferences.getString("room", null)
        
        return deviceId != null && branchId != null && room != null
    }

    // Add method to get device information
    fun getDeviceInformation(): Map<String, String?> {
        return mapOf(
            "deviceID" to sharedPreferences.getString("deviceID", null),
            "branchId" to sharedPreferences.getString("branchId", null),
            "room" to sharedPreferences.getString("room", null)
        )
    }

    // Add method to handle device shutdown
    fun handleDeviceShutdown() {
        scope.launch {
            try {
                val deviceId = sharedPreferences.getString("deviceID", null)
                if (deviceId != null) {
                    try {
                        val (versionName, versionCode) = getAppVersion()
                        val updateMap = mapOf(
                            "status" to "offline",
                            "lastSeen" to System.currentTimeMillis(),
                            "name" to getDeviceName(),
                            "room" to (sharedPreferences.getString("room", "") ?: ""),
                            "branchId" to (sharedPreferences.getString("branchId", "") ?: ""),
                            "osVersion" to Build.VERSION.RELEASE,
                            "deviceModel" to Build.MODEL,
                            "manufacturer" to Build.MANUFACTURER,
                            "ipAddress" to getIpAddress(),
                            "appVersion" to versionName,
                            "appVersionCode" to versionCode
                        )
                        deviceRef.child(deviceId).updateChildren(updateMap).await()
                        Log.d("DeviceManager", "Device status updated to offline before shutdown")
                    } catch (e: Exception) {
                        Log.e("DeviceManager", "Failed to update offline status: ${e.message}")
                    } finally {
                        stopNetworkMonitoring()
                    }
                }
            } catch (e: Exception) {
                Log.e("DeviceManager", "Error updating offline status: ${e.message}")
            }
        }
    }

    // Method to return to pairing mode
    fun returnToPairingMode() {
        scope.launch {
            try {
                val deviceId = sharedPreferences.getString("deviceID", null)
                if (deviceId != null) {
                    try {
                        // Remove device data from Firebase
                        deviceRef.child(deviceId).removeValue().await()
                        Log.d("DeviceManager", "Successfully removed device data from Firebase")
                    } catch (e: Exception) {
                        Log.e("DeviceManager", "Failed to remove device data from Firebase: ${e.message}")
                    }
                    
                    // Clear pairing information from SharedPreferences
                    sharedPreferences.edit().apply {
                        remove("deviceID")
                        remove("branchId")
                        remove("room")
                        apply()
                    }
                    
                    Log.d("DeviceManager", "Successfully returned to pairing mode")
                }
            } catch (e: Exception) {
                Log.e("DeviceManager", "Error returning to pairing mode: ${e.message}")
            }
        }
    }

    private fun monitorDeviceExistence() {
        scope.launch {
            try {
                val deviceId = sharedPreferences.getString("deviceID", null)
                if (deviceId != null) {
                    deviceRef.child(deviceId).addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                        override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                            if (!snapshot.exists()) {
                                Log.d("DeviceManager", "Device no longer exists in Firebase, unpairing...")
                                returnToPairingMode()
                                deviceStatusListener?.onPairingModeRequired()
                            }
                        }

                        override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                            Log.e("DeviceManager", "Error monitoring device existence: ${error.message}")
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e("DeviceManager", "Error setting up device existence monitoring: ${e.message}")
            }
        }
    }
}