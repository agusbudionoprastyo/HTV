@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.animation.ExperimentalAnimationApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
package com.dafamsemarang.dhtv

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import coil.compose.rememberAsyncImagePainter
import com.dafamsemarang.dhtv.CachedAsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.ui.input.key.*

private val routeCache = java.util.concurrent.ConcurrentHashMap<String, RouteInfo>()

object HotelInfoFocus {
    val firstItemRequester = FocusRequester()
}

@Composable
fun HotelInfoScreen(navController: androidx.navigation.NavHostController? = null) {
    var selectedButton by remember { mutableStateOf(0) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    val hotelFacilities by DataRepository.hotelFacilities
    val roomFacilities by DataRepository.roomFacilities
    val emergencyProcedure by DataRepository.emergencyProcedure
    val healthAndWellness by DataRepository.healthAndWellness
    val discoverDestination by DataRepository.discoverDestination
    val branchLatLng by DataRepository.branchLatLng
    

    val isLoadingHotelFacilities = !DataRepository.isHotelFacilitiesLoaded.value
    val isLoadingRoomFacilities = !DataRepository.isRoomFacilitiesLoaded.value
    val isLoadingEmergencyProcedure = !DataRepository.isEmergencyProcedureLoaded.value
    val isLoadingHealthWellness = !DataRepository.isHealthWellnessLoaded.value
    val isLoadingDiscoverDestination = !DataRepository.isDiscoverDestinationLoaded.value
    
    val currentIsLoading = when(selectedButton) {
        0 -> isLoadingHotelFacilities
        1 -> isLoadingRoomFacilities
        2 -> isLoadingEmergencyProcedure
        3 -> isLoadingHealthWellness
        4 -> isLoadingDiscoverDestination
        else -> false
    }

    var shimmerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(currentIsLoading) {
        if (currentIsLoading) {
            delay(550)
            if (currentIsLoading) shimmerVisible = true
        } else {
            shimmerVisible = false
        }
    }
    val buttonLabels = listOf(
        "HOTEL FACILITY",
        "ROOMS FACILITY",
        "EMERGENCY PROCEDURE",
        "HEALTH & WELLNESS",
        "DISCOVER DESTINATION"
    )

    val scope = rememberCoroutineScope()
    
    var focusedItemIndex by remember { mutableIntStateOf(0) }
    val rowState = remember(selectedButton) { androidx.compose.foundation.lazy.LazyListState() }

    val categoriesList = listOf(
        Pair("HOTEL FACILITY", hotelFacilities),
        Pair("ROOMS FACILITY", roomFacilities),
        Pair("EMERGENCY PROCEDURE", emergencyProcedure),
        Pair("HEALTH & WELLNESS", healthAndWellness),
        Pair("DISCOVER DESTINATION", discoverDestination)
    )

    val currentFocusedItem = categoriesList.getOrNull(selectedButton)?.second?.getOrNull(focusedItemIndex)
    var debouncedFocusedItem by remember { mutableStateOf<Item?>(currentFocusedItem) }

    var lastFocusChangeTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(currentFocusedItem) {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastChange = currentTime - lastFocusChangeTime
        lastFocusChangeTime = currentTime
        
        if (debouncedFocusedItem == null || timeSinceLastChange > 300) {
            debouncedFocusedItem = currentFocusedItem
        } else {
            kotlinx.coroutines.delay(250)
            debouncedFocusedItem = currentFocusedItem
        }
    }

    LaunchedEffect(debouncedFocusedItem) {
        if (debouncedFocusedItem != null) {
            com.dafamsemarang.dhtv.DataRepository.globalHotelImageUrl.value = debouncedFocusedItem?.imageUrl
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // Transparent so the global decoupled background shows through
    ) {

        val density = androidx.compose.ui.platform.LocalDensity.current

        val tabRequesters = remember { List(buttonLabels.size) { FocusRequester() } }



        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 95.dp, bottom = 55.dp), // Restrict layout area balanced below the header (95.dp) and snug above the footer (55.dp)
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
            val categorySlideDistance = (screenWidthPx * 0.20f).toInt()
            val GoogleTvEasing = CubicBezierEasing(0.18f, 0.85f, 0.18f, 1.00f)
            val SLIDE_DURATION = 800
            // Hoisted here so the detail AnimatedContent can suppress its animation
            // while the carousel is scrolling horizontally — preventing GPU contention.
            var isNavigatingHorizontally by remember { mutableStateOf(false) }

            val startPaddingPx = with(density) { 58.dp.toPx() }
            val defaultSpec = LocalBringIntoViewSpec.current

            val categoryBringIntoViewSpec = remember(defaultSpec, startPaddingPx) {
                object : BringIntoViewSpec {
                    override val scrollAnimationSpec: androidx.compose.animation.core.AnimationSpec<Float>
                        get() {
                            val duration = if (android.os.Build.VERSION.SDK_INT < 31) 60 else 100
                            return androidx.compose.animation.core.tween(
                                durationMillis = duration,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        }

                    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                        return offset - startPaddingPx
                    }
                }
            }
            
            val itemBringIntoViewSpec = remember(defaultSpec, startPaddingPx, isNavigatingHorizontally) {
                object : BringIntoViewSpec {
                    override val scrollAnimationSpec: androidx.compose.animation.core.AnimationSpec<Float>
                        get() {
                            val duration = if (android.os.Build.VERSION.SDK_INT < 31) 90 else 150
                            return androidx.compose.animation.core.tween(
                                durationMillis = duration,
                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                            )
                        }

                    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                        return offset - startPaddingPx
                    }
                }
            }

            // 1. TOP AREA: Category Tabs (LazyRow right at the top)
            CompositionLocalProvider(LocalBringIntoViewSpec provides categoryBringIntoViewSpec) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 58.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(buttonLabels) { index, label ->
                    var isTabFocused by remember { mutableStateOf(false) }
                    val isTabSelected = selectedButton == index
                    
                    val categoryItems = categoriesList.getOrNull(index)?.second ?: emptyList()
                    Box(
                        modifier = Modifier
                            .focusRequester(tabRequesters[index])
                            .then(
                                if (categoryItems.isEmpty() && isTabSelected) {
                                    Modifier.focusRequester(HotelInfoFocus.firstItemRequester)
                                } else {
                                    Modifier
                                }
                            )
                            .onFocusChanged {
                                isTabFocused = it.isFocused
                                if (it.isFocused) {
                                    if (selectedButton != index) {
                                        selectedButton = index
                                        focusedItemIndex = 0
                                    }
                                }
                            }
                            .focusable()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Transparent)
                            .then(
                                if (isTabFocused) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                if (selectedButton != index) {
                                    selectedButton = index
                                    focusedItemIndex = 0
                                }
                            }
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = if (isTabSelected || isTabFocused) FontWeight.Bold else FontWeight.Medium,
                                color = if (isTabSelected || isTabFocused) Color.White else Color.White.copy(alpha = 0.5f),
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            } // end categoryBringIntoViewSpec

            Spacer(modifier = Modifier.height(10.dp))

            // 2. MIDDLE AREA: Cinematic Details (wrapped in slide-in/out AnimatedContent)
            AnimatedContent(
                targetState = selectedButton,
                transitionSpec = {
                    if (targetState >= initialState) {
                        (slideInHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { categorySlideDistance } +
                                fadeIn(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { -categorySlideDistance } +
                                        fadeOut(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
                            )
                            .using(androidx.compose.animation.SizeTransform { _, _ -> tween(0) })
                    } else {
                        (slideInHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { -categorySlideDistance } +
                                fadeIn(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { categorySlideDistance } +
                                        fadeOut(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
                            )
                            .using(androidx.compose.animation.SizeTransform { _, _ -> tween(0) })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "CinematicDetailsCategoryTransition"
            ) { targetIndex ->
                val activeCategory = categoriesList.getOrNull(targetIndex)
                val itemsList = activeCategory?.second ?: emptyList()
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedContent(
                        targetState = focusedItemIndex,
                        transitionSpec = {
                            // While scrolling horizontally, use instant (0ms) transitions so the detail
                            // slide animation doesn't compete with the carousel scroll animation.
                            val dur = if (isNavigatingHorizontally) 0 else 500
                            val itemSlideDistance = 150
                            if (targetState >= initialState) {
                                (slideInHorizontally(animationSpec = tween(dur, easing = GoogleTvEasing)) { itemSlideDistance } +
                                        fadeIn(animationSpec = tween(dur, easing = GoogleTvEasing)))
                                    .togetherWith(
                                        slideOutHorizontally(animationSpec = tween(dur, easing = GoogleTvEasing)) { -itemSlideDistance } +
                                                fadeOut(animationSpec = tween(dur, easing = GoogleTvEasing))
                                    )
                                    .using(androidx.compose.animation.SizeTransform { _, _ -> tween(0) })
                            } else {
                                (slideInHorizontally(animationSpec = tween(dur, easing = GoogleTvEasing)) { -itemSlideDistance } +
                                        fadeIn(animationSpec = tween(dur, easing = GoogleTvEasing)))
                                    .togetherWith(
                                        slideOutHorizontally(animationSpec = tween(dur, easing = GoogleTvEasing)) { itemSlideDistance } +
                                                fadeOut(animationSpec = tween(dur, easing = GoogleTvEasing))
                                    )
                                    .using(androidx.compose.animation.SizeTransform { _, _ -> tween(0) })
                            }
                        },
                        label = "CinematicDetailsCardTransition"
                    ) { targetCardIndex ->
                        val item = itemsList.getOrNull(targetCardIndex) ?: debouncedFocusedItem
                        if (item != null) {
                            // For Discover Destination (targetIndex == 4), show a two-column layout
                            // with title+description on the left and a static map image on the right.
                            if (targetIndex == 4) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 58.dp, end = 58.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: Title + Description
                                    Column(
                                        modifier = Modifier.weight(0.75f)
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = TextStyle(
                                                fontSize = 26.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                letterSpacing = (-0.5).sp
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = item.description,
                                            style = TextStyle(
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Color.White.copy(alpha = 0.75f)
                                            ),
                                            maxLines = 7,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .weight(1.25f)
                                            .fillMaxHeight(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                         // 1. Static Map Card (Left Column)
                                         val destLatLng = debouncedFocusedItem?.longlat?.takeIf { it.isNotEmpty() }
                                         val originLatLng = branchLatLng.takeIf { !it.isNullOrEmpty() }
                                         val travelMode = debouncedFocusedItem?.travelMode?.takeIf { it.isNotEmpty() } ?: "driving"
                                         val apiKey = "AIzaSyAlZ1fPEOKMmywDHbZNvmCTEXvPPTCsVTo"

                                         fun processStaticMapUrl(url: String): String {
                                             if (url.isEmpty() || url.contains("signature=")) return url
                                             var resUrl = url
                                             resUrl = if (resUrl.contains("size=")) {
                                                 resUrl.replace(Regex("size=\\d+x\\d+"), "size=300x300")
                                             } else {
                                                 resUrl + "&size=300x300"
                                             }
                                             if (!resUrl.contains("scale=")) {
                                                 resUrl += "&scale=2"
                                             }
                                             resUrl = resUrl.replace(Regex("&style=[^&]*"), "")
                                             try {
                                                 val customStyle = listOf(
                                                     "feature:poi|visibility:off",
                                                     "feature:transit|visibility:off",
                                                     "feature:administrative|visibility:off",
                                                     "feature:landscape|color:0xffffff",
                                                     "feature:water|color:0xe0e0e0",
                                                     "feature:road.local|visibility:off",
                                                     "feature:road.highway|element:geometry|color:0x666666",
                                                     "feature:road.arterial|element:geometry|color:0x888888",
                                                     "feature:road|element:labels|visibility:on",
                                                     "feature:road|element:labels.text.fill|color:0x444444",
                                                     "feature:road|element:labels.text.stroke|visibility:on|color:0xffffff"
                                                 ).joinToString("") { "&style=${java.net.URLEncoder.encode(it, "UTF-8")}" }
                                                 resUrl += customStyle
                                             } catch (e: Exception) {}
                                             return resUrl
                                         }

                                         val cacheKey = "${debouncedFocusedItem?.name ?: ""}_$travelMode"
                                         val cachedRoute = routeCache[cacheKey]

                                         val dynamicRouteState = androidx.compose.runtime.produceState<RouteInfo?>(
                                             initialValue = cachedRoute,
                                             debouncedFocusedItem?.name, travelMode
                                         ) {
                                             if (cachedRoute != null) {
                                                 value = cachedRoute
                                             } else {
                                                 val currentDest = destLatLng
                                                 if (currentDest != null && originLatLng != null) {
                                                     val routeInfo = fetchDirectionsRouteInfo(originLatLng, currentDest, travelMode, apiKey)
                                                     if (routeInfo != null) {
                                                         routeCache[cacheKey] = routeInfo
                                                         value = routeInfo
                                                     } else {
                                                         value = null
                                                     }
                                                 } else {
                                                     value = null
                                                 }
                                             }
                                         }
                                         val routeInfo = dynamicRouteState.value

                                         val highResMapUrl = remember(routeInfo, debouncedFocusedItem?.staticMapUrl) {
                                             val currentRoute = routeInfo
                                             val currentDest = destLatLng
                                             val currentItem = debouncedFocusedItem
                                             if (currentRoute != null && currentDest != null && originLatLng != null) {
                                                 val encodedPolyline = java.net.URLEncoder.encode(currentRoute.polyline, "UTF-8")
                                                 val customStyle = listOf(
                                                     "feature:poi|visibility:off",
                                                     "feature:transit|visibility:off",
                                                     "feature:administrative|visibility:off",
                                                     "feature:landscape|color:0xffffff",
                                                     "feature:water|color:0xe0e0e0",
                                                     "feature:road.local|visibility:off",
                                                     "feature:road.highway|element:geometry|color:0x666666",
                                                     "feature:road.arterial|element:geometry|color:0x888888",
                                                     "feature:road|element:labels|visibility:on",
                                                     "feature:road|element:labels.text.fill|color:0x444444",
                                                     "feature:road|element:labels.text.stroke|visibility:on|color:0xffffff"
                                                 ).joinToString("") { "&style=${java.net.URLEncoder.encode(it, "UTF-8")}" }
                                                 
                                                 // Parse origin lat/lng to add visibility padding for short routes to prevent over-zooming
                                                 val originParts = originLatLng.split(",")
                                                 val originLat = originParts.getOrNull(0)?.toDoubleOrNull()
                                                 val originLng = originParts.getOrNull(1)?.toDoubleOrNull()
                                                 val visibleParam = if (originLat != null && originLng != null) {
                                                     "&visible=${originLat + 0.0035},${originLng + 0.0035}&visible=${originLat - 0.0035},${originLng - 0.0035}"
                                                 } else ""
                                                 
                                                 "https://maps.googleapis.com/maps/api/staticmap?size=300x300&scale=2&path=color:0xff0000ff|weight:5|enc:$encodedPolyline&markers=color:blue|label:H|$originLatLng&markers=color:red|label:D|$currentDest&key=$apiKey$customStyle$visibleParam"
                                             } else if (currentItem != null) {
                                                 processStaticMapUrl(currentItem.staticMapUrl)
                                             } else {
                                                 ""
                                             }
                                         }
                                         val hasMap = highResMapUrl.isNotEmpty()

                                         if (hasMap) {
                                              Column(
                                                  modifier = Modifier
                                                      .fillMaxHeight()
                                                      .aspectRatio(0.75f)
                                                      .clip(RoundedCornerShape(16.dp))
                                                      .background(Color.White.copy(alpha = 0.3f))
                                                      .padding(8.dp),
                                                  horizontalAlignment = Alignment.CenterHorizontally
                                              ) {
                                                  Box(
                                                      modifier = Modifier
                                                          .fillMaxWidth()
                                                          .aspectRatio(1f)
                                                          .clip(RoundedCornerShape(12.dp)),
                                                      contentAlignment = Alignment.Center
                                                  ) {
                                                      val context = LocalContext.current
                                                      val mapRequest = remember(highResMapUrl) {
                                                          coil.request.ImageRequest.Builder(context)
                                                              .data(highResMapUrl)
                                                              .build()
                                                      }
                                                      coil.compose.AsyncImage(
                                                          model = mapRequest,
                                                          contentDescription = "Peta Rute",
                                                          contentScale = ContentScale.Crop,
                                                          modifier = Modifier.fillMaxSize()
                                                      )
                                                  }
                                                  Spacer(modifier = Modifier.height(8.dp))
                                                  if (routeInfo != null && routeInfo.distance.isNotEmpty() && routeInfo.duration.isNotEmpty()) {
                                                      val travelText = if (travelMode == "walking") "jalan kaki" else "berkendara"
                                                      Text(
                                                          text = "${routeInfo.distance} (${routeInfo.duration} $travelText)",
                                                          style = TextStyle(
                                                              fontSize = 10.sp,
                                                              lineHeight = 14.sp,
                                                              color = Color.White.copy(alpha = 0.8f),
                                                              fontWeight = FontWeight.Medium,
                                                              textAlign = TextAlign.Left
                                                          ),
                                                          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                                      )
                                                  }
                                              }
                                              Spacer(modifier = Modifier.width(12.dp))
                                         }

                                         // 2. QR Code Card (Right Column)
                                         val qrUrl = remember(debouncedFocusedItem?.name, destLatLng, originLatLng, travelMode) {
                                             val currentItem = debouncedFocusedItem
                                             if (currentItem == null) ""
                                             else if (destLatLng != null && originLatLng != null) {
                                                 // Directions: hotel → destination
                                                 "https://www.google.com/maps/dir/?api=1&origin=${java.net.URLEncoder.encode(originLatLng, "UTF-8")}&destination=${java.net.URLEncoder.encode(destLatLng, "UTF-8")}&travelmode=$travelMode"
                                             } else if (destLatLng != null) {
                                                 "https://www.google.com/maps/search/?api=1&query=${java.net.URLEncoder.encode(destLatLng, "UTF-8")}"
                                             } else {
                                                 "https://www.google.com/maps/search/?api=1&query=${java.net.URLEncoder.encode(currentItem.name, "UTF-8")}"
                                             }
                                         }
                                         val qrBitmapState = produceState<android.graphics.Bitmap?>(initialValue = null, qrUrl) {
                                             value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                                 try {
                                                     if (qrUrl.isEmpty()) null
                                                     else {
                                                         val writer = com.google.zxing.qrcode.QRCodeWriter()
                                                         val bitMatrix = writer.encode(qrUrl, com.google.zxing.BarcodeFormat.QR_CODE, 180, 180)
                                                         val bmp = android.graphics.Bitmap.createBitmap(180, 180, android.graphics.Bitmap.Config.ARGB_8888)
                                                         for (x in 0 until 180) {
                                                             for (y in 0 until 180) {
                                                                 bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.parseColor("#666666") else android.graphics.Color.TRANSPARENT)
                                                             }
                                                         }
                                                         bmp
                                                     }
                                                 } catch (e: Exception) { null }
                                             }
                                         }
                                         val qrBitmap = qrBitmapState.value

                                         Column(
                                             modifier = Modifier
                                                 .fillMaxHeight()
                                                 .aspectRatio(0.75f)
                                                 .clip(RoundedCornerShape(16.dp))
                                                 .background(Color.White.copy(alpha = 0.3f))
                                                 .padding(8.dp),
                                             horizontalAlignment = Alignment.CenterHorizontally
                                         ) {
                                             Box(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .aspectRatio(1f)
                                                     .clip(RoundedCornerShape(12.dp))
                                                     .background(Color.White),
                                                 contentAlignment = Alignment.Center
                                             ) {
                                                 if (qrBitmap != null) {
                                                     androidx.compose.foundation.Image(
                                                         bitmap = qrBitmap.asImageBitmap(),
                                                         contentDescription = "QR Code",
                                                         modifier = Modifier
                                                             .fillMaxSize()
                                                             .padding(2.dp)
                                                             .clip(RoundedCornerShape(8.dp))
                                                     )
                                                 }
                                             }

                                             Spacer(modifier = Modifier.height(8.dp))

                                              Text(
                                                  text = "Akses dari ponsel anda",
                                                  style = TextStyle(
                                                      fontSize = 10.sp,
                                                      lineHeight = 14.sp,
                                                      color = Color.White.copy(alpha = 0.8f),
                                                      fontWeight = FontWeight.Medium,
                                                      textAlign = TextAlign.Left
                                                  ),
                                                  modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                                              )
                                         }
                                    }
                                }
                            } else {
                                // Default layout for all other tabs
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 58.dp, end = 58.dp, bottom = 8.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        style = TextStyle(
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.description,
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = Color.White.copy(alpha = 0.75f)
                                        ),
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. BOTTOM AREA: Carousel Row (wrapped in slide-in/out AnimatedContent)
            AnimatedContent(
                targetState = selectedButton,
                transitionSpec = {
                    if (targetState >= initialState) {
                        (slideInHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { categorySlideDistance } +
                                fadeIn(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { -categorySlideDistance } +
                                        fadeOut(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
                            )
                            .using(androidx.compose.animation.SizeTransform { _, _ -> tween(0) })
                    } else {
                        (slideInHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { -categorySlideDistance } +
                                fadeIn(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing)) { categorySlideDistance } +
                                        fadeOut(animationSpec = tween(SLIDE_DURATION, easing = GoogleTvEasing))
                            )
                            .using(androidx.compose.animation.SizeTransform { _, _ -> tween(0) })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                label = "CarouselCategoryTransition"
            ) { targetIndex ->
                val activeCategory = categoriesList.getOrNull(targetIndex)
                val itemsList = activeCategory?.second ?: emptyList()
                // Each AnimatedContent state gets its own FocusRequester list to avoid
                // dual-attachment crashes when both old and new content compose simultaneously.
                val itemRequesters = remember { List(100) { FocusRequester() } }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val snapBehavior = rememberSnapFlingBehavior(lazyListState = rowState)
                    // isNavigatingHorizontally is hoisted to Column scope (shared with detail AnimatedContent)

                    LaunchedEffect(targetIndex) {
                        rowState.scrollToItem(0)
                    }
                    CompositionLocalProvider(LocalBringIntoViewSpec provides itemBringIntoViewSpec) {
                        LazyRow(
                            state = rowState,
                            flingBehavior = snapBehavior,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onPreviewKeyEvent { keyEvent ->
                                    if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                        when (keyEvent.key) {
                                            androidx.compose.ui.input.key.Key.DirectionLeft,
                                            androidx.compose.ui.input.key.Key.DirectionRight -> {
                                                isNavigatingHorizontally = true
                                            }
                                            androidx.compose.ui.input.key.Key.DirectionUp,
                                            androidx.compose.ui.input.key.Key.DirectionDown -> {
                                                isNavigatingHorizontally = false
                                            }
                                        }
                                    }
                                    false
                                }
                                .focusProperties {
                                    // Restore focus to the last selected item (e.g., when coming back up from the footer)
                                    // When coming down from a tab, focusedItemIndex is already explicitly set to 0.
                                    enter = { itemRequesters.getOrElse(focusedItemIndex) { FocusRequester.Default } }
                                },
                            contentPadding = PaddingValues(horizontal = 58.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isLoadingCategory = when (targetIndex) {
                                0 -> isLoadingHotelFacilities
                                1 -> isLoadingRoomFacilities
                                2 -> isLoadingEmergencyProcedure
                                3 -> isLoadingHealthWellness
                                4 -> isLoadingDiscoverDestination
                                else -> false
                            }

                            if (isLoadingCategory && shimmerVisible) {
                                items(5) {
                                    ItemCardShimmer()
                                }
                            } else {
                                itemsIndexed(
                                    items = itemsList,
                                    key = { _, it -> it.name + it.imageUrl }
                                ) { index, item: Item ->
                                    ItemCard(
                                        item = item,
                                        onClick = { selectedItem = item },
                                        modifier = Modifier
                                            .focusRequester(if (index < itemRequesters.size) itemRequesters[index] else FocusRequester.Default)
                                            .then(
                                                if (index == focusedItemIndex && targetIndex == selectedButton) Modifier.focusRequester(HotelInfoFocus.firstItemRequester)
                                                else Modifier
                                            )
                                            .focusProperties {
                                                up = tabRequesters[targetIndex]
                                            }
                                            .onFocusChanged {
                                                if (it.isFocused) {
                                                    focusedItemIndex = index
                                                }
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ItemCard(item: Item, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isClicked by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // Dynamic focus pulse animations
    val focusFadeAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 300),
        label = "FocusFadeAlpha"
    )
    // Google TV zoom scale on focus
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "ItemCardScale"
    )

    val pulseAlpha = remember { Animatable(0.4f) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            pulseAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAlpha.snapTo(0.4f)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            }
    ) {
        Box(
            modifier = modifier
                .width(196.dp)
                .height(120.dp)
                .onFocusChanged { isFocused = it.isFocused }
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 3.dp,
                            color = Color.White.copy(alpha = pulseAlpha.value * focusFadeAlpha),
                            shape = RoundedCornerShape(24.dp)
                        )
                    } else {
                        Modifier // Completely borderless when not focused!
                    }
                )
                .padding(6.dp) // Gap space between border and image is exactly 6.dp (2x thick as the border!)
                .clip(RoundedCornerShape(18.dp)) // Concentric balanced inner radius: 24.dp outer - 6.dp padding = 18.dp!
                .clickable(
                    onClick = {
                        onClick()
                        isClicked = !isClicked
                    },
                    indication = null,
                    interactionSource = interactionSource
                )
        ) {
            CachedAsyncImage(
                imageUrl = item.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                cachePrefix = "img",
                showShimmer = false,
                error = R.drawable.err
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = item.name,
            style = TextStyle(
                fontSize = 11.sp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.7f),
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(186.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ItemCardShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "itemCardShimmer")
    val shimmerTranslateAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.05f),
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.05f)
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(196.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                        end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                    )
                )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                        end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                    )
                )
        )
    }
}

data class RouteInfo(
    val polyline: String,
    val distance: String,
    val duration: String
)

suspend fun fetchDirectionsRouteInfo(origin: String, destination: String, mode: String, apiKey: String): RouteInfo? {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val urlStr = "https://maps.googleapis.com/maps/api/directions/json?origin=${java.net.URLEncoder.encode(origin, "UTF-8")}&destination=${java.net.URLEncoder.encode(destination, "UTF-8")}&mode=${java.net.URLEncoder.encode(mode, "UTF-8")}&key=$apiKey"
            val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = org.json.JSONObject(responseText)
            if (json.optString("status") == "OK") {
                val routes = json.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val routeObj = routes.getJSONObject(0)
                    val overviewPolyline = routeObj.optJSONObject("overview_polyline")
                    val polylinePoints = overviewPolyline?.optString("points") ?: ""
                    
                    val legs = routeObj.optJSONArray("legs")
                    var distanceText = ""
                    var durationText = ""
                    if (legs != null && legs.length() > 0) {
                        val legObj = legs.getJSONObject(0)
                        distanceText = legObj.optJSONObject("distance")?.optString("text") ?: ""
                        durationText = legObj.optJSONObject("duration")?.optString("text") ?: ""
                    }
                    return@withContext RouteInfo(polylinePoints, distanceText, durationText)
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}