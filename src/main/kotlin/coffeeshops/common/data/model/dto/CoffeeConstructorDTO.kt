package com.ducks.coffeeshops.common.data.model.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class CoffeeConstructorDTO(
    val id: Long,
    val name: String,
    val categoryId: Long,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
    val isInStock: Boolean,
)