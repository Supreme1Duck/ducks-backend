package com.ducks.features.orders.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class OrderProductDTO(
    val id: Long,
    val orderId: Long,
    val name: String,
    val constructors: String?,
    val imageUrl: String?,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)