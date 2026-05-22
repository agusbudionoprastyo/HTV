package com.dafamsemarang.dhtv

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.dafamsemarang.dhtv.DataRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Composable
fun PreloadScreen(onPreloadFinished: () -> Unit) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Initializing...") }
    var totalItems by remember { mutableIntStateOf(0) }
    var downloadedItems by remember { mutableIntStateOf(0) }
    
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)

    LaunchedEffect(Unit) {
        if (branchId == null) {
            Log.e("PreloadScreen", "Branch ID is null, skipping preload")
            onPreloadFinished()
            return@LaunchedEffect
        }
        // Start preloading data for menu and requests
        com.dafamsemarang.dhtv.DataRepository.startPreload(context, branchId)

        // Start preloading shortcut icons concurrently in background
        launch {
            com.dafamsemarang.dhtv.ShortcutIconCache.preload(context)
        }

        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch all URLs from Firebase
                withContext(Dispatchers.Main) { statusText = "Fetching content list..." }
                val urls = fetchAllContentUrls(context, branchId)
                
                withContext(Dispatchers.Main) { 
                    totalItems = urls.size
                    if (totalItems == 0) {
                        onPreloadFinished()
                        return@withContext
                    }
                    statusText = "Downloading resources... 0/${totalItems}"
                }

                // 2. Download/Check Cache for each item
                urls.forEachIndexed { index, url ->
                    if (url.endsWith(".mp4")) {
                        // Video
                         preloadVideo(context, url)
                    } else {
                        // Image
                        preloadImage(context, url)
                    }

                    withContext(Dispatchers.Main) {
                        downloadedItems++
                        progress = downloadedItems.toFloat() / totalItems
                        statusText = "Downloading resources... $downloadedItems/${totalItems}"
                    }
                }

                // 3. Finish
                withContext(Dispatchers.Main) {
                    onPreloadFinished()
                }

            } catch (e: Exception) {
                Log.e("PreloadScreen", "Error during preload: ${e.message}")
                withContext(Dispatchers.Main) {
                    onPreloadFinished() // Proceed even if error
                }
            }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = statusText,
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

// Data structures handling
private suspend fun fetchAllContentUrls(context: Context, branchId: String): List<String> = suspendCoroutine { continuation ->
    val database = FirebaseDatabase.getInstance().reference
    val branchRef = database.child("BRANCHES").child(branchId)
    val urls = mutableListOf<String>()

    branchRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            try {
                // 1. Videos
                snapshot.child("VIDEO").child("videoUrl").children.forEach {
                    val status = it.child("status").getValue(String::class.java)
                    val url = it.child("url").getValue(String::class.java)
                    if (status == "active" && !url.isNullOrEmpty()) urls.add(url)
                }

                // 2. Slideshow
                snapshot.child("SLIDESHOW").child("imageUrls").children.forEach {
                    val status = it.child("status").getValue(String::class.java)
                    val url = it.child("url").getValue(String::class.java)
                    if (status == "active" && !url.isNullOrEmpty()) urls.add(url)
                }

                // 3. F&B (Menu Items)
                snapshot.child("FOOD_BEVERAGE").child("food").children.forEach {
                    val isActive = it.child("isActive").getValue(Boolean::class.java) == true
                    val url = it.child("imageUrl").getValue(String::class.java)
                    if (isActive && !url.isNullOrEmpty()) urls.add(url)
                }

                // 4. Hotel Info (Facilities, Rooms, etc.)
                val hotelInfo = snapshot.child("HOTEL_INFO")
                listOf("HOTEL_FACILITY", "ROOMS_FACILITY", "EMERGENCY_PROCEDURE", "HEALTH_WELLNESS", "DISCOVER_DESTINATION").forEach { category ->
                    hotelInfo.child(category).children.forEach { 
                        val url = it.child("imageUrl").getValue(String::class.java)
                        if (!url.isNullOrEmpty()) urls.add(url)
                    }
                }

                // 5. Guest Request (Items)
                 snapshot.child("GUEST_REQUEST").child("requests").children.forEach {
                    val url = it.child("imageUrl").getValue(String::class.java)
                    if (!url.isNullOrEmpty()) urls.add(url)
                 }

                // 6. Welcome Screen Assets
                val welcomeRef = snapshot.child("WELCOME_LETTER")
                welcomeRef.child("backgroundUrl").getValue(String::class.java)?.let { if (it.isNotEmpty()) urls.add(it) }
                welcomeRef.child("roomImageUrl").getValue(String::class.java)?.let { if (it.isNotEmpty()) urls.add(it) }
                welcomeRef.child("signUrl").getValue(String::class.java)?.let { if (it.isNotEmpty()) urls.add(it) }

                // 7. Company Icon
                val iconUrl = snapshot.child("SETTING").child("COMPANY_ICON").child("iconUrl").getValue(String::class.java)
                if (!iconUrl.isNullOrEmpty()) urls.add(iconUrl)

                // 8. Wallpaper
                val wallpaperUrl = snapshot.child("SETTING").child("WALLPAPER").child("imageUrl").getValue(String::class.java)
                if (!wallpaperUrl.isNullOrEmpty()) {
                    urls.add(wallpaperUrl)
                    Log.d("PreloadScreen", "Added wallpaper to preload: $wallpaperUrl")
                }

                // 9. Guest Image Preloading
                val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val roomId = sharedPreferences.getString("room", null)
                if (roomId != null) {
                    val guestFolio = snapshot.child("FOGUEST").child(roomId).child("folio").getValue(Int::class.java)
                    if (guestFolio != null) {
                        val guestImageUrl = snapshot.child("GUESTIMAGE").child(guestFolio.toString()).child("imageUrl").getValue(String::class.java)
                        if (!guestImageUrl.isNullOrEmpty()) {
                            urls.add(guestImageUrl)
                            Log.d("PreloadScreen", "Added guest image to preload: $guestImageUrl")
                        }
                    }
                }

                continuation.resume(urls)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            continuation.resume(emptyList()) // Return empty on error to not crash
        }
    })
}

private suspend fun preloadImage(context: Context, url: String) = suspendCoroutine<Unit> { continuation ->
    val filename = getImageCacheFileName(url)
    val file = File(context.cacheDir, filename)
    
    if (file.exists() && file.length() > 0) {
        // Already cached
        continuation.resume(Unit)
    } else {
        // Download
        downloadAndCacheImage(context, url, filename, 
            onSuccess = { continuation.resume(Unit) },
            onError = { 
                Log.e("PreloadScreen", "Failed to cache image: $url")
                continuation.resume(Unit) // Continue anyway
            }
        )
    }
}

private suspend fun preloadVideo(context: Context, url: String) = suspendCoroutine<Unit> { continuation ->
    val filename = url.hashCode().toString() + ".mp4"
    val file = File(context.cacheDir, filename)
    
    if (file.exists() && file.length() > 0) {
        // Already cached
         continuation.resume(Unit)
    } else {
        // Download
        downloadAndCacheVideo(url, file, 
            onSuccess = { continuation.resume(Unit) },
            onError = {
                Log.e("PreloadScreen", "Failed to cache video: $url")
                continuation.resume(Unit) // Continue anyway
            }
        )
    }
}
