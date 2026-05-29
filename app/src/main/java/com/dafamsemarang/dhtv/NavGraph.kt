package com.dafamsemarang.dhtv

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.navigation.NavBackStackEntry
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.core.content.edit
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Ordered list of main screens — determines slide direction
private val mainScreenOrder = listOf("home", "cantingfood", "contact", "hotel_guide")

/** Returns +1 if navigating forward (slide from right), -1 if backward (slide from left) */
private fun slideDirection(from: String?, to: String?): Int {
    val fromIndex = mainScreenOrder.indexOf(from ?: "")
    val toIndex = mainScreenOrder.indexOf(to ?: "")
    return if (toIndex >= fromIndex) 1 else -1
}

private const val SLIDE_DURATION = 800
private val GoogleTvEasing = CubicBezierEasing(0.18f, 0.85f, 0.18f, 1.00f)
private const val SLIDE_OFFSET_PCT = 0.20f

private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainEnterTransition(slideDistance: Int): EnterTransition {
    val dir = slideDirection(initialState.destination.route, targetState.destination.route)
    return if (dir >= 0) {
        slideInHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { slideDistance } +
        fadeIn(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
    } else {
        slideInHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { -slideDistance } +
        fadeIn(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainExitTransition(slideDistance: Int): ExitTransition {
    val dir = slideDirection(initialState.destination.route, targetState.destination.route)
    return if (dir >= 0) {
        slideOutHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { -slideDistance } +
        fadeOut(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
    } else {
        slideOutHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { slideDistance } +
        fadeOut(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
    }
}

object NavigationTrigger {
    var pendingRoute by mutableStateOf<String?>(null)
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Observe and handle external navigation triggers (e.g. from screen saver buttons)
    val pendingRoute = NavigationTrigger.pendingRoute
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            android.util.Log.d("AppNavigation", "Navigating to externally triggered route: $pendingRoute")
            navController.navigate(pendingRoute) {
                // Ensure correct state pop up
                val startDestId = navController.graph.startDestinationId
                popUpTo(startDestId) { inclusive = false }
                launchSingleTop = true
            }
            NavigationTrigger.pendingRoute = null
        }
    }

    // Akses SharedPreferences langsung menggunakan LocalContext
    val sharedPreferences = LocalContext.current.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Ambil nilai deviceID dan status preload dari SharedPreferences
    val savedDeviceId = sharedPreferences.getString("deviceID", null)
    val hasPreloaded = sharedPreferences.getBoolean("hasPreloaded", false)

    // Start background preload on app launch if branchId exists
    val context = LocalContext.current
    val branchId = remember(sharedPreferences) { sharedPreferences.getString("branchId", null) }
    LaunchedEffect(branchId) {
        if (branchId != null) {
            com.dafamsemarang.dhtv.DataRepository.startPreload(context, branchId)
        }
    }

    // Tentukan tampilan awal (Direct to HomeScreen, skipping WelcomeScreen in main flow)
    val startDestination = if (savedDeviceId == null) {
        "pairing"  // Jika perangkat belum dipairing, mulai dengan Pairing Screen
    } else if (!hasPreloaded) {
        "preload"  // Jika sudah dipairing tapi belum pernah preload sama sekali
    } else {
        "home"     // Beralih langsung ke HomeScreen, WelcomeScreen hanya aktif sebagai screensaver!
    }

    // Observe current route to toggle Header visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val density = androidx.compose.ui.platform.LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val slideDistance = (screenWidthPx * SLIDE_OFFSET_PCT).toInt()

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Persistent wallpaper — sits behind everything, never animates ──
        WallpaperSection()

        // Navigasi berdasarkan status savedDeviceId
        NavHost(
            navController = navController,
            startDestination = startDestination,
            // Default transitions for non-main screens (preload, pairing, welcome)
            enterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.96f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(300, easing = FastOutSlowInEasing))
            }
        ) {
            composable("preload") {
                PreloadScreen(onPreloadFinished = {
                    // Tandai bahwa satu kali inisiasi resource awal sudah berhasil
                    sharedPreferences.edit { putBoolean("hasPreloaded", true) }
                    // Trigger screensaver overlay to display first!
                    ScreenSaverManager.isScreenSaverActive = true
                    navController.navigate("home") {
                        popUpTo("preload") { inclusive = true }
                        launchSingleTop = true
                    }
                })
            }

            composable("pairing") {
                PairingScreen(
                    onDeviceIdSaved = { deviceId ->
                        // Simpan device ID setelah pairing dan navigasi ke Preload Screen
                        sharedPreferences.edit { putString("deviceID", deviceId) }
                        navController.navigate("preload") {
                            // Pastikan tampilan pairing tidak bisa kembali
                            popUpTo("pairing") { inclusive = true }
                        }
                    },
                    sharedPreferences = sharedPreferences,
                    deviceManager = DeviceManager(LocalContext.current)
                )
            }

            composable(
                "welcome",
                enterTransition = {
                    fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(500, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing))
                }
            ) {
                WelcomeScreen(
                    onNavigateToHome = {
                        // Transition smoothly from welcome to home screen
                        navController.navigate("home") {
                            // Ensure we cannot go back into loading/welcome states directly
                            popUpTo("welcome") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
                // Checkout reminder - muncul di semua screen
                CheckoutReminder()
            }

            // ── HOME ────────────────────────────────────────────────────────
            composable(
                "home",
                enterTransition = { mainEnterTransition(slideDistance) },
                exitTransition = { mainExitTransition(slideDistance) },
                popEnterTransition = { mainEnterTransition(slideDistance) },
                popExitTransition = { mainExitTransition(slideDistance) }
            ) {
                HomeScreen(navController)
                CheckoutReminder()
            }

            // ── F&B ─────────────────────────────────────────────────────────
            composable(
                "cantingfood",
                enterTransition = { mainEnterTransition(slideDistance) },
                exitTransition = { mainExitTransition(slideDistance) },
                popEnterTransition = { mainEnterTransition(slideDistance) },
                popExitTransition = { mainExitTransition(slideDistance) }
            ) {
                FoodBeverageScreen()
                CheckoutReminder()
            }

            // ── REQUEST / CONTACT ────────────────────────────────────────────
            composable(
                "contact",
                enterTransition = { mainEnterTransition(slideDistance) },
                exitTransition = { mainExitTransition(slideDistance) },
                popEnterTransition = { mainEnterTransition(slideDistance) },
                popExitTransition = { mainExitTransition(slideDistance) }
            ) {
                ContactUsScreen()
                CheckoutReminder()
            }

            // ── HOTEL INFO ───────────────────────────────────────────────────
            composable(
                "hotel_guide",
                enterTransition = {
                    fadeIn(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
                }
            ) {
                HotelInfoScreen()
                CheckoutReminder()
            }
        } // end NavHost

        // Hoisted Persistent Header and Footer - Immediately ready when entering from Welcome Screen.
        // Retains full persistence and buttery-smooth rendering without Haze GPU shader overhead.
        val showHeaderFooter = currentRoute in listOf("welcome", "home", "hotel_guide", "contact", "cantingfood")
        AnimatedVisibility(
            visible = showHeaderFooter,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500))
        ) {
             val isWelcome = currentRoute == "welcome"
             val targetAlpha = if (isWelcome) 0f else 1f
             val animatedAlpha by animateFloatAsState(
                 targetValue = targetAlpha,
                 animationSpec = tween(durationMillis = 500),
                 label = "HeaderFooterAlpha"
             )
             Box(
                 modifier = Modifier
                     .zIndex(1f)
                     .fillMaxSize()
                     .graphicsLayer { alpha = animatedAlpha }
             ) {
                  HeaderSection(currentRoute)
                  FooterSection(navController)
             }
        }
    }
}
