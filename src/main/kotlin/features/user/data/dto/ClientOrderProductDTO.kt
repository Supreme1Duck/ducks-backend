package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ClientOrderProductDTO(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val quantity: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)
