import re

with open('app/src/main/java/com/dafamsemarang/dhtv/SmartHomeScreen.kt', 'r') as f:
    lines = f.readlines()

# find where @Composable fun SmartHomeScreen starts
start_idx = -1
for i, line in enumerate(lines):
    if line.strip() == "@Composable" and i+1 < len(lines) and "fun SmartHomeScreen" in lines[i+1]:
        start_idx = i
        break

if start_idx != -1:
    top_part = "".join(lines[:start_idx])
else:
    top_part = "".join(lines)

new_ui = """@Composable
fun SmartHomeScreen(navController: androidx.navigation.NavHostController? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val smartDevices by DataRepository.smartDevicesList
    var focusedItem by remember { mutableStateOf<String?>(null) }

    val acDevice = smartDevices.find { it.type == "ac" }
    val curtainDevice = smartDevices.find { it.type == "curtain" }
    val switchDevice = smartDevices.find { it.type == "switch" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(207, 223, 237).copy(alpha = 0.15f))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "Smart Room Controls",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (smartDevices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Smart devices belum dikonfigurasi\\nuntuk kamar ini melalui CMS.",
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
                        // BARIS 1: AC (2 kolom) dan Curtain (1 kolom)
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1.5f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // AC Widget
                            Box(modifier = Modifier.weight(2f)) {
                                if (acDevice != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .border(
                                                width = if (focusedItem == "ac_temp" || focusedItem == "ac_power") 3.dp else 0.dp,
                                                color = if (focusedItem == "ac_temp" || focusedItem == "ac_power") Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(acDevice.deviceName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("${acDevice.acTemp}°C", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            sendTuyaCommand(acDevice.deviceId, "temp_set", acDevice.acTemp - 1)
                                                        }
                                                    },
                                                    modifier = Modifier.onFocusChanged { if (it.isFocused) focusedItem = "ac_temp" }
                                                ) { Text("-") }
                                                
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            sendTuyaCommand(acDevice.deviceId, "switch", !acDevice.acPowerState)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = if (acDevice.acPowerState) Color.Green.copy(0.7f) else Color.Red.copy(0.7f)),
                                                    modifier = Modifier
                                                        .onFocusChanged { if (it.isFocused) focusedItem = "ac_power" }
                                                        .focusRequester(SmartRoomGlobalFocus.focusRequester)
                                                ) { Text(if (acDevice.acPowerState) "ON" else "OFF") }
                                                
                                                Button(
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            sendTuyaCommand(acDevice.deviceId, "temp_set", acDevice.acTemp + 1)
                                                        }
                                                    },
                                                    modifier = Modifier.onFocusChanged { if (it.isFocused) focusedItem = "ac_temp" }
                                                ) { Text("+") }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Curtain Widget
                            Box(modifier = Modifier.weight(1f)) {
                                if (curtainDevice != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .border(
                                                width = if (focusedItem == "curtain") 3.dp else 0.dp,
                                                color = if (focusedItem == "curtain") Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(curtainDevice.deviceName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { coroutineScope.launch { sendTuyaCommand(curtainDevice.deviceId, "control", "open") } },
                                                modifier = Modifier.fillMaxWidth(0.8f).onFocusChanged { if (it.isFocused) focusedItem = "curtain" }
                                            ) { Text("Open") }
                                            
                                            Button(
                                                onClick = { coroutineScope.launch { sendTuyaCommand(curtainDevice.deviceId, "control", "stop") } },
                                                modifier = Modifier.fillMaxWidth(0.8f).onFocusChanged { if (it.isFocused) focusedItem = "curtain" }
                                            ) { Text("Pause") }
                                            
                                            Button(
                                                onClick = { coroutineScope.launch { sendTuyaCommand(curtainDevice.deviceId, "control", "close") } },
                                                modifier = Modifier.fillMaxWidth(0.8f).onFocusChanged { if (it.isFocused) focusedItem = "curtain" }
                                            ) { Text("Close") }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // BARIS 2: Switch 1, 2, 3
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (switchDevice != null && switchDevice.switch1Name != null) {
                                    SmartSwitchWidget("${switchDevice.deviceId}_1", switchDevice.switch1Name!!, switchDevice.deviceId, switchDevice.switch1State, focusedItem == "${switchDevice.deviceId}_1", { focusedItem = "${switchDevice.deviceId}_1" }) {
                                        coroutineScope.launch { sendTuyaCommand(switchDevice.deviceId, "switch_1", !switchDevice.switch1State) }
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (switchDevice != null && switchDevice.switch2Name != null) {
                                    SmartSwitchWidget("${switchDevice.deviceId}_2", switchDevice.switch2Name!!, switchDevice.deviceId, switchDevice.switch2State, focusedItem == "${switchDevice.deviceId}_2", { focusedItem = "${switchDevice.deviceId}_2" }) {
                                        coroutineScope.launch { sendTuyaCommand(switchDevice.deviceId, "switch_2", !switchDevice.switch2State) }
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (switchDevice != null && switchDevice.switch3Name != null) {
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

@Composable
fun SmartSwitchWidget(id: String, name: String, deviceId: String?, state: Boolean, isFocused: Boolean, onFocus: () -> Unit, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(if (state) Color.Yellow.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f))
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = deviceId != null) { onToggle() }
            .onFocusChanged { if (it.isFocused) onFocus() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(name, color = if (state) Color.Black else Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (state) "ON" else "OFF", color = if (state) Color.Black else Color.White, fontSize = 18.sp)
        }
    }
}
"""

with open('app/src/main/java/com/dafamsemarang/dhtv/SmartHomeScreen.kt', 'w') as f:
    f.write(top_part + new_ui)

