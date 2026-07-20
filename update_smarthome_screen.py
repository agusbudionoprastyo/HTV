import re

with open('app/src/main/java/com/dafamsemarang/dhtv/SmartHomeScreen.kt', 'r') as f:
    content = f.read()

# Update sendTuyaCommand
old_send = """suspend fun sendTuyaCommand(deviceId: String, switchCode: String, value: Boolean): Boolean {"""
new_send = """suspend fun sendTuyaCommand(deviceId: String, switchCode: String, value: Any): Boolean {"""
content = content.replace(old_send, new_send)

# Replace the formatting of value in the json payload for sendTuyaCommand
old_json = """                    "commands": [
                        { "code": "$switchCode", "value": $value }
                    ]"""
new_json = """                    "commands": [
                        { "code": "$switchCode", "value": ${if (value is String) "\"$value\"" else value} }
                    ]"""
content = content.replace(old_json, new_json)

# Now for the main UI structure. We will overwrite from SmartHomeScreen down to the end.
# We'll regex replace everything from `@Composable\nfun SmartHomeScreen` to the end of the file.

new_ui = """@Composable
fun SmartHomeScreen(navController: androidx.navigation.NavHostController? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val switch1Id by DataRepository.tuyaSwitch1Id
    val switch2Id by DataRepository.tuyaSwitch2Id
    val switch3Id by DataRepository.tuyaSwitch3Id
    val acId by DataRepository.tuyaAcId
    val curtainId by DataRepository.tuyaCurtainId
    
    val switch1Name by DataRepository.tuyaSwitch1Name
    val switch2Name by DataRepository.tuyaSwitch2Name
    val switch3Name by DataRepository.tuyaSwitch3Name
    val acName by DataRepository.tuyaAcName
    val curtainName by DataRepository.tuyaCurtainName
    
    var switch1State by DataRepository.tuyaSwitch1State
    var switch2State by DataRepository.tuyaSwitch2State
    var switch3State by DataRepository.tuyaSwitch3State
    
    var acPowerState by DataRepository.tuyaAcPowerState
    var acTemp by DataRepository.tuyaAcTemp
    var acMode by DataRepository.tuyaAcMode
    
    var curtainState by DataRepository.tuyaCurtainState

    var focusedItem by remember { mutableStateOf<String?>("switch1") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(207, 223, 237).copy(alpha = 0.15f))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Smart Room Controls",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Switches Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Lights", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        SmartSwitchWidget("switch1", switch1Name ?: "Switch 1", switch1Id, switch1State, focusedItem == "switch1", { focusedItem = "switch1" }) {
                            coroutineScope.launch {
                                if (switch1Id != null) {
                                    val success = sendTuyaCommand(switch1Id!!, "switch_1", !switch1State)
                                    if (success) switch1State = !switch1State
                                }
                            }
                        }
                        SmartSwitchWidget("switch2", switch2Name ?: "Switch 2", switch2Id, switch2State, focusedItem == "switch2", { focusedItem = "switch2" }) {
                            coroutineScope.launch {
                                if (switch2Id != null) {
                                    val success = sendTuyaCommand(switch2Id!!, "switch_1", !switch2State)
                                    if (success) switch2State = !switch2State
                                }
                            }
                        }
                        SmartSwitchWidget("switch3", switch3Name ?: "Switch 3", switch3Id, switch3State, focusedItem == "switch3", { focusedItem = "switch3" }) {
                            coroutineScope.launch {
                                if (switch3Id != null) {
                                    val success = sendTuyaCommand(switch3Id!!, "switch_1", !switch3State)
                                    if (success) switch3State = !switch3State
                                }
                            }
                        }
                    }

                    // AC Column
                    Column(
                        modifier = Modifier.weight(1.5f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(acName ?: "Air Conditioner", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(
                                    width = if (focusedItem == "ac_temp" || focusedItem == "ac_power") 3.dp else 0.dp,
                                    color = if (focusedItem == "ac_temp" || focusedItem == "ac_power") Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text("${acTemp}°C", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                if (acId != null) {
                                                    val success = sendTuyaCommand(acId!!, "temp_set", acTemp - 1)
                                                    if (success) acTemp -= 1
                                                }
                                            }
                                        },
                                        modifier = Modifier.onFocusChanged { if (it.isFocused) focusedItem = "ac_temp" }
                                    ) { Text("-") }
                                    
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                if (acId != null) {
                                                    val success = sendTuyaCommand(acId!!, "switch", !acPowerState)
                                                    if (success) acPowerState = !acPowerState
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (acPowerState) Color.Green.copy(0.7f) else Color.Red.copy(0.7f)),
                                        modifier = Modifier.onFocusChanged { if (it.isFocused) focusedItem = "ac_power" }.focusRequester(SmartRoomGlobalFocus.focusRequester)
                                    ) { Text(if (acPowerState) "ON" else "OFF") }
                                    
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                if (acId != null) {
                                                    val success = sendTuyaCommand(acId!!, "temp_set", acTemp + 1)
                                                    if (success) acTemp += 1
                                                }
                                            }
                                        },
                                        modifier = Modifier.onFocusChanged { if (it.isFocused) focusedItem = "ac_temp" }
                                    ) { Text("+") }
                                }
                            }
                        }
                    }

                    // Curtain Column
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(curtainName ?: "Curtain", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(
                                    width = if (focusedItem == "curtain") 3.dp else 0.dp,
                                    color = if (focusedItem == "curtain") Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (curtainId != null) {
                                                val success = sendTuyaCommand(curtainId!!, "control", "open")
                                                if (success) curtainState = "open"
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusedItem = "curtain" }
                                ) { Text("Open") }
                                
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (curtainId != null) {
                                                val success = sendTuyaCommand(curtainId!!, "control", "stop")
                                                if (success) curtainState = "stop"
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusedItem = "curtain" }
                                ) { Text("Pause") }
                                
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (curtainId != null) {
                                                val success = sendTuyaCommand(curtainId!!, "control", "close")
                                                if (success) curtainState = "close"
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) focusedItem = "curtain" }
                                ) { Text("Close") }
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
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (state) Color.Yellow.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f))
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = deviceId != null) { onToggle() }
            .onFocusChanged { if (it.isFocused) onFocus() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = if (state) Color.Black else Color.White, fontWeight = FontWeight.Bold)
            Text(if (state) "ON" else "OFF", color = if (state) Color.Black else Color.White)
        }
    }
}
"""

# Replace everything starting from @Composable fun SmartHomeScreen
start_idx = content.find("@Composable\nfun SmartHomeScreen")
if start_idx != -1:
    content = content[:start_idx] + new_ui

with open('app/src/main/java/com/dafamsemarang/dhtv/SmartHomeScreen.kt', 'w') as f:
    f.write(content)

