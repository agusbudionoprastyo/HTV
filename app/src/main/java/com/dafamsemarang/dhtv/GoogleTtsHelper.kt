package com.dafamsemarang.dhtv

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleTtsHelper {
    suspend fun synthesizeSpeech(
        context: Context,
        text: String,
        languageCode: String,
        voiceName: String
    ): File? = withContext(Dispatchers.IO) {
        try {
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
                    val tempFile = File(context.cacheDir, "temp_greeting_${System.currentTimeMillis()}.mp3")
                    tempFile.writeBytes(audioBytes)
                    Log.d("GoogleTtsHelper", "Speech synthesized successfully: ${tempFile.absolutePath}")
                    return@withContext tempFile
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
