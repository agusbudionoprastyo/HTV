package com.dafamsemarang.dhtv

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShortcutIconCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, Drawable>()
    
    @Volatile
    private var preloadedInstalledApps: List<SupportedApp> = emptyList()

    fun get(packageName: String): Drawable? {
        if (packageName == "EMPTY_SLOT") return null
        return cache[packageName]
    }

    fun put(packageName: String, drawable: Drawable) {
        if (packageName == "EMPTY_SLOT") return
        cache[packageName] = drawable
    }

    fun remove(packageName: String) {
        cache.remove(packageName)
    }

    fun clear() {
        cache.clear()
        preloadedInstalledApps = emptyList()
    }

    fun getInstalledAppsList(): List<SupportedApp> {
        return preloadedInstalledApps
    }

    // Set preloaded list (e.g. if we update it dynamically from HomeScreen)
    fun setInstalledAppsList(apps: List<SupportedApp>) {
        preloadedInstalledApps = apps
    }

    // Preload all active shortcut package banners and cache installed apps list
    suspend fun preload(context: Context) = withContext(Dispatchers.IO) {
        // 1. Fetch installed apps list and save it
        val apps = fetchInstalledAppsDirectly(context)
        preloadedInstalledApps = apps
        Log.d("ShortcutIconCache", "Preloaded installed apps: ${apps.size}")

        // 2. Preload active shortcut package banners from SharedPreferences
        val prefs = context.getSharedPreferences("app_shortcuts", Context.MODE_PRIVATE)
        val defaultPackages = listOf(
            "com.dh.iptv",
            "com.spotify.tv.android",
            "com.amazon.amazonvideo.livingroom",
            "com.netflix.ninja",
            "com.google.android.youtube.tv",
            "EMPTY_SLOT"
        )
        
        val pm = context.packageManager
        for (i in 0..5) {
            val defaultPkg = defaultPackages.getOrElse(i) { "EMPTY_SLOT" }
            val packageName = prefs.getString("slot_$i", defaultPkg) ?: defaultPkg
            if (packageName != "EMPTY_SLOT" && !cache.containsKey(packageName)) {
                // Check if we already loaded it in the apps list
                val appFromList = apps.find { it.packageName == packageName }
                if (appFromList?.banner != null) {
                    cache[packageName] = appFromList.banner
                    Log.d("ShortcutIconCache", "Cached banner from preloaded list for: $packageName")
                } else {
                    try {
                        val appInfo = pm.getApplicationInfo(packageName, 0)
                        val banner = appInfo.loadBanner(pm)
                        if (banner != null) {
                            cache[packageName] = banner
                            Log.d("ShortcutIconCache", "Preloaded banner from PackageManager for: $packageName")
                        }
                    } catch (e: Exception) {
                        Log.e("ShortcutIconCache", "Failed to preload banner for: $packageName: ${e.message}")
                    }
                }
            }
        }
    }

    // Helper to fetch installed apps (matches HomeScreen's getInstalledApps exactly)
    private fun fetchInstalledAppsDirectly(context: Context): List<SupportedApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        val tvIntent = Intent(Intent.ACTION_MAIN, null)
        tvIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)

        val apps = mutableListOf<SupportedApp>()
        val seenPackages = mutableSetOf<String>()

        val resolveInfos = pm.queryIntentActivities(intent, 0) + pm.queryIntentActivities(tvIntent, 0)

        for (resolveInfo in resolveInfos) {
            val packageName = resolveInfo.activityInfo.packageName
            if (packageName !in seenPackages && packageName != context.packageName) {
                seenPackages.add(packageName)
                
                var banner = resolveInfo.activityInfo.loadBanner(pm)
                if (banner == null) {
                    banner = resolveInfo.activityInfo.applicationInfo.loadBanner(pm)
                }

                apps.add(
                    SupportedApp(
                        packageName = packageName,
                        label = resolveInfo.loadLabel(pm).toString(),
                        icon = resolveInfo.loadIcon(pm),
                        banner = banner
                    )
                )
            }
        }
        return apps.sortedBy { it.label }
    }

    // Preload a single package in background
    suspend fun preloadSingle(context: Context, packageName: String) = withContext(Dispatchers.IO) {
        if (packageName == "EMPTY_SLOT" || cache.containsKey(packageName)) return@withContext
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val banner = appInfo.loadBanner(pm)
            if (banner != null) {
                cache[packageName] = banner
                Log.d("ShortcutIconCache", "Preloaded single banner for: $packageName")
            }
        } catch (e: Exception) {
            Log.e("ShortcutIconCache", "Failed to preload single banner for: $packageName: ${e.message}")
        }
    }
}
