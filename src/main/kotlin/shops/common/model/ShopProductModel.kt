package com.ducks.shops.common.model

import java.math.BigDecimal

data class ShopProductModel(
    val name: String,
    val description: String?,
    val shopId: Long,
    val brandName: String?,
    val imageUrls: List<String>,
    val price: BigDecimal?,
)