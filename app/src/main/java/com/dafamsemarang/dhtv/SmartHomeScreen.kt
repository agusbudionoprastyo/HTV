package com.dafamsemarang.dhtv
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.drawscope.translate
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
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
import androidx.compose.ui.graphics.drawscope.translate
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
    var isCurtainSliderActive by remember { mutableStateOf(false) }
    var curtainSliderLocalPercent by remember { mutableStateOf(0) }

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
                    .background(Color(207, 223, 237).copy(alpha = 0.3f))
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
                Box(modifier = Modifier.weight(1f).fillMaxHeight().focusProperties { down = GlobalCartState.smartHomeFooterFocusRequester }) {
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
                                        .background(Color(207, 223, 237).copy(alpha = 0.3f))
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
                                                    val sweep = sweepAngle * progress
                                                    val currentAngle = startAngle + sweep
                                                    
                                                    val pekat = Color(0xFF00E9F8) // Light blue (power ON color) for 18C
                                                    val cerah = Color(0xFFB3FBFF) // Brighter cyan for 30C
                                                    val colorAt0 = androidx.compose.ui.graphics.lerp(pekat, cerah, 225f / 270f)
                                                    
                                                    drawArc(
                                                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                                            0.0f to colorAt0,
                                                            0.125f to cerah,
                                                            0.375f to pekat,
                                                            1.0f to colorAt0,
                                                            center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                                                        ),
                                                        startAngle = startAngle,
                                                        sweepAngle = sweep,
                                                        useCenter = false,
                                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                                    )
                                                    
                                                    // Indicator circle
                                                    val arcRadius = size.minDimension / 2
                                                    val angleInRadians = Math.toRadians(currentAngle.toDouble())
                                                    val cx = size.width / 2 + arcRadius * Math.cos(angleInRadians).toFloat()
                                                    val cy = size.height / 2 + arcRadius * Math.sin(angleInRadians).toFloat()
                                                    
                                                    drawCircle(
                                                        color = Color.White,
                                                        radius = strokeWidth / 2f,
                                                        center = androidx.compose.ui.geometry.Offset(cx, cy)
                                                    )
                                                }
                                            }
                                            
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (acDevice.acTemp > 0) "${acDevice.acTemp}°C" else "--°C", 
                                                    color = if (acDevice.acTemp > 0) Color.White else Color.Transparent, 
                                                    fontSize = 54.sp, 
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        
                                        val acModeFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                                        val acFanFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                                        val acPowerFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                                        
                                        // +/- BUTTONS
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = (-32).dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SmartActionBtn(
                                                iconRes = R.drawable.ic_minus,
                                                isFocused = focusedItem == "ac_temp_down",
                                                transparentWhenUnfocused = true,
                                                onFocus = { focusedItem = "ac_temp_down" },
                                                onClickAction = { 
                                                    val newTemp = maxOf(18, acDevice.acTemp - 1)
                                                    DataRepository.setIgnoreDeviceUpdates(acDevice.deviceId)
                                                    DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                        if (d.deviceId == acDevice.deviceId) d.copy(acTemp = newTemp) else d 
                                                    }
                                                    coroutineScope.launch {
                                                        sendTuyaCommand(acDevice.deviceId, "T", newTemp.toString())
                                                    }
                                                },
                                                modifier = Modifier.size(42.dp).onKeyEvent {
                                                    if (it.key == androidx.compose.ui.input.key.Key.DirectionDown && it.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                                        try { acModeFocusRequester.requestFocus() } catch (e: Exception) {}
                                                        true
                                                    } else false
                                                }
                                            )
                                            
                                            SmartActionBtn(
                                                iconRes = R.drawable.ic_add,
                                                isFocused = focusedItem == "ac_temp_up",
                                                transparentWhenUnfocused = true,
                                                onFocus = { focusedItem = "ac_temp_up" },
                                                onClickAction = { 
                                                    val newTemp = minOf(30, acDevice.acTemp + 1)
                                                    DataRepository.setIgnoreDeviceUpdates(acDevice.deviceId)
                                                    DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                        if (d.deviceId == acDevice.deviceId) d.copy(acTemp = newTemp) else d 
                                                    }
                                                    coroutineScope.launch {
                                                        sendTuyaCommand(acDevice.deviceId, "T", newTemp.toString())
                                                    }
                                                },
                                                modifier = Modifier.size(42.dp).onKeyEvent {
                                                    if (it.key == androidx.compose.ui.input.key.Key.DirectionDown && it.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                                        try { acPowerFocusRequester.requestFocus() } catch (e: Exception) {}
                                                        true
                                                    } else false
                                                }
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
                                                val modeStr = when(acDevice.acMode) { "0" -> "cold"; "1" -> "heat"; "2" -> "auto"; "3" -> "dry"; "4" -> "fan"; else -> "auto" }
                                                val modeIconRes = when(acDevice.acMode) { "0" -> R.drawable.cold; "1" -> R.drawable.heat; "2" -> R.drawable.auto; "3" -> R.drawable.humidi; "4" -> R.drawable.fan; else -> R.drawable.auto }
                                                
                                                Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(modeStr, color = Color.White, fontWeight = FontWeight.Normal, fontSize = 14.sp)
                                                        androidx.compose.material3.Icon(
                                                            painter = androidx.compose.ui.res.painterResource(id = modeIconRes),
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))

                                                val isModeFocused = focusedItem == "ac_mode_btn"
                                                val modeContentColor = if (isModeFocused) Color(0xFF1E1E1E) else Color.White
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(50))
                                                        .background(if (isModeFocused) Color.White else Color.White.copy(alpha = 0.3f))
                                                        .onFocusChanged { if (it.isFocused) focusedItem = "ac_mode_btn" }
                                                        .focusRequester(acModeFocusRequester)
                                                        .clickable(enabled = acDevice.deviceId != null) {
                                                            val modeList = listOf("0", "1", "2", "3", "4") // COLD, HEAT, AUTO, DRY, FAN
                                                            val nextMode = modeList[(maxOf(0, modeList.indexOf(acDevice.acMode)) + 1) % modeList.size]
                                                            DataRepository.setIgnoreDeviceUpdates(acDevice.deviceId)
                                                            DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                                if (d.deviceId == acDevice.deviceId) d.copy(acMode = nextMode) else d 
                                                            }
                                                            coroutineScope.launch {
                                                                sendTuyaCommand(acDevice.deviceId, "M", nextMode)
                                                            }
                                                        },
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
                                                val fanBars = when(acDevice.acFan) { "1" -> 1; "2" -> 2; "3" -> 3; else -> 0 }
                                                Box(
                                                    modifier = Modifier.height(24.dp).offset(y = 2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        for (i in 1..3) {
                                                            val isActive = i <= fanBars
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(4.dp)
                                                                    .height(10.dp)
                                                                    .clip(RoundedCornerShape(2.dp))
                                                                    .background(if (isActive) Color.White else Color.White.copy(alpha = 0.3f))
                                                            )
                                                        }
                                                        
                                                        val isAutoActive = acDevice.acFan == "0" || acDevice.acFan == "auto"
                                                        androidx.compose.material3.Icon(
                                                            painter = androidx.compose.ui.res.painterResource(id = R.drawable.auto),
                                                            contentDescription = "Auto",
                                                            tint = if (isAutoActive) Color.White else Color.White.copy(alpha = 0.3f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))

                                                val isFanFocused = focusedItem == "ac_fan_btn"
                                                val fanContentColor = if (isFanFocused) Color(0xFF1E1E1E) else Color.White
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                        .background(if (isFanFocused) Color.White else Color.White.copy(alpha = 0.3f))
                                                        .onFocusChanged { if (it.isFocused) focusedItem = "ac_fan_btn" }
                                                        .focusRequester(acFanFocusRequester)
                                                        .clickable(enabled = acDevice.deviceId != null) {
                                                            val fanModes = listOf("0", "1", "2", "3")
                                                            val nextFan = fanModes[(maxOf(0, fanModes.indexOf(acDevice.acFan)) + 1) % fanModes.size]
                                                            DataRepository.setIgnoreDeviceUpdates(acDevice.deviceId)
                                                            DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                                if (d.deviceId == acDevice.deviceId) d.copy(acFan = nextFan) else d 
                                                            }
                                                            coroutineScope.launch {
                                                                sendTuyaCommand(acDevice.deviceId, "F", nextFan)
                                                            }
                                                        },
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
                                                val powerText = if (acDevice.acPowerState) "on" else "off"
                                                Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                                                    Text(powerText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))

                                                val isPowerFocused = focusedItem == "ac_power_btn"
                                                val powerContentColor = if (isPowerFocused) Color(0xFF1E1E1E) else if (acDevice.acPowerState) Color(0xFF00E9F8) else Color.White
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                        .background(if (isPowerFocused) Color.White else Color.White.copy(alpha = 0.3f))
                                                        .onFocusChanged { if (it.isFocused) focusedItem = "ac_power_btn" }
                                                        .focusRequester(acPowerFocusRequester)
                                                        .clickable(enabled = acDevice.deviceId != null) {
                                                            coroutineScope.launch {
                                                                val newState = !acDevice.acPowerState
                                                                DataRepository.setIgnoreDeviceUpdates(acDevice.deviceId)
                                                                DataRepository.smartDevicesList.value = DataRepository.smartDevicesList.value.map { d -> 
                                                                    if (d.deviceId == acDevice.deviceId) d.copy(acPowerState = newState) else d 
                                                                }
                                                                if (newState) sendTuyaCommand(acDevice.deviceId, "PowerOn", "PowerOn")
                                                                else sendTuyaCommand(acDevice.deviceId, "PowerOff", "PowerOff")
                                                            }
                                                        },
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
                                        .background(Color(207, 223, 237).copy(alpha = 0.3f))
                                        
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.weight(0.5f).fillMaxSize()
                                        ) {
                                            // TOP HEADER
                                            Column(
                                                modifier = Modifier.fillMaxWidth().offset(y = (-8).dp),
                                                horizontalAlignment = Alignment.Start
                                            ) {
                                                Text(
                                                    "Smart Curtain", 
                                                    color = Color.White, 
                                                    fontWeight = FontWeight.Bold, 
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    "experimental", 
                                                    color = Color.White.copy(alpha = 0.5f), 
                                                    fontSize = 12.sp, 
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    modifier = Modifier.offset(y = (-6).dp)
                                                )
                                            }
                                            
                                            Column(
                                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
                                                verticalArrangement = Arrangement.Bottom,
                                                horizontalAlignment = Alignment.Start
                                            ) {
                                                Row(
                                                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                SmartActionBtn(
                                                    text = "",
                                                    iconRes = R.drawable.curtain_open,
                                                    isFocused = focusedItem == "curtain_open",
                                                    onFocus = { focusedItem = "curtain_open" },
                                                    onClickAction = { sendTuyaCommand(curtainDevice.deviceId, "control", "open") },
                                                    modifier = Modifier.size(52.dp)
                                                )
                                                
                                                SmartActionBtn(
                                                    text = "",
                                                    iconRes = R.drawable.pause,
                                                    isFocused = focusedItem == "curtain_pause",
                                                    onFocus = { focusedItem = "curtain_pause" },
                                                    onClickAction = { sendTuyaCommand(curtainDevice.deviceId, "control", "stop") },
                                                    modifier = Modifier.size(52.dp)
                                                )
                                                
                                                SmartActionBtn(
                                                    text = "",
                                                    iconRes = R.drawable.curtain_close,
                                                    isFocused = focusedItem == "curtain_close",
                                                    onFocus = { focusedItem = "curtain_close" },
                                                    onClickAction = { sendTuyaCommand(curtainDevice.deviceId, "control", "close") },
                                                    modifier = Modifier.size(52.dp)
                                                )
                                            } // end of Row
                                            } // end of Column
                                        } // end of Box (left area)
                                        val leftCurtainFocus = remember { androidx.compose.ui.focus.FocusRequester() }
                                        val rightCurtainFocus = remember { androidx.compose.ui.focus.FocusRequester() }

                                        Box(
                                            modifier = Modifier
                                                .weight(0.5f)
                                                .fillMaxHeight()
                                        ) {
                                            CurtainVisualizer(
                                                deviceId = curtainDevice.deviceId,
                                                curtainState = curtainDevice.curtainState,
                                                curtainPercent = curtainDevice.curtainPercent,
                                                focusedThumb = when(focusedItem) {
                                                    "curtain_slider_left" -> "left"
                                                    "curtain_slider_right" -> "right"
                                                    else -> null
                                                },
                                                isSliderActive = isCurtainSliderActive,
                                                localPercent = if (isCurtainSliderActive) curtainSliderLocalPercent else null,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            
                                            Row(modifier = Modifier.fillMaxSize()) {
                                                // LEFT THUMB CONTROL
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .focusRequester(leftCurtainFocus)
                                                        .onFocusChanged { if (it.isFocused) focusedItem = "curtain_slider_left" }
                                                        .focusable()
                                                        .onPreviewKeyEvent { event ->
                                                            if (event.type == KeyEventType.KeyDown) {
                                                                if (isCurtainSliderActive) {
                                                                    when (event.key) {
                                                                        Key.DirectionLeft -> {
                                                                            curtainSliderLocalPercent = maxOf(0, curtainSliderLocalPercent - 1)
                                                                            true
                                                                        }
                                                                        Key.DirectionRight -> {
                                                                            curtainSliderLocalPercent = minOf(100, curtainSliderLocalPercent + 1)
                                                                            true
                                                                        }
                                                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Back, Key.Escape -> true
                                                                        else -> true
                                                                    }
                                                                } else false
                                                            } else if (event.type == KeyEventType.KeyUp) {
                                                                if (isCurtainSliderActive) {
                                                                    when (event.key) {
                                                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Back, Key.Escape -> {
                                                                            coroutineScope.launch {
                                                                                sendTuyaCommand(curtainDevice.deviceId, "control", "stop")
                                                                                sendTuyaCommand(curtainDevice.deviceId, "percent_control", curtainSliderLocalPercent)
                                                                            }
                                                                            isCurtainSliderActive = false
                                                                            true
                                                                        }
                                                                        Key.DirectionLeft, Key.DirectionRight -> {
                                                                            coroutineScope.launch {
                                                                                sendTuyaCommand(curtainDevice.deviceId, "control", "stop")
                                                                                sendTuyaCommand(curtainDevice.deviceId, "percent_control", curtainSliderLocalPercent)
                                                                            }
                                                                            true
                                                                        }
                                                                        else -> true
                                                                    }
                                                                } else {
                                                                    if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                                                        val currentVisualProgress = DataRepository.localCurtainPositions[curtainDevice.deviceId] ?: (1f - (curtainDevice.curtainPercent / 100f))
                                                                        curtainSliderLocalPercent = ((1f - currentVisualProgress) * 100).toInt().coerceIn(0, 100)
                                                                        isCurtainSliderActive = true
                                                                        true
                                                                    } else false
                                                                }
                                                            } else false
                                                        }
                                                )
                                                // RIGHT THUMB CONTROL
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxHeight()
                                                        .focusRequester(rightCurtainFocus)
                                                        .onFocusChanged { if (it.isFocused) focusedItem = "curtain_slider_right" }
                                                        .focusable()
                                                        .onPreviewKeyEvent { event ->
                                                            if (event.type == KeyEventType.KeyDown) {
                                                                if (isCurtainSliderActive) {
                                                                    when (event.key) {
                                                                        Key.DirectionLeft -> {
                                                                            curtainSliderLocalPercent = minOf(100, curtainSliderLocalPercent + 1)
                                                                            true
                                                                        }
                                                                        Key.DirectionRight -> {
                                                                            curtainSliderLocalPercent = maxOf(0, curtainSliderLocalPercent - 1)
                                                                            true
                                                                        }
                                                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Back, Key.Escape -> true
                                                                        else -> true
                                                                    }
                                                                } else false
                                                            } else if (event.type == KeyEventType.KeyUp) {
                                                                if (isCurtainSliderActive) {
                                                                    when (event.key) {
                                                                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Back, Key.Escape -> {
                                                                            coroutineScope.launch {
                                                                                sendTuyaCommand(curtainDevice.deviceId, "control", "stop")
                                                                                sendTuyaCommand(curtainDevice.deviceId, "percent_control", curtainSliderLocalPercent)
                                                                            }
                                                                            isCurtainSliderActive = false
                                                                            true
                                                                        }
                                                                        Key.DirectionLeft, Key.DirectionRight -> {
                                                                            coroutineScope.launch {
                                                                                sendTuyaCommand(curtainDevice.deviceId, "control", "stop")
                                                                                sendTuyaCommand(curtainDevice.deviceId, "percent_control", curtainSliderLocalPercent)
                                                                            }
                                                                            true
                                                                        }
                                                                        else -> true
                                                                    }
                                                                } else {
                                                                    if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                                                                        val currentVisualProgress = DataRepository.localCurtainPositions[curtainDevice.deviceId] ?: (1f - (curtainDevice.curtainPercent / 100f))
                                                                        curtainSliderLocalPercent = ((1f - currentVisualProgress) * 100).toInt().coerceIn(0, 100)
                                                                        isCurtainSliderActive = true
                                                                        true
                                                                    } else false
                                                                }
                                                            } else false
                                                        }
                                                )
                                            }
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
                                        .background(Color(207, 223, 237).copy(alpha = 0.3f))
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.8f to Color.White.copy(alpha = 0.4f),
                                1.0f to Color.White.copy(alpha = 0.4f)
                            )
                        )
                )

                // Main Top Surface
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (state) 2.dp else 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.8f to if (state) Color.White else Color.White.copy(alpha = 0.4f),
                                1.0f to if (state) Color.White else Color.White.copy(alpha = 0.4f)
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
                    
                    val contentColor = if (isFocused) Color(0xFF1E1E1E) else Color.Black.copy(alpha = 0.6f)
                    
                    // Hanging Lamp & Light Beam Animation
                    androidx.compose.animation.AnimatedVisibility(
                        visible = state,
                        enter = androidx.compose.animation.slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = androidx.compose.animation.core.tween(500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)),
                        exit = androidx.compose.animation.slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = androidx.compose.animation.core.tween(500, delayMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                        ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300, delayMillis = 600)),
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.85f), // Light beam spans down to cover most of the switch
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Lamp Icon
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.hanging_lamp),
                                contentDescription = "Hanging Lamp",
                                modifier = Modifier.size(56.dp)
                            )
                            
                            val lightAlpha by transition.animateFloat(
                                transitionSpec = {
                                    if (targetState == androidx.compose.animation.EnterExitState.Visible) {
                                        androidx.compose.animation.core.tween(durationMillis = 300, delayMillis = 700)
                                    } else {
                                        androidx.compose.animation.core.tween(durationMillis = 300)
                                    }
                                },
                                label = "LightAlpha"
                            ) { enterExitState ->
                                if (enterExitState == androidx.compose.animation.EnterExitState.Visible) 1f else 0f
                            }
                            
                            // Lit Lamp Icon (overlays the unlit one)
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.hanging_lamp_lit),
                                contentDescription = "Hanging Lamp Lit",
                                modifier = Modifier
                                    .size(56.dp)
                                    .alpha(lightAlpha)
                            )
                            
                            // Light Beam Cone (Sequenced via alpha)
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(lightAlpha)
                            ) {
                                val bulbCenterY = (112.5f / 128f) * 56.dp.toPx()
                                val bulbRadius = (14f / 128f) * 56.dp.toPx()
                                
                                // Light Beam Cone
                                val lampBottomY = bulbCenterY + bulbRadius * 0.2f
                                val topBeamWidth = 20.dp.toPx() // Restore the wide base of the light beam
                                
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(size.width / 2f - topBeamWidth, lampBottomY)
                                    lineTo(size.width / 2f + topBeamWidth, lampBottomY)
                                    lineTo(size.width * 0.9f, size.height) // Widen towards the bottom
                                    lineTo(size.width * 0.1f, size.height)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        0.0f to Color(0xFFFFD700).copy(alpha = 0.0f),  // Transparent at the very top
                                        0.15f to Color(0xFFFFD700).copy(alpha = 0.25f), // Much softer peak brightness
                                        0.5f to Color(0xFFFFD700).copy(alpha = 0.05f),  // Gently fades in the middle
                                        1.0f to Color(0xFFFFD700).copy(alpha = 0.0f),  // Fully transparent at the bottom
                                        startY = lampBottomY,
                                        endY = size.height
                                    )
                                )
                            }
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val indicatorColor = if (state) Color(0xFF00E9F8) else Color.Black.copy(alpha = 0.2f)
                        
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
    useGradient: Boolean = false,
    transparentWhenUnfocused: Boolean = false
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
            containerColor = if (useGradient) Color.Transparent else if (isFocused) Color.White else if (isActive == true) Color(0xFF00E9F8) else if (transparentWhenUnfocused) Color.Transparent else Color.White.copy(alpha = 0.3f)
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

