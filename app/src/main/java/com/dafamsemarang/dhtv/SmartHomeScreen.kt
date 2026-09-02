package com.dafamsemarang.dhtv

import androidx.compose.ui.composed
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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

suspend fun sendTuyaCommand(deviceId: String, switchCode: String, value: Any): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val projectId = com.google.firebase.FirebaseApp.getInstance().options.projectId
            val url = "https://us-central1-$projectId.cloudfunctions.net/controlTuyaDevice"
            val client = OkHttpClient()
            val json = """
                {
                    "deviceId": "$deviceId",
                    "commands": [
                        { "code": "$switchCode", "value": ${if (value is String) "\"$value\"" else value} }
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
    
    val smartDevices by DataRepository.smartDevicesList
    var focusedItem by remember { mutableStateOf<String?>(null) }
    var debounceJobTemp by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var debounceJobMode by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var debounceJobFan by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val acDevice = smartDevices.find { it.type == "ac" }
    val curtainDevice = smartDevices.find { it.type == "curtain" }
    val switchDevices = smartDevices.filter { it.type == "switch" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(top = 100.dp, bottom = 70.dp, start = 58.dp, end = 58.dp)
            .onFocusChanged { if (!it.hasFocus) focusedItem = null },
        contentAlignment = Alignment.Center
    ) {
        if (smartDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(207, 223, 237).copy(alpha = 0.15f))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Smart devices belum dikonfigurasi\nuntuk kamar ini melalui CMS.",
                    color = Color.Red.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // KOLOM 1: AC (1/3 lebar)
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (acDevice != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(207, 223, 237).copy(alpha = 0.15f))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // TOP HEADER
                                        Box(
                                            modifier = Modifier.fillMaxWidth().offset(y = (-8).dp)
                                        ) {
                                            // Title Centered
                                            Text(
                                                "Air Conditioner", 
                                                color = Color.White, 
                                                fontWeight = FontWeight.Bold, 
                                                fontSize = 16.sp,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                        
                                        // TEMPERATURE DIAL
                                        Box(
                                            modifier = Modifier.size(180.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                                val strokeWidth = 8.dp.toPx()
                                                val startAngle = 135f
                                                val sweepAngle = 270f
                                                
                                                // Background arc
                                                drawArc(
                                                    color = Color.White.copy(alpha = 0.2f),
                                                    startAngle = startAngle,
                                                    sweepAngle = sweepAngle,
                                                    useCenter = false,
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                )
                                                
                                                // Foreground (Progress) arc
                                                if (acDevice.acPowerState) {
                                                    val temp = acDevice.acTemp.coerceIn(18, 30)
                                                    val progress = (temp - 18f) / (30f - 18f)
                                                    drawArc(
                                                        color = Color(0xFF29B6F6),
                                                        startAngle = startAngle,
                                                        sweepAngle = sweepAngle * progress,
                                                        useCenter = false,
                                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                    )
                                                }
                                            }
                                            
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "${acDevice.acTemp}°C", 
                                                    color = Color.White, 
                                                    fontSize = 54.sp, 
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.split_ac))
                                                val progress by animateLottieCompositionAsState(
                                                    composition = composition,
                                                    iterations = LottieConstants.IterateForever,
                                                    isPlaying = acDevice.acPowerState
                                                )
                                                LottieAnimation(
                                                    composition = composition,
                                                    progress = { progress },
                                                    modifier = Modifier.height(30.dp)
                                                )
                                            }
                                        }
                                        
                                        // +/- BUTTONS
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = (-32).dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SmartActionBtn(
                                                iconRes = R.drawable.ic_minus,
                                                isFocused = focusedItem == "ac_temp_down",
                                                onFocus = { focusedItem = "ac_temp_down" },
                                                onClickAction = { 
                                                    val newTemp = maxOf(18, acDevice.acTemp - 1)
                                                    DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                        if (d.deviceId == acDevice.deviceId) d.copy(acTemp = newTemp) else d 
                                                    }
                                                    debounceJobTemp?.cancel()
                                                    debounceJobTemp = coroutineScope.launch {
                                                        kotlinx.coroutines.delay(600)
                                                        sendTuyaCommand(acDevice.deviceId, "T", newTemp.toString())
                                                    }
                                                },
                                                modifier = Modifier.size(42.dp)
                                            )
                                            
                                            SmartActionBtn(
                                                iconRes = R.drawable.ic_add,
                                                isFocused = focusedItem == "ac_temp_up",
                                                onFocus = { focusedItem = "ac_temp_up" },
                                                onClickAction = { 
                                                    val newTemp = minOf(30, acDevice.acTemp + 1)
                                                    DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                        if (d.deviceId == acDevice.deviceId) d.copy(acTemp = newTemp) else d 
                                                    }
                                                    debounceJobTemp?.cancel()
                                                    debounceJobTemp = coroutineScope.launch {
                                                        kotlinx.coroutines.delay(600)
                                                        sendTuyaCommand(acDevice.deviceId, "T", newTemp.toString())
                                                    }
                                                },
                                                modifier = Modifier.size(42.dp)
                                            )
                                        }
                                        
                                        // MODES AND FAN
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // MODE SECTION
                                            Column(
                                                modifier = Modifier.weight(2f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                val modeStr = when(acDevice.acMode) { "0" -> "COLD"; "1" -> "HEAT"; "2" -> "AUTO"; "3" -> "DRY"; "4" -> "FAN"; else -> "AUTO" }
                                                val modeIconRes = when(acDevice.acMode) { "0" -> R.drawable.cold; "1" -> R.drawable.heat; "3" -> R.drawable.humidi; "4" -> R.drawable.fan; else -> R.drawable.ic_setting }
                                                
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    androidx.compose.material3.Icon(
                                                        painter = androidx.compose.ui.res.painterResource(id = modeIconRes),
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(modeStr, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))

                                                val isModeFocused = focusedItem == "ac_mode_btn"
                                                val modeContentColor = if (isModeFocused) Color(0xFF1E1E1E) else Color.White
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(50))
                                                        .background(if (isModeFocused) Color.White else Color.White.copy(alpha = 0.15f))
                                                        .onFocusChanged { if (it.isFocused) focusedItem = "ac_mode_btn" }
                                                        .clickable(enabled = acDevice.deviceId != null) {
                                                            val modeList = listOf("0", "1", "2", "3", "4") // COLD, HEAT, AUTO, DRY, FAN
                                                            val nextMode = modeList[(maxOf(0, modeList.indexOf(acDevice.acMode)) + 1) % modeList.size]
                                                            DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                                if (d.deviceId == acDevice.deviceId) d.copy(acMode = nextMode) else d 
                                                            }
                                                            debounceJobMode?.cancel()
                                                            debounceJobMode = coroutineScope.launch {
                                                                kotlinx.coroutines.delay(600)
                                                                sendTuyaCommand(acDevice.deviceId, "M", nextMode)
                                                            }
                                                        }
                                                        .focusable(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("MODE", color = modeContentColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }

                                            // FAN SECTION
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                val fanText = when(acDevice.acFan) { "0" -> "AUTO"; "1" -> "LOW"; "2" -> "MID"; "3" -> "HIGH"; else -> "AUTO" }
                                                Text(fanText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.height(8.dp))

                                                val isFanFocused = focusedItem == "ac_fan_btn"
                                                val fanContentColor = if (isFanFocused) Color(0xFF1E1E1E) else Color.White
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                        .background(if (isFanFocused) Color.White else Color.White.copy(alpha = 0.15f))
                                                        .onFocusChanged { if (it.isFocused) focusedItem = "ac_fan_btn" }
                                                        .clickable(enabled = acDevice.deviceId != null) {
                                                            val fanModes = listOf("0", "1", "2", "3")
                                                            val nextFan = fanModes[(maxOf(0, fanModes.indexOf(acDevice.acFan)) + 1) % fanModes.size]
                                                            DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                                if (d.deviceId == acDevice.deviceId) d.copy(acFan = nextFan) else d 
                                                            }
                                                            debounceJobFan?.cancel()
                                                            debounceJobFan = coroutineScope.launch {
                                                                kotlinx.coroutines.delay(600)
                                                                sendTuyaCommand(acDevice.deviceId, "F", nextFan)
                                                            }
                                                        }
                                                        .focusable(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.material3.Icon(
                                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.fan),
                                                        contentDescription = "Fan",
                                                        tint = fanContentColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }

                                            // POWER SECTION
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                val powerText = if (acDevice.acPowerState) "ON" else "OFF"
                                                Text(powerText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.height(8.dp))

                                                val isPowerFocused = focusedItem == "ac_power_btn"
                                                val powerContentColor = if (isPowerFocused) Color(0xFF1E1E1E) else Color.White
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                        .background(if (isPowerFocused) Color.White else if(acDevice.acPowerState) Color(0xFF29B6F6) else Color.White.copy(alpha = 0.15f))
                                                        .onFocusChanged { if (it.isFocused) focusedItem = "ac_power_btn" }
                                                        .clickable(enabled = acDevice.deviceId != null) {
                                                            coroutineScope.launch {
                                                                val newState = !acDevice.acPowerState
                                                                DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                                    if (d.deviceId == acDevice.deviceId) d.copy(acPowerState = newState) else d 
                                                                }
                                                                if (newState) sendTuyaCommand(acDevice.deviceId, "PowerOn", "PowerOn")
                                                                else sendTuyaCommand(acDevice.deviceId, "PowerOff", "PowerOff")
                                                            }
                                                        }
                                                        .focusable(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.material3.Icon(
                                                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.power_button),
                                                        contentDescription = "Power",
                                                        tint = powerContentColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                        

                                    }
                                } }
                        }
                    }
                    
                    // KOLOM 2: Curtain (Baris atas) dan Switch (Baris bawah)
                    Column(
                        modifier = Modifier.weight(2f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Curtain Card
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (curtainDevice != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(207, 223, 237).copy(alpha = 0.15f))
                                        
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SmartActionBtn(
                                            text = "Open",
                                            isFocused = focusedItem == "curtain_open",
                                            onFocus = { focusedItem = "curtain_open" },
                                            onClickAction = { sendTuyaCommand(curtainDevice.deviceId, "control", "open") },
                                            modifier = Modifier.weight(1f).height(60.dp)
                                        )
                                        
                                        SmartActionBtn(
                                            text = "Pause",
                                            isFocused = focusedItem == "curtain_pause",
                                            onFocus = { focusedItem = "curtain_pause" },
                                            onClickAction = { sendTuyaCommand(curtainDevice.deviceId, "control", "stop") },
                                            modifier = Modifier.weight(1f).height(60.dp)
                                        )
                                        
                                        SmartActionBtn(
                                            text = "Close",
                                            isFocused = focusedItem == "curtain_close",
                                            onFocus = { focusedItem = "curtain_close" },
                                            onClickAction = { sendTuyaCommand(curtainDevice.deviceId, "control", "close") },
                                            modifier = Modifier.weight(1f).height(60.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                        // BARIS 2: Cards untuk Switch (Dinamis sesuai jumlah device)
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1.5f).focusProperties { down = GlobalCartState.smartHomeFooterFocusRequester },
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                    switchDevices.forEach { switchDevice ->
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(Color(207, 223, 237).copy(alpha = 0.15f))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                    
                                    // Dinamis menghitung berapa gang (kolom)
                                    val btnCount = listOfNotNull(switchDevice.switch1Name, switchDevice.switch2Name, switchDevice.switch3Name).size
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        if (switchDevice.switch1Name != null) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                SmartSwitchWidget("${switchDevice.deviceId}_1", switchDevice.switch1Name!!, switchDevice.deviceId, switchDevice.switch1State, focusedItem == "${switchDevice.deviceId}_1", { focusedItem = "${switchDevice.deviceId}_1" }) {
                                                    coroutineScope.launch { sendTuyaCommand(switchDevice.deviceId, "switch_1", !switchDevice.switch1State) }
                                                }
                                            }
                                        }
                                        if (switchDevice.switch2Name != null) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                SmartSwitchWidget("${switchDevice.deviceId}_2", switchDevice.switch2Name!!, switchDevice.deviceId, switchDevice.switch2State, focusedItem == "${switchDevice.deviceId}_2", { focusedItem = "${switchDevice.deviceId}_2" }) {
                                                    coroutineScope.launch { sendTuyaCommand(switchDevice.deviceId, "switch_2", !switchDevice.switch2State) }
                                                }
                                            }
                                        }
                                        if (switchDevice.switch3Name != null) {
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                SmartSwitchWidget("${switchDevice.deviceId}_3", switchDevice.switch3Name!!, switchDevice.deviceId, switchDevice.switch3State, focusedItem == "${switchDevice.deviceId}_3", { focusedItem = "${switchDevice.deviceId}_3" }) {
                                                    coroutineScope.launch { sendTuyaCommand(switchDevice.deviceId, "switch_3", !switchDevice.switch3State) }
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
    }
}
}
@Composable
fun SmartSwitchWidget(id: String, name: String, deviceId: String?, state: Boolean, isFocused: Boolean, onFocus: () -> Unit, onToggle: suspend () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { if (it.isFocused) onFocus() }
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = deviceId != null) {
                    coroutineScope.launch {
                        onToggle()
                    }
                }
        ) {
            // Container for gap between focus border and switch
            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                // 3D Base Lip (visible when OFF)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (state) androidx.compose.ui.graphics.SolidColor(Color.Transparent) 
                            else androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.25f))
                            )
                        )
                )

                // Main Top Surface
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (state) 0.dp else 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.White)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Focus Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isFocused) Color.White else Color.Transparent
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val contentColor = if (isFocused) Color(0xFF1E1E1E) else Color.Black.copy(alpha = 0.6f)
                        val indicatorColor = if (state) Color(0xFF29B6F6) else Color.Black.copy(alpha = 0.2f)
                        
                        // Text Label
                        Text(
                            text = name,
                            color = contentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Indicator Bar
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(indicatorColor)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SmartActionBtn(
    text: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconRes: Int? = null,
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    isActive: Boolean? = null,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onClickAction: suspend () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    useGradient: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val contentColor = if (isFocused) Color(0xFF1E1E1E) else Color.White

    Button(
        onClick = {
            coroutineScope.launch {
                onClickAction()
            }
        },
        modifier = modifier
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
            .then(
                if (useGradient) Modifier.background(Color.Transparent).border(2.dp, Color.White.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape) else Modifier
            )
            .onFocusChanged { if (it.isFocused) onFocus() },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (useGradient) Color.Transparent else if (isFocused) Color.White else if (isActive == true) Color(0xFF29B6F6) else Color.White.copy(alpha = 0.15f)
        )
    ) {
        if (iconRes != null) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        } else if (icon != null) {
            androidx.compose.material3.Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
        }
        if ((icon != null || iconRes != null) && text.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (text.isNotEmpty()) {
            Text(text, fontSize = fontSize, color = contentColor)
        }
    }
}


