package com.ducks.coffeeshops.seller.routings.request.constructor

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class CreateConstructorRequest(
    val name: String,
    val categoryId: Long,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
    val isInStock: Boolean,
)