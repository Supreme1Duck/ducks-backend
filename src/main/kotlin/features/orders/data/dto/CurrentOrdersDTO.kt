package com.ducks.features.orders.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CurrentOrdersDTO(
    val activeOrdersDTO: List<OrderDTO>?,
    val pendingOrders: List<OrderDTO>,
)