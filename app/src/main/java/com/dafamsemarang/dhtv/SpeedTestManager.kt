package com.dafamsemarang.dhtv

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import kotlin.system.measureTimeMillis

object SpeedTestManager {

    private const val TAG = "SpeedTestManager"
    private const val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=10000000" // 10MB from Cloudflare
    private const val FALLBACK_URL = "https://proof.ovh.net/files/10Mb.dat" // OVH fallback
    private const val PING_HOST = "8.8.8.8" // Google DNS for ping test

    /**
     * Measure ping/latency to a server
     * @return latency in milliseconds, or -1 if failed
     */
    suspend fun runPingTest(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val address = InetAddress.getByName(PING_HOST)
                val startTime = System.currentTimeMillis()
                val reachable = address.isReachable(5000) // 5 second timeout
                val endTime = System.currentTimeMillis()
                
                if (reachable) {
                    val latency = (endTime - startTime).toInt()
                    Log.d(TAG, "Ping Test: ${latency}ms to $PING_HOST")
                    return@withContext latency
                } else {
                    Log.e(TAG, "Ping failed: Host unreachable")
                    return@withContext -1
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ping test failed", e)
                return@withContext -1
            }
        }
    }

    /**
     * Measure download speed
     * @return speed in Mbps, or -1.0 if failed
     */
    suspend fun runDownloadTest(): Double {
        return withContext(Dispatchers.IO) {
            // Try primary URL first
            var result = tryDownloadTest(DOWNLOAD_URL)
            
            // If failed, try fallback URL
            if (result <= 0.0) {
                Log.w(TAG, "Primary download test failed, trying fallback...")
                result = tryDownloadTest(FALLBACK_URL)
            }
            
            return@withContext result
        }
    }
    
    private fun tryDownloadTest(testUrl: String): Double {
        try {
            Log.d(TAG, "Starting download test from $testUrl")
            val url = URL(testUrl)
            val startTime = System.currentTimeMillis()
            
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.doInput = true
            connection.connect()

            val responseCode = connection.responseCode
            Log.d(TAG, "HTTP Response: $responseCode")
            
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP $responseCode")
                connection.disconnect()
                return 0.0
            }

            val inputStream: InputStream = connection.inputStream
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
            }

            val endTime = System.currentTimeMillis()
            val durationMs = endTime - startTime
            
            inputStream.close()
            connection.disconnect()

            if (durationMs == 0L || totalBytesRead == 0L) {
                Log.e(TAG, "Invalid test result: duration=$durationMs, bytes=$totalBytesRead")
                return 0.0
            }

            // Calculate speed in Mbps
            val sizeInMB = totalBytesRead / 1_000_000.0
            val durationSec = durationMs / 1000.0
            val speedMbps = (sizeInMB * 8) / durationSec

            Log.d(TAG, "Download Test Complete: ${String.format("%.2f", speedMbps)} Mbps (${totalBytesRead} bytes in ${durationMs}ms)")
            
            return speedMbps

        } catch (e: Exception) {
            Log.e(TAG, "Download test failed: ${e.message}", e)
            return 0.0
        }
    }

    /**
     * Measure upload speed (placeholder for future implementation)
     * @return speed in Mbps, or 0.0 if not implemented/failed
     */
    suspend fun runUploadTest(): Double {
        // Upload test would require a server endpoint that accepts POST/PUT
        // Not implemented yet
        return 0.0
    }
}
