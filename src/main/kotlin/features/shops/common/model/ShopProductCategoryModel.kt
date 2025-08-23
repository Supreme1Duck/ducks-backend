package com.ducks.features.shops.common.model

data class ShopProductCategoryModel(
    val id: Long,
    val name: String,
    val parentId: Long?,
    val children: List<ShopProductCategoryModel>?,
    val superCategoryId: Long?,
    val isSuperCategory: Boolean,
)