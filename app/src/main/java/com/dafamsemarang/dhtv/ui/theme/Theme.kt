package com.dafamsemarang.dhtv.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color.DarkGray,
    onPrimary = Color.LightGray,
    secondary = Color(0xFFE91E63),
    onSecondary = Color(0xFFFFFFFF)
)


@SuppressLint("ComposableNaming")
@Composable
fun dhtvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}