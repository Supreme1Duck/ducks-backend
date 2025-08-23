package com.ducks.features.shops.database.dbmodels

import kotlinx.serialization.Serializable

@Serializable
data class ShopProductImageDTO(
    val id: String,
    val url: String
)
