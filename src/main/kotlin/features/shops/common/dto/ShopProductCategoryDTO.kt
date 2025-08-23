package com.ducks.features.shops.common.dto

import kotlinx.serialization.Serializable

@Serializable
data class ShopProductCategoryDTO(
    val id: Long,
    val name: String,
    val description: String? = null,
    val isSuperCategory: Boolean = false,
    val superCategoryId: Long? = null,
    val parentCategoryId: Long? = null,
)
