package com.dafamsemarang.dhtv

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleTtsHelper {

    private fun md5(string: String): String {
        return try {
            val bytes = MessageDigest.getInstance("MD5").digest(string.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            string.hashCode().toString() // Fallback
        }
    }

    suspend fun synthesizeSpeech(
        context: Context,
        text: String,
        languageCode: String,
        voiceName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val cacheKey = md5("${text}_${languageCode}_${voiceName}")
            val cachedFile = File(context.cacheDir, "tts_cache_$cacheKey.mp3")

            if (cachedFile.exists() && cachedFile.length() > 0) {
                Log.d("GoogleTtsHelper", "Using cached TTS audio: ${cachedFile.absolutePath}")
                return@withContext cachedFile
            }

            val apiKey = com.google.firebase.FirebaseApp.getInstance().options.apiKey
            if (apiKey.isNullOrEmpty()) {
                Log.e("GoogleTtsHelper", "Firebase API Key is null or empty")
                return@withContext null
            }
            val urlStr = "https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey"
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000

            val requestBodyJson = JSONObject().apply {
                put("input", JSONObject().apply { put("text", text) })
                put("voice", JSONObject().apply {
                    put("languageCode", languageCode)
                    put("name", voiceName)
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "MP3")
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBodyJson.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(responseText)
                val audioContentBase64 = jsonObject.optString("audioContent", "")
                
                if (audioContentBase64.isNotEmpty()) {
                    val audioBytes = Base64.decode(audioContentBase64, Base64.DEFAULT)
                    cachedFile.writeBytes(audioBytes)
                    Log.d("GoogleTtsHelper", "Speech synthesized and cached successfully: ${cachedFile.absolutePath}")
                    
                    try {
                        context.cacheDir.listFiles()?.forEach { file ->
                            if (file.name.startsWith("temp_greeting_")) {
                                file.delete()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("GoogleTtsHelper", "Failed to clean old temp files", e)
                    }

                    return@withContext cachedFile
                }
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e("GoogleTtsHelper", "GCP TTS Error: $responseCode - $errorMsg")
            }
        } catch (e: Exception) {
            Log.e("GoogleTtsHelper", "GCP TTS Exception: ${e.message}", e)
        }
        return@withContext null
    }
}
