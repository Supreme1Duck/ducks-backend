package com.ducks.routings.request

import kotlinx.serialization.Serializable

@Serializable
data class OtpRequest(
    val phoneNumber: String,
)