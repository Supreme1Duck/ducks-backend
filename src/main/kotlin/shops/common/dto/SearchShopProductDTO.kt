package com.ducks.shops.common.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class SearchShopProductDTO(
    val id: Long,
    val name: String,
    val brandName: String?,
    val shopName: String,
    @Contextual
    val price: BigDecimal?,
    val imageUrls: List<String>,
)