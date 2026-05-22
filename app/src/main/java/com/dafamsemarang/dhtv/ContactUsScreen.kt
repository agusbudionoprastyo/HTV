@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@file:Suppress("NAME_SHADOWING")

package com.dafamsemarang.dhtv

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import com.dafamsemarang.dhtv.DataRepository
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlinx.coroutines.delay
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
//import kotlinx.serialization.Serializable
import android.speech.RecognizerIntent
import android.content.Intent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.os.Bundle
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.ui.input.key.*

@SuppressLint("UseOfNonLambdaOffsetOverload", "UnusedMaterial3ScaffoldPaddingParameter")

@Composable
fun ContactUsScreen(navController: androidx.navigation.NavHostController? = null) {
   var isVisible by remember { mutableStateOf(false) }
   val requestItems by DataRepository.requestItems
   val categories by remember(requestItems) { derivedStateOf { requestItems.map { it.category }.distinct() } }
   var selectedCategory by remember { mutableStateOf<String?>(null) }
   var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val sharedPreferences = remember(context) { 
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) 
    }
    val deviceID = remember { sharedPreferences.getString("deviceID", null) }
    val branchId = remember { sharedPreferences.getString("branchId", null) }
    val roomId = remember { sharedPreferences.getString("room", null) }
    var guestInfo by remember { mutableStateOf<GuestInfo?>(null) }
    var folioId by remember { mutableStateOf<Int?>(null) }

    val database: DatabaseReference = remember { Firebase.database.reference }

    // 🚀 CRITICAL FIX: Fetch guestInfo and folioId in real-time so users can submit requests!
    DisposableEffect(roomId, branchId) {
        var guestRef: DatabaseReference? = null
        var guestListener: ValueEventListener? = null

        if (roomId != null && branchId != null) {
            val path = "BRANCHES/$branchId/FOGUEST/$roomId"
            Log.d("ContactUsScreen", "Setting up active guest info listener for: $path")
            guestRef = database.child(path)
            
            val listener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val info = dataSnapshot.getValue(GuestInfo::class.java)
                        guestInfo = info
                        folioId = info?.folio
                        Log.d("ContactUsScreen", "Active guest info retrieved successfully: folioId = $folioId")
                    } else {
                        guestInfo = null
                        folioId = null
                        Log.w("ContactUsScreen", "Guest info data path does not exist!")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("ContactUsScreen", "Firebase error: ${databaseError.message}")
                    guestInfo = null
                    folioId = null
                }
            }
            guestListener = listener
            guestRef.addValueEventListener(listener)
        } else {
            Log.w("ContactUsScreen", "Unable to load guest info: roomId=$roomId, branchId=$branchId")
        }

        onDispose {
            if (guestRef != null && guestListener != null) {
                guestRef.removeEventListener(guestListener)
                Log.d("ContactUsScreen", "Guest info listener successfully released")
            }
        }
    }

   // Use derivedStateOf for filtering - more performant, only recomputes when inputs change
   val displayRequests = remember(requestItems, selectedCategory) {
       if (selectedCategory.isNullOrEmpty()) {
           requestItems
       } else {
           requestItems.filter { it.category == selectedCategory }
       }
   }
   
   // Loading state for shimmer
   var isLoadingRequests by remember { mutableStateOf(true) }
   var isFiltering by remember { mutableStateOf(false) }
   // Delay shimmer visibility until after screen transition completes (500ms)
   var shimmerVisible by remember { mutableStateOf(false) }
   LaunchedEffect(isLoadingRequests) {
       if (isLoadingRequests) {
           kotlinx.coroutines.delay(550)
           if (isLoadingRequests) shimmerVisible = true
       } else {
           shimmerVisible = false
       }
   }
   
    val isRequestLoaded by com.dafamsemarang.dhtv.DataRepository.isRequestLoaded
    // Update loading state when request items are loaded or finished loading
    LaunchedEffect(isRequestLoaded) {
        if (isRequestLoaded) {
            isLoadingRequests = false
        }
    }
   
    val currentCategoryIndex = remember(selectedCategory, categories) {
        val cats = listOf(null) + categories
        cats.indexOf(selectedCategory).coerceAtLeast(0)
    }


   // Snap states — declared at screen level to survive recomposition
   val categoryListState = rememberLazyListState()
   val categorySnapBehavior = rememberSnapFlingBehavior(lazyListState = categoryListState)
   val itemListState = remember(selectedCategory) { androidx.compose.foundation.lazy.LazyListState() }
   val itemSnapBehavior = rememberSnapFlingBehavior(lazyListState = itemListState)
   val focusScope = rememberCoroutineScope()
   
   var focusedCategoryIndex by remember { mutableIntStateOf(0) }
    var isNavigatingHorizontally by remember { mutableStateOf(false) }
   var categoryScrollTrigger by remember { mutableIntStateOf(0) }

   var focusedItemIndex by remember { mutableIntStateOf(0) }
   var itemScrollTrigger by remember { mutableIntStateOf(0) }

    var categoryChanged by remember { mutableStateOf(false) }
    val categoryRequesters = remember(categories) { List(categories.size + 1) { FocusRequester() } }
   val itemRequesters = remember(displayRequests) { List(displayRequests.size + 10) { FocusRequester() } }

   // Reset scroll to start when category changes
   LaunchedEffect(selectedCategory) {
       itemListState.scrollToItem(0)
       focusedItemIndex = 0
   }
   
   // FORCE PIVOT: High-performance BringIntoViewSpec scroll logic from F&B
    val density = androidx.compose.ui.platform.LocalDensity.current
    val startPaddingPx = with(density) { 58.dp.toPx() }
    val defaultSpec = LocalBringIntoViewSpec.current
    
    LaunchedEffect(focusedCategoryIndex) {
         delay(60) // Tiny delay to let focus jump visually first
         categoryListState.animateScrollToItem(
             index = focusedCategoryIndex,
             scrollOffset = 0
         )
     }

     val categoryBringIntoViewSpec = remember(defaultSpec) {
         object : BringIntoViewSpec {
             override val scrollAnimationSpec: androidx.compose.animation.core.AnimationSpec<Float>
                 get() = androidx.compose.animation.core.tween(
                     durationMillis = 250,
                     easing = androidx.compose.animation.core.FastOutSlowInEasing
                 )

             override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                 return 0f // Let LaunchedEffect with delay handle the smooth flowing scroll!
             }
         }
     }

    val itemBringIntoViewSpec = remember(defaultSpec, startPaddingPx) {
        object : BringIntoViewSpec {
            override val scrollAnimationSpec: androidx.compose.animation.core.AnimationSpec<Float>
                get() = androidx.compose.animation.core.tween(
                    durationMillis = 150, // 150ms for items
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )

            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                return offset - startPaddingPx
            }
        }
    }