@Composable
fun CurtainVisualizer(
    deviceId: String,
    curtainState: String, // "open", "close", "stop"
    curtainPercent: Int, // 0-100 real percentage (0 = open, 100 = closed)
    focusedThumb: String?, // "left", "right", or null
    isSliderActive: Boolean = false,
    localPercent: Int? = null,
    modifier: Modifier = Modifier
) {
    // Read initial position from app memory (defaults to 0f if not yet saved)
    val initialPercent = androidx.compose.runtime.remember { DataRepository.localCurtainPositions[deviceId] ?: 0f }
    val openProgress = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(initialPercent) }
    
    val isInitialLoad = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    var prevState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(curtainState) }
    var prevPercent = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(curtainPercent) }
    
    // Save current position to app memory constantly as it animates
    androidx.compose.runtime.LaunchedEffect(openProgress.value) {
        DataRepository.localCurtainPositions[deviceId] = openProgress.value
    }
    
    androidx.compose.runtime.LaunchedEffect(localPercent) {
        if (localPercent != null) {
            val targetProgress = 1f - (localPercent / 100f)
            openProgress.animateTo(targetProgress, androidx.compose.animation.core.tween(300))
        }
    }
    
    androidx.compose.runtime.LaunchedEffect(curtainState, curtainPercent) {
        if (localPercent != null) return@LaunchedEffect
        val targetProgress = 1f - (curtainPercent / 100f)
        
        if (isInitialLoad.value) {
            isInitialLoad.value = false
            prevState.value = curtainState
            prevPercent.value = curtainPercent
            return@LaunchedEffect
        }
        
        if (curtainState != prevState.value) {
            prevState.value = curtainState
            when (curtainState) {
                "open" -> {
                    val durationLeft = ((1f - openProgress.value) * 5000).toInt()
                    if (durationLeft > 0) {
                        openProgress.animateTo(1f, androidx.compose.animation.core.tween(durationLeft, easing = androidx.compose.animation.core.LinearEasing))
                    }
                }
                "close" -> {
                    val durationLeft = (openProgress.value * 5000).toInt()
                    if (durationLeft > 0) {
                        openProgress.animateTo(0f, androidx.compose.animation.core.tween(durationLeft, easing = androidx.compose.animation.core.LinearEasing))
                    }
                }
                "stop" -> openProgress.stop()
            }
        } else if (curtainPercent != prevPercent.value) {
            prevPercent.value = curtainPercent
            val distance = kotlin.math.abs(targetProgress - openProgress.value)
            val duration = (distance * 5000).toInt()
            if (duration > 0) {
                openProgress.animateTo(targetProgress, androidx.compose.animation.core.tween(duration, easing = androidx.compose.animation.core.LinearEasing))
            }
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition()
    val waveAnim = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        )
    )
    
    val leftIconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.left)
    val rightIconPainter = androidx.compose.ui.res.painterResource(id = R.drawable.right)
    
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val railHeight = 12.dp.toPx()
        val railY = 0f
        
        val dotRadius = 6.dp.toPx()
        val outerDotRadius = dotRadius + 6.dp.toPx()
        
        val maxOpenDistance = (w / 2f) - (dotRadius * 2f)
        val currentOpenOffset = openProgress.value * maxOpenDistance
        
        val pleatCount = 6
        val centerGap = 1.dp.toPx() // Gap from center (total gap will be 2.dp)
        val basePleatWidth = ((w / 2f) - centerGap) / pleatCount
        val compressedPleatWidth = ((w / 2f) - maxOpenDistance) / pleatCount
        
        val currentPleatWidth = basePleatWidth - (basePleatWidth - compressedPleatWidth) * openProgress.value
        val curtainExtension = dotRadius
        
        // Draw Left Curtain
        for (i in pleatCount - 1 downTo 0) {
            val pleatX = currentPleatWidth * i
            val path = androidx.compose.ui.graphics.Path()
            path.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = pleatX,
                    top = railY + railHeight / 2f,
                    right = pleatX + currentPleatWidth * if (i == pleatCount - 1) 1.0f else 1.15f,
                    bottom = h,
                    topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero,
                    topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero,
                    bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                    bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero
                )
            )
            drawPath(
                path = path, 
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.3f),
                    1.0f to Color.White.copy(alpha = 0f),
                    startY = railY + railHeight / 2f,
                    endY = h
                )
            )
        }
        
        // Draw Right Curtain
        for (i in pleatCount - 1 downTo 0) {
            val pleatX = w - (currentPleatWidth * (i + 1))
            val path = androidx.compose.ui.graphics.Path()
            path.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = pleatX - currentPleatWidth * if (i == pleatCount - 1) 0.0f else 0.15f,
                    top = railY + railHeight / 2f,
                    right = pleatX + currentPleatWidth,
                    bottom = h,
                    topLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero,
                    topRightCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero,
                    bottomRightCornerRadius = androidx.compose.ui.geometry.CornerRadius.Zero,
                    bottomLeftCornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                )
            )
            drawPath(
                path = path, 
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.3f),
                    1.0f to Color.White.copy(alpha = 0f),
                    startY = railY + railHeight / 2f,
                    endY = h
                )
            )
        }
        
        // Rail Drop Shadow
        val shadowPaint = androidx.compose.ui.graphics.Paint().apply {
            color = Color.Black.copy(alpha = 0.3f)
            asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(
                6.dp.toPx(),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        drawContext.canvas.drawRoundRect(
            left = 0f,
            top = railY + 4.dp.toPx(),
            right = w,
            bottom = railY + railHeight + 4.dp.toPx(),
            radiusX = railHeight / 2f,
            radiusY = railHeight / 2f,
            paint = shadowPaint
        )
        
        // Draw Rail
        drawRoundRect(
            color = Color.White.copy(alpha = 0.6f),
            topLeft = androidx.compose.ui.geometry.Offset(0f, railY),
            size = androidx.compose.ui.geometry.Size(w, railHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(railHeight / 2)
        )
        
        // Draw Indicator Dots
        val leftDotX = (currentPleatWidth * pleatCount) - curtainExtension
        val rightDotX = w - (currentPleatWidth * pleatCount) + curtainExtension
        
        val dotColor = Color(0xFF00E9F8)
        val blueRingThickness = 2.dp.toPx()
        
        // --- Left dot ---
        if (focusedThumb == "left" && isSliderActive) {
            val leftCenter = androidx.compose.ui.geometry.Offset(leftDotX, railY + railHeight / 2f)
            val waveSpacing = 8.dp.toPx()
            val iconSize = 12.dp.toPx()
            
            for (i in 0 until 2) { // 2 arrows sequentially
                val progress = i + waveAnim.value
                val xOffset = progress * waveSpacing
                val waveAlpha = when {
                    progress < 0.3f -> progress / 0.3f // fade in quickly
                    progress > 1.2f -> (2f - progress) / 0.8f // fade out slowly
                    else -> 1f
                }.coerceIn(0f, 1f)
                
                // Left Arrow (only if not fully open)
                if (localPercent != 0) {
                    translate(
                        left = leftCenter.x - dotRadius - iconSize - xOffset,
                        top = leftCenter.y - (iconSize / 2f)
                    ) {
                        with(leftIconPainter) {
                            draw(size = androidx.compose.ui.geometry.Size(iconSize, iconSize), alpha = waveAlpha, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(dotColor))
                        }
                    }
                }
                
                // Right Arrow (only if not fully closed)
                if (localPercent != 100) {
                    translate(
                        left = leftCenter.x + dotRadius + xOffset,
                        top = leftCenter.y - (iconSize / 2f)
                    ) {
                        with(rightIconPainter) {
                            draw(size = androidx.compose.ui.geometry.Size(iconSize, iconSize), alpha = waveAlpha, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(dotColor))
                        }
                    }
                }
            }
        }
        drawCircle(
            color = if (focusedThumb == "left" && !isSliderActive) Color.White.copy(alpha = 0.6f) else Color.Transparent,
            radius = dotRadius + 6.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(leftDotX, railY + railHeight / 2f)
        )
        // Big Blueish Dot
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = androidx.compose.ui.geometry.Offset(leftDotX, railY + railHeight / 2f)
        )
        // Thin White Ring
        drawCircle(
            color = Color.White,
            radius = dotRadius,
            center = androidx.compose.ui.geometry.Offset(leftDotX, railY + railHeight / 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = blueRingThickness)
        )

        // --- Right dot ---
        if (focusedThumb == "right" && isSliderActive) {
            val rightCenter = androidx.compose.ui.geometry.Offset(rightDotX, railY + railHeight / 2f)
            val waveSpacing = 8.dp.toPx()
            val iconSize = 12.dp.toPx()
            
            for (i in 0 until 2) {
                val progress = i + waveAnim.value
                val xOffset = progress * waveSpacing
                val waveAlpha = when {
                    progress < 0.3f -> progress / 0.3f // fade in quickly
                    progress > 1.2f -> (2f - progress) / 0.8f // fade out slowly
                    else -> 1f
                }.coerceIn(0f, 1f)
                
                // Left Arrow (only if not fully closed)
                if (localPercent != 100) {
                    translate(
                        left = rightCenter.x - dotRadius - iconSize - xOffset,
                        top = rightCenter.y - (iconSize / 2f)
                    ) {
                        with(leftIconPainter) {
                            draw(size = androidx.compose.ui.geometry.Size(iconSize, iconSize), alpha = waveAlpha, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(dotColor))
                        }
                    }
                }
                
                // Right Arrow (only if not fully open)
                if (localPercent != 0) {
                    translate(
                        left = rightCenter.x + dotRadius + xOffset,
                        top = rightCenter.y - (iconSize / 2f)
                    ) {
                        with(rightIconPainter) {
                            draw(size = androidx.compose.ui.geometry.Size(iconSize, iconSize), alpha = waveAlpha, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(dotColor))
                        }
                    }
                }
            }
        }
        drawCircle(
            color = if (focusedThumb == "right" && !isSliderActive) Color.White.copy(alpha = 0.6f) else Color.Transparent,
            radius = dotRadius + 6.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(rightDotX, railY + railHeight / 2f)
        )
        // Big Blueish Dot
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = androidx.compose.ui.geometry.Offset(rightDotX, railY + railHeight / 2f)
        )
        // Thin White Ring
        drawCircle(
            color = Color.White,
            radius = dotRadius,
            center = androidx.compose.ui.geometry.Offset(rightDotX, railY + railHeight / 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = blueRingThickness)
        )
    }
}


