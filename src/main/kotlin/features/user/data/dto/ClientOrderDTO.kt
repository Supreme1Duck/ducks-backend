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
    val status: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)
