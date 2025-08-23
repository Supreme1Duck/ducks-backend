package com.ducks.features.coffeeshops.client.routings.request

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
class CreateOrderRequest(
    val clientId: Long,
    val shopId: Long,
    val products: List<OrderProductRequest>,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    val comment: String?,
)

@Serializable
class OrderProductRequest(
    val productId: Long,
    val sizeId: Long?,
    val constructorIds: List<Long>?,
    val needToCook: Boolean,
)