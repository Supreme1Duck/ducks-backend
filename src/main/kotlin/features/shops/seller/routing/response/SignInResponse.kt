package com.ducks.features.shops.seller.routing.response

import kotlinx.serialization.Serializable

@Serializable
data class SignInResponse(
    val id: Long,
    val token: String,
)