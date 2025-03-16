package com.ducks.routing.response

import com.ducks.util.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UserResponse(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val phone: String,
    val firstName: String?,
    val lastName: String?,
)
