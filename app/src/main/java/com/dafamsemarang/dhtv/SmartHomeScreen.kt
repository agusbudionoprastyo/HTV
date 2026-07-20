package com.dafamsemarang.dhtv

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.border
import androidx.compose.ui.draw.scale
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

object SmartRoomGlobalFocus {
    val focusRequester = androidx.compose.ui.focus.FocusRequester()
}

@Composable
fun SmartHomeScreen(navController: androidx.navigation.NavHostController? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val deviceId by DataRepository.tuyaDeviceId
    val deviceName by DataRepository.tuyaDeviceName
    
    var switch1State by DataRepository.tuyaSwitch1State
    var switch2State by DataRepository.tuyaSwitch2State
    val switch1Name by DataRepository.tuyaSwitch1Name
    val switch2Name by DataRepository.tuyaSwitch2Name
    
    var isLoading1 by remember { mutableStateOf(false) }
    var isLoading2 by remember { mutableStateOf(false) }

    val switch1Focus = remember { FocusRequester() }
    val switch2Focus = remember { FocusRequester() }

    var focusedItem by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(207, 223, 237).copy(alpha = 0.15f))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = deviceName ?: "Room Controls",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (deviceId == null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Smart Switch belum dikonfigurasi\nuntuk kamar ini melalui CMS.",
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                    ) {
                        // Switch 1
                        val isSwitch1Focused = focusedItem == "switch1"
                        val borderAlpha1 = remember { Animatable(0.4f) }
                        LaunchedEffect(isSwitch1Focused) {
                            if (isSwitch1Focused) {
                                borderAlpha1.animateTo(
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                            } else {
                                borderAlpha1.snapTo(0.4f)
                            }
                        }
                        val scale1 by animateFloatAsState(
                            targetValue = if (isSwitch1Focused) 1.05f else 1.0f,
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            label = "Switch1Scale"
                        )
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .scale(scale1)
                                    .then(
                                        if (isLoading1) {
                                            Modifier.liquidGlass(
                                                cornerRadius = 18.dp,
                                                glassColor = Color.Transparent,
                                                alphaInitial = 0f,
                                                alphaFinal = 0f,
                                                isLedStrip = true,
                                                borderAlpha = 1f,
                                                borderWidth = 3.dp
                                            )
                                        } else if (isSwitch1Focused) {
                                            Modifier.border(
                                                width = 3.dp,
                                                color = Color.White.copy(alpha = borderAlpha1.value),
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                        } else Modifier
                                    )
                                    .focusRequester(switch1Focus)
                                    .onFocusChanged { 
                                        if (it.isFocused) focusedItem = "switch1"
                                        else if (focusedItem == "switch1") focusedItem = null
                                    }
                                    .clip(RoundedCornerShape(18.dp))
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
                                    }
                                    .onKeyEvent {
                                        if (it.key == Key.DirectionRight) {
                                            try { switch2Focus.requestFocus() } catch (e: Exception) {}
                                            true
                                        } else if (it.key == Key.DirectionDown) {
                                            try { SmartRoomGlobalFocus.focusRequester.requestFocus() } catch (e: Exception) {}
                                            true
                                        } else false
                                    }
                            ) {
                                // Container for gap between focus border and switch
                                Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    // 3D Base Lip (visible when OFF)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (switch1State) Color.Transparent else Color.Black.copy(alpha = 0.35f))
                                    )

                                    // Main Top Surface
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = if (switch1State) 0.dp else 6.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (switch1State) androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    colors = listOf(Color(0xFF1976D2), Color(0xFF29B6F6))
                                                )
                                                else androidx.compose.ui.graphics.SolidColor(Color(0xFF424242).copy(alpha = 0.95f))
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (switch1State) Color.Transparent else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Indicator Bar
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 16.dp)
                                                .width(24.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    if (switch1State) Color(0xFFB2EBF2)
                                                    else Color.White.copy(alpha = 0.15f)
                                                )
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = switch1Name ?: "",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // Switch 2
                        val isSwitch2Focused = focusedItem == "switch2"
                        val borderAlpha2 = remember { Animatable(0.4f) }
                        LaunchedEffect(isSwitch2Focused) {
                            if (isSwitch2Focused) {
                                borderAlpha2.animateTo(
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                            } else {
                                borderAlpha2.snapTo(0.4f)
                            }
                        }
                        val scale2 by animateFloatAsState(
                            targetValue = if (isSwitch2Focused) 1.05f else 1.0f,
                            animationSpec = tween(350, easing = FastOutSlowInEasing),
                            label = "Switch2Scale"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .scale(scale2)
                                    .then(
                                        if (isLoading2) {
                                            Modifier.liquidGlass(
                                                cornerRadius = 18.dp,
                                                glassColor = Color.Transparent,
                                                alphaInitial = 0f,
                                                alphaFinal = 0f,
                                                isLedStrip = true,
                                                borderAlpha = 1f,
                                                borderWidth = 3.dp
                                            )
                                        } else if (isSwitch2Focused) {
                                            Modifier.border(
                                                width = 3.dp,
                                                color = Color.White.copy(alpha = borderAlpha2.value),
                                                shape = RoundedCornerShape(18.dp)
                                            )
                                        } else Modifier
                                    )
                                    .focusRequester(switch2Focus)
                                    .onFocusChanged { 
                                        if (it.isFocused) focusedItem = "switch2"
                                        else if (focusedItem == "switch2") focusedItem = null
                                    }
                                    .clip(RoundedCornerShape(18.dp))
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
                                    }
                                    .onKeyEvent {
                                        if (it.key == Key.DirectionLeft) {
                                            try { switch1Focus.requestFocus() } catch (e: Exception) {}
                                            true
                                        } else if (it.key == Key.DirectionDown) {
                                            try { SmartRoomGlobalFocus.focusRequester.requestFocus() } catch (e: Exception) {}
                                            true
                                        } else false
                                    }
                            ) {
                                // Container for gap between focus border and switch
                                Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                                    // 3D Base Lip (visible when OFF)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (switch2State) Color.Transparent else Color.Black.copy(alpha = 0.35f))
                                    )

                                    // Main Top Surface
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = if (switch2State) 0.dp else 6.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (switch2State) androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    colors = listOf(Color(0xFF1976D2), Color(0xFF29B6F6))
                                                )
                                                else androidx.compose.ui.graphics.SolidColor(Color(0xFF424242).copy(alpha = 0.95f))
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (switch2State) Color.Transparent else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Indicator Bar
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 16.dp)
                                                .width(24.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    if (switch2State) Color(0xFFB2EBF2)
                                                    else Color.White.copy(alpha = 0.15f)
                                                )
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = switch2Name ?: "",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
