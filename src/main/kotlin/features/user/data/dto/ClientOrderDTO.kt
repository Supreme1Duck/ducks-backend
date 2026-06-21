package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

enum class OrderStatus(val value: Int) {
    IN_PROGRESS(0),
    COMPLETED(1),
    CANCELLED(2),
}

@Serializable
data class ClientOrderDTO(
    val id: Long,
    val shopName: String,
    val finishedAt: Long,
    val products: List<ClientOrderProductDTO>,
    val comment: String?,
    val status: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)
