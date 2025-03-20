package com.ducks.routings.request

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val phoneNumber: String,
    val otp: String,
    val firstName: String? = null,
    val lastName: String? = null
)
