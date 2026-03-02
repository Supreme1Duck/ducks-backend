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
    val comment: String?,

    // Является ли заказ "ко времени"
    val isToTime: Boolean = false,
    val estimatedTimeToFinish: Long,
)

@Serializable
class OrderProductRequest(
    val productId: Long,
    val sizeId: String,
    val constructorIds: List<Long>?,
    val quantity: Int?,
)