// Duplicate density deleted
   

   
// Deactivated item scroll LaunchedEffect deleted
   
// Deactivated category scroll LaunchedEffect deleted

   // Main Content & Footer Root Wrapper - Unified with HomeScreen
   Box(
       modifier = Modifier
           .fillMaxSize()
   ) {
       Box(
           modifier = Modifier.fillMaxSize()
       ) {
           // Header section

           // Show error message
           if (errorMessage != null) {
               Box(
                   modifier = Modifier.fillMaxSize(),
                   contentAlignment = Alignment.Center
               ) {
                   Text(errorMessage!!, color = Color.Red)
               }
           } else {
               var sidebarVisible by remember { mutableStateOf(false) }

               val sidebarOffset by animateDpAsState(
                   targetValue = if (sidebarVisible) 0.dp else 268.dp,
                   animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                   label = "SidebarSlideAnimation"
               )

               val paddingOffset by animateDpAsState(
                   targetValue = if (sidebarVisible) 258.dp else 0.dp,
                   animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                   label = "PaddingSlideAnimation"
               )
               CompositionLocalProvider(LocalBringIntoViewSpec provides categoryBringIntoViewSpec) {
                LazyRow(
                     state = categoryListState,
                     flingBehavior = categorySnapBehavior,
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
                         .padding(end = paddingOffset)
                         .padding(top = 110.dp, bottom = 12.dp),
                     horizontalArrangement = Arrangement.spacedBy(16.dp),
                     verticalAlignment = Alignment.CenterVertically,
                     contentPadding = PaddingValues(start = 58.dp, end = 58.dp)
                 ) {
                     item {
                         var isFocused by remember { mutableStateOf(false) }

                          val borderAlpha = remember { androidx.compose.animation.core.Animatable(0.5f) }
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
                          val isSelected = selectedCategory == null
                         val scale by animateFloatAsState(
                             targetValue = if (isFocused) 1.1f else 1.0f,
                             animationSpec = tween(350, easing = FastOutSlowInEasing),
                             label = "AllScale"
                         )
                         val bgColor by animateColorAsState(
                             targetValue = if (isSelected) Color(207, 223, 237).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f),
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
                                          if (selectedCategory != null) {
                                              categoryChanged = true
                                              focusScope.launch { itemListState.scrollToItem(0) }
                                          }
                                          selectedCategory = null
                                          focusedCategoryIndex = 0
                                          categoryScrollTrigger++
                                      }
                                  }
                                  .clickable(
                                      onClick = { selectedCategory = null },
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
                          if (false) Button(
                             onClick = { selectedCategory = null },
                             modifier = Modifier
                                 .padding(horizontal = 8.dp)
                                 .scale(scale)
                                 .focusRequester(categoryRequesters[0])
                                 .onFocusChanged { focusState ->
                                     if (focusState.isFocused) {
                                         if (selectedCategory != null) {
                                             categoryChanged = true
                                             focusScope.launch { itemListState.scrollToItem(0) }
                                         }
                                         selectedCategory = null
                                         focusedCategoryIndex = 0
                                         categoryScrollTrigger++
                                     }
                                 },
                             colors = ButtonDefaults.colors(
                                 containerColor = bgColor
                             )
                         ) {
                             Text("All", style = TextStyle(color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                         }
                     }

                     // Loop through the categories to display each category button
                     items(categories) { category ->
                         var isFocused by remember { mutableStateOf(false) }

                          val borderAlpha = remember { androidx.compose.animation.core.Animatable(0.5f) }
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
                          val isSelected = selectedCategory == category
                         val scale by animateFloatAsState(
                         targetValue = if (isFocused) 1.1f else 1.0f,
                         animationSpec = tween(350, easing = FastOutSlowInEasing),
                         label = "CategoryScale"
                         )
                         val bgColor by animateColorAsState(
                              targetValue = if (isSelected) Color(207, 223, 237).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f),
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
                                  .focusRequester(if (categories.indexOf(category) + 1 < categoryRequesters.size) categoryRequesters[categories.indexOf(category) + 1] else FocusRequester.Default)
                                  .onFocusChanged { focusState ->
                                      isFocused = focusState.isFocused
                                      if (focusState.isFocused) {
                                          val targetIndex = categories.indexOf(category) + 1
                                          if (selectedCategory != category) {
                                              categoryChanged = true
                                              focusScope.launch { itemListState.scrollToItem(0) }
                                          }
                                          selectedCategory = category
                                          focusedCategoryIndex = targetIndex
                                          categoryScrollTrigger++
                                      }
                                  }
                                  .clickable(
                                      onClick = { selectedCategory = category },
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
                          if (false) Button(
                             onClick = { selectedCategory = category },
                             modifier = Modifier
                                 .padding(horizontal = 8.dp)
                                 .scale(scale)
                                 .focusRequester(if (categories.indexOf(category) + 1 < categoryRequesters.size) categoryRequesters[categories.indexOf(category) + 1] else FocusRequester.Default)
                                 .onFocusChanged { focusState ->
                                      if (focusState.isFocused) {
                                          if (selectedCategory != category) {
                                              categoryChanged = true
                                              focusScope.launch { itemListState.scrollToItem(0) }
                                          }
                                          selectedCategory = category
                                          focusedCategoryIndex = categories.indexOf(category) + 1
                                          categoryScrollTrigger++
                                      }
                                  },
                             colors = ButtonDefaults.colors(
                                 containerColor = bgColor
                             )
                         ) {
                             Text(text = category, style = TextStyle(color = textColor, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                         }
                     }
                }
                 }

                 // Content dengan shimmer loading dan animasi perpindahan halus
                 AnimatedContent(
                      targetState = Triple(currentCategoryIndex, isFiltering, displayRequests),
                       transitionSpec = {
                           (fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith
                           fadeOut(animationSpec = tween(400, easing = FastOutSlowInEasing)))
                               .using(androidx.compose.animation.SizeTransform { _, _ -> tween(0) })
                       },
                      label = "RequestContentTransition",
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(top = 162.dp)
                 ) { (_, isFilteringItems, requests) ->
                     val isShowingShimmer = (isLoadingRequests && shimmerVisible) || isFilteringItems
                     
                     if (isShowingShimmer || requests.isNotEmpty()) {
                         CompositionLocalProvider(LocalBringIntoViewSpec provides itemBringIntoViewSpec) {
                             LazyRow(
                                 state = itemListState,
                                 flingBehavior = itemSnapBehavior,
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
                                     .wrapContentHeight(Alignment.CenterVertically)
                                     .focusProperties { 
                                         enter = { itemRequesters.getOrNull(focusedItemIndex) ?: FocusRequester.Default }
                                     },
                                 horizontalArrangement = Arrangement.spacedBy(if (isShowingShimmer) 8.dp else 6.dp),
                                 contentPadding = PaddingValues(start = 58.dp, end = 58.dp)
                             ) {
                                 if (isShowingShimmer) {
                                     items(5) { // Show 5 shimmer items
                                         RequestItemShimmer()
                                     }
                                 } else {
                                     itemsIndexed(
                                         items = requests,
                                         key = { _, request -> 
                                             "${request.request_title}_${request.category}_${request.imageUrl}"
                                         }
                                     ) { index, request: GuestRequest ->
                                         var showDialog by remember { mutableStateOf(false) }
                                         
                                         RequestItem(
                                             request = request,
                                             guestInfo = guestInfo,
                                             folioId = folioId,
                                             guestRoom = guestInfo?.room,
                                             guestName = guestInfo?.fname,
                                             guestPhone = guestInfo?.phone,
                                             modifier = Modifier
                                                 .focusRequester(if (index < itemRequesters.size) itemRequesters[index] else FocusRequester.Default)
                                                 .onFocusChanged {
                                                 if (it.isFocused) {
                                                     focusedItemIndex = index
                                                     itemScrollTrigger++
                                                 }
                                             },
                                             onItemClick = { showDialog = true }
                                         )
                                         
                                         // Dialog moved outside RequestItem for better performance
                                         if (showDialog) {
                                             RequestDialog(
                                                 request = request,
                                                 guestInfo = guestInfo,
                                                 folioId = folioId,
                                                 guestRoom = guestInfo?.room,
                                                 guestName = guestInfo?.fname,
                                                 guestPhone = guestInfo?.phone,
                                                 onDismiss = { showDialog = false }
                                             )
                                         }
                                     }
                                 }
                             }
                         }
                     } else {
                         // No data available
                         Box(
                             modifier = Modifier.fillMaxWidth(),
                             contentAlignment = Alignment.Center
                         ) {
                             Text("No requests available", style = MaterialTheme.typography.bodyMedium)
                         }
                     }
                 }

           }
       } // End of inner Content Box
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {

        }
   } // End of main Root Box
}

@Composable
fun ImageShimmer(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "imageShimmer")
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
        modifier = modifier
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

@Composable
fun RequestItemShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
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
            .size(196.dp)
            .padding(6.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        // Shimmer background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(shimmerTranslateAnim - 400f, shimmerTranslateAnim - 400f),
                        end = Offset(shimmerTranslateAnim, shimmerTranslateAnim)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        )
        
        // Content placeholder
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Shimmer untuk title
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.8f))
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Shimmer untuk description line 1
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.25f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Shimmer untuk description line 2
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Gray.copy(alpha = 0.25f))
            )
        }
    }
}

