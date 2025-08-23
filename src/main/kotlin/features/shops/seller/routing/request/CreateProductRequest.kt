package com.ducks.features.shops.seller.routing.request

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
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
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
)