package com.dafamsemarang.dhtv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection

class UpdateManager(private val context: Context) {
    private val database = FirebaseDatabase.getInstance()
    private val updateRef = database.getReference("app_updates/latest")

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private var downloadedApkFile: File? = null

    @SuppressLint("ObsoleteSdkInt")
    private fun checkInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun requestInstallPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            intent.data = "package:${context.packageName}".toUri()
            activity.startActivityForResult(intent, INSTALL_PERMISSION_REQUEST_CODE)
        }
    }

    fun checkForUpdate(): Flow<UpdateInfo?> = callbackFlow {
        Log.d("UpdateManager", "Starting update check...")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }
                    
                    @Suppress("DEPRECATION")
                    val currentVersion = packageInfo.versionCode
                    Log.d("UpdateManager", "Current app version: $currentVersion")
                    
                    val updateInfo = snapshot.getValue(UpdateInfo::class.java)
                    Log.d("UpdateManager", "Update info from Firebase: $updateInfo")
                    
                    if (updateInfo != null) {
                        Log.d("UpdateManager", "Firebase version: ${updateInfo.versionCode}")
                        if (updateInfo.versionCode > currentVersion) {
                            Log.d("UpdateManager", "Update available!")
                            trySend(updateInfo)
                        } else {
                            Log.d("UpdateManager", "No update needed")
                            trySend(null)
                        }
                    } else {
                        Log.d("UpdateManager", "No update info found in Firebase")
                        trySend(null)
                    }
                } catch (e: Exception) {
                    Log.e("UpdateManager", "Error checking update: ${e.message}")
                    trySend(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UpdateManager", "Database error: ${error.message}")
                trySend(null)
            }
        }

        updateRef.addValueEventListener(listener)
        awaitClose { updateRef.removeEventListener(listener) }
    }

    suspend fun downloadAndInstallUpdate(updateInfo: UpdateInfo, activity: Activity) {
        withContext(Dispatchers.IO) {
            try {
                if (!checkInstallPermission()) {
                    withContext(Dispatchers.Main) {
                        requestInstallPermission(activity)
                        Toast.makeText(context, "Please allow installation from unknown sources", Toast.LENGTH_LONG).show()
                    }
                    return@withContext
                }

                _isDownloading.value = true
                _downloadProgress.value = 0f

                Log.d("UpdateManager", "Starting download process...")
                Log.d("UpdateManager", "Update info: $updateInfo")

                val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates")
                if (!downloadDir.exists()) {
                    val created = downloadDir.mkdirs()
                    Log.d("UpdateManager", "Created download directory: $created")
                }

                val apkFile = File(downloadDir, "update.apk")
                Log.d("UpdateManager", "APK will be saved to: ${apkFile.absolutePath}")

                // Download from URL
                val url = URL(updateInfo.downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileSize = connection.contentLength
                Log.d("UpdateManager", "File size: $fileSize bytes")

                FileOutputStream(apkFile).use { outputStream ->
                    connection.inputStream.use { inputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // Update progress
                            val progress = (totalBytesRead.toFloat() / fileSize) * 100
                            _downloadProgress.value = progress
                            Log.d("UpdateManager", "Download progress: $progress%")
                        }
                    }
                }

                Log.d("UpdateManager", "Download completed. File size: ${apkFile.length()} bytes")

                if (apkFile.length() == 0L) {
                    throw Exception("Downloaded file is empty")
                }

                // Verify APK signature
                val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
                if (packageInfo == null) {
                    throw Exception("Invalid APK file - File may be corrupted or not a valid APK")
                }

                downloadedApkFile = apkFile
                
                // Launch system installation dialog
                withContext(Dispatchers.Main) {
                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                    
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra("android.intent.extra.INSTALLER_PACKAGE_NAME", context.packageName)
                    }
                    
                    activity.startActivity(intent)
                }

            } catch (e: Exception) {
                val errorMessage = "Error downloading update: ${e.message}"
                Log.e("UpdateManager", errorMessage, e)
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show()
                }
            } finally {
                _isDownloading.value = false
            }
        }
    }

    companion object {
        const val INSTALL_PERMISSION_REQUEST_CODE = 1001
    }

    data class UpdateInfo(
        val versionCode: Int = 0,
        val versionName: String = "",
        val downloadUrl: String = "",
        val releaseNotes: String = ""
    )
} 