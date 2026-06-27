@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@file:Suppress("NAME_SHADOWING")

package com.dafamsemarang.dhtv

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.focusable

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
    var focusedCategoryForSelection by remember { mutableStateOf<String?>(null) }
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

   val displayRequests = remember(requestItems, selectedCategory) {
       if (selectedCategory.isNullOrEmpty()) {
           requestItems
       } else {
           requestItems.filter { it.category == selectedCategory }
       }
   }
   
   var isLoadingRequests by remember { mutableStateOf(true) }
   var isFiltering by remember { mutableStateOf(false) }
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
    LaunchedEffect(isRequestLoaded) {
        if (isRequestLoaded) {
            isLoadingRequests = false
        }
    }
   
    val currentCategoryIndex = remember(selectedCategory, categories) {
        val cats = listOf(null) + categories
        cats.indexOf(selectedCategory).coerceAtLeast(0)
    }

   val categoryListState = rememberLazyListState()
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

   LaunchedEffect(selectedCategory) {
       itemListState.scrollToItem(0)
       focusedItemIndex = 0
   }
   
    LaunchedEffect(focusedCategoryForSelection) {
        delay(200)
        selectedCategory = focusedCategoryForSelection
    }
   
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

   Box(
       modifier = Modifier
           .fillMaxSize()
   ) {
       Box(
           modifier = Modifier.fillMaxSize()
       ) {
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
                      label = "RequestContentTransition",
                     modifier = Modifier
                         .fillMaxWidth()
                         .padding(top = 162.dp)
                 ) { (_, isFilteringItems, requests) ->
                     val isShowingShimmer = (isLoadingRequests && shimmerVisible) || isFilteringItems
                     
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
    
    val calendar = remember { Calendar.getInstance() }
    
    // Parse initial date
    var day by remember { 
        mutableStateOf(
            try { formattedDate.split("/")[0].toInt() } 
            catch(e: Exception) { calendar.get(Calendar.DAY_OF_MONTH) }
        )
    }
    var month by remember { 
        mutableStateOf(
            try { formattedDate.split("/")[1].toInt() } 
            catch(e: Exception) { calendar.get(Calendar.MONTH) + 1 }
        )
    }
    var year by remember { 
        mutableStateOf(
            try { formattedDate.split("/")[2].toInt() } 
            catch(e: Exception) { calendar.get(Calendar.YEAR) }
        )
    }
    
    // Parse initial time
    var hour by remember { 
        mutableStateOf(
            try { formattedTime.split(":")[0].toInt() } 
            catch(e: Exception) { calendar.get(Calendar.HOUR_OF_DAY) }
        )
    }
    var minute by remember { 
        mutableStateOf(
            try { formattedTime.split(":")[1].toInt() } 
            catch(e: Exception) { calendar.get(Calendar.MINUTE) }
        )
    }

    // Helper to get max days in selected month/year
    val maxDays = remember(month, year) {
        val tempCal = Calendar.getInstance()
        tempCal.set(Calendar.YEAR, year)
        tempCal.set(Calendar.MONTH, month - 1)
        tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    
    // Adjust day if it exceeds maxDays
    LaunchedEffect(maxDays) {
        if (day > maxDays) {
            day = maxDays
        }
    }

    val selectedDate = "$day/$month/$year"
    val selectedTime = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    var isEditingDateTime by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    
    val hourFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isEditingDateTime) {
        if (isEditingDateTime) {
            delay(100)
            try {
                hourFocusRequester.requestFocus()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

                // Date & Time picker (Inline)
                Text(text = "Request Date & Time")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditingDateTime) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Date Pickers
                            PickerColumn(label = "Day", value = day, range = 1..maxDays, onValueChange = { day = it })
                            PickerColumn(label = "Month", value = month, range = 1..12, onValueChange = { month = it })
                            PickerColumn(label = "Year", value = year, range = 2026..2036, onValueChange = { year = it })

                            Spacer(modifier = Modifier.width(4.dp))
                            Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.Gray.copy(alpha = 0.3f)))
                            Spacer(modifier = Modifier.width(4.dp))

                            // Time Pickers
                            PickerColumn(label = "Hour", value = hour, range = 0..23, onValueChange = { hour = it }, zeroPad = true, modifier = Modifier.focusRequester(hourFocusRequester))
                            PickerColumn(label = "Minute", value = minute, range = 0..59, onValueChange = { minute = it }, zeroPad = true)
                        }

                        // Check / Done Button
                        var isCheckFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .onFocusChanged { isCheckFocused = it.isFocused }
                                .background(if (isCheckFocused) Color(0xFFCFDFED) else Color.Transparent)
                                .clickable { isEditingDateTime = false }
                                .focusable(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = "Done",
                                tint = if (isCheckFocused) Color(0xFF1E2026) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(id = R.drawable.ic_date_time),
                                contentDescription = "Date & Time",
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$selectedDate $selectedTime",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Edit Button
                        var isEditFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .onFocusChanged { isEditFocused = it.isFocused }
                                .background(if (isEditFocused) Color(0xFFCFDFED) else Color.Transparent)
                                .clickable { isEditingDateTime = true }
                                .focusable(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = "Edit",
                                tint = if (isEditFocused) Color(0xFF1E2026) else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Note")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp)
                        .background(
                            Color.LightGray.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        var isMicFocused by remember { mutableStateOf(false) }
                        val micBgColor = if (isListening || isMicFocused) Color(0xFFCFDFED) else Color.Gray.copy(alpha = 0.2f)
                        val micIconTint = if (isListening || isMicFocused) Color(0xFF1E2026) else Color.Gray

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(micBgColor)
                                .onFocusChanged { isMicFocused = it.isFocused }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var isCancelFocused by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .onFocusChanged { isCancelFocused = it.isFocused }
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = onDismiss
                        )
                        .background(if (isCancelFocused) Color(0xFFCFDFED) else Color.Gray.copy(alpha = 0.2f))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        color = if (isCancelFocused) Color(0xFF1E2026) else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                var isSubmitFocused by remember { mutableStateOf(false) }
                val isSubmitEnabled = folioId != null && folioId != 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .onFocusChanged { isSubmitFocused = it.isFocused }
                        .clickable(
                            enabled = isSubmitEnabled,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
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
                                        note,
                                        guestInfo?.gender
                                    )
                                }
                                onDismiss()
                            }
                        )
                        .background(
                            if (!isSubmitEnabled) {
                                Color.Gray.copy(alpha = 0.05f)
                            } else if (isSubmitFocused) {
                                Color(0xFFCFDFED)
                            } else {
                                Color.Gray.copy(alpha = 0.2f)
                            }
                        )
                        .focusable(enabled = isSubmitEnabled)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Submit Request",
                        color = if (!isSubmitEnabled) {
                            Color.Gray.copy(alpha = 0.4f)
                        } else if (isSubmitFocused) {
                            Color(0xFF1E2026)
                        } else {
                            Color.Gray
                        },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        dismissButton = null
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
   note: String, // Added note
   gender: String? = null
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
       // Trigger FCM Notification
       FcmHelper.sendFcmNotification(
           context = context,
           type = "REQUEST",
           title = "Request Baru",
           bodyText = "Kamar $guestRoom - $guestName meminta: " + (requests.firstOrNull()?.request_title ?: ""),
           additionalData = mapOf(
               "requestId" to requestId,
               "room" to guestRoom,
               "guestName" to guestName,
               "requestTitle" to (requests.firstOrNull()?.request_title ?: ""),
               "note" to note
           )
       )
   }.addOnFailureListener {
       Log.e("DHTV_CONTACT", "Failed to save request to Firebase")
   }
}



@Composable
fun PickerColumn(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    zeroPad: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        androidx.compose.ui.input.key.Key.DirectionUp -> {
                            val newValue = if (value + 1 > range.last) range.first else value + 1
                            onValueChange(newValue)
                            true
                        }
                        androidx.compose.ui.input.key.Key.DirectionDown -> {
                            val newValue = if (value - 1 < range.first) range.last else value - 1
                            onValueChange(newValue)
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .background(
                color = if (isFocused) Color(0xFFCFDFED) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isFocused) Color(0xFF071434).copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = if (zeroPad) value.toString().padStart(2, '0') else value.toString(),
            color = if (isFocused) Color(0xFF071434) else Color.Gray,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}
