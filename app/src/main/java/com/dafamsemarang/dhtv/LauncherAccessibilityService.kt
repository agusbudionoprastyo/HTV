package com.dafamsemarang.dhtv

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * HIGH-PERFORMANCE MULTI-PROCESS ACCESSIBILITY SERVICE
 * 
 * Runs in its own dedicated process ":accessibility" to completely bypass any main thread
 * scheduling overhead from Jetpack Compose. Captures key events with near-zero latency, 
 * ensuring 100% smooth remote D-pad navigation while providing an absolute, unescapable
 * trap for the system Home Button.
 */
class LauncherAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LauncherAccessService"
        private const val OWN_PACKAGE = "com.dafamsemarang.dhtv"
    }

    // Track focused window to prevent redundant task stack reloads when already active
    private var currentFocusedPackage: String = OWN_PACKAGE

    // Time-lock debounce to neutralize Android OS 'ghost transitions' during key swallowing
    private var lastSwallowedHomeTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Prime the launcher cache immediately
        updateLauncherCache()
        Log.d(TAG, "Isolated Multi-Process Service connected. Auto-starting...")
        redirectToMain()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            currentFocusedPackage = packageName
            
            if (packageName != OWN_PACKAGE) {
                val className = event.className?.toString() ?: ""
                
                // Check if target window is a system launcher
                if (isOtherLauncher(packageName, className)) {
                    // GHOST-EVENT SHIELD:
                    // When the Home Key is swallowed, the OS often fires a false-positive shift alert 
                    // to Projectivy. We block these phantoms with an 800ms safe lock window.
                    val elapsed = System.currentTimeMillis() - lastSwallowedHomeTime
                    if (elapsed < 800) {
                        Log.d(TAG, "Ignored phantom launcher shift (elapsed: ${elapsed}ms). Staying firm.")
                        return
                    }

                    Log.d(TAG, "Universal Match: Rival launcher detected ($packageName). Resetting...")
                    redirectToMain()
                }
            }
        }
    }

    // Caching layer stores precise "package/class" signatures to prevent blocking settings
    private val knownLauncherComponents = mutableSetOf<String>()
    private var lastCacheUpdate = 0L

    private fun isOtherLauncher(pkg: String, cls: String): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // 1. NEVER block anything if it is our own app
        if (pkg == OWN_PACKAGE) return false
        
        // 2. EXPLICIT SAFETY WHITELIST: Never block settings or setup wizards
        if (pkg.contains("settings", ignoreCase = true) || cls.contains("settings", ignoreCase = true)) return false
        if (pkg.contains("setup", ignoreCase = true) || cls.contains("setup", ignoreCase = true)) return false

        val signature = "$pkg/$cls"

        // 3. Fast Path: Check cache
        if (knownLauncherComponents.contains(signature)) return true

        // 4. Throttle re-querying
        if (currentTime - lastCacheUpdate > 10_000) {
            updateLauncherCache()
            return knownLauncherComponents.contains(signature)
        }

        return false
    }

    private fun updateLauncherCache() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            
            val newCache = mutableSetOf<String>()
            for (info in resolveInfos) {
                val foundPkg = info.activityInfo.packageName
                val foundClass = info.activityInfo.name
                if (foundPkg != OWN_PACKAGE) {
                    newCache.add("$foundPkg/$foundClass")
                }
            }
            
            knownLauncherComponents.clear()
            knownLauncherComponents.addAll(newCache)
            lastCacheUpdate = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying launchers", e)
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        val keyCode = event?.keyCode ?: return super.onKeyEvent(event)
        val action = event.action
        
        // ABSOLUTE HOME BUTTON INTERCEPT
        // Since we are in an isolated process, checking this and returning super takes <1ms.
        // Captures HOME immediately and redirects BEFORE system launcher renders.
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            if (action == KeyEvent.ACTION_DOWN) {
                // CRITICAL VIDEO BANNER FIX: 
                // If our app is already the foreground window, just swallow the key quietly!
                // Calling redirectToMain() while in-app triggers OS task stack checks,
                // causing temporary activity pauses that stop the MediaPlayer banner.
                if (currentFocusedPackage != OWN_PACKAGE) {
                    Log.d(TAG, "Intercepted Home key from external app. Redirecting back...")
                    redirectToMain()
                } else {
                    Log.d(TAG, "Intercepted Home key while in-app. Swallowing to keep video playing.")
                    // Trigger the anti-phantom state lock
                    lastSwallowedHomeTime = System.currentTimeMillis()
                }
            }
            return true // Swallows the Home intent COMPLETELY. User never leaves our app!
        }
        
        return super.onKeyEvent(event)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
    }

    private fun redirectToMain() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            } ?: Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MainActivity: ${e.message}")
        }
    }
}
