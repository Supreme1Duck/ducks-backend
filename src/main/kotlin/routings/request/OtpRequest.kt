package com.ducks.routing.request

import kotlinx.serialization.Serializable

@Serializable
data class OtpRequest(
    val phoneNumber: String,
)