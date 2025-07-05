package com.ducks.auth

import kotlinx.serialization.Serializable

@Serializable
data class SellerLoginRequest(
    val login: String,
    val password: String,
)