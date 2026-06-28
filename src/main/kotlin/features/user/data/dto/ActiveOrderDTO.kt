package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ActiveOrderDTO(
    val id: Long,
    val shopName: String,
    val shopAddress: String,
    val isAccepted: Boolean,
    val estimatedFinishTime: Long,

    val products: List<ActiveOrderProductDTO>,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val tips: BigDecimal?,
)