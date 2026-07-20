import re

with open('app/src/main/java/com/dafamsemarang/dhtv/SmartHomeScreen.kt', 'r') as f:
    content = f.read()

# We want to replace the SmartSwitchWidget composable entirely.
old_widget = """@Composable
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
}"""

new_widget = """@Composable
fun SmartSwitchWidget(id: String, name: String, deviceId: String?, state: Boolean, isFocused: Boolean, onFocus: () -> Unit, onToggle: () -> Unit) {
    var isLoading by remember { mutableStateOf(false) }
    
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
    
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "SwitchScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .scale(scale)
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 3.dp,
                            color = Color.White.copy(alpha = borderAlpha.value),
                            shape = RoundedCornerShape(18.dp)
                        )
                    } else Modifier
                )
                .onFocusChanged { if (it.isFocused) onFocus() }
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = deviceId != null && !isLoading) {
                    isLoading = true
                    onToggle()
                    isLoading = false
                }
        ) {
            // Container for gap between focus border and switch
            Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                // 3D Base Lip (visible when OFF)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state) Color.Transparent else Color.Black.copy(alpha = 0.35f))
                )

                // Main Top Surface
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (state) 0.dp else 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (state) androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color(0xFF1976D2), Color(0xFF29B6F6))
                            )
                            else androidx.compose.ui.graphics.SolidColor(Color(0xFF424242).copy(alpha = 0.95f))
                        )
                        .border(
                            width = 1.dp,
                            color = if (state) Color.Transparent else Color.White.copy(alpha = 0.08f),
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
                                if (state) Color(0xFFB2EBF2)
                                else Color.White.copy(alpha = 0.15f)
                            )
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = name,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal
        )
    }
}"""

content = content.replace(old_widget, new_widget)

with open('app/src/main/java/com/dafamsemarang/dhtv/SmartHomeScreen.kt', 'w') as f:
    f.write(content)

