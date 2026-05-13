package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClientOrderProductDTO(
    val id: Long,
    val name: String,
    val imageUrl: String?,
)
