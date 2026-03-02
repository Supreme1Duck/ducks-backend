package com.ducks.features.orders.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CurrentOrdersDTO(
    val activeOrders: List<OrderDTO>?,
    val pendingOrders: List<OrderDTO>,
)