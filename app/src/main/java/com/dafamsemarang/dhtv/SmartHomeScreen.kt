package com.dafamsemarang.dhtv

import androidx.compose.ui.composed
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat

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
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // BARIS 1: AC (1/4 lebar) dan Curtain (3/4 lebar)
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // AC Card (1/4)
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
                                        
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        val composition by com.airbnb.lottie.compose.rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.RawRes(R.raw.split_ac))
                                        val progress by com.airbnb.lottie.compose.animateLottieCompositionAsState(
                                            composition = composition,
                                            iterations = com.airbnb.lottie.compose.LottieConstants.IterateForever
                                        )
                                        com.airbnb.lottie.compose.LottieAnimation(
                                            composition = composition,
                                            progress = { progress },
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${acDevice.acTemp}°C", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SmartActionBtn(
                                            iconRes = R.drawable.ic_power_batton,
                                            useGradient = true,
                                            isActive = acDevice.acPowerState,
                                            isFocused = focusedItem == "ac_power",
                                            onFocus = { focusedItem = "ac_power" },
                                            onClickAction = { sendTuyaCommand(acDevice.deviceId, "switch", !acDevice.acPowerState) },
                                            modifier = Modifier.size(40.dp).focusRequester(SmartRoomGlobalFocus.focusRequester)
                                        )
                                        
                                        SmartActionBtn(
                                            text = "MODE",
                                            isFocused = focusedItem == "ac_mode",
                                            onFocus = { focusedItem = "ac_mode" },
                                            onClickAction = {  },
                                            modifier = Modifier.width(76.dp).height(40.dp),
                                            fontSize = 12.sp,
                                            useGradient = true
                                        )
                                        
                                        SmartActionBtn(
                                            iconRes = R.drawable.ic_add,
                                            useGradient = true,
                                            isFocused = focusedItem == "ac_temp_up",
                                            onFocus = { focusedItem = "ac_temp_up" },
                                            onClickAction = { sendTuyaCommand(acDevice.deviceId, "temp_set", acDevice.acTemp + 1) },
                                            modifier = Modifier.size(40.dp)
                                        )

                                        SmartActionBtn(
                                            iconRes = R.drawable.ic_minus,
                                            useGradient = true,
                                            isFocused = focusedItem == "ac_temp_down",
                                            onFocus = { focusedItem = "ac_temp_down" },
                                            onClickAction = { sendTuyaCommand(acDevice.deviceId, "temp_set", acDevice.acTemp - 1) },
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }
                    
                    // Curtain Card
                    Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
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
@Composable
fun SmartSwitchWidget(id: String, name: String, deviceId: String?, state: Boolean, isFocused: Boolean, onFocus: () -> Unit, onToggle: suspend () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    val borderAlpha = remember { Animatable(0.4f) }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            borderAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            borderAlpha.snapTo(0.4f)
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition()
    val ledRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "LedRotation"
    )
    


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                
                .then(
                    if (isLoading) {
                        Modifier.drawWithCache {
                            val strokeWidth = 3.dp.toPx()
                            val stroke = Stroke(width = strokeWidth)
                            val inset = strokeWidth / 2f
                            onDrawWithContent {
                                drawContent()
                                val shader = android.graphics.SweepGradient(
                                    size.width / 2f, 
                                    size.height / 2f, 
                                    intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.parseColor("#80FFFFFF"), android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT),
                                    floatArrayOf(0f, 0.4f, 0.5f, 1f)
                                )
                                val matrix = android.graphics.Matrix()
                                matrix.postRotate(ledRotation, size.width / 2f, size.height / 2f)
                                shader.setLocalMatrix(matrix)
                                val rotatedBrush = ShaderBrush(shader)

                                drawRoundRect(
                                    brush = rotatedBrush,
                                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                                    size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                                    style = stroke,
                                    cornerRadius = CornerRadius(14.dp.toPx() - inset, 14.dp.toPx() - inset)
                                )
                            }
                        }
                    } else if (isFocused) {
                        Modifier.border(
                            width = 3.dp,
                            color = Color.White.copy(alpha = borderAlpha.value),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else Modifier
                )
                .onFocusChanged { if (it.isFocused) onFocus() }
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = deviceId != null) {
                    if (!isLoading) {
                        coroutineScope.launch {
                            isLoading = true
                            onToggle()
                            kotlinx.coroutines.delay(1500)
                            isLoading = false
                        }
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
                            if (state) androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF29B6F6))
                            )
                            else androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.White)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Text Label
                        Text(
                            text = name,
                            color = if (state) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Indicator Bar
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (state) Color.White.copy(alpha = 0.8f)
                                    else Color.Black.copy(alpha = 0.4f)
                                )
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
    isActive: Boolean? = null,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onClickAction: suspend () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    useGradient: Boolean = false
) {
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            if (isLoading) return@Button
            coroutineScope.launch {
                isLoading = true
                onClickAction()
                kotlinx.coroutines.delay(1500)
                isLoading = false
            }
        },
        modifier = modifier
            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
            .then(
                if (useGradient) Modifier.background(Color.Transparent).border(2.dp, Color.White.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape) else Modifier
            )
            .onFocusChanged { if (it.isFocused) onFocus() }
            .smartButtonBorder(isFocused = isFocused, isLoading = isLoading),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (useGradient) Color.Transparent else if (isActive == true) Color(0xFF1976D2) else if (isActive == false) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha=0.15f)
        )
    ) {
        if (iconRes != null) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else if (icon != null) {
            androidx.compose.material3.Icon(imageVector = icon, contentDescription = null, tint = Color.White)
        }
        if ((icon != null || iconRes != null) && text.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (text.isNotEmpty()) {
            Text(text, fontSize = fontSize, color = Color.White)
        }
    }
}

fun Modifier.smartButtonBorder(
    isFocused: Boolean,
    isLoading: Boolean
): Modifier = composed {
    val borderAlpha = remember { Animatable(0.4f) }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            borderAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            borderAlpha.snapTo(0.4f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val ledRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "LedRotation"
    )

    this.then(
        if (isLoading) {
            Modifier.drawWithCache {
                val strokeWidth = 3.dp.toPx()
                val stroke = Stroke(width = strokeWidth)
                val inset = strokeWidth / 2f
                onDrawWithContent {
                    drawContent()
                    val shader = android.graphics.SweepGradient(
                        size.width / 2f, 
                        size.height / 2f, 
                        intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.parseColor("#80FFFFFF"), android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT),
                        floatArrayOf(0f, 0.4f, 0.5f, 1f)
                    )
                    val matrix = android.graphics.Matrix()
                    matrix.postRotate(ledRotation, size.width / 2f, size.height / 2f)
                    shader.setLocalMatrix(matrix)
                    val rotatedBrush = ShaderBrush(shader)

                    drawRoundRect(
                        brush = rotatedBrush,
                        topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = stroke,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f - inset, size.height / 2f - inset)
                    )
                }
            }
        } else if (isFocused) {
            Modifier.border(
                width = 3.dp,
                color = Color.White.copy(alpha = borderAlpha.value),
                shape = RoundedCornerShape(50)
            )
        } else Modifier
    )
}
