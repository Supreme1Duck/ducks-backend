package com.ducks.model

import java.math.BigDecimal

data class ShopProductModel(
    val name: String,
    val description: String?,
    val shopId: Long,
    val brandName: String?,
    val imageUrls: List<String>,
    val categoryId: Long,
    val sizeIds: List<Long>,
    val colorId: Int?,
    val seasonModel: SeasonModel?,
    val price: BigDecimal?,
)