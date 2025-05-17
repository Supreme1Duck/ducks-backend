package com.ducks.model

import java.math.BigDecimal

data class ShopProductModel(
    val name: String,
    val description: String?,
    val imageUrls: List<String>,
    val brandName: String,
    val price: BigDecimal?,
)