package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ActiveOrderProductDTO(
    val name: String,
    // Добавки через запятую
    val constructors: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)
