package com.ducks.shops.common.database.dbmodels

import kotlinx.serialization.Serializable

@Serializable
data class ShopProductImageDTO(
    val id: String,
    val url: String
)
