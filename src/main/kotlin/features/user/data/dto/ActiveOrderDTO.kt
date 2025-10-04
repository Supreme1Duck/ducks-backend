package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ActiveOrderDTO(
    val id: Long,
    val shopName: String,
    val isAccepted: Boolean,
    val estimatedFinishTime: Long,

    // Названия продуктов через запятую
    val products: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)