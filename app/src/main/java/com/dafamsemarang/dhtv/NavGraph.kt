package com.dafamsemarang.dhtv

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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


// Ordered list of main screens — determines slide direction
private val mainScreenOrder = listOf("home", "cantingfood", "contact", "hotel_guide")

/** Returns +1 if navigating forward (slide from right), -1 if backward (slide from left) */
private fun slideDirection(from: String?, to: String?): Int {
    val fromIndex = mainScreenOrder.indexOf(from ?: "")
    val toIndex = mainScreenOrder.indexOf(to ?: "")
    return if (toIndex >= fromIndex) 1 else -1
}

private const val SLIDE_DURATION = 500
private const val SLIDE_OFFSET_FRACTION = 0.06f  // 6% of screen width — dominant fade, subtle direction hint like Google TV

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Akses SharedPreferences langsung menggunakan LocalContext
    val sharedPreferences = LocalContext.current.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Ambil nilai deviceID dan status preload dari SharedPreferences
    val savedDeviceId = sharedPreferences.getString("deviceID", null)
    val hasPreloaded = sharedPreferences.getBoolean("hasPreloaded", false)

    // Tentukan tampilan awal
    val startDestination = if (savedDeviceId == null) {
        "pairing"  // Jika perangkat belum dipairing, mulai dengan Pairing Screen
    } else if (!hasPreloaded) {
        "preload"  // Jika sudah dipairing tapi belum pernah preload sama sekali
    } else {
        "welcome"  // Jika sudah pernah preload sekali, langsung masuk Welcome Screen
    }

    // Observe current route to toggle Header visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
                    navController.navigate("welcome") {
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

            composable("welcome") {
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
            // Fade-only on enter to prevent ExoPlayer SurfaceView glitches.
            // Participates in directional slide on exit.
            composable(
                "home",
                enterTransition = {
                    fadeIn(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    val dir = slideDirection("home", targetState.destination.route)
                    slideOutHorizontally(
                        targetOffsetX = { w -> (-w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = {
                    fadeOut(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                }
            ) {
                HomeScreen(navController)
                CheckoutReminder()
            }

            // ── F&B ─────────────────────────────────────────────────────────
            composable(
                "cantingfood",
                enterTransition = {
                    val dir = slideDirection(initialState.destination.route, "cantingfood")
                    slideInHorizontally(
                        initialOffsetX = { w -> (w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    val dir = slideDirection("cantingfood", targetState.destination.route)
                    slideOutHorizontally(
                        targetOffsetX = { w -> (-w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    val dir = slideDirection(initialState.destination.route, "cantingfood")
                    slideInHorizontally(
                        initialOffsetX = { w -> (w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    val dir = slideDirection("cantingfood", targetState.destination.route)
                    slideOutHorizontally(
                        targetOffsetX = { w -> (-w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                }
            ) {
                FoodBeverageScreen()
                CheckoutReminder()
            }

            // ── REQUEST / CONTACT ────────────────────────────────────────────
            composable(
                "contact",
                enterTransition = {
                    val dir = slideDirection(initialState.destination.route, "contact")
                    slideInHorizontally(
                        initialOffsetX = { w -> (w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    val dir = slideDirection("contact", targetState.destination.route)
                    slideOutHorizontally(
                        targetOffsetX = { w -> (-w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    val dir = slideDirection(initialState.destination.route, "contact")
                    slideInHorizontally(
                        initialOffsetX = { w -> (w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    val dir = slideDirection("contact", targetState.destination.route)
                    slideOutHorizontally(
                        targetOffsetX = { w -> (-w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                }
            ) {
                ContactUsScreen()
                CheckoutReminder()
            }

            // ── HOTEL INFO ───────────────────────────────────────────────────
            composable(
                "hotel_guide",
                enterTransition = {
                    val dir = slideDirection(initialState.destination.route, "hotel_guide")
                    slideInHorizontally(
                        initialOffsetX = { w -> (w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    val dir = slideDirection("hotel_guide", targetState.destination.route)
                    slideOutHorizontally(
                        targetOffsetX = { w -> (-w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                popEnterTransition = {
                    val dir = slideDirection(initialState.destination.route, "hotel_guide")
                    slideInHorizontally(
                        initialOffsetX = { w -> (w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    val dir = slideDirection("hotel_guide", targetState.destination.route)
                    slideOutHorizontally(
                        targetOffsetX = { w -> (-w * SLIDE_OFFSET_FRACTION * dir).toInt() },
                        animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(SLIDE_DURATION, easing = FastOutSlowInEasing))
                }
            ) {
                HotelInfoScreen()
                CheckoutReminder()
            }
        }

        // Persistent Header and Footer (Hoisted)
        // Only show on main screens (Home, Hotel Info, Contact, F&B)
        // Use AnimatedVisibility with a delay to prevent them from appearing
        // on the previous screen (Welcome) before the new screen (Home) has loaded.
        AnimatedVisibility(
            visible = currentRoute in listOf("home", "hotel_guide", "contact", "cantingfood"),
            enter = fadeIn(animationSpec = tween(durationMillis = 500, delayMillis = 300)),
            exit = fadeOut(animationSpec = tween(durationMillis = 500))
        ) {
             Box(modifier = Modifier.zIndex(1f).fillMaxSize()) {
                 HeaderSection()
                 FooterSection(navController)
             }
        }
    }
}
