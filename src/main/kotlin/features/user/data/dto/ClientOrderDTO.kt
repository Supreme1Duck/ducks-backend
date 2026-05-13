package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ClientOrderDTO(
    val id: Long,
    val shopName: String,
    val createdAt: Long,
    val estimatedFinishTime: Long,
    val products: List<ClientOrderProductDTO>,
    val comment: String?,
    val isActive: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)
