package com.ducks.features.orders.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class OrderDTO(
    val id: Long,
    val createdAt: Long,
    val userPhoneNumber: String,
    val products: List<OrderProductDTO>,
    val isActive: Boolean,
    val comment: String?,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)