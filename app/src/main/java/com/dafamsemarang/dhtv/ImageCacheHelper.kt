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



// Singleton ImageLoader to share memory cache across all CachedAsyncImage instances
private var sharedSvgImageLoader: coil.ImageLoader? = null

private fun getSharedSvgImageLoader(context: Context): coil.ImageLoader {
    return sharedSvgImageLoader ?: coil.ImageLoader.Builder(context.applicationContext)
        .components {
            add(coil.decode.SvgDecoder.Factory())
        }
        .build().also { sharedSvgImageLoader = it }
}

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
    val sanitizedUrl = imageUrl.replace(" ", "%20")
    if (sanitizedUrl.isEmpty()) {
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
            val url = java.net.URL(sanitizedUrl)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.requestMethod = "GET"
            
            val responseCode = conn.responseCode
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("ImageCacheHelper", "Image cached successfully: $cacheFileName")
                withContext(Dispatchers.Main) {
                    onSuccess(cacheFile.absolutePath)
                }
            } else {
                throw Exception("HTTP error code: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("ImageCacheHelper", "Failed to cache image: ${e.message}")
            try {
                if (cacheFile.exists()) {
                    cacheFile.delete()
                }
            } catch (ignored: Exception) {}
            withContext(Dispatchers.Main) {
                onError(e)
            }
        }
    }
}

/**
 * Get cache file name from image URL
 */
fun getImageCacheFileName(imageUrl: String, prefix: String = "img"): String {
    val sanitizedUrl = imageUrl.replace(" ", "%20")
    val cleanUrl = sanitizedUrl.substringBefore('?').substringBefore('#')
    val rawExtension = cleanUrl.substringAfterLast('.', "")
    val extension = rawExtension.lowercase().filter { it.isLetterOrDigit() }.take(4).ifEmpty { "png" }
    val hash = sanitizedUrl.hashCode().toString().replace("-", "n")
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
    val svgAwareImageLoader = getSharedSvgImageLoader(context)
    
    val sanitizedUrl = remember(imageUrl) { imageUrl.replace(" ", "%20") }
    val cacheFileName = remember(sanitizedUrl) {
        getImageCacheFileName(sanitizedUrl, cachePrefix)
    }
    val existingPath = remember(sanitizedUrl) {
        if (sanitizedUrl.isEmpty()) null else getCachedImagePath(context, cacheFileName)
    }
    
    var cachedImagePath by remember(sanitizedUrl) { mutableStateOf<String?>(existingPath) }
    var isLoading by remember(sanitizedUrl) { mutableStateOf(existingPath == null && sanitizedUrl.isNotEmpty()) }
    
    // Check/refresh cache in background
    LaunchedEffect(sanitizedUrl) {
        if (sanitizedUrl.isEmpty()) {
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
                imageUrl = sanitizedUrl,
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
                    model = ImageRequest.Builder(context)
                        .data(imgFile)
                        .crossfade(true) // Crossfade for smooth entry, preventing harsh pop-ins
                        .allowHardware(true) // Speed up rendering via direct GPU hooks
                        .build(),
                    imageLoader = svgAwareImageLoader,
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
    val sanitizedUrl = remember(url) { url.replace(" ", "%20") }
    val model = remember(sanitizedUrl) {
        if (sanitizedUrl.isNotEmpty()) {
            val cacheFileName = getImageCacheFileName(sanitizedUrl)
            val cachedPath = getCachedImagePath(context, cacheFileName)
            if (cachedPath != null) {
                File(cachedPath)
            } else {
                sanitizedUrl
            }
        } else {
            sanitizedUrl
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

