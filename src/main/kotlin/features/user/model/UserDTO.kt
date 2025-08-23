package com.ducks.features.user.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDTO(
    val id: Long,
    val phoneNumber: String,
    val firstName: String?,
    val lastName: String?
)