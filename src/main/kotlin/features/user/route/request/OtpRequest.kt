package com.ducks.features.user.route.request

import kotlinx.serialization.Serializable

@Serializable
data class OtpRequest(
    val phoneNumber: String,
)