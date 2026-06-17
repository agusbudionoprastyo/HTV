package com.dafamsemarang.dhtv

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioCacheHelper {
    fun getAudioCacheFile(context: Context, url: String): File {
        val hash = url.hashCode().toString().replace("-", "n")
        return File(context.cacheDir, "audio_$hash.mp3")
    }

    fun isAudioCached(context: Context, url: String): Boolean {
        val file = getAudioCacheFile(context, url)
        return file.exists() && file.length() > 0
    }

    suspend fun downloadAndCacheAudio(
        context: Context,
        urlStr: String
    ): File? = withContext(Dispatchers.IO) {
        val file = getAudioCacheFile(context, urlStr)
        if (file.exists() && file.length() > 0) {
            return@withContext file
        }

        try {
            val url = URL(urlStr.replace(" ", "%20"))
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("AudioCacheHelper", "Audio cached successfully: ${file.absolutePath}")
                return@withContext file
            }
        } catch (e: Exception) {
            Log.e("AudioCacheHelper", "Failed to cache audio from $urlStr: ${e.message}", e)
        }
        return@withContext null
    }
}
