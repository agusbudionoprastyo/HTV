package com.dafamsemarang.dhtv

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

suspend fun sendTuyaCommand(deviceId: String, switchCode: String, value: Boolean): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId
            val url = "https://us-central1-$projectId.cloudfunctions.net/controlTuyaDevice"
            val client = OkHttpClient()
            val json = """
                {
                    "deviceId": "$deviceId",
                    "commands": [
                        { "code": "$switchCode", "value": $value }
                    ]
                }
            """.trimIndent()
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

@Composable
fun SmartRoomDialog(
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val deviceId by DataRepository.tuyaDeviceId
    val deviceName by DataRepository.tuyaDeviceName
    
    // Tuya default codes for a 2-gang switch are usually "switch_1" and "switch_2"
    var switch1State by DataRepository.tuyaSwitch1State
    var switch2State by DataRepository.tuyaSwitch2State
    var isLoading1 by remember { mutableStateOf(false) }
    var isLoading2 by remember { mutableStateOf(false) }

    val closeFocusRequester = remember { FocusRequester() }
    val switch1Focus = remember { FocusRequester() }
    val switch2Focus = remember { FocusRequester() }

    var focusedItem by remember { mutableStateOf("switch1") }

    LaunchedEffect(Unit) {
        try {
            switch1Focus.requestFocus()
        } catch (e: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismissRequest() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E1E1E))
                    .padding(24.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_room_service),
                        contentDescription = "Smart Room",
                        tint = if (switch1State || switch2State) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = deviceName ?: "Room Controls",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (deviceId == null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Smart Switch belum dikonfigurasi\nuntuk kamar ini melalui CMS.",
                            color = Color.Red.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Switch 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (focusedItem == "switch1") Color(0xFFE91E63)
                                        else if (switch1State) Color(0xFF2E7D32)
                                        else Color(0xFF333333)
                                    )
                                    .focusRequester(switch1Focus)
                                    .onFocusChanged { if (it.isFocused) focusedItem = "switch1" }
                                    .clickable {
                                        if (!isLoading1) {
                                            isLoading1 = true
                                            val newState = !switch1State
                                            coroutineScope.launch {
                                                val success = sendTuyaCommand(deviceId!!, "switch_1", newState)
                                                if (success) {
                                                    switch1State = newState
                                                } else {
                                                    Toast.makeText(context, "Gagal mengubah lampu 1", Toast.LENGTH_SHORT).show()
                                                }
                                                isLoading1 = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (isLoading1) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text(
                                            text = "Lampu 1",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = if (switch1State) "ON" else "OFF",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            // Switch 2
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (focusedItem == "switch2") Color(0xFFE91E63)
                                        else if (switch2State) Color(0xFF2E7D32)
                                        else Color(0xFF333333)
                                    )
                                    .focusRequester(switch2Focus)
                                    .onFocusChanged { if (it.isFocused) focusedItem = "switch2" }
                                    .clickable {
                                        if (!isLoading2) {
                                            isLoading2 = true
                                            val newState = !switch2State
                                            coroutineScope.launch {
                                                val success = sendTuyaCommand(deviceId!!, "switch_2", newState)
                                                if (success) {
                                                    switch2State = newState
                                                } else {
                                                    Toast.makeText(context, "Gagal mengubah lampu 2", Toast.LENGTH_SHORT).show()
                                                }
                                                isLoading2 = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (isLoading2) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text(
                                            text = "Lampu 2",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = if (switch2State) "ON" else "OFF",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Dismiss Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (focusedItem == "close") Color.DarkGray.copy(alpha = 0.8f) else Color.Transparent)
                            .focusRequester(closeFocusRequester)
                            .onFocusChanged { if (it.isFocused) focusedItem = "close" }
                            .clickable { onDismissRequest() }
                            .onKeyEvent {
                                if (it.key == Key.Back || it.key == Key.Escape) {
                                    onDismissRequest()
                                    true
                                } else {
                                    false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tutup (Tekan Back)",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
