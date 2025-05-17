package com.ducks.model

data class ShopProductCategoryModel(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val children: List<ShopProductCategoryModel>?,
    val superCategoryId: Long?,
    val isSuperCategory: Boolean,
)