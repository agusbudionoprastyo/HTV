import re

file_path = "/Users/ag/Documents/GitHub/HTV/app/src/main/java/com/dafamsemarang/dhtv/SmartHomeScreen.kt"

with open(file_path, 'r') as f:
    content = f.read()

# 1. AC Minus
content = content.replace('iconRes = R.drawable.ic_minus,\n                                                isFocused = focusedItem == "ac_temp_down",', 
'iconRes = R.drawable.ic_minus,\n                                                isFocused = focusedItem == "ac_temp_down",\n                                                isLoading = (DataRepository.ignoreDeviceUpdatesUntil[acDevice.deviceId] ?: 0L) > System.currentTimeMillis(),')

# 2. AC Plus
content = content.replace('iconRes = R.drawable.ic_add,\n                                                isFocused = focusedItem == "ac_temp_up",', 
'iconRes = R.drawable.ic_add,\n                                                isFocused = focusedItem == "ac_temp_up",\n                                                isLoading = (DataRepository.ignoreDeviceUpdatesUntil[acDevice.deviceId] ?: 0L) > System.currentTimeMillis(),')

# 3. Curtain Open
content = content.replace('text = "Open",\n                                            isFocused = focusedItem == "curtain_open",',
'text = "Open",\n                                            isFocused = focusedItem == "curtain_open",\n                                            isLoading = (DataRepository.ignoreDeviceUpdatesUntil[curtainDevice.deviceId] ?: 0L) > System.currentTimeMillis(),')

# 4. Curtain Pause
content = content.replace('text = "Pause",\n                                            isFocused = focusedItem == "curtain_pause",',
'text = "Pause",\n                                            isFocused = focusedItem == "curtain_pause",\n                                            isLoading = (DataRepository.ignoreDeviceUpdatesUntil[curtainDevice.deviceId] ?: 0L) > System.currentTimeMillis(),')

# 5. Curtain Close
content = content.replace('text = "Close",\n                                            isFocused = focusedItem == "curtain_close",',
'text = "Close",\n                                            isFocused = focusedItem == "curtain_close",\n                                            isLoading = (DataRepository.ignoreDeviceUpdatesUntil[curtainDevice.deviceId] ?: 0L) > System.currentTimeMillis(),')

# 6. SmartSwitchWidget
content = content.replace('label = switchDevice.switch1Name ?: "Switch 1",\n                                                state = switchDevice.switch1State,',
'label = switchDevice.switch1Name ?: "Switch 1",\n                                                state = switchDevice.switch1State,\n                                                isLoading = (DataRepository.ignoreDeviceUpdatesUntil[switchDevice.deviceId] ?: 0L) > System.currentTimeMillis(),')

# 7. AC Mode Box
old_mode = 'val isModeFocused = focusedItem == "ac_mode_btn"\n                                                val modeContentColor = if (isModeFocused) Color(0xFF1E1E1E) else Color.White\n                                                Box(\n                                                    modifier = Modifier\n                                                        .fillMaxWidth()\n                                                        .height(44.dp)\n                                                        .clip(RoundedCornerShape(50))\n                                                        .background(if (isModeFocused) Color.White else Color.White.copy(alpha = 0.15f))'
new_mode = '''val isModeFocused = focusedItem == "ac_mode_btn"
                                                val modeContentColor = if (isModeFocused) Color(0xFF1E1E1E) else Color.White
                                                val isLoadingMode = (DataRepository.ignoreDeviceUpdatesUntil[acDevice.deviceId] ?: 0L) > System.currentTimeMillis()
                                                val modeShimmer = if (isLoadingMode && isModeFocused) rememberShimmerBrush() else null
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(50))
                                                        .then(if (modeShimmer != null) Modifier.background(modeShimmer) else Modifier.background(if (isModeFocused) Color.White else Color.White.copy(alpha = 0.15f)))'''
content = content.replace(old_mode, new_mode)

# 8. AC Fan Box
old_fan = 'val isFanFocused = focusedItem == "ac_fan_btn"\n                                                val fanContentColor = if (isFanFocused) Color(0xFF1E1E1E) else Color.White\n                                                Box(\n                                                    modifier = Modifier\n                                                        .size(44.dp)\n                                                        .clip(androidx.compose.foundation.shape.CircleShape)\n                                                        .background(if (isFanFocused) Color.White else Color.White.copy(alpha = 0.15f))'
new_fan = '''val isFanFocused = focusedItem == "ac_fan_btn"
                                                val fanContentColor = if (isFanFocused) Color(0xFF1E1E1E) else Color.White
                                                val isLoadingFan = (DataRepository.ignoreDeviceUpdatesUntil[acDevice.deviceId] ?: 0L) > System.currentTimeMillis()
                                                val fanShimmer = if (isLoadingFan && isFanFocused) rememberShimmerBrush() else null
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                        .then(if (fanShimmer != null) Modifier.background(fanShimmer) else Modifier.background(if (isFanFocused) Color.White else Color.White.copy(alpha = 0.15f)))'''
content = content.replace(old_fan, new_fan)

# 9. AC Power Box
old_power = 'val isPowerFocused = focusedItem == "ac_power_btn"\n                                                val powerContentColor = if (isPowerFocused) Color(0xFF1E1E1E) else Color.White\n                                                Box(\n                                                    modifier = Modifier\n                                                        .size(44.dp)\n                                                        .clip(androidx.compose.foundation.shape.CircleShape)\n                                                        .background(if (isPowerFocused) Color.White else if(acDevice.acPowerState) Color(0xFF29B6F6) else Color.White.copy(alpha = 0.15f))'
new_power = '''val isPowerFocused = focusedItem == "ac_power_btn"
                                                val powerContentColor = if (isPowerFocused) Color(0xFF1E1E1E) else Color.White
                                                val isLoadingPower = (DataRepository.ignoreDeviceUpdatesUntil[acDevice.deviceId] ?: 0L) > System.currentTimeMillis()
                                                val powerShimmer = if (isLoadingPower && isPowerFocused) rememberShimmerBrush() else null
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                        .then(if (powerShimmer != null) Modifier.background(powerShimmer) else Modifier.background(if (isPowerFocused) Color.White else if(acDevice.acPowerState) Color(0xFF29B6F6) else Color.White.copy(alpha = 0.15f)))'''
content = content.replace(old_power, new_power)

with open(file_path, 'w') as f:
    f.write(content)

print("Patch applied")