@Composable
fun PulsingBadge(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE91E63)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnimation"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaAnimation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Pulsing background
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
                .alpha(alpha)
                .background(
                    color = color,
                    shape = CircleShape
                )
        )
        
        // Main badge
        Badge(
            containerColor = color
        )
    }
}

@Composable
fun RequestItem(
    request: GuestRequest,
    guestInfo: GuestInfo?,
    folioId: Int?,
    guestRoom: String?,
    guestName: String?,
    guestPhone: String?,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track TV focus state
    var isFocused by remember { mutableStateOf(false) }

    val borderAlpha = remember { androidx.compose.animation.core.Animatable(0.5f) }
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
    
    // Elegant scale transition (1.05f expansion on focus for smooth Google TV feel)
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 450, easing = LinearOutSlowInEasing),
        label = "RequestItemScale"
    )

    // Smooth fade in/out transition for focus visibility (LED Glow)
    val focusFadeAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 300),
        label = "FocusFadeAlpha"
    )

    // Pure pulse border animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // 🚀 PERFORMANCE: Apply pure pulsing border only when focused
    val borderModifier = if (isFocused) {
        Modifier.border(
            width = 3.dp,
            color = Color.White.copy(alpha = pulseAlpha * focusFadeAlpha),
            shape = RoundedCornerShape(38.dp)
        )
    } else {
        Modifier // Perfectly clean & zero-overhead for non-focused items!
    }

    // Handle interaction when RequestItem is clicked
    Box(
        modifier = modifier
            .size(196.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(
                onClick = onItemClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 3.dp,
                            color = Color.White.copy(alpha = borderAlpha.value),
                            shape = RoundedCornerShape(38.dp)
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(6.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(207, 223, 237).copy(alpha = 0.25f)) // Footer background color!
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Compact Circular Image - Adjusted to 8.dp padding for a closer top-right corner placement
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    if (request.imageUrl.isNotEmpty()) {
                        CachedAsyncImage(
                            imageUrl = request.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            placeholder = R.drawable.err,
                            error = R.drawable.err,
                            cachePrefix = "guest_request"
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.err),
                            contentDescription = "Image not available",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Title and Description starting at a fixed top offset so all titles align perfectly
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 72.dp, start = 14.dp, end = 14.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = request.request_title,
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        textAlign = TextAlign.Start,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = request.description,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp
                        ),
                        textAlign = TextAlign.Start,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    if (false) Box(
        modifier = modifier
            .size(200.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .scale(scale)
            // Removed outer border to prevent extra spacing gap!
            .clip(RoundedCornerShape(32.dp))
            .clickable(
                onClick = onItemClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (request.imageUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(192.dp)
                        .then(borderModifier) // Conditionally injected! Zero-overhead when unfocused!
                        .padding(4.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    CachedAsyncImage(
                        imageUrl = request.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = R.drawable.err,
                        error = R.drawable.err,
                        cachePrefix = "guest_request"
                    )
                    // Black gradient overlay to ensure absolute text readability
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.65f)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomStart),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = request.request_title,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = request.description,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                // Fallback for missing image URL
                Box(
                    modifier = Modifier
                        .size(192.dp)
                        .then(borderModifier) // Conditionally injected! Zero-overhead when unfocused!
                        .padding(4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.err), // Default error image
                        contentDescription = "Image not available",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Black gradient overlay to ensure absolute text readability
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.65f)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomStart),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = request.request_title,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = request.description,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// Separate dialog component - only created when needed
@Composable
fun RequestDialog(
    request: GuestRequest,
    guestInfo: GuestInfo?,
    folioId: Int?,
    guestRoom: String?,
    guestName: String?,
    guestPhone: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val timeStamp = System.currentTimeMillis()
    
    // Get current date and time - use remember to avoid recalculation
    val currentDateTime = remember { LocalDateTime.now() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val formattedDate = remember { currentDateTime.format(dateFormatter) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val formattedTime = remember { currentDateTime.format(timeFormatter) }
    
    var selectedDate by remember { mutableStateOf<String?>(formattedDate) }
    var selectedTime by remember { mutableStateOf<String?>(formattedTime) }
    var note by remember { mutableStateOf("") }
    
    val calendar = remember { Calendar.getInstance() }
    val year = remember { calendar.get(Calendar.YEAR) }
    val month = remember { calendar.get(Calendar.MONTH) }
    val day = remember { calendar.get(Calendar.DAY_OF_MONTH) }
    val hour = remember { calendar.get(Calendar.HOUR_OF_DAY) }
    val minute = remember { calendar.get(Calendar.MINUTE) }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                selectedDate = "$selectedDayOfMonth/${selectedMonth + 1}/$selectedYear"
            },
            year, month, day
        )
    }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                selectedTime = "$selectedHour:${selectedMinute.toString().padStart(2, '0')}"
            },
            hour, minute, true
        )
    }
    
    // SpeechRecognizer state - only created when dialog is visible
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var isMicReady by remember { mutableStateOf(false) }
    var currentRms by remember { mutableFloatStateOf(-2.0f) }
    
    val startVoiceRecognition: () -> Unit = {
        try {
            if (speechRecognizer != null) {
                if (isListening) {
                    speechRecognizer?.stopListening()
                    isListening = false
                    isMicReady = false
                } else {
                    isMicReady = false // Tampilkan spinner loader saat mulai
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
                }
            }
        } catch (e: Exception) {
            Log.e("DHTV_CONTACT", "Error in startVoiceRecognition: ${e.message}")
            isListening = false
            isMicReady = false
        }
    }
    
    LaunchedEffect(Unit) {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        isMicReady = true
                        currentRms = -2.0f
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.get(0)?.let { spokenText ->
                            note = spokenText
                        }
                        isListening = false
                        isMicReady = false
                        currentRms = -2.0f
                    }
                    override fun onError(error: Int) {
                        isListening = false
                        isMicReady = false
                        currentRms = -2.0f
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        currentRms = rmsdB
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                        isMicReady = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            Log.e("DHTV_CONTACT", "Error initializing SpeechRecognizer: ${e.message}")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e("DHTV_CONTACT", "Error disposing SpeechRecognizer: ${e.message}")
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(request.request_title) },
        text = {
            Column {
                Text(
                    text = request.description,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date picker
                Text(text = "Request Date")
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(24.dp),
                            painter = painterResource(id = R.drawable.date_outline_badged_svgrepo_com),
                            contentDescription = "Choose Date"
                        )
                    }
                    selectedDate?.let { Text(it, modifier = Modifier.padding(start = 8.dp)) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time picker
                Text(text = "Request Time")
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { timePickerDialog.show() }) {
                        Icon(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(24.dp),
                            painter = painterResource(id = R.drawable.clock_circle_svgrepo_com),
                            contentDescription = "Choose Time"
                        )
                    }
                    selectedTime?.let { Text(it, modifier = Modifier.padding(start = 8.dp)) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Note")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.LightGray.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Main clean text display
                        Text(
                            text = if (isListening) "Silakan berbicara..." else note,
                            color = if (isListening) Color(0xFFE91E63) else Color.Black,
                            fontWeight = if (isListening) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

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

                        // 2. Unified Click-to-Talk Mic Button State Machine
                        val interactionSource = remember { MutableInteractionSource() }
                        val micBgColor = if (isListening) Color(0xFFE91E63) else Color.Gray.copy(alpha = 0.2f)
                        val micIconTint = if (isListening) Color.White else Color.Gray

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(micBgColor)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = ripple(bounded = true),
                                    onClick = { startVoiceRecognition() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isListening && !isMicReady) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    color = Color.White,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.mic),
                                    contentDescription = "Voice Input",
                                    modifier = Modifier.size(12.dp),
                                    tint = micIconTint
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val requestId = generateRequestId()
                    folioId?.let {
                        sendRequestNotification(context, it, requestId, request, timeStamp)
                        sendRequestToDatabase(
                            context,
                            it,
                            guestName ?: "",
                            guestPhone ?: "",
                            guestRoom ?: "",
                            request,
                            "submitted",
                            requestId,
                            timeStamp,
                            selectedDate ?: "",
                            selectedTime ?: "",
                            note
                        )
                    }
                    onDismiss()
                }
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MyRequestItem(request: Request) {
   val timestamp = request.timestamp ?: System.currentTimeMillis()
   var formattedTimestamp by remember { mutableStateOf(getTimeAgo(timestamp)) }
   var isDialogOpen by remember { mutableStateOf(false) }

   // Update formatted timestamp every second
   LaunchedEffect(timestamp) {
       while (true) {
           delay(1000)
           formattedTimestamp = getTimeAgo(timestamp)
       }
   }

   Box(
       modifier = Modifier
           .clip(RoundedCornerShape(16.dp))
           .clickable(
               onClick = { isDialogOpen = true },
               indication = ripple(color = Color.Black),
               interactionSource = remember { MutableInteractionSource() }
           )
   ) {
       Box(
           modifier = Modifier
               .padding(4.dp)
               .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
           contentAlignment = Alignment.Center
       ) {
           Row(
               modifier = Modifier
                   .fillMaxWidth()
                   .padding(8.dp),
               verticalAlignment = Alignment.CenterVertically
           ) {
               Box(
                   modifier = Modifier
                       .padding(4.dp)
                       .background(color = Color(0xFF1A1919), shape = RoundedCornerShape(8.dp))
                       .clip(RoundedCornerShape(8.dp))
                       .size(48.dp),
                   contentAlignment = Alignment.Center
               ) {
                   val guestRequest = request.requests?.firstOrNull()
                   val imageUrl = guestRequest?.imageUrl ?: ""
                   if (imageUrl.isNotEmpty()) {
                       CachedAsyncImage(
                           imageUrl = imageUrl,
                           contentDescription = "Request Icon",
                           modifier = Modifier.fillMaxSize(),
                           contentScale = ContentScale.Crop,
                           placeholder = R.drawable.err,
                           error = R.drawable.err,
                           cachePrefix = "guest_request"
                       )
                   } else {
                       Image(
                           painter = painterResource(id = R.drawable.err),
                           contentDescription = "Request Icon",
                           modifier = Modifier.fillMaxSize(),
                           contentScale = ContentScale.Crop
                       )
                   }
               }

               Spacer(modifier = Modifier.width(8.dp))

               Column(
                   modifier = Modifier
                       .weight(1f)
                       .padding(end = 4.dp)
               ) {
                   request.requests?.forEach { guestRequest ->
                       Text(
                           text = guestRequest.request_title,
                           style = MaterialTheme.typography.labelLarge,
                           fontWeight = FontWeight.Bold,
                           color = Color.Black.copy(alpha = 0.6f)
                       )
                   }

                   Text(
                       text = "Request ${request.status ?: "Status: Not Available"}",
                       style = MaterialTheme.typography.labelMedium,
                       color = Color.Black.copy(alpha = 0.5f)
                   )

                   Text(
                       text = formattedTimestamp,
                       style = MaterialTheme.typography.labelMedium,
                       color = Color.Black.copy(alpha = 0.5f)
                   )
               }
           }
       }
   }
   if (isDialogOpen) {
       Dialog(onDismissRequest = { isDialogOpen = false }) {
           RequestDetailDialog(request = request, onDismiss = { isDialogOpen = false })
       }
   }
}

fun generateRequestId(): String {
   return "${System.currentTimeMillis()}"
}

fun sendRequestNotification(context: Context, folioId: Int, requestId: String, selectedItem: GuestRequest?, timeStamp: Long) {
   val database = Firebase.database.reference
   val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
   val branchId = sharedPreferences.getString("branchId", null)

   if (branchId == null) {
       Log.e("DHTV_CONTACT", "Branch ID is null")
       return
   }

   // Create a message containing the selected request title
   val guestRequest = selectedItem?.request_title ?: "No request selected"
   val orderMessage = "Your request for $guestRequest has been Submitted"

   val notification = Notification(
       id = requestId,  // Use the generated requestId
       title = guestRequest,
       message = orderMessage,
       timestamp = timeStamp,
       type = "GUEST_REQUEST"
   )

   val notificationsRef = database.child("BRANCHES").child(branchId).child("NOTIFICATIONS").child(folioId.toString())
   notificationsRef.push().setValue(notification)
}

fun sendRequestToDatabase(
   context: Context,
   folioId: Int,
   guestName: String,
   guestPhone: String,
   guestRoom: String,
   selectedItem: GuestRequest?,
   orderStatus: String,
   requestId: String,
   timeStamp: Long,
   selectedDate: String,  // Added date
   selectedTime: String,  // Added time
   note: String // Added note
) {
   val database = Firebase.database.reference
   val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
   val branchId = sharedPreferences.getString("branchId", null)

   if (branchId == null) {
       Log.e("DHTV_CONTACT", "Branch ID is null")
       return
   }

   // Initialize 'requests' list, can be empty if no 'selectedItem' is provided
   val requests = selectedItem?.let { listOf(it) } ?: emptyList()

   // Define the request object including the new fields for date, time, and note
   val request = Request(
       folioId = folioId,
       guestName = guestName,
       guestPhone = guestPhone,
       guestRoom = guestRoom,
       status = orderStatus,  // Initial status as "open"
       timestamp = timeStamp, // Time when the order was placed
       requestId = requestId,
       requests = requests,
       selectedDate = selectedDate, // New field for date
       selectedTime = selectedTime, // New field for time
       note = note // New field for note
   )

   // Push request to Firebase under "BRANCHES/{branchId}/REQUEST" node
   val requestRef = database.child("BRANCHES").child(branchId).child("REQUEST").push()
   requestRef.setValue(request).addOnSuccessListener {
       // After successfully saving to Firebase, send the request notification
       sendPostRequestToApi(
           guestName,
           requests,
           requestId,
           selectedDate,
           selectedTime,
           guestRoom,
           note,
           branchId
       )
   }.addOnFailureListener {
       Log.e("DHTV_CONTACT", "Failed to save request to Firebase")
   }
}

private fun sendPostRequestToApi(
    guestName: String,
    requests: List<GuestRequest>,
    requestId: String,
    selectedDate: String,
    selectedTime: String,
    guestRoom: String,
    note: String,
    branchId: String
) {
    val database = Firebase.database.reference
    val telegramGatewayRef = database.child("BRANCHES").child(branchId).child("SETTING").child("TELEGRAM_REQUEST")
    // Get siteUrl from its own node
    val siteUrlRef = database.child("siteUrl")

    telegramGatewayRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Firebase structure: chatId = chat ID (group ID), tokenBot = bot token
            val chatId = snapshot.child("chatId").getValue(Any::class.java) // Can be Long or Number
            val botToken = snapshot.child("tokenBot").getValue(String::class.java)

            // Get siteUrl from its own node
            siteUrlRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(siteUrlSnapshot: DataSnapshot) {
                    val siteUrl = siteUrlSnapshot.getValue(String::class.java)

                    if (chatId != null && botToken != null && siteUrl != null) {
                        // Build Telegram Bot API endpoint URL
                        val endpointUrl = "https://api.telegram.org/bot$botToken/sendMessage"
                        // Escape HTML special characters
                        fun escapeHtml(text: String): String {
                            return text
                                .replace("&", "&amp;")
                                .replace("<", "&lt;")
                                .replace(">", "&gt;")
                        }
                        
                        val requestDetails = requests.joinToString(separator = "\n\n") { request ->
                            val escapedTitle = escapeHtml(request.request_title)
                            val escapedDesc = escapeHtml(request.description)
                            val escapedCategory = escapeHtml(request.category)
                            "<b>$escapedTitle</b>\n<i>$escapedDesc</i>\n\n<b>Kategori:</b> $escapedCategory"
                        }
                        
                        val escapedGuestName = escapeHtml(formatName(guestName))
                        val escapedNote = if (note.isNotEmpty()) escapeHtml(note) else ""
                        
                        val message = """
<b>Request Baru</b>

Halo! Ada request baru yang perlu ditangani.

<a href="$siteUrl/requests/$requestId">Klik di sini untuk konfirmasi</a>

<b>Detail Request</b>
$requestDetails

<b>Request ID:</b> <code>$requestId</code>
<b>Tanggal Request:</b> $selectedDate
<b>Waktu Request:</b> $selectedTime

<b>Detail Tamu</b>
<b>Nama:</b> $escapedGuestName
<b>Kamar:</b> $guestRoom

${if (escapedNote.isNotEmpty()) {
    "<b>Catatan:</b>\n<i>$escapedNote</i>"
} else "<b>Catatan:</b> <i>Tidak ada catatan tambahan</i>"}
                        """.trimIndent()

                        // Create the request body for Telegram Bot API
                        // Convert chat_id to Long (Telegram group IDs are always numbers)
                        val chatIdNumber: Long = when (chatId) {
                            is Long -> chatId
                            is Number -> chatId.toLong()
                            is String -> {
                                try {
                                    chatId.toLong()
                                } catch (e: NumberFormatException) {
                                    Log.e("DHTV_CONTACT", "chatId is not a valid number: $chatId")
                                    throw IllegalArgumentException("chatId must be a number, got: $chatId")
                                }
                            }
                            else -> {
                                Log.e("DHTV_CONTACT", "chatId has unsupported type: ${chatId.javaClass}")
                                throw IllegalArgumentException("chatId must be a number, got: ${chatId.javaClass}")
                            }
                        }
                        
                        // Create request body using data class for proper serialization
                        val requestBody = TelegramMessageRequest(
                            chat_id = chatIdNumber,  // Telegram group chat ID (Long)
                            text = message,         // Message text with HTML formatting
                            parse_mode = "HTML"     // Enable HTML parsing for <b>bold</b> and <i>italic</i>
                        )
                        
                        Log.d("DHTV_CONTACT", "Endpoint URL: $endpointUrl")
                        Log.d("DHTV_CONTACT", "Chat ID: $chatIdNumber")

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val client = HttpClient(Android) {
                                    install(ContentNegotiation) {
                                        json(Json { prettyPrint = true; isLenient = true })
                                    }
                                }

                                Log.d("DHTV_CONTACT", "Making API call to: $endpointUrl")
                                Log.d("DHTV_CONTACT", "Request body: $requestBody")

                                val response = client.post(endpointUrl) {
                                    contentType(ContentType.Application.Json)
                                    setBody(requestBody)
                                }

                                val responseBody = response.bodyAsText()
                                Log.d("DHTV_CONTACT", "Response status: ${response.status}")
                                Log.d("DHTV_CONTACT", "Response body: $responseBody")

                                if (response.status == HttpStatusCode.OK) {
                                    Log.d("DHTV_CONTACT", "Message sent successfully to Telegram")
                                } else {
                                    Log.e("DHTV_CONTACT", "Error: ${response.status}")
                                    Log.e("DHTV_CONTACT", "Response Body: $responseBody")
                                }

                                client.close()

                            } catch (e: Exception) {
                                Log.e("DHTV_CONTACT", "Request failed: ${e.message}")
                                Log.e("DHTV_CONTACT", "Exception type: ${e.javaClass.simpleName}")
                                e.printStackTrace()
                            }
                        }
                    } else {
                        Log.e("DHTV_CONTACT", "Incomplete TELEGRAM_REQUEST settings in branch $branchId")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DHTV_CONTACT", "Failed to retrieve siteUrl", error.toException())
                }
            })
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("DHTV_CONTACT", "Failed to retrieve TELEGRAM_REQUEST settings from branch $branchId", error.toException())
        }
    })
}
