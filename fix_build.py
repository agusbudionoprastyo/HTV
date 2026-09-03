import re

# 1. Fix LiquidGlassModifier.kt
file_path_lgm = "/Users/ag/Documents/GitHub/HTV/app/src/main/java/com/dafamsemarang/dhtv/LiquidGlassModifier.kt"
with open(file_path_lgm, 'r') as f:
    lgm = f.read()

lgm = lgm.replace("fun Modifier.shimmerEffect(): Modifier", "fun Modifier.liquidShimmerEffect(): Modifier")

with open(file_path_lgm, 'w') as f:
    f.write(lgm)


# 2. Fix SmartHomeScreen.kt imports
file_path_shs = "/Users/ag/Documents/GitHub/HTV/app/src/main/java/com/dafamsemarang/dhtv/SmartHomeScreen.kt"
with open(file_path_shs, 'r') as f:
    shs = f.read()

# Remove the incorrectly placed rememberShimmerBrush from the top
bad_block = """@Composable
fun rememberShimmerBrush(): androidx.compose.ui.graphics.Brush {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        )
    )
    return androidx.compose.ui.graphics.Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.5f),
            Color.White,
            Color.White.copy(alpha = 0.5f)
        ),
        start = androidx.compose.ui.geometry.Offset(translateAnim - 200f, translateAnim - 200f),
        end = androidx.compose.ui.geometry.Offset(translateAnim, translateAnim)
    )
}"""
shs = shs.replace(bad_block, "")

# Append rememberShimmerBrush at the end of the file
shs += "\n\n" + bad_block + "\n"

# 3. Fix SmartSwitchWidget signature and definition
# Current signature:
# fun SmartSwitchWidget(id: String, name: String, deviceId: String?, state: Boolean, isFocused: Boolean, onFocus: () -> Unit, onToggle: suspend () -> Unit) {
old_sig = "fun SmartSwitchWidget(id: String, name: String, deviceId: String?, state: Boolean, isFocused: Boolean, onFocus: () -> Unit, onToggle: suspend () -> Unit) {\n    val coroutineScope = rememberCoroutineScope()"

new_sig = """fun SmartSwitchWidget(id: String, name: String, deviceId: String?, state: Boolean, isFocused: Boolean, onFocus: () -> Unit, onToggle: suspend () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val isLoading = deviceId != null && (DataRepository.ignoreDeviceUpdatesUntil[deviceId] ?: 0L) > System.currentTimeMillis()
    val shimmerBrush = if (isLoading && isFocused) rememberShimmerBrush() else null"""
shs = shs.replace(old_sig, new_sig)

with open(file_path_shs, 'w') as f:
    f.write(shs)

print("Build fixes applied.")
