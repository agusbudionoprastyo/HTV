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

object HotelInfoFocus {
    val firstItemRequester = FocusRequester()
}

@Composable
fun HotelInfoScreen(navController: androidx.navigation.NavHostController? = null) {
    var selectedButton by remember { mutableStateOf(0) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    var hotelFacilities by remember { mutableStateOf<List<Item>>(emptyList()) }
    var roomFacilities by remember { mutableStateOf<List<Item>>(emptyList()) }
    var emergencyProcedure by remember { mutableStateOf<List<Item>>(emptyList()) }
    var healthAndWellness by remember { mutableStateOf<List<Item>>(emptyList()) }
    var discoverDestination by remember { mutableStateOf<List<Item>>(emptyList()) }
    
    // Loading states for shimmer
    var isLoadingHotelFacilities by remember { mutableStateOf(true) }
    var isLoadingRoomFacilities by remember { mutableStateOf(true) }
    var isLoadingEmergencyProcedure by remember { mutableStateOf(true) }
    var isLoadingHealthWellness by remember { mutableStateOf(true) }
    var isLoadingDiscoverDestination by remember { mutableStateOf(true) }
    
    // Delay shimmer visibility until after screen transition completes (500ms)
    var shimmerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(550)
        shimmerVisible = true
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
    val verticalListState = rememberLazyListState()

    // Get context and branchId from SharedPreferences
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)

    // Firebase Real-Time Listener with deterministic lifecycle control
    DisposableEffect(key1 = branchId) {
        var ref1: com.google.firebase.database.DatabaseReference? = null
        var ref2: com.google.firebase.database.DatabaseReference? = null
        var ref3: com.google.firebase.database.DatabaseReference? = null
        var ref4: com.google.firebase.database.DatabaseReference? = null
        var ref5: com.google.firebase.database.DatabaseReference? = null
        
        var l1: com.google.firebase.database.ValueEventListener? = null
        var l2: com.google.firebase.database.ValueEventListener? = null
        var l3: com.google.firebase.database.ValueEventListener? = null
        var l4: com.google.firebase.database.ValueEventListener? = null
        var l5: com.google.firebase.database.ValueEventListener? = null

        if (branchId != null) {
            val db = com.google.firebase.database.FirebaseDatabase.getInstance().reference
            
            ref1 = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("HOTEL_FACILITY")
            l1 = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    hotelFacilities = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                    if (isLoadingHotelFacilities) isLoadingHotelFacilities = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingHotelFacilities = false }
            }
            ref1.addValueEventListener(l1)

            ref2 = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("ROOMS_FACILITY")
            l2 = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    roomFacilities = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                    if (isLoadingRoomFacilities) isLoadingRoomFacilities = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingRoomFacilities = false }
            }
            ref2.addValueEventListener(l2)

            ref3 = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("EMERGENCY_PROCEDURE")
            l3 = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    emergencyProcedure = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                    if (isLoadingEmergencyProcedure) isLoadingEmergencyProcedure = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingEmergencyProcedure = false }
            }
            ref3.addValueEventListener(l3)

            ref4 = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("HEALTH_WELLNESS")
            l4 = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    healthAndWellness = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                    if (isLoadingHealthWellness) isLoadingHealthWellness = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingHealthWellness = false }
            }
            ref4.addValueEventListener(l4)

            ref5 = db.child("BRANCHES").child(branchId).child("HOTEL_INFO").child("DISCOVER_DESTINATION")
            l5 = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    discoverDestination = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
                    if (isLoadingDiscoverDestination) isLoadingDiscoverDestination = false
                }
                override fun onCancelled(error: DatabaseError) { isLoadingDiscoverDestination = false }
            }
            ref5.addValueEventListener(l5)
        }

        onDispose {
            if (ref1 != null && l1 != null) ref1.removeEventListener(l1)
            if (ref2 != null && l2 != null) ref2.removeEventListener(l2)
            if (ref3 != null && l3 != null) ref3.removeEventListener(l3)
            if (ref4 != null && l4 != null) ref4.removeEventListener(l4)
            if (ref5 != null && l5 != null) ref5.removeEventListener(l5)
        }
    }

    val categoriesList = listOf(
        Pair("HOTEL FACILITY", hotelFacilities),
        Pair("ROOMS FACILITY", roomFacilities),
        Pair("EMERGENCY PROCEDURE", emergencyProcedure),
        Pair("HEALTH & WELLNESS", healthAndWellness),
        Pair("DISCOVER DESTINATION", discoverDestination)
    )

    val currentFocusedItem = categoriesList.getOrNull(selectedButton)?.second?.getOrNull(focusedItemIndex)
    var debouncedFocusedItem by remember { mutableStateOf<Item?>(null) }

    // Efficient TV scroll debouncer: Renders the first item instantly on load, then applies 250ms delay for active navigation.
    // Also prevents background image loading and text re-rendering during active vertical scroll to ensure absolute 60fps smoothness!
    LaunchedEffect(currentFocusedItem, verticalListState.isScrollInProgress) {
        val isScrolling = verticalListState.isScrollInProgress && verticalListState.firstVisibleItemIndex > 0
        if (!isScrolling) {
            if (debouncedFocusedItem == null && currentFocusedItem != null) {
                debouncedFocusedItem = currentFocusedItem
            } else {
                delay(250)
                debouncedFocusedItem = currentFocusedItem
            }
        }
    }

    var currentBgUrl by remember { mutableStateOf("") }

    LaunchedEffect(debouncedFocusedItem?.imageUrl) {
        val newUrl = debouncedFocusedItem?.imageUrl ?: ""
        if (newUrl != currentBgUrl) {
            currentBgUrl = newUrl
        }
    }

    val contentSlideOffset = remember { Animatable(100f) } // starts 100.dp to the right
    val GoogleTvEasing = CubicBezierEasing(0.18f, 0.85f, 0.18f, 1.00f)
    LaunchedEffect(Unit) {
        contentSlideOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 800, easing = GoogleTvEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0C)) // Deep cinematic black root background
    ) {
        // Immersive Hero Background with premium Crossfade
        Crossfade(
            targetState = currentBgUrl,
            animationSpec = tween(400),
            label = "BackgroundCrossfade",
            modifier = Modifier.fillMaxSize()
        ) { url ->
            if (url.isNotEmpty()) {
                CachedAsyncImage(
                    imageUrl = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    cachePrefix = "img"
                )
            }
        }

            // Double Overlay vignette to guarantee absolute text readability while remaining beautifully bright!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0C0C0C).copy(alpha = 0.70f),
                                Color(0xFF0C0C0C).copy(alpha = 0.50f),
                                Color(0xFF0C0C0C).copy(alpha = 0.30f),
                                Color(0xFF0C0C0C).copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0C0C0C).copy(alpha = 0.20f),
                                Color(0xFF0C0C0C).copy(alpha = 0.50f)
                            )
                        )
                    )
            )

        // Layout Column (Edge-to-Edge with 58.dp safety zone)
        val density = androidx.compose.ui.platform.LocalDensity.current
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

        val itemBringIntoViewSpec = remember(defaultSpec, startPaddingPx) {
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

        val tabRequesters = remember { List(buttonLabels.size) { FocusRequester() } }
        // Incremented whenever any tab gains focus — signals the carousel to scroll back to item 0.
        var tabFocusTrigger by remember { mutableIntStateOf(0) }


        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = contentSlideOffset.value.dp)
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
                    
                    val borderAlpha = remember { Animatable(0.5f) }
                    LaunchedEffect(isTabFocused) {
                        if (isTabFocused) {
                            borderAlpha.animateTo(
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                        } else {
                            borderAlpha.snapTo(0.5f)
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .focusRequester(tabRequesters[index])
                            .onFocusChanged {
                                isTabFocused = it.isFocused
                                if (it.isFocused) {
                                    selectedButton = index
                                    focusedItemIndex = 0
                                    tabFocusTrigger++ // trigger carousel scroll to item 0
                                }
                            }
                            .focusable()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Transparent)
                            .then(
                                if (isTabFocused) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = Color.White.copy(alpha = borderAlpha.value),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                selectedButton = index
                                focusedItemIndex = 0
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
            } // end CompositionLocalProvider (categoryBringIntoViewSpec)

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
                    val rowState = rememberLazyListState()
                    val snapBehavior = rememberSnapFlingBehavior(lazyListState = rowState)
                    // isNavigatingHorizontally is hoisted to Column scope (shared with detail AnimatedContent)

                    LaunchedEffect(targetIndex) {
                        rowState.scrollToItem(0)
                    }
                    // When any tab gains focus (user pressed UP from items), scroll back to item 0
                    // so that itemRequesters[0] is attached before focusProperties.enter fires.
                    LaunchedEffect(tabFocusTrigger) {
                        if (tabFocusTrigger > 0) rowState.scrollToItem(0)
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
                                    // Always enter the LazyRow at item 0 (first item) when coming from tab above.
                                    enter = { itemRequesters.getOrElse(0) { FocusRequester.Default } }
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
                                                if (index == 0) Modifier.focusRequester(HotelInfoFocus.firstItemRequester)
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
    val pulseAlpha = remember { Animatable(0.0f) }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            pulseAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAlpha.snapTo(0.0f)
        }
    }

    // Google TV zoom scale on focus
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "ItemCardScale"
    )

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
                            width = 2.5.dp,
                            color = Color.White.copy(alpha = pulseAlpha.value * focusFadeAlpha),
                            shape = RoundedCornerShape(24.dp)
                        )
                    } else {
                        Modifier // Completely borderless when not focused!
                    }
                )
                .padding(5.dp) // Gap space between border and image is exactly 2.5.dp (as thick as the border itself!)
                .clip(RoundedCornerShape(19.dp)) // Concentric balanced inner radius: 24.dp outer - 5.dp padding = 19.dp!
                .clickable(
                    onClick = {
                        onClick()
                        isClicked = !isClicked
                    },
                    indication = null,
                    interactionSource = interactionSource
                )
        ) {
            if (item.imageUrl.isNotEmpty()) {
                CachedAsyncImage(
                    imageUrl = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    cachePrefix = "img"
                )

                // Bottom subtle dark gradient inside card
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                )
                            )
                        )
                )
            }
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