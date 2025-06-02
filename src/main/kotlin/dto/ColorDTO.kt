package com.ducks.dto

import kotlinx.serialization.Serializable

@Serializable
data class ColorDTO(
    val id: Int,
    val name: String,
)
