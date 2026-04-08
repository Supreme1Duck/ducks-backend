package com.ducks.features.coffeeshops.client.routings.request

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
class CreateOrderRequest(
    val shopId: Long,
    val products: List<OrderProductRequest>,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val tips: BigDecimal? = null,
    val comment: String? = null,

    // Является ли заказ "ко времени"
    val isToTime: Boolean = false,
    val estimatedTimeToFinish: Long,
)

@Serializable
class OrderProductRequest(
    val productId: Long,
    val sizeId: String,
    val constructorIds: List<Long>? = null,
    val quantity: Int?,
)