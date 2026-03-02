package com.ducks.features.orders.data.dto

import com.ducks.features.orders.database.model.OrderedProductConstructorDBModel
import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class OrderProductDTO(
    val id: Long,
    val orderId: Long,
    val name: String,
    val constructors: List<OrderedProductConstructorDBModel>?,
    val imageUrl: String?,
    val size: String?,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    val quantity: Int,
)