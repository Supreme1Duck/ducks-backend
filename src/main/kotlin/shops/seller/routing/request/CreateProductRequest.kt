package com.ducks.shops.seller.routing.request

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String,
    val sizeIds: List<Long>,
    val categoryId: Long,
    val colorId: Long,
    val seasonId: Int,
    val brandName: String,
    val mainImageUrl: String,
    val imageUrls: List<String>,
    @Contextual
    val price: BigDecimal?,
)