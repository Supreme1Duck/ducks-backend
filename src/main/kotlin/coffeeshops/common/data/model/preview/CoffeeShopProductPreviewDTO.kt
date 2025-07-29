package com.ducks.coffeeshops.common.data.model.preview

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class CoffeeShopProductPreviewDTO(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val categoryId: Long,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)



