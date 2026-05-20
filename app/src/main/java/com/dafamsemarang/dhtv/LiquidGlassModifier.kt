package com.dafamsemarang.dhtv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Import Offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

fun Modifier.liquidGlass(
    cornerRadius: Dp = 16.dp,
    glassColor: Color = Color.White,
    alphaInitial: Float = 0.22f,
    alphaFinal: Float = 0.05f,
    hasTopRimLight: Boolean = true, // Default to true (Dual Lighting)
    isFullBorder: Boolean = false, // If true, applies uniform border light
    isHorizontalRim: Boolean = false, // If true, applies Left/Right border light
    isLedStrip: Boolean = false, // If true, animates a rotating LED strip
    isPulse: Boolean = false, // If true, animates a pulsing border glow
    borderAlpha: Float = 1f, // Alpha multiplier for the rim light
    borderWidth: Dp = 1.dp // Width of the rim light border
): Modifier = composed {
    // 1. Base Gradient (Background) - Shared
    val glassGradient = remember(glassColor, alphaInitial, alphaFinal) {
        Brush.verticalGradient(
            colors = listOf(
                glassColor.copy(alpha = alphaInitial),
                glassColor.copy(alpha = alphaFinal)
            )
        )
    }
    
    val shineGradient = remember(glassColor) {
        Brush.linearGradient(
            colors = listOf(
                glassColor.copy(alpha = 0.15f),
                Color.Transparent
            ),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    // PERFORMANCE CRITICAL: Branch entirely to avoid invoking animation hooks for static elements!
    if (!isLedStrip && !isPulse) {
        // --- LIGHTWEIGHT STATIC PATH (99.9% of usages) ---
        val borderGradient = remember(borderAlpha, isHorizontalRim, isFullBorder, hasTopRimLight) {
            if (isHorizontalRim) {
                Brush.horizontalGradient(
                    0.0f to Color.White.copy(alpha = 1.0f * borderAlpha),
                    0.2f to Color.White.copy(alpha = 0.1f * borderAlpha),
                    0.8f to Color.White.copy(alpha = 0.1f * borderAlpha),
                    1.0f to Color.White.copy(alpha = 1.0f * borderAlpha)
                )
            } else if (isFullBorder) {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f * borderAlpha),
                        Color.White.copy(alpha = 0.8f * borderAlpha)
                    )
                )
            } else if (hasTopRimLight) {
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.9f * borderAlpha),
                    0.15f to Color.White.copy(alpha = 0.2f * borderAlpha),
                    0.5f to Color.White.copy(alpha = 0.05f * borderAlpha),
                    0.85f to Color.White.copy(alpha = 0.2f * borderAlpha),
                    1.0f to Color.White.copy(alpha = 0.6f * borderAlpha)
                )
            } else {
                Brush.verticalGradient(
                    0.0f to Color.White.copy(alpha = 0.1f * borderAlpha),
                    0.15f to Color.White.copy(alpha = 0.1f * borderAlpha),
                    0.5f to Color.White.copy(alpha = 0.05f * borderAlpha),
                    0.85f to Color.White.copy(alpha = 0.2f * borderAlpha),
                    1.0f to Color.White.copy(alpha = 0.6f * borderAlpha)
                )
            }
        }
        
        this.clip(RoundedCornerShape(cornerRadius))
            .background(brush = glassGradient)
            .background(brush = shineGradient)
            .border(
                width = borderWidth,
                brush = borderGradient,
                shape = RoundedCornerShape(cornerRadius)
            )
    } else if (isPulse) {
        // --- PULSING BORDER PATH ---
        val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f * borderAlpha,
            targetValue = 1.0f * borderAlpha,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        
        val borderGradient = remember(pulseAlpha) {
            Brush.verticalGradient(
                0.0f to Color.White.copy(alpha = 0.9f * pulseAlpha),
                0.15f to Color.White.copy(alpha = 0.2f * pulseAlpha),
                0.5f to Color.White.copy(alpha = 0.05f * pulseAlpha),
                0.85f to Color.White.copy(alpha = 0.2f * pulseAlpha),
                1.0f to Color.White.copy(alpha = 0.6f * pulseAlpha)
            )
        }
        
        this.clip(RoundedCornerShape(cornerRadius))
            .background(brush = glassGradient)
            .background(brush = shineGradient)
            .border(
                width = borderWidth,
                brush = borderGradient,
                shape = RoundedCornerShape(cornerRadius)
            )
    } else {
        // --- HEAVY ANIMATED PATH (Isolated here so it doesn't drain other component resources) ---
        val infiniteTransition = rememberInfiniteTransition(label = "led_spin")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "angle"
        )
        
        val borderGradient = remember(angle, borderAlpha) {
             object : ShaderBrush() {
                override fun createShader(size: Size): Shader {
                    val width = size.width
                    val height = size.height
                    val stripColorAlpha = Color.White.copy(alpha = borderAlpha).toArgb()
                    val transparent = Color.Transparent.toArgb()
                    
                    val colors = intArrayOf(transparent, transparent, stripColorAlpha, transparent, transparent)
                    val stops = floatArrayOf(0.0f, 0.075f, 0.5f, 0.925f, 1.0f)
                    
                    return SweepGradient(width / 2, height / 2, colors, stops).apply {
                        val matrix = Matrix()
                        matrix.postRotate(angle, width / 2, height / 2)
                        setLocalMatrix(matrix)
                    }
                }
             }
        }
        
        this.clip(RoundedCornerShape(cornerRadius))
            .background(brush = glassGradient)
            .background(brush = shineGradient)
            .border(
                width = borderWidth,
                brush = borderGradient,
                shape = RoundedCornerShape(cornerRadius)
            )
    }
}
