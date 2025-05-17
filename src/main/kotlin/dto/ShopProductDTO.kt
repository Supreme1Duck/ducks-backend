package com.ducks.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ShopProductDTO(
    val id: Long,
    val name: String,
    val description: String?,
    @Contextual
    val price: BigDecimal?,
    val shopId: Long,
    val shopName: String,
    val brandName: String,
    val imageUrls: List<String>,
)
