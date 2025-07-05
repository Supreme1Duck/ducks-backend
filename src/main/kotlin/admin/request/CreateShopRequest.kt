package com.ducks.admin.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateShopRequest(
    val name: String,
    val address: String,
    val description: String?,
    val photoUrls: List<String>,
    val unp: String,
    val initialPass: String,
)