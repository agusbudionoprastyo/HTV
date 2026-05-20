package com.dafamsemarang.dhtv

import android.content.Context
import android.util.Log
import android.widget.ImageView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RequestDetailDialog(request: Request, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = .9f), RoundedCornerShape(20.dp))
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .align(Alignment.TopEnd)
            ) {
                Text(
                    text = "\uF057",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    fontFamily = FontFamily(Font(R.font.icons)),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column (
                modifier = Modifier
                    .padding(8.dp)
            ){
                when (request.status) {
                    "submitted" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DisplayGif(R.drawable.submitted)

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    "Your request has been submitted and is awaiting confirmation.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    getTimeAgo(request.timestamp!!),
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                RequestnotifDetails(request)
                            }
                        }
                    }

                    "confirm" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DisplayGif(R.drawable.confirm)

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    "Your request has been confirmed and is being processed.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    getTimeAgo(request.timestamp!!),
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                RequestnotifDetails(request)
                            }
                        }
                    }

                    "completed" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DisplayGif(R.drawable.complete)

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    "Your request has been completed.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    getTimeAgo(request.timestamp!!),
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                RequestnotifDetails(request)
                            }
                        }
                    }
                    else -> {
                        Text(
                            "Request status: ${request.status}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestnotifDetails(requestDetails: Request?) {
    // Check if requestDetails is not null and has a non-empty requests list
    if (requestDetails?.requests.isNullOrEmpty()) {
        Text(
            "No requests available",
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        // Iterate over each request and display it
        requestDetails.requests.forEach { request ->
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.request_title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = request.description,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = request.imageUrl
                        ),
                        contentDescription = request.request_title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    RequestStatusRow(requestDetails)
    Text(
        "Request date ${requestDetails?.selectedDate} | Request time ${requestDetails?.selectedTime}",
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 8.sp)
    )
}

@Composable
fun RequestStatusRow(
    requestDetails: Request?,
    submittedReqColor: Color = Color(0xFFfc4c75),
    confirmReqColor: Color = Color.LightGray,
    completedReqColor: Color = Color.LightGray
) {
    // Check the status and assign colors based on the orderDetails status
    val currentSubmittedReqColor: Color
    val currentConfirmReqColor: Color
    val currentCompletedReqColor: Color

    when (requestDetails?.status) {
        "submitted" -> {
            currentSubmittedReqColor = Color(0xFFfc4c75)
            currentConfirmReqColor = Color.LightGray
            currentCompletedReqColor = Color.LightGray
        }
        "confirm" -> {
            currentSubmittedReqColor = Color(0xFFfc4c75)
            currentConfirmReqColor = Color(0xFFfc4c75)
            currentCompletedReqColor = Color.LightGray
        }
        "completed" -> {
            currentSubmittedReqColor = Color(0xFFfc4c75)
            currentConfirmReqColor = Color(0xFFfc4c75)
            currentCompletedReqColor = Color(0xFFfc4c75)
        }
        else -> {
            // Default case if status is null or doesn't match any known value
            currentSubmittedReqColor = submittedReqColor
            currentConfirmReqColor = confirmReqColor
            currentCompletedReqColor = completedReqColor
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placed Status
        Text(
            "Submitted",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = currentSubmittedReqColor,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(24.dp)
                .background(currentConfirmReqColor, RoundedCornerShape(50))
        )

        // Confirm Status
        Text(
            "Confirm",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = currentConfirmReqColor,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(24.dp)
                .background(currentCompletedReqColor, RoundedCornerShape(50))
        )

        // Completed Status
        Text(
            "Completed",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = currentCompletedReqColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun OrderDetailDialog(order: Order, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = .9f), RoundedCornerShape(20.dp))
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .align(Alignment.TopEnd)
            ) {
                Text(
                    text = "\uF057",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    fontFamily = FontFamily(Font(R.font.icons)),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column{
                when (order.status) {
                    "placed" -> {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier
                                .fillMaxWidth(.4f)

                            ) {
                                Column {
                                    Text(
                                        "Thank you for your purchase!",
                                        style = TextStyle(fontSize = 16.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Your order will be processed. We will notify you once order has been shipped",
                                        style = TextStyle(fontSize = 8.sp, color = Color.DarkGray)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Guest detail",
                                        style = TextStyle(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Column {
                                            Text(
                                                "Name",
                                                style = TextStyle(fontSize = 8.sp),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Room",
                                                style = TextStyle(fontSize = 8.sp),
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Phone",
                                                style = TextStyle(fontSize = 8.sp),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(modifier = Modifier
                                                .padding(end = 32.dp)
                                            ){
                                                Text(
                                                    "${order.guestName?.let { formatName(it) }}",
                                                    style = TextStyle(
                                                        fontSize = 8.sp,
                                                        color = Color.DarkGray
                                                    )
                                                )
                                                Text(
                                                    "${order.guestRoom}",
                                                    style = TextStyle(
                                                        fontSize = 8.sp,
                                                        color = Color.DarkGray
                                                    )
                                                )
                                                Text(
                                                    "${order.guestPhone}",
                                                    style = TextStyle(
                                                        fontSize = 8.sp,
                                                        color = Color.DarkGray
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                            ){
                                OrdernotifDetails(order)
                            }
                        }
                    }
                    "confirm" -> {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(0.4f)) {
                                Column {
                                    Text(
                                        "Your order has been confirmed!",
                                        style = TextStyle(fontSize = 16.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Your order will be processed. We will notify you once order has been shipped",
                                        style = TextStyle(fontSize = 8.sp, color = Color.DarkGray)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    DisplayGif(R.drawable.orderconfirm)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                            ){
                                OrdernotifDetails(order)
                            }
                        }
                    }
                    "process" -> {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(0.4f)) {
                                Column {
                                    Text(
                                        "Your order has been processed!",
                                        style = TextStyle(fontSize = 16.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Your order is being prepared, we'll notify you when it's ready to ship.",
                                        style = TextStyle(fontSize = 8.sp, color = Color.DarkGray)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    DisplayGif(R.drawable.orderprocess)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OrdernotifDetails(order)  // Assume you have this composable
                        }
                    }
                    "deliver" -> {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(0.4f)) {
                                Column {
                                    Text(
                                        "Your order is on its way!",
                                        style = TextStyle(fontSize = 16.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Hang tight! We'll notify you once your order arrives.",
                                        style = TextStyle(fontSize = 8.sp, color = Color.DarkGray)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    DisplayGif(R.drawable.orderdeliver)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OrdernotifDetails(order)  // Assume you have this composable
                        }
                    }
                    "completed" -> {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(0.4f)) {
                                Column {
                                    Text(
                                        "Your order has been successfully delivered. Enjoy your meal!",
                                        style = TextStyle(fontSize = 16.sp),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Thank you for choosing us. We hope to serve you again soon!",
                                        style = TextStyle(fontSize = 8.sp, color = Color.DarkGray)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    DisplayGif(R.drawable.ordercomplete)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OrdernotifDetails(order)  // Assume you have this composable
                        }
                    }
                    else -> {
                        Text("Order ${order.status}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun OrdernotifDetails(orderDetails: Order?) {
    if (orderDetails?.items.isNullOrEmpty()) {
        Text(
            "No requests available",
            style = MaterialTheme.typography.bodySmall
        )
    } else {
        Column {
            Text(
                "Order Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            HorizontalDivider(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(),
                thickness = .5.dp,
                color = Color.LightGray
            )

            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())
                .withZone(ZoneId.systemDefault())

            val formattedDate = orderDetails.timestamp?.let {
                val instant = Instant.ofEpochMilli(it)
                formatter.format(instant)
            } ?: "Tanggal tidak tersedia"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Date/Time",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.DarkGray
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp),
                    thickness = .5.dp,
                    color = Color.LightGray
                )

                Column {
                    Text(
                        text = "Order Id",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.DarkGray
                    )
                    Text(
                        text = orderDetails.orderId.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(24.dp),
                    thickness = .5.dp,
                    color = Color.LightGray
                )

                Column {
                    Text(
                        text = "Payment",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.DarkGray
                    )
                    Text(
                        text = orderDetails.paymentMethod.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                    )
                }
            }

            DashedDivider(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
                    .height(.5.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            OrderStatusRow(orderDetails = orderDetails)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(orderDetails.items) { order ->

                    Row(
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                        ) {

                            Image(
                                painter = rememberAsyncImagePainter(order.imageUrl),
                                contentDescription = order.itemName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Badge(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-4).dp, y = (-4).dp)
                                    .size(14.dp),
                                containerColor = Color(0xFFE91E63)
                            ) {
                                Text(
                                    text = order.quantity.toString(),
                                    color = Color.White,
                                    fontSize = 6.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Text(
                                text = order.itemName ?: "No item name", // Fallback for null name
                                style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                            )

                            Text(
                                text = order.variant ?: "No variant",
                                style = TextStyle(
                                    fontSize = 6.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Text(
                                text = if (order.note.isNullOrEmpty()) {
                                    "No additional note"
                                } else {
                                    "note ${order.note}"
                                },
                                style = TextStyle(
                                    fontSize = 6.sp,
                                    color = Color.DarkGray
                                )
                            )

                            Text(
                                text = formatIDR(order.price ?: 0),
                                style = TextStyle(fontSize = 6.sp, color = Color.DarkGray)
                            )
                        }
                        Column (
                            horizontalAlignment = Alignment.End
                        ){
                            Text(
                                text = formatIDR((order.price ?: 0) * (order.quantity ?: 0)),
                                style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Subtotal",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.DarkGray
                    )
                    Text(
                        text = "Tax",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Order Total",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column ( modifier = Modifier
                    .fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                )
                {
                    Text(
                        text = formatIDR(orderDetails.subtotal),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.DarkGray,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = formatIDR(orderDetails.taxService),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.DarkGray,
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatIDR(orderDetails.total),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun OrderStatusRow(
    orderDetails: Order?,
    placedColor: Color = Color(0xFFfc4c75),
    confirmColor: Color = Color.LightGray,
    processColor: Color = Color.LightGray,
    deliverColor: Color = Color.LightGray,
    completedColor: Color = Color.LightGray
) {
    // Check the status and assign colors based on the orderDetails status
    val currentPlacedColor: Color
    val currentConfirmColor: Color
    val currentProcessColor: Color
    val currentDeliverColor: Color
    val currentCompletedColor: Color

    when (orderDetails?.status) {
        "placed" -> {
            currentPlacedColor = Color(0xFFfc4c75)
            currentConfirmColor = Color.LightGray
            currentProcessColor = Color.LightGray
            currentDeliverColor = Color.LightGray
            currentCompletedColor = Color.LightGray
        }
        "confirm" -> {
            currentPlacedColor = Color(0xFFfc4c75)
            currentConfirmColor = Color(0xFFfc4c75)
            currentProcessColor = Color.LightGray
            currentDeliverColor = Color.LightGray
            currentCompletedColor = Color.LightGray
        }
        "process" -> {
            currentPlacedColor = Color(0xFFfc4c75)
            currentConfirmColor = Color(0xFFfc4c75)
            currentProcessColor = Color(0xFFfc4c75)
            currentDeliverColor = Color.LightGray
            currentCompletedColor = Color.LightGray
        }
        "deliver" -> {
            currentPlacedColor = Color(0xFFfc4c75)
            currentConfirmColor = Color(0xFFfc4c75)
            currentProcessColor = Color(0xFFfc4c75)
            currentDeliverColor = Color(0xFFfc4c75)
            currentCompletedColor = Color.LightGray
        }
        "completed" -> {
            currentPlacedColor = Color(0xFFfc4c75)
            currentConfirmColor = Color(0xFFfc4c75)
            currentProcessColor = Color(0xFFfc4c75)
            currentDeliverColor = Color(0xFFfc4c75)
            currentCompletedColor = Color(0xFFfc4c75)
        }
        else -> {
            // Default case if status is null or doesn't match any known value
            currentPlacedColor = placedColor
            currentConfirmColor = confirmColor
            currentProcessColor = processColor
            currentDeliverColor = deliverColor
            currentCompletedColor = completedColor
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placed Status
        Text(
            "Placed",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = currentPlacedColor,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(12.dp)
                .background(currentConfirmColor, RoundedCornerShape(50))
        )

        // Confirm Status
        Text(
            "Confirm",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = currentConfirmColor,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(12.dp)
                .background(currentProcessColor, RoundedCornerShape(50))
        )

        // Process Status
        Text(
            "Process",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = currentProcessColor,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(12.dp)
                .background(currentDeliverColor, RoundedCornerShape(50))
        )

        // Deliver Status
        Text(
            "Deliver",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = currentDeliverColor,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(12.dp)
                .background(currentCompletedColor, RoundedCornerShape(50))
        )

        // Completed Status
        Text(
            "Completed",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = currentCompletedColor,
            fontWeight = FontWeight.Bold
        )
    }
}

fun getOrderDetailsFromFirebase(context: Context, notificationId: String, onResult: (Order?) -> Unit) {
    // Mendapatkan referensi ke node ORDERS di Firebase
    val database = FirebaseDatabase.getInstance().reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    val ordersRef = database.child("BRANCHES").child(branchId ?: "").child("ORDERS")

    // Query untuk mendapatkan order berdasarkan orderId yang cocok dengan notification.id
    val query = ordersRef.orderByChild("orderId").equalTo(notificationId)

    // Menjalankan query dan mendengarkan hasilnya
    query.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            // Jika ada hasil data
            if (snapshot.exists()) {
                // Ambil data pertama (harusnya hanya ada satu data yang cocok)
                val order = snapshot.children.firstOrNull()?.getValue(Order::class.java)
                onResult(order)
            } else {
                // Jika tidak ada order yang ditemukan
                onResult(null)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            // Jika terjadi kesalahan saat mengambil data
            Log.e("Firebase", "Error getting data: ${error.message}")
            onResult(null)
        }
    })
}

fun getRequestDetailsFromFirebase(context: Context, notificationId: String, onResult: (Request?) -> Unit) {
    val database = FirebaseDatabase.getInstance().reference
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val branchId = sharedPreferences.getString("branchId", null)
    val requestsRef = database.child("BRANCHES").child(branchId ?: "").child("REQUEST")

    val query = requestsRef.orderByChild("requestId").equalTo(notificationId)

    query.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                val request = snapshot.children.firstOrNull()?.getValue(Request::class.java)
                onResult(request)
            } else {
                onResult(null)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Error getting data: ${error.message}")
            onResult(null)
        }
    })
}

@Composable
fun DisplayGif(gifResId: Int) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                Glide.with(context)
                    .asGif()
                    .load(gifResId)
                    .into(this)
            }
        },
        modifier = Modifier
            .size(150.dp)
            .padding(8.dp)
    )
}

@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    // Get the density of the screen (density = dp to pixels conversion factor)
    val density = LocalDensity.current.density

    Canvas(modifier = modifier) {
        val dashWidth = 10f
        val dashGap = 5f
        val startX = 0f
        val endX = size.width
        var currentX = startX

        // Convert 0.5 dp to pixels
        val strokeWidth = 0.5f * density

        while (currentX < endX) {
            drawLine(
                color = Color.Gray,
                start = Offset(currentX, size.height / 2),
                end = Offset(currentX + dashWidth, size.height / 2),
                strokeWidth = strokeWidth // Use the stroke width in pixels
            )
            currentX += dashWidth + dashGap
        }
    }
}