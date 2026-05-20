package com.dafamsemarang.dhtv

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

@Composable
fun SettingsOptionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.8f)) // Match PIN dialog overlay
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss // Clicking outside dialog content dismisses it
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(520.dp)
                    .wrapContentHeight()
                    .clickable(enabled = false) {} // Consume clicks inside modal
                    .padding(28.dp)
            ) {                
                // Options List Section
                val focusRequester = remember { FocusRequester() }
                
                LaunchedEffect(Unit) {
                    try {
                        focusRequester.requestFocus()
                    } catch (e: Exception) {}
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val options = listOf(
                        SettingsOptionItem(
                            title = "Aksesibilitas (Auto Tombol Home)",
                            desc = "Mengaktifkan proteksi tombol Home secara permanen",
                            icon = R.drawable.ic_accessibility_custom,
                            onClick = {
                                openAccessibilitySettings(context)
                                onDismiss()
                            }
                        ),
                        SettingsOptionItem(
                            title = "Jadikan Default Launcher",
                            desc = "Atur aplikasi ini sebagai beranda utama televisi",
                            icon = R.drawable.ic_home_launcher_custom,
                            onClick = {
                                openHomeSettings(context)
                                onDismiss()
                            }
                        ),
                        SettingsOptionItem(
                            title = "Pengaturan Android (Sistem)",
                            desc = "Membuka konfigurasi Wi-Fi, Aplikasi, dan sistem OS",
                            icon = R.drawable.ic_setting_system_custom,
                            onClick = {
                                openAndroidSettings(context)
                                onDismiss()
                            }
                        )
                    )
                    
                    options.forEachIndexed { index, option ->
                        val isFirst = index == 0
                        SettingsOptionRow(
                            item = option,
                            modifier = if (isFirst) Modifier.focusRequester(focusRequester) else Modifier
                        )
                    }
                }
            }
        }
    }
}

data class SettingsOptionItem(
    val title: String,
    val desc: String,
    val icon: Int,
    val onClick: () -> Unit
)

@Composable
fun SettingsOptionRow(
    item: SettingsOptionItem,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val bgAlpha = if (isFocused) 0.2f else 0.06f
    val borderColor = if (isFocused) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f)
    val scale = if (isFocused) 1.02f else 1f
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Handled manually for TV feel
                onClick = item.onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 18.dp)
    ) {
        // Safe fallback for icon, use standard text box if drawable fails
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isFocused) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
        ) {
            Icon(
                painter = painterResource(id = item.icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.desc,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
        
        // Text block removed per user design refinement
    }
}

private fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membuka menu Aksesibilitas", Toast.LENGTH_SHORT).show()
    }
}

private fun openHomeSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Toast.makeText(context, "Cari 'Aplikasi Beranda' di Menu Pengaturan", Toast.LENGTH_LONG).show()
        } catch (e2: Exception) {
            Toast.makeText(context, "Gagal membuka pengaturan Beranda", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openAndroidSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membuka Pengaturan Sistem", Toast.LENGTH_SHORT).show()
    }
}
