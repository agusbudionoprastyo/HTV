package com.dafamsemarang.dhtv

import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import androidx.activity.compose.rememberLauncherForActivityResult
import android.os.Build

@Composable
fun PairingScreen(
    onDeviceIdSaved: (String) -> Unit,
    sharedPreferences: SharedPreferences,
    deviceManager: DeviceManager
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf("Mengirim informasi perangkat ke server...") }
    var deviceId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Request Microphone Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("PairingScreen", "Microphone permission granted")
        } else {
            Log.e("PairingScreen", "Microphone permission denied")
        }
    }

    LaunchedEffect(Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, 
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Generate or get deviceId
    LaunchedEffect(Unit) {
        // Check if deviceId already exists in SharedPreferences
        val existingDeviceId = sharedPreferences.getString("deviceID", null)
        if (existingDeviceId != null) {
            deviceId = existingDeviceId
            Log.d("PairingScreen", "Using existing deviceId: $existingDeviceId")
        } else {
            // Generate new deviceId using Android ID
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val newDeviceId = "device_${androidId.take(8)}"
            deviceId = newDeviceId
            Log.d("PairingScreen", "Generated new deviceId: $newDeviceId")
        }

        // Send device info to Firebase
        deviceId?.let { id ->
            sendDeviceInfoToFirebase(id, deviceManager) { success, error ->
                if (success) {
                    // Check if device already has branchId and room configured
                    checkExistingConfiguration(id, sharedPreferences, deviceManager, onDeviceIdSaved) { hasConfig ->
                        if (hasConfig) {
                            statusMessage = "Perangkat sudah dikonfigurasi"
                        } else {
                            statusMessage = "Menunggu admin mengkonfigurasi perangkat..."
                            // Start listening for branchId and room
                            listenForAdminConfiguration(id, sharedPreferences, deviceManager, onDeviceIdSaved) { error ->
                                errorMessage = error
                            }
                        }
                    }
                } else {
                    errorMessage = error ?: "Gagal mengirim informasi perangkat"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Konfigurasi Perangkat",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = statusMessage,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        deviceId?.let { id ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Device ID: $id",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        // Display error message if any
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun sendDeviceInfoToFirebase(
    deviceId: String,
    deviceManager: DeviceManager,
    onComplete: (Boolean, String?) -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference
    val deviceRef = database.child("DEVICES").child(deviceId)
    
    Log.d("PairingScreen", "Sending device info to Firebase for deviceId: $deviceId")
    
    // Get app version
    val (versionName, versionCode) = deviceManager.getAppVersion()
    
    // Use updateChildren to preserve existing data like branchId and room
    val deviceInfo = mapOf(
        "deviceId" to deviceId,
        "lastSeen" to System.currentTimeMillis(),
        "name" to (Build.MANUFACTURER + " " + Build.MODEL),
        "deviceModel" to Build.MODEL,
        "manufacturer" to Build.MANUFACTURER,
        "osVersion" to Build.VERSION.RELEASE,
        "ipAddress" to deviceManager.getIpAddress(),
        "appVersion" to versionName,
        "appVersionCode" to versionCode
    )
    
    // Check if device exists first
    deviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                // Device exists, update only device info, preserve branchId and room
                val existingBranchId = snapshot.child("branchId").getValue(String::class.java)
                val existingRoom = snapshot.child("room").getValue(String::class.java)
                
                val updateMap = deviceInfo.toMutableMap()
                // Only set status to "waiting" if branchId and room are not set
                if (existingBranchId == null || existingRoom == null || 
                    existingBranchId.isEmpty() || existingRoom.isEmpty()) {
                    updateMap["status"] = "waiting"
                } else {
                    updateMap["status"] = "online"
                }
                
                deviceRef.updateChildren(updateMap as Map<String, Any>)
                    .addOnSuccessListener {
                        Log.d("PairingScreen", "Successfully updated device info in Firebase")
                        onComplete(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e("PairingScreen", "Failed to update device info in Firebase: ${e.message}")
                        onComplete(false, e.message)
                    }
            } else {
                // Device doesn't exist, create new with waiting status
                val newDeviceInfo = deviceInfo.toMutableMap()
                newDeviceInfo["status"] = "waiting"
                
                deviceRef.setValue(newDeviceInfo)
                    .addOnSuccessListener {
                        Log.d("PairingScreen", "Successfully created device in Firebase")
                        onComplete(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e("PairingScreen", "Failed to create device in Firebase: ${e.message}")
                        onComplete(false, e.message)
                    }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("PairingScreen", "Database error: ${error.message}")
            onComplete(false, error.message)
        }
    })
}

private fun checkExistingConfiguration(
    deviceId: String,
    sharedPreferences: SharedPreferences,
    deviceManager: DeviceManager,
    onDeviceIdSaved: (String) -> Unit,
    onResult: (Boolean) -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference
    val deviceRef = database.child("DEVICES").child(deviceId)
    
    Log.d("PairingScreen", "Checking existing configuration for deviceId: $deviceId")
    
    deviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists()) {
                Log.d("PairingScreen", "Device does not exist in Firebase")
                onResult(false)
                return
            }
            
            val branchId = snapshot.child("branchId").getValue(String::class.java)
            val room = snapshot.child("room").getValue(String::class.java)
            
            Log.d("PairingScreen", "Existing configuration - branchId: $branchId, room: $room")
            
            if (branchId != null && room != null && branchId.isNotEmpty() && room.isNotEmpty()) {
                Log.d("PairingScreen", "Device already configured, saving to SharedPreferences")
                
                // Save to SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString("deviceID", deviceId)
                editor.putString("branchId", branchId)
                editor.putString("room", room)
                
                if (editor.commit()) {
                    Log.d("PairingScreen", "Successfully saved existing device configuration")
                    
                    // Update device status to online
                    val (versionName, versionCode) = deviceManager.getAppVersion()
                    val updateMap = mapOf(
                        "status" to "online",
                        "lastSeen" to System.currentTimeMillis(),
                        "appVersion" to versionName,
                        "appVersionCode" to versionCode
                    )
                    
                    deviceRef.updateChildren(updateMap as Map<String, Any>)
                        .addOnSuccessListener {
                            Log.d("PairingScreen", "Successfully updated device status to online")
                            onDeviceIdSaved(deviceId)
                            onResult(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e("PairingScreen", "Failed to update device status: ${e.message}")
                            onResult(false)
                        }
                } else {
                    Log.e("PairingScreen", "Failed to save device configuration to SharedPreferences")
                    onResult(false)
                }
            } else {
                Log.d("PairingScreen", "Device not yet configured by admin")
                onResult(false)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("PairingScreen", "Database error while checking configuration: ${error.message}")
            onResult(false)
        }
    })
}

private fun listenForAdminConfiguration(
    deviceId: String,
    sharedPreferences: SharedPreferences,
    deviceManager: DeviceManager,
    onDeviceIdSaved: (String) -> Unit,
    onError: (String) -> Unit
) {
    val database = FirebaseDatabase.getInstance().reference
    val deviceRef = database.child("DEVICES").child(deviceId)
    
    Log.d("PairingScreen", "Starting to listen for admin configuration for deviceId: $deviceId")
    
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (!snapshot.exists()) {
                Log.d("PairingScreen", "Device no longer exists in Firebase")
                onError("Perangkat tidak ditemukan di server")
                return
            }
            
            val branchId = snapshot.child("branchId").getValue(String::class.java)
            val room = snapshot.child("room").getValue(String::class.java)
            
            Log.d("PairingScreen", "Checking configuration - branchId: $branchId, room: $room")
            
            if (branchId != null && room != null && branchId.isNotEmpty() && room.isNotEmpty()) {
                Log.d("PairingScreen", "Admin configuration received - branchId: $branchId, room: $room")
                
                // Save to SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString("deviceID", deviceId)
                editor.putString("branchId", branchId)
                editor.putString("room", room)
                
                if (editor.commit()) {
                    Log.d("PairingScreen", "Successfully saved device information to SharedPreferences")
                    
                    // Update device status to online
                    val (versionName, versionCode) = deviceManager.getAppVersion()
                    val updateMap = mapOf(
                        "status" to "online",
                        "lastSeen" to System.currentTimeMillis(),
                        "appVersion" to versionName,
                        "appVersionCode" to versionCode
                    )
                    
                    deviceRef.updateChildren(updateMap as Map<String, Any>)
                        .addOnSuccessListener {
                            Log.d("PairingScreen", "Successfully updated device status to online")
                            // Remove listener before proceeding
                            deviceRef.removeEventListener(this)
                            onDeviceIdSaved(deviceId)
                        }
                        .addOnFailureListener { e ->
                            Log.e("PairingScreen", "Failed to update device status: ${e.message}")
                            deviceRef.removeEventListener(this)
                            onError("Gagal memperbarui status perangkat")
                        }
                } else {
                    Log.e("PairingScreen", "Failed to save device information to SharedPreferences")
                    deviceRef.removeEventListener(this)
                    onError("Gagal menyimpan informasi perangkat")
                }
            } else {
                Log.d("PairingScreen", "Waiting for admin to set branchId and room...")
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("PairingScreen", "Database error while listening: ${error.message}")
            onError("Error database: ${error.message}")
        }
    }
    
    deviceRef.addValueEventListener(listener)
}