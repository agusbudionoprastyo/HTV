package com.dafamsemarang.dhtv

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ApiRequest(
    val api_key: String,
    val sender: String,
    val number: String,
    val message: String
)

@Serializable
data class ApiOrder(
    val api_key: String,
    val sender: String,
    val number: String,
    val message: String
)

data class WelcomeData(
    val welcomeMessage: String = "",
    val voEn: String = "",
    val voId: String = "",
    val roomImageUrl: String = "",
    val backgroundUrl: String = "",
    val gm: String = "",
    val signUrl: String = ""
)

@Serializable
data class GuestInfo(
    val folio: Int = 0,
    val dateci: String = "",
    val dateco: String = "",
    val datecreate: String = "",
    val fname: String = "",
    val foliostatus: String = "",
    val email: String = "",
    val phone: String = "",
    val room: String = "",
    val roomnight: Int = 0,
    val roomtype: String = "",
    val guestImageUrl: String = "",
    val isSmoking: Boolean = false
)

data class Item(
    val name: String = "",
    val description: String = "",
    val imageUrl: String = ""
)

data class Variant(
    val id: String = "",
    val name: String = "",
    val price: Int = 0
)

data class MenuItemData(
    val id: String = "",
    val name: String = "",
    val price: Int = 0,
    val tax: Int = 0,
    val discount: Int = 0,
    val variant: List<Variant>? = null,
    val category: String = "",
    val imageRes: String = "",
    val description: String = "",
    val branchId: String = "",
    val isActive: Boolean = true
)

data class SelectedItem(
    val item: MenuItemData,
    var quantity: Int,
    val specialInstruction: String,
    val selectedVariant: Variant? = null // Add the variant as part of the selected item
)

object GlobalCartState {
    val selectedItems = androidx.compose.runtime.mutableStateListOf<SelectedItem>()
}

data class OrderItem(
    val itemName: String? = null,
    val variant: String? = null,
    val quantity: Int? = null,
    val price: Int? = null,
    val note: String? = "",
    val imageUrl: String = ""
)

data class Order(
    val folioId: Int? = null,
    val guestName: String? = null,
    val guestPhone: String? = null,
    val guestRoom: String? = null,
    val paymentMethod: String? = null,
    val status: String? = null,
    val items: List<OrderItem>? = null,
    val timestamp: Long? = null,
    val orderId: String? = null,
    val subtotal: Int? = null,
    val taxService: Int? = null,
    val total: Int? = null,
    val branchId: String? = null
)

data class GuestRequest(
    val request_title: String = "",
    val category: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val date: String? = null,
    val time: String? = null
)

data class Request(
    val folioId: Int? = null,
    val guestName: String? = null,
    val guestPhone: String? = null,
    val guestRoom: String? = null,
    val status: String? = null,
    val requests: List<GuestRequest>? = null,
    val selectedDate: String? = null,
    val selectedTime: String? = null,
    val timestamp: Long? = null,
    val requestId: String? = null,
    val date: String? = null,
    val time: String? = null,
    val note: String? = null
)

data class Notification(
    val id: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val title: String = "",
    val type: String = "",
    val status: String = "unread"
)

@Serializable
data class TelegramMessageRequest(
    @SerialName("chat_id")
    val chat_id: Long,
    val text: String,
    @SerialName("parse_mode")
    val parse_mode: String
)