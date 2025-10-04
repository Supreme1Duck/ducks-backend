package com.ducks.features.orders.data.dto

import com.ducks.features.orders.database.model.OrderedProductConstructorDBModel
import kotlinx.serialization.Serializable

@Serializable
data class OrderProductDTO(
    val id: Long,
    val orderId: Long,
    val name: String,
    val constructors: List<OrderedProductConstructorDBModel>?,
    val imageUrl: String?,
    val size: String?,
)