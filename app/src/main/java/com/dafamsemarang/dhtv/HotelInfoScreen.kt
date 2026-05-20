@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package com.dafamsemarang.dhtv

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import coil.compose.rememberAsyncImagePainter
import com.dafamsemarang.dhtv.CachedAsyncImage
import androidx.compose.foundation.lazy.LazyRow
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
    var isFilteringCategory by remember { mutableStateOf(false) }
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

    // Snap states
    val categoryListState = rememberLazyListState()
    val categorySnapBehavior = rememberSnapFlingBehavior(lazyListState = categoryListState)
    val itemListState = rememberLazyListState()
    val itemSnapBehavior = rememberSnapFlingBehavior(lazyListState = itemListState)
    
    var focusedItemIndex by remember { mutableIntStateOf(0) }
    var itemScrollTrigger by remember { mutableIntStateOf(0) }
    

    // Reset scroll to start when category changes
    LaunchedEffect(selectedButton) {
        itemListState.scrollToItem(0)
        focusedItemIndex = 0
    }
    
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemWidthPx = with(density) { 152.dp.toPx() } // 120dp image + 32dp padding

    LaunchedEffect(focusedItemIndex, itemScrollTrigger) {
        val targetOffset = focusedItemIndex * itemWidthPx
        val currentOffset = itemListState.firstVisibleItemIndex * itemWidthPx + itemListState.firstVisibleItemScrollOffset
        val delta = targetOffset - currentOffset

        if (kotlin.math.abs(delta) > 1f) {
            itemListState.animateScrollBy(
                value = delta,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 600,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        }
    }

    val focusScope = rememberCoroutineScope()

    val buttonIcons = listOf(
        R.drawable.transition_dissolve,
        R.drawable.scene,
        R.drawable.emergency_heat,
        R.drawable.spa,
        R.drawable.share_location
    )

    // Get context and branchId from SharedPreferences
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)

    // Firebase database reference
    val database = Firebase.database.reference

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {


        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 116.dp, bottom = 16.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val selectedItems = when (selectedButton) {
                0 -> hotelFacilities
                1 -> roomFacilities
                2 -> emergencyProcedure
                3 -> healthAndWellness
                4 -> discoverDestination
                else -> emptyList()
            }
            val itemRequesters = remember(selectedItems) { List(selectedItems.size + 10) { FocusRequester() } }
            var categoryChanged by remember { mutableStateOf(false) }
            
            // Determine loading state based on selected category
            val isLoadingSelected = when (selectedButton) {
                0 -> isLoadingHotelFacilities
                1 -> isLoadingRoomFacilities
                2 -> isLoadingEmergencyProcedure
                3 -> isLoadingHealthWellness
                4 -> isLoadingDiscoverDestination
                else -> false
            }
            
            // Show shimmer when category changes
            LaunchedEffect(selectedButton) {
                if (!isLoadingSelected) {
                    isFilteringCategory = true
                    delay(150)
                    isFilteringCategory = false
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(.9f)
                        .background(color = HotelInfoBox, shape = RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {

                        Column {
                            buttonLabels.forEachIndexed { index, label ->
                                val interactionSource = remember { MutableInteractionSource() }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .onFocusChanged { 
                                            if (it.isFocused) {
                                                if (selectedButton != index) {
                                                    categoryChanged = true
                                                }
                                                selectedButton = index
                                                selectedItem = null
                                            }
                                        }
                                        .clickable(
                                            onClick = {
                                                selectedButton = index
                                                selectedItem = null
                                            },
                                            indication = ripple(color = HotelInfoRipple),
                                            interactionSource = interactionSource
                                        )
                                        .padding(8.dp) // Apply scaled padding
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                painter = painterResource(id = buttonIcons[index]),
                                                contentDescription = label,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .padding(end = 8.dp),
                                                tint = HotelInfoIcon
                                            )

                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = HotelInfoText,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(start = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(.9f)
                            .fillMaxWidth()
                            .background(color = HotelInfoBox, shape = RoundedCornerShape(24.dp))
                    ) {
                        LazyRow(
                            state = itemListState,
                            flingBehavior = itemSnapBehavior,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties { 
                                     enter = { direction ->
                                         if (direction == FocusDirection.Up) {
                                             itemRequesters[0]
                                         } else {
                                             if (categoryChanged) {
                                                 categoryChanged = false
                                                 itemRequesters[0]
                                             } else {
                                                 FocusRequester.Default
                                             }
                                         }
                                     }
                                 },
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            if ((isLoadingSelected && shimmerVisible) || isFilteringCategory) {
                                items(5) {
                                    ItemCardShimmer()
                                }
                            } else {
                                itemsIndexed(
                                    items = selectedItems,
                                    key = { _, it -> it.name + it.imageUrl }
                                ) { index, item: Item ->
                                    ItemCard(
                                        item = item, 
                                        onClick = { selectedItem = item },
                                        modifier = Modifier
                                            .focusRequester(if (index < itemRequesters.size) itemRequesters[index] else FocusRequester.Default)
                                            .onFocusChanged {
                                                if (it.isFocused) {
                                                    focusedItemIndex = index
                                                    itemScrollTrigger++
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

@Composable
fun ItemCard(item: Item, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var isClicked by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val rippleIndication = ripple(color = HotelInfoRipple)

    // Dynamic focus pulse animations
    val focusFadeAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 350),
        label = "FocusFadeAlpha"
    )
    val pulseAlpha = remember { Animatable(0.0f) }
    LaunchedEffect(isFocused) {
        if (isFocused) {
            pulseAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAlpha.snapTo(0.0f)
        }
    }

    // Google TV zoom scale on focus
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ItemCardScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            }
    ) {
        Box(
            modifier = Modifier
                .size(150.dp) // Maintain exact size
                .onFocusChanged { isFocused = it.isFocused }
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 3.dp,
                            color = Color.White.copy(alpha = pulseAlpha.value * focusFadeAlpha),
                            shape = RoundedCornerShape(24.dp) // Concentrically balanced with the 24.dp clip and 3.dp width!
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(4.dp) // Balance gap
                .clip(RoundedCornerShape(20.dp)) // Concentric inner clip: 24.dp outer - 4.dp padding = 20.dp! Perfect balance!
                .clickable(
                    onClick = {
                        onClick()
                        isClicked = !isClicked
                    },
                    indication = rippleIndication,
                    interactionSource = interactionSource
                )
        ) {
            if (item.imageUrl.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CachedAsyncImage(
                        imageUrl = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        cachePrefix = "hotel_info"
                    )

                    // Gradient overlay for description text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )

                    // Description text inside the image, bottom-right aligned
                    Text(
                        text = item.description,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Normal,
                            fontSize = 8.sp // Ultra premium micro-typography
                        ),
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = item.name,
            style = TextStyle(
                fontSize = 12.sp,
                color = HotelInfoText,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// Shimmer component for item cards
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
        Color.Gray.copy(alpha = 0.2f),
        Color.Gray.copy(alpha = 0.4f),
        Color.Gray.copy(alpha = 0.2f)
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = shimmerColors,
                                start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                                end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                )
            }
        }
        // Title shimmer
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(14.dp)
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                        end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                    ),
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}