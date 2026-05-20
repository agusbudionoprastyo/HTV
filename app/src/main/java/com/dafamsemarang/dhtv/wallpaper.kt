package com.dafamsemarang.dhtv

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WallpaperSection() {
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var localWallpaperPath by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)

    // Setup Firebase Realtime Database references
    val database = FirebaseDatabase.getInstance().reference

    // Firebase listener with explicit teardown
    DisposableEffect(key1 = branchId) {
        var activeRef: com.google.firebase.database.DatabaseReference? = null
        var activeListener: com.google.firebase.database.ValueEventListener? = null

        if (branchId != null) {
            val db = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            val ref = db.child("BRANCHES").child(branchId).child("SETTING/WALLPAPER").child("imageUrl")
            
            val listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val url = snapshot.getValue(String::class.java)
                    imageUrl = url
                    if (!url.isNullOrEmpty()) {
                        val fileName = "wallpaper_${branchId}.jpg"
                        val file = File(context.cacheDir, fileName)
                        if (!file.exists() || sharedPreferences.getString("cached_wallpaper_url", null) != url) {
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val req = ImageRequest.Builder(context).data(url).build()
                                    val res = context.imageLoader.execute(req)
                                    if (res is SuccessResult) {
                                        val bMap = (res.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                        bMap?.let {
                                            FileOutputStream(file).use { out ->
                                                it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, out)
                                            }
                                            sharedPreferences.edit().putString("cached_wallpaper_path", file.absolutePath).apply()
                                            sharedPreferences.edit().putString("cached_wallpaper_url", url).apply()
                                            withContext(Dispatchers.Main) { localWallpaperPath = file.absolutePath }
                                        }
                                    }
                                } catch (e: Exception) { }
                            }
                        } else {
                            localWallpaperPath = file.absolutePath
                        }
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) { }
            }
            activeRef = ref
            activeListener = listener
            ref.addValueEventListener(listener)
        }

        onDispose {
            if (activeRef != null && activeListener != null) {
                activeRef.removeEventListener(activeListener)
            }
        }
    }

    // Local cached fallback logic
    val wallpaperFilePath = localWallpaperPath ?: sharedPreferences.getString("cached_wallpaper_path", null)
    val wallpaperFile = wallpaperFilePath?.let { File(it) }
    val useLocal = wallpaperFile?.exists() == true

    Box(modifier = Modifier.fillMaxSize()) {
        if (useLocal) {
            val bitmap: ImageBitmap? = remember(wallpaperFilePath) {
                try {
                    val bmp = android.graphics.BitmapFactory.decodeFile(wallpaperFilePath)
                    bmp?.asImageBitmap()
                } catch (e: Exception) { null }
            }
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "wallpaper",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else if (imageUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(imageUrl),
                contentDescription = "wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Full-screen transparent black overlay for optimal contrast & readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
        )
    }
}
