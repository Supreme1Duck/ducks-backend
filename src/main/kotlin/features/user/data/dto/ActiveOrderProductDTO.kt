package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ActiveOrderProductDTO(
    val id: Long,
    val name: String,
    val quantity: Int,
    val constructors: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)
