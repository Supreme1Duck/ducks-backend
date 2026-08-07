package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ClientOrderDTO(
    val id: Long,
    val shopId: Long,
    val shopName: String,
    val shopAddress: String,
    val finishedAt: Long,
    val products: List<ClientOrderProductDTO>,
    val comment: String?,
    val cancelledMessage: String? = null,
    // true — заказ с собой, false — на месте.
    val isTakeaway: Boolean,
    val status: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)
