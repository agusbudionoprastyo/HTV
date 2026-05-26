@file:OptIn(
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
package com.dafamsemarang.dhtv

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.tv.foundation.PivotOffsets
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.foundation.lazy.list.items as tvItems
import androidx.tv.foundation.lazy.list.itemsIndexed as tvItemsIndexed
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.dafamsemarang.dhtv.CachedAsyncImage

import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults

import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.text.DecimalFormat
import android.Manifest
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.mutableFloatStateOf
import io.ktor.client.request.header

class CartPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("cart_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    @SuppressLint("UseKtx")
    fun saveCart(items: List<SelectedItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString("cart_items", json).apply()
    }

    fun getCart(): List<SelectedItem> {
        val json = prefs.getString("cart_items", null)
        return if (json != null) {
            gson.fromJson(json, Array<SelectedItem>::class.java).toList()
        } else {
            emptyList()
        }
    }

    @SuppressLint("UseKtx")
    fun clearCart() {
        prefs.edit().remove("cart_items").apply()
    }
}

@Composable
fun FoodBeverageScreen(navController: androidx.navigation.NavHostController? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val menuItems by com.dafamsemarang.dhtv.DataRepository.menuItems
    var showDialog by remember { mutableStateOf(false) }
    var selectedItemDetail by remember { mutableStateOf<MenuItemData?>(null) }
    val selectedItems = GlobalCartState.selectedItems
    val cartPreferences = remember { CartPreferences(context) }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var focusedCategoryForSelection by remember { mutableStateOf<String?>(null) }

    val categories = remember(menuItems) { derivedStateOf { menuItems.map { it.category }.distinct() } }
    
    // Loading state for shimmer
    var isLoadingMenuItems by remember { mutableStateOf(true) }
    var isFilteringCategory by remember { mutableStateOf(false) }
    // Delay shimmer visibility until after screen transition completes (500ms)
    var shimmerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isLoadingMenuItems) {
        if (isLoadingMenuItems) {
            kotlinx.coroutines.delay(550)
            if (isLoadingMenuItems) shimmerVisible = true
        } else {
            shimmerVisible = false
        }
    }

    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)

    // Sync selected items with current branchId
    LaunchedEffect(branchId) {
        if (branchId != null) {
            selectedItems.clear()
            val cartItems = cartPreferences.getCart()
            selectedItems.addAll(cartItems.filter { it.item.branchId == branchId })
        }
    }
    val isMenuLoaded by com.dafamsemarang.dhtv.DataRepository.isMenuLoaded
    // Update loading state when menu items are loaded or finished loading
    LaunchedEffect(isMenuLoaded) {
        if (isMenuLoaded) {
            isLoadingMenuItems = false
        }
    }

    val filteredMenuItems = if (selectedCategory.isNullOrEmpty()) {
        menuItems
    } else {
        menuItems.filter { it.category == selectedCategory }
    }
    
    val currentCategoryIndex = remember(selectedCategory, categories.value) {
        val cats = listOf(null) + categories.value
        cats.indexOf(selectedCategory).coerceAtLeast(0)
    }
    
    val focusScope = rememberCoroutineScope()

    // Snap & Focus States
    val categoryListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val itemListState = remember(selectedCategory) { androidx.compose.foundation.lazy.LazyListState() }
    val categorySnapBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = categoryListState)
    val itemSnapBehavior = androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior(lazyListState = itemListState)
    
    var focusedCategoryIndex by remember { mutableIntStateOf(0) }
    var isNavigatingHorizontally by remember { mutableStateOf(false) }
    var categoryScrollTrigger by remember { mutableIntStateOf(0) }

    var focusedItemIndex by remember { mutableIntStateOf(0) }
    var itemScrollTrigger by remember { mutableIntStateOf(0) }
    
    val categoryRequesters = remember(categories.value) { List(categories.value.size + 1) { FocusRequester() } }
    val itemRequesters = remember(filteredMenuItems) { List(filteredMenuItems.size + 10) { FocusRequester() } }
    

    // Reset scroll to start when category changes
    LaunchedEffect(selectedCategory) {
        itemListState.scrollToItem(0)
        focusedItemIndex = 0
    }

    LaunchedEffect(focusedCategoryForSelection) {
        delay(200)
        selectedCategory = focusedCategoryForSelection
    }

    // Unified with HomeScreen: Root 8.dp padding removed to allow edge-to-edge scrolling!
    Box(
        modifier = Modifier 
            .fillMaxSize()
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val startPaddingPx = with(density) { 58.dp.toPx() }
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        val categorySlideDistance = (screenWidthPx * 0.20f).toInt()
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
        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultSpec) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides categoryBringIntoViewSpec) {
                LazyRow(
                    state = categoryListState,
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
                            enter = { 
                                val firstVisible = focusedCategoryIndex
                                if (firstVisible < categoryRequesters.size) {
                                    categoryRequesters[firstVisible]
                                } else {
                                    FocusRequester.Default
                                }
                            }
                        }
                        .padding(top = 110.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(start = 58.dp, end = 58.dp)
                ) {
                             item {
                                 val isSelected = selectedCategory == null
                                 var isFocused by remember { mutableStateOf(false) }

                                 val borderAlpha = remember { Animatable(0.5f) }
                                 LaunchedEffect(isFocused) {
                                     if (isFocused) {
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

                                 val scale by animateFloatAsState(
                                     targetValue = if (isFocused) 1.1f else 1.0f,
                                     animationSpec = tween(350, easing = FastOutSlowInEasing),
                                     label = "AllScale"
                                 )
                                 val bgColor by animateColorAsState(
                                     targetValue = if (isSelected) Color(0xFFE91E63) else Color.Black.copy(alpha = 0.2f),
                                     animationSpec = tween(250),
                                     label = "AllBgColor"
                                 )
                                 val textColor by animateColorAsState(
                                     targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                     animationSpec = tween(250),
                                     label = "AllTextColor"
                                 )

                                 Box(
                                     modifier = Modifier
                                         .padding(horizontal = 4.dp)
                                         .scale(scale)
                                         .then(
                                             if (isFocused) {
                                                 Modifier.border(
                                                     width = 3.dp,
                                                     color = Color.White.copy(alpha = borderAlpha.value),
                                                     shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
                                                 )
                                             } else {
                                                 Modifier
                                             }
                                         )
                                         .padding(6.dp)
                                         .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                                         .background(bgColor)
                                         .focusRequester(categoryRequesters[0])
                                         .onFocusChanged { focusState ->
                                             isFocused = focusState.isFocused
                                             if (focusState.isFocused) {
                                                 focusedCategoryForSelection = null
                                                 focusedCategoryIndex = 0
                                                 categoryScrollTrigger++
                                             }
                                         }
                                         .clickable(
                                             onClick = {
                                                 focusedCategoryForSelection = null
                                                 selectedCategory = null
                                             },
                                             indication = null,
                                             interactionSource = remember { MutableInteractionSource() }
                                         )
                                 ) {
                                     Text(
                                         text = "All",
                                         style = TextStyle(
                                             color = textColor,
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 12.sp
                                         ),
                                         modifier = Modifier
                                             .padding(horizontal = 14.dp, vertical = 6.dp)
                                             .align(Alignment.Center)
                                     )
                                 }
                             }
                            items(categories.value) { category ->
                                 val isSelected = selectedCategory == category
                                 var isFocused by remember { mutableStateOf(false) }

                                 val borderAlpha = remember { Animatable(0.5f) }
                                 LaunchedEffect(isFocused) {
                                     if (isFocused) {
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
                                val scale by animateFloatAsState(
                                     targetValue = if (isFocused) 1.1f else 1.0f,
                                     animationSpec = tween(350, easing = FastOutSlowInEasing),
                                     label = "CategoryScale"
                                 )
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFFE91E63) else Color.Black.copy(alpha = 0.2f),
                                    animationSpec = tween(250),
                                    label = "CategoryBgColor"
                                )
                                val textColor by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                    animationSpec = tween(250),
                                    label = "CategoryTextColor"
                                )

                                 Box(
                                     modifier = Modifier
                                         .padding(horizontal = 4.dp)
                                         .scale(scale)
                                         .then(
                                             if (isFocused) {
                                                 Modifier.border(
                                                     width = 3.dp,
                                                     color = Color.White.copy(alpha = borderAlpha.value),
                                                     shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
                                                 )
                                             } else {
                                                 Modifier
                                             }
                                         )
                                         .padding(6.dp)
                                         .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                                         .background(bgColor)
                                         .focusRequester(if (categories.value.indexOf(category) + 1 < categoryRequesters.size) categoryRequesters[categories.value.indexOf(category) + 1] else FocusRequester.Default)
                                         .onFocusChanged { focusState ->
                                             isFocused = focusState.isFocused
                                             if (focusState.isFocused) {
                                                 val targetIndex = categories.value.indexOf(category) + 1
                                                 focusedCategoryForSelection = category
                                                 focusedCategoryIndex = targetIndex
                                                 categoryScrollTrigger++
                                             }
                                         }
                                         .clickable(
                                             onClick = {
                                                 focusedCategoryForSelection = category
                                                 selectedCategory = category
                                             },
                                             indication = null,
                                             interactionSource = remember { MutableInteractionSource() }
                                         )
                                 ) {
                                     Text(
                                         text = category,
                                         style = TextStyle(
                                             color = textColor,
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 12.sp
                                         ),
                                         modifier = Modifier
                                             .padding(horizontal = 14.dp, vertical = 6.dp)
                                             .align(Alignment.Center)
                                     )
                                 }
                             }
                        }
                    }

                        AnimatedContent(
                            targetState = Triple(currentCategoryIndex, isFilteringCategory, filteredMenuItems),
                            transitionSpec = {
                                val GoogleTvEasing = CubicBezierEasing(0.18f, 0.85f, 0.18f, 1.00f)
                                val SLIDE_DURATION = 800
                                
                                val isForward = targetState.first >= initialState.first
                                if (isForward) {
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
                                .padding(top = 158.dp)
                                .height(300.dp),
                            label = "MenuItemTransition"
                        ) { (_, isFiltering, itemsList) ->
                            CompositionLocalProvider(LocalBringIntoViewSpec provides itemBringIntoViewSpec) {
                                LazyRow(
                                    state = itemListState,
                                flingBehavior = itemSnapBehavior,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                .focusProperties { 
                                        enter = { 
                                            itemRequesters.getOrNull(focusedItemIndex) ?: FocusRequester.Default
                                        }
                                    },
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top,
                                contentPadding = PaddingValues(start = 58.dp, end = 58.dp)
                            ) {
                                if ((isLoadingMenuItems && shimmerVisible) || isFiltering) {
                                    // Show shimmer items while loading or filtering
                                    items(5) {
                                        MenuItemShimmer()
                                    }
                                } else {
                                    itemsIndexed(itemsList) { index, item: MenuItemData ->
                                        val itemModifier = if (index < itemRequesters.size) {
                                            Modifier.focusRequester(itemRequesters[index])
                                        } else {
                                            Modifier
                                        }
                                        
                                        MenuItem(
                                            item = item,
                                            modifier = itemModifier.onFocusChanged { 
                                                if (it.isFocused) {
                                                    focusedItemIndex = index
                                                    itemScrollTrigger++
                                                }
                                            },
                                            onCardClick = {
                                                selectedItemDetail = item
                                                showDialog = true
                                            },
                                            onAddClick = { clickOffset ->
                                                 // Direct Add to Cart Logic!
                                                 val existingItem = selectedItems.find { 
                                                     it.item.name == item.name && 
                                                     it.selectedVariant == null && 
                                                     it.specialInstruction.isEmpty() 
                                                 }
                                                 if (existingItem != null) {
                                                     existingItem.quantity += 1
                                                 } else {
                                                     selectedItems.add(SelectedItem(item, 1, "", null))
                                                 }
                                                 
                                                 // Save to persistent preferences
                                                 CartPreferences(context).saveCart(selectedItems)
                                                 
                                                 // Trigger flying dot animation!
                                                 GlobalCartState.animStartOffset.value = clickOffset
                                                 GlobalCartState.animateTrigger.value++
                                             }
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
        }

        if (showDialog && selectedItemDetail != null) {
            val cartPreferences = CartPreferences(context)
            ItemDialog(
                item = selectedItemDetail!!,
                onAddToCart = { item, quantity, specialInstruction, selectedVariant ->
                    val existingIndex = selectedItems.indexOfFirst {
                        it.item.name == item.name &&
                                it.selectedVariant == selectedVariant &&
                                (it.specialInstruction == specialInstruction ||
                                        (specialInstruction.isEmpty() && it.specialInstruction.isEmpty()))
                    }

                    if (existingIndex != -1) {
                        // Replace item in list to trigger SnapshotStateList notification → Compose recomposes → badge count updates
                        val existing = selectedItems[existingIndex]
                        selectedItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
                    } else {
                        selectedItems.add(SelectedItem(item, quantity, specialInstruction, selectedVariant))
                    }

                    cartPreferences.saveCart(selectedItems)
                },
                onDismiss = { showDialog = false }
            )
        }
    }

fun formatIDR(amount: Int?): String {
    val formatter = DecimalFormat("Rp#,###")
    return formatter.format(amount).replace(",", ".")
}

// Function to escape HTML special characters for Telegram
fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
// Function to listen for guest info updates based on roomId
fun listenForUpdates(
    database: DatabaseReference,
    roomId: String,
    context: Context,
    onGuestInfoChange: (GuestInfo?) -> Unit
) {
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    
    val path = "BRANCHES/$branchId/FOGUEST/$roomId"
    Log.d("DHTV_FOOD", "Setting up Firebase listener for path: $path")
    database.child(path).addValueEventListener(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            Log.d("DHTV_FOOD", "Firebase data changed for path: $path")
            Log.d("DHTV_FOOD", "Data exists: ${dataSnapshot.exists()}")
            Log.d("DHTV_FOOD", "Raw data: ${dataSnapshot.value}")
            
            if (dataSnapshot.exists()) {
                val guestInfo = dataSnapshot.getValue(GuestInfo::class.java)
                Log.d("DHTV_FOOD", "Guest info from Firebase: $guestInfo")
                onGuestInfoChange(guestInfo)
            } else {
                Log.e("DHTV_FOOD", "No data found in Firebase for path: $path")
                onGuestInfoChange(null)
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.e("DHTV_FOOD", "Firebase listener cancelled: ${databaseError.message}")
            onGuestInfoChange(null)
        }
    })
}

// Function to generate a unique orderId
fun generateOrderId(): String {
    return "${System.currentTimeMillis()}"
}

fun sendOrderNotification(context: Context, folioId: Int, paymentMethod: String, orderId: String, selectedItems: List<SelectedItem>) {
    val database = Firebase.database.reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)

    // Buat string yang berisi nama-nama item yang dipesan
    val itemNames = selectedItems.joinToString(", ") { it.item.name }

    // Pesan notifikasi sekarang mencakup item yang dipesan
    val orderMessage = "$itemNames has been placed with $paymentMethod as the payment method."

    val notification = Notification(
        id = orderId,  // Gunakan orderId yang sudah dibuat
        title = "Order Placed",
        message = orderMessage,
        timestamp = System.currentTimeMillis(),
        type = "ROOM_SERVICE"
    )

    val notificationsRef = database.child("BRANCHES").child(branchId!!).child("NOTIFICATIONS").child(folioId.toString())
    notificationsRef.push().setValue(notification)
}

// Function to send order to Firebase under the "orders" node
fun sendOrderToDatabase(context: Context, folioId: Int, guestName: String, guestPhone: String, guestRoom: String, paymentMethod: String, selectedItems: List<SelectedItem>, orderStatus: String, orderId: String) {
    val database = Firebase.database.reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    
    val order = Order(
        folioId = folioId,
        guestName = guestName,
        guestPhone = guestPhone,
        guestRoom = guestRoom,
        paymentMethod = paymentMethod,
        status = orderStatus,  // Initial status as "open"
        items = selectedItems.map {
            // Create an order item detail for each item, including the note
            OrderItem(
                itemName = it.item.name,
                quantity = it.quantity,
                price = it.item.price + (it.selectedVariant?.price ?: 0),  // Safely add variant price if selected
                variant = it.selectedVariant?.name,  // Only include variant if it's selected
                note = it.specialInstruction, // Include note for each item
                imageUrl = it.item.imageRes
            )
        },
        timestamp = System.currentTimeMillis(), // Time when the order was placed
        orderId = orderId,  // Include the generated orderId
        branchId = branchId,  // Add branchId to the order

        // Corrected calculations
        subtotal = selectedItems.sumOf { (it.item.price + (it.selectedVariant?.price ?: 0)) * it.quantity },
        taxService = selectedItems.sumOf {
            (it.item.price + (it.selectedVariant?.price ?: 0)) * it.quantity * it.item.tax / 100
        },
        total = selectedItems.sumOf {
            (it.item.price + (it.selectedVariant?.price ?: 0)) * it.quantity
        } + selectedItems.sumOf {
            (it.item.price + (it.selectedVariant?.price ?: 0)) * it.quantity * it.item.tax / 100
        }
    )

    val ordersRef = database.child("BRANCHES").child(branchId!!).child("ORDERS").push()
    ordersRef.setValue(order)
}

fun sendPostOrderToApi(
    context: Context,
    folioId: Int,
    guestName: String,
    guestPhone: String,
    guestRoom: String,
    paymentMethod: String,
    selectedItems: List<SelectedItem>,
    orderId: String
) {
    val items = selectedItems.map {
        OrderItem(
            itemName = it.item.name,
            quantity = it.quantity,
            price = (it.item.price + (it.selectedVariant?.price ?: 0)),  // Safely add variant price if selected
            note = it.specialInstruction,  // Include note for each item
            variant = it.selectedVariant?.name,  // Only include variant if it's selected
            imageUrl = it.item.imageRes
        )
    }

    // Calculate subtotal, tax, and total (handling variants properly)
    val subtotal = selectedItems.sumOf {
        (it.item.price + (it.selectedVariant?.price ?: 0)) * it.quantity
    }

    val taxService = selectedItems.sumOf {
        (it.item.price + (it.selectedVariant?.price ?: 0)) * it.quantity * it.item.tax / 100
    }

    val total = subtotal + taxService  // Add subtotal and tax to get total

    // Get Firebase database reference
    val database = Firebase.database.reference
    
    // Get branchId from SharedPreferences
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)

    // Only proceed if branchId is available
    if (branchId != null) {
        // Get TELEGRAM_ORDER settings from the specific branch
        val telegramOrderRef = database.child("BRANCHES").child(branchId).child("SETTING").child("TELEGRAM_ORDER")
        // Get siteUrl from its own node
        val siteUrlRef = database.child("siteUrl")

        telegramOrderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SuspiciousIndentation")
            override fun onDataChange(snapshot: DataSnapshot) {
                // Firebase structure: chatId = chat ID (group ID), tokenBot = bot token
                val chatId = snapshot.child("chatId").getValue(Any::class.java) // Can be Long or Number
                val botToken = snapshot.child("tokenBot").getValue(String::class.java)

                // Get siteUrl from its own node
                siteUrlRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(siteUrlSnapshot: DataSnapshot) {
                        val siteUrl = siteUrlSnapshot.getValue(String::class.java)

                        // Check if all the required parameters are available
                        if (chatId != null && botToken != null && siteUrl != null) {
                            // Build Telegram Bot API endpoint URL
                            val endpointUrl = "https://api.telegram.org/bot$botToken/sendMessage"
                            // Create message format for Telegram (HTML)
                            // Escape HTML special characters in dynamic content
                            val escapedGuestName = escapeHtml(formatName(guestName))
                            val escapedPaymentMethod = escapeHtml(paymentMethod)
                            
                            val message = """
<b>Pesanan Baru</b>

Halo! Ada pesanan baru yang perlu dikonfirmasi.

<a href="$siteUrl/orders/$orderId">Klik di sini untuk konfirmasi</a>

<b>Detail Tamu</b>
<b>Nama:</b> $escapedGuestName
<b>Folio:</b> $folioId
<b>Kamar:</b> $guestRoom
${if (guestPhone.isEmpty()) "" else "<b>Telepon:</b> $guestPhone"}

<b>Ringkasan Pesanan</b>
<b>Order ID:</b> <code>$orderId</code>

<b>Item Pesanan:</b>
${items.joinToString("\n") { item ->
val escapedItemName = escapeHtml(item.itemName ?: "")
val escapedVariant = if(item.variant.isNullOrEmpty()) "" else escapeHtml(item.variant)
val escapedNote = if(item.note.isNullOrEmpty()) "" else escapeHtml(item.note)
val variantText = if(escapedVariant.isEmpty()) "" else " (<i>$escapedVariant</i>)"
val noteText = if(escapedNote.isEmpty()) "" else "\n  <i>$escapedNote</i>"
"• <b>$escapedItemName</b>$variantText x${item.quantity}\n  ${formatIDR(item.price)}$noteText"
}}

<b>Metode Pembayaran:</b> $escapedPaymentMethod

<b>Subtotal:</b> ${formatIDR(subtotal)}
<b>Pajak & Service:</b> ${formatIDR(taxService)}
<b>Total:</b> ${formatIDR(total)}

Terima kasih, semoga banyak orderan!
                            """.trimIndent()

                            // Log the message to verify it's correct
                            Log.d("TelegramGatewayHandler", "Message to API: $message")

                            // Create the request body for Telegram Bot API
                            // Convert chat_id to Long (Telegram group IDs are always numbers)
                            val chatIdNumber: Long = when (chatId) {
                                is Long -> chatId
                                is Number -> chatId.toLong()
                                is String -> {
                                    try {
                                        chatId.toLong()
                                    } catch (e: NumberFormatException) {
                                        Log.e("TelegramGatewayHandler", "chatId is not a valid number: $chatId")
                                        throw IllegalArgumentException("chatId must be a number, got: $chatId")
                                    }
                                }
                                else -> {
                                    Log.e("TelegramGatewayHandler", "chatId has unsupported type: ${chatId.javaClass}")
                                    throw IllegalArgumentException("chatId must be a number, got: ${chatId.javaClass}")
                                }
                            }
                            
                            // Create request body using data class for proper serialization
                            val requestBody = TelegramMessageRequest(
                                chat_id = chatIdNumber,  // Telegram group chat ID (Long)
                                text = message,         // Message text with HTML formatting
                                parse_mode = "HTML"     // Enable HTML parsing for <b>bold</b> and <i>italic</i>
                            )
                            
                            Log.d("TelegramGatewayHandler", "Endpoint URL: $endpointUrl")

                            // Use CoroutineScope to send the API request asynchronously
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    // Initialize Ktor HttpClient for sending the request
                                    val client = HttpClient(Android) {
                                        install(ContentNegotiation) {
                                            json(Json { prettyPrint = true; isLenient = true })
                                        }
                                    }

                                    Log.d("TelegramGatewayHandler", "Making API call to: $endpointUrl")
                                    Log.d("TelegramGatewayHandler", "Request body: $requestBody")

                                    // Send the POST request to the URL
                                    val response = client.post(endpointUrl) {
                                        contentType(ContentType.Application.Json)
                                        setBody(requestBody)
                                    }

                                    val responseBody = response.bodyAsText()
                                    Log.d("TelegramGatewayHandler", "Response status: ${response.status}")
                                    Log.d("TelegramGatewayHandler", "Response body: $responseBody")

                                    // Check the response status
                                    if (response.status == HttpStatusCode.OK) {
                                        Log.d("TelegramGatewayHandler", "Message sent successfully to Telegram")
                                    } else {
                                        Log.e("TelegramGatewayHandler", "Error: ${response.status}")
                                        Log.e("TelegramGatewayHandler", "Response Body: $responseBody")
                                    }

                                    client.close()

                                } catch (e: Exception) {
                                    // Log the error if the request fails
                                    Log.e("TelegramGatewayHandler", "Request failed: ${e.message}")
                                    Log.e("TelegramGatewayHandler", "Exception type: ${e.javaClass.simpleName}")
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            // Log error if any required Firebase data is missing
                            Log.e("sendPostOrderToApi", "Incomplete TELEGRAM_ORDER settings in branch $branchId")
                            Log.e("sendPostOrderToApi", "Missing values: ${listOfNotNull(
                                if (chatId == null) "chatId (chat ID)" else null,
                                if (botToken == null) "tokenBot (bot token)" else null,
                                if (siteUrl == null) "siteUrl" else null
                            ).joinToString(", ")}")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("sendPostOrderToApi", "Failed to retrieve siteUrl", error.toException())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("sendPostOrderToApi", "Failed to retrieve TELEGRAM_ORDER settings from branch $branchId", error.toException())
            }
        })
    } else {
        Log.e("TelegramGatewayHandler", "Branch ID not found in SharedPreferences")
    }
}

@Composable
fun MenuItem(
    item: MenuItemData, 
    onCardClick: () -> Unit,
    onAddClick: (androidx.compose.ui.geometry.Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    // Tracks overall remote focus on the card itself
    var isFocused by remember { mutableStateOf(false) }
    
    // Manages sub-focus state machine internally
    var isAddActive by remember { mutableStateOf(false) }

    var addButtonCoords by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    // Reset state machine to top content body whenever this card loses focus
    LaunchedEffect(isFocused) {
        if (!isFocused) {
            isAddActive = false
        }
    }

    // Snappy Google TV focus zoom scale transition
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "MenuItemScale"
    )

    // Smooth fade in/out transition for focus visibility (LED Glow)
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

    // 🚀 PERFORMANCE: Apply elegant white border that pulses and fades in smoothly on focus
    // Border pulse ONLY when card focused AND add button is NOT active
    val borderModifier = if (isFocused && !isAddActive) {
        Modifier.border(
            width = 3.dp,
            color = Color.White.copy(alpha = pulseAlpha.value * focusFadeAlpha),
            shape = RoundedCornerShape(30.dp)
        )
    } else {
        Modifier // Clean when not focused OR when add button is active
    }

    // The SINGLE focus target for the whole card, containing the state machine
    Box(
        modifier = modifier
            .width(196.dp)
            .height(280.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
            }
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                // Listen only to press-down events
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.DirectionDown -> {
                            if (!isAddActive) {
                                isAddActive = true
                                true // Consume event to stay inside the card!
                            } else {
                                val cartRequester = GlobalCartState.cartFocusRequester
                                if (cartRequester != null) {
                                    try {
                                        cartRequester.requestFocus()
                                        true // Consume event
                                    } catch (e: Exception) {
                                        false // Pass-through fallback
                                    }
                                } else {
                                    false // Pass-through fallback
                                }
                            }
                        }
                        Key.DirectionUp -> {
                            if (isAddActive) {
                                isAddActive = false
                                true // Consume event to return to card body!
                            } else {
                                false // Pass-through to Categories row above
                            }
                        }
                        else -> false // Pass Left/Right through freely for card scrolling!
                    }
                } else {
                    false
                }
            }
            .clickable(
                onClick = {
                    if (isAddActive) {
                        val offset = addButtonCoords?.let {
                            androidx.compose.ui.geometry.Offset(
                                x = it.left + it.width / 2,
                                y = it.top + it.height / 2
                            )
                        } ?: androidx.compose.ui.geometry.Offset.Zero
                        onAddClick(offset)
                    } else {
                        onCardClick()
                    }
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        // Inner Card with Floating border & Air Gap!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(borderModifier) // Conditionally injected! Zero overhead when not focused!
                .padding(6.dp) // The Floating Air Gap (6.dp padding - 3.dp border width = 3.dp gap, equal to border size!)
                .clip(RoundedCornerShape(24.dp))
                .background(color = Color(207, 223, 237).copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // 1. CONTENT VISUAL CONTAINER (Stretches to fill space)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.65f)
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        CachedAsyncImage(
                            imageUrl = item.imageRes,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = R.drawable.err,
                            cachePrefix = "food"
                        )
                    }
                    
                    // Title stays outside, below the image
                    Text(
                        text = item.name, 
                        style = MaterialTheme.typography.titleSmall, 
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 2. FIXED BOTTOM ROW
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 4.dp, 8.dp, 8.dp) // Perfectly baseline-locked
                ) {
                    Text(
                        text = formatIDR(item.price), 
                        style = MaterialTheme.typography.bodyMedium, 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    // Compute dynamic button colors based on the state machine
                    val btnBg = if (isAddActive && isFocused) {
                        Color(0xFFE91E63) // Pink padat ketika difokuskan / ditekan DirectionDown
                    } else {
                        Color.White.copy(alpha = 0.15f) // Putih transparan tipis saat pasif
                    }
                    val btnText = Color.White
                    
                    // Passive visual Add Button (Appearance driven by parent focus state)
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                addButtonCoords = coords.boundsInRoot()
                            }
                            .clip(RoundedCornerShape(50))
                            .background(
                                color = btnBg,
                                shape = RoundedCornerShape(50)
                            )
                            .then(
                                if (isAddActive && isFocused) {
                                    Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                                } else Modifier
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cart),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = btnText
                            )
                            Text(
                                text = "Add",
                                fontSize = 11.sp,
                                color = btnText,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemDialog(
    item: MenuItemData,
    onAddToCart: (MenuItemData, Int, String, Variant?) -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableIntStateOf(1) }
    var specialInstruction by remember { mutableStateOf("") }
    var selectedVariant by remember { mutableStateOf<Variant?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var isMicReady by remember { mutableStateOf(false) }
    var currentRms by remember { mutableFloatStateOf(-2.0f) }  // Add RMS state
    val context = LocalContext.current

    // Function to start voice recognition
    val startVoiceRecognition: () -> Unit = {
        Log.d("DHTV_FOOD", "Executing clean Speech Session, currentlyListening=$isListening")
        try {
            if (speechRecognizer != null) {
                isMicReady = false // Menampilkan spinner loader saat inisialisasi mesin Google
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra("calling_package", context.packageName) // Headless route
                }
                isListening = true
                speechRecognizer?.startListening(intent)
            } else {
                Log.e("DHTV_FOOD", "SpeechRecognizer is null inside function scope!")
            }
        } catch (e: Exception) {
            Log.e("DHTV_FOOD", "Error starting voice session: ${e.message}")
            isListening = false
            isMicReady = false
        }
    }





    // Initialize SpeechRecognizer
    LaunchedEffect(Unit) {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.d("DHTV_FOOD", "Speech recognition is available")
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("DHTV_FOOD", "Ready for speech")
                        isListening = true
                        isMicReady = true
                        currentRms = -2.0f  // Reset RMS when starting
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spokenText = matches?.get(0) ?: ""
                        Log.d("DHTV_FOOD", "Speech recognition successful result: $spokenText")
                        
                        if (spokenText.isNotEmpty()) {
                            specialInstruction = spokenText // Clean single shot overwrite
                        }
                        isListening = false
                        isMicReady = false
                        currentRms = -2.0f
                    }

                    override fun onError(error: Int) {
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
                            SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
                            SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
                            SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
                            SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
                            else -> "UNKNOWN_ERROR ($error)"
                        }
                        Log.e("DHTV_FOOD", "Speech session error code: $errorMessage")
                        
                        isListening = false
                        isMicReady = false
                        currentRms = -2.0f
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("DHTV_FOOD", "Beginning of speech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        Log.d("DHTV_FOOD", "RMS changed: $rmsdB")
                        currentRms = rmsdB
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d("DHTV_FOOD", "EndOfSpeech - system finalizing capture")
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        Log.d("DHTV_FOOD", "Partial results: ${matches?.get(0)}")
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        Log.d("DHTV_FOOD", "Event: $eventType")
                    }
                })
                Log.d("DHTV_FOOD", "SpeechRecognizer initialized successfully")
            } else {
                Log.e("DHTV_FOOD", "Speech recognition is not available")
            }
        } catch (e: Exception) {
            Log.e("DHTV_FOOD", "Error initializing SpeechRecognizer: ${e.message}")
        }
    }

    // Clean up SpeechRecognizer
    DisposableEffect(Unit) {
        onDispose {
            Log.d("DHTV_FOOD", "Disposing SpeechRecognizer")
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e("DHTV_FOOD", "Error disposing SpeechRecognizer: ${e.message}")
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        // Custom Dialog content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .align(Alignment.TopEnd)
            ) {
                Text(
                    text = "\uF057",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFE91E63),
                    fontFamily = FontFamily(Font(R.font.icons)),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                // Item Name, Description, and Price
                Text(
                    item.name,
                    style = TextStyle(
                        fontSize = 24.sp,
                        color = Color(0xff071434),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    item.description,
                    style = TextStyle(fontSize = 16.sp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    formatIDR(item.price),
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Quantity Section
                Row(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quantity",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Color.LightGray.copy(alpha = .5f), shape = RoundedCornerShape(50))
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decrease quantity
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        onClick = { if (quantity > 1) quantity-- },
                                        indication = ripple(color = Color.Black),
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                            ) {
                                Text(
                                    text = "\uF056", // Minus icon
                                    color = Color(0xff071434),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = FontFamily(Font(R.font.icons)),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "$quantity",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Increase quantity
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        onClick = { quantity++ },
                                        indication = ripple(color = Color.Black),
                                        interactionSource = remember { MutableInteractionSource() }
                                    )
                            ) {
                                Text(
                                    text = "\uF055", // Plus icon
                                    color = Color(0xff071434),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontFamily = FontFamily(Font(R.font.icons)),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (item.variant?.isNotEmpty() == true) {
                    // Set selectedVariant to the first variant by default
                    if (selectedVariant == null) {
                        selectedVariant = item.variant.first()
                    }
                    Text(
                        text = "Variant",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 128.dp), // Batas maksimal tinggi agar tidak terlalu panjang
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(item.variant) { variant ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedVariant = variant // Update selected variant
                                    }
                                    .border(
                                        width = 2.dp,
                                        color = if (selectedVariant == variant) Color(0xFFFF2B85) else Color.LightGray.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .background(
                                        if (selectedVariant == variant) Color(0xFFFFEFEF) else Color.White,
                                        shape = RoundedCornerShape(16.dp)
                                    ) // Rounded corners for the background
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp)
                                ) {
                                    // RadioButton for variant selection
                                    RadioButton(
                                        selected = selectedVariant == variant,
                                        onClick = { selectedVariant = variant },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFFFF2B85),  // Color when the radio button is selected
                                            unselectedColor = Color.LightGray.copy(alpha = 0.5f) // Color when the radio button is unselected
                                        )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    ) {
                                        Text(
                                            text = variant.name,
                                            style = TextStyle(fontSize = 12.sp),
                                            maxLines = 2, overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (variant.price == 0) {
                                                "No add price"
                                            } else {
                                                "+${formatIDR(variant.price)}"
                                            },
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Special Instructions Section
                Text(
                    text = "Special Instructions",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray.copy(alpha = .2f), shape = RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = specialInstruction,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Keep track of focus state machine
                        var isMicFocused by remember { mutableStateOf(false) }

                        // Dynamic instructions based on listening state
                        val instructionText = if (isListening) {
                            "Silakan berbicara..."
                        } else if (specialInstruction.isEmpty()) {
                            "Klik [OK] untuk bicara"
                        } else {
                            "" // Sembunyikan instruksi ketika note sudah terisi!
                        }
                        val instructionColor = if (isListening) Color(0xFFE91E63) else Color.Gray
                        val instructionWeight = if (isListening) FontWeight.Bold else FontWeight.Normal

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            // 1. Clear instruction text placed to the left of the Mic
                            if (instructionText.isNotEmpty()) {
                                Text(
                                    text = instructionText,
                                    style = TextStyle(
                                        color = instructionColor,
                                        fontSize = 11.sp,
                                        fontWeight = instructionWeight
                                    ),
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }

                            // 1.5 Dynamic Voice Waveform Virtualizer (Placed precisely to the left of the Mic!)
                            if (isListening && isMicReady) {
                                val normalizedRms = ((currentRms + 2f) / 12f).coerceIn(0.1f, 1.0f)
                                val height1 by animateDpAsState(targetValue = 4.dp + (10.dp * normalizedRms), label = "v1")
                                val height2 by animateDpAsState(targetValue = 6.dp + (20.dp * normalizedRms), label = "v2")
                                val height3 by animateDpAsState(targetValue = 4.dp + (14.dp * normalizedRms), label = "v3")
                                val height4 by animateDpAsState(targetValue = 2.dp + (8.dp * normalizedRms), label = "v4")

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .height(24.dp)
                                ) {
                                    listOf(height1, height2, height3, height4).forEach { h ->
                                        Box(
                                            modifier = Modifier
                                                .width(2.5.dp)
                                                .height(h)
                                                .background(Color(0xFFE91E63), shape = RoundedCornerShape(50))
                                        )
                                    }
                                }
                            }

                            // 2. UNIFIED TV MIC BUTTON (Captures hold and release events!)
                            val interactionSource = remember { MutableInteractionSource() }
                            val micBgColor = if (isListening) Color(0xFFE91E63) else if (isMicFocused) Color.LightGray else Color.White.copy(alpha = 0.5f)
                            val micIconTint = if (isListening) Color.White else if (isMicFocused) Color(0xFFE91E63) else Color.Gray

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .scale(if (isListening) 1.1f else 1.0f)
                                    .clip(CircleShape)
                                    .background(micBgColor)
                                    .onFocusChanged { isMicFocused = it.isFocused }
                                    .onPreviewKeyEvent { keyEvent ->
                                        // Click-to-Talk toggle triggered by Remote D-Pad Center/Enter button
                                        if (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) {
                                            if (keyEvent.type == KeyEventType.KeyUp) {
                                                Log.d("DHTV_FOOD", "Click-to-Talk triggered: isListening=$isListening")
                                                if (isListening) {
                                                    Log.d("DHTV_FOOD", "USER MANUALLY TERMINATED RECORDING SESSION!")
                                                    try {
                                                        speechRecognizer?.stopListening()
                                                    } catch (e: Exception) {}
                                                    isListening = false
                                                    isMicReady = false
                                                } else {
                                                    Log.d("DHTV_FOOD", "USER MANUALLY STARTED RECORDING SESSION!")
                                                    specialInstruction = "" // Clean start
                                                    startVoiceRecognition()
                                                }
                                                true // Consume release event
                                            } else if (keyEvent.type == KeyEventType.KeyDown) {
                                                true // Consume press event to lock standard actions
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = ripple(bounded = true),
                                        onClick = { /* Action handled entirely via KeyDown / KeyUp! */ }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Center Content State Machine: Spinner during launching, Icon otherwise
                                if (isListening && !isMicReady) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.mic),
                                        contentDescription = "Voice Input",
                                        modifier = Modifier.size(18.dp),
                                        tint = micIconTint
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Add to Cart Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .clickable(
                            onClick = {
                                // Pass the selected variant (can be null) to the cart handler
                                onAddToCart(item, quantity, specialInstruction, selectedVariant)
                                onDismiss()
                            },
                            indication = ripple(color = Color.Black),
                            interactionSource = remember { MutableInteractionSource() }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFE91E63))
                            .align(Alignment.Center)
                    ) {
                        Text(
                            text = "Add to Cart - ${formatIDR(item.price * quantity + (selectedVariant?.price ?: 0) * quantity)}",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            modifier = Modifier
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}

// Shimmer component for menu items
@Composable
fun MenuItemShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "menuItemShimmer")
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
    
    Box(
        modifier = Modifier
            .width(196.dp) // Compact width to match MenuItem
            .height(280.dp) // Compact height to match MenuItem
            .padding(6.dp)
            .background(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Image shimmer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .background(
                        brush = Brush.linearGradient(
                            colors = shimmerColors,
                            start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                            end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
            )
            
            // Text shimmer placeholders
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                // Title shimmer
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .padding(bottom = 8.dp)
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
                
                // Description shimmer (2 lines)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(12.dp)
                        .padding(bottom = 4.dp)
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
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .padding(bottom = 16.dp)
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
                
                // Price and button shimmer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 0.dp, 8.dp, 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Price shimmer
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
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
                    
                    // Button shimmer
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = shimmerColors,
                                    start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                                    end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                                ),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}
