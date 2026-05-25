package com.dafamsemarang.dhtv

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color



/**
 * Download and cache image to local storage
 */
fun downloadAndCacheImage(
    context: Context,
    imageUrl: String,
    cacheFileName: String,
    onSuccess: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    if (imageUrl.isEmpty()) {
        onError(Exception("Empty image URL"))
        return
    }
    
    val cacheFile = File(context.cacheDir, cacheFileName)
    
    // Check if file already exists and is valid
    if (cacheFile.exists() && cacheFile.length() > 0) {
        onSuccess(cacheFile.absolutePath)
        return
    }
    
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // Disable hardware bitmaps during caching download
                .build()
            
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                val bitmap = try {
                    drawable.toBitmap()
                } catch (e: Exception) {
                    (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                }
                
                if (bitmap != null) {
                    FileOutputStream(cacheFile).use { out ->
                        // Use PNG to preserve transparency (Alpha channel)
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    }
                    
                    Log.d("ImageCacheHelper", "Image cached successfully: $cacheFileName")
                    withContext(Dispatchers.Main) {
                        onSuccess(cacheFile.absolutePath)
                    }
                } else {
                    onError(Exception("Failed to get bitmap from drawable"))
                }
            } else {
                onError(Exception("Failed to load image"))
            }
        } catch (e: Exception) {
            Log.e("ImageCacheHelper", "Failed to cache image: ${e.message}")
            onError(e)
        }
    }
}

/**
 * Get cache file name from image URL
 */
fun getImageCacheFileName(imageUrl: String, prefix: String = "img"): String {
    val cleanUrl = imageUrl.substringBefore('?').substringBefore('#')
    val rawExtension = cleanUrl.substringAfterLast('.', "")
    val extension = rawExtension.lowercase().filter { it.isLetterOrDigit() }.take(4).ifEmpty { "png" }
    val hash = imageUrl.hashCode().toString().replace("-", "n")
    // v2: Invalidate cache to force PNG re-download (fixes black background transparency issue)
    return "${prefix}_${hash}_v2.$extension"
}

/**
 * Get cached image file path if exists
 */
fun getCachedImagePath(context: Context, cacheFileName: String): String? {
    val cacheFile = File(context.cacheDir, cacheFileName)
    return if (cacheFile.exists() && cacheFile.length() > 0) {
        cacheFile.absolutePath
    } else {
        null
    }
}

/**
 * Composable function to display cached image with automatic caching
 */
@Composable
fun CachedAsyncImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Int? = null,
    error: Int? = null,
    cachePrefix: String = "img",
    onImageLoaded: (() -> Unit)? = null,
    onError: (() -> Unit)? = null,
    showShimmer: Boolean = true
) {
    val context = LocalContext.current
    val cacheFileName = remember(imageUrl) {
        getImageCacheFileName(imageUrl, cachePrefix)
    }
    val existingPath = remember(imageUrl) {
        if (imageUrl.isEmpty()) null else getCachedImagePath(context, cacheFileName)
    }
    
    var cachedImagePath by remember(imageUrl) { mutableStateOf<String?>(existingPath) }
    var isLoading by remember(imageUrl) { mutableStateOf(existingPath == null && imageUrl.isNotEmpty()) }
    
    // Check/refresh cache in background
    LaunchedEffect(imageUrl) {
        if (imageUrl.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }
        
        val latestPath = getCachedImagePath(context, cacheFileName)
        if (latestPath != null) {
            cachedImagePath = latestPath
            isLoading = false
            onImageLoaded?.invoke()
        } else {
            // Download and cache if not exists
            downloadAndCacheImage(
                context = context,
                imageUrl = imageUrl,
                cacheFileName = cacheFileName,
                onSuccess = { path ->
                    cachedImagePath = path
                    isLoading = false
                    onImageLoaded?.invoke()
                },
                onError = { e ->
                    Log.e("CachedAsyncImage", "Failed to cache image: ${e.message}")
                    isLoading = false
                    onError?.invoke()
                }
            )
        }
    }
    
    when {
        cachedImagePath != null -> {
            // Use COIL even for local cache! Coil will automatically downsample 
            // high-res photos to perfectly fit the UI constraints, saving massive memory!
            val imgFile = remember(cachedImagePath) { File(cachedImagePath!!) }
            
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imgFile)
                        .crossfade(false)
                        .allowHardware(true) // Speed up rendering via direct GPU hooks
                        .build(),
                    onSuccess = { onImageLoaded?.invoke() },
                    onError = { Log.e("CachedAsyncImage", "Coil failed file: $cachedImagePath") }
                ),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        
        isLoading -> {
            if (placeholder != null) {
                Image(
                    painter = painterResource(id = placeholder),
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            } else if (showShimmer) {
                Box(
                    modifier = modifier.shimmerEffect()
                )
            } else {
                Box(modifier = modifier)
            }
        }
        
        else -> {
            // Show from URL while caching in background or failed
            Image(
                painter = rememberAsyncImagePainter(
                    model = imageUrl,
                    placeholder = placeholder?.let { painterResource(id = it) },
                    error = error?.let { painterResource(id = it) }
                ),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.2f),
        Color.Gray.copy(alpha = 0.4f),
        Color.Gray.copy(alpha = 0.2f)
    )

    return this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
            end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
        )
    )
}

/**
 * Helper to get painter (Local File > Network)
 */
@Composable
fun rememberCachedPainter(url: String, errorPlaceholder: Int? = null): coil.compose.AsyncImagePainter {
    val context = LocalContext.current
    val model = remember(url) {
        if (url.isNotEmpty()) {
            val cacheFileName = getImageCacheFileName(url)
            val cachedPath = getCachedImagePath(context, cacheFileName)
            if (cachedPath != null) {
                File(cachedPath)
            } else {
                url
            }
        } else {
            url
        }
    }

    val request = remember(model, errorPlaceholder) {
        ImageRequest.Builder(context)
            .data(model)
            .apply {
                if (errorPlaceholder != null) error(errorPlaceholder)
            }
            .crossfade(false)
            .build()
    }

    return rememberAsyncImagePainter(model = request)
}

