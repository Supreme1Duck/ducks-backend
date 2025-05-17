package com.ducks.dto

data class ShopProductCategoryDTO(
    val name: String,
    val description: String? = null,
    val isSuperCategory: Boolean = false,
    val superCategoryId: Long? = null,
    val parentCategoryId: Long? = null,
)
