package com.ducks.features.orders.database.model

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class OrderedProductConstructorDBModel(
    val id: Long,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
)