package com.ducks.features.user.route.request

import kotlinx.serialization.Serializable

@Serializable
data class VerifyDeletionOtpRequest(
    val otp: String,
)
