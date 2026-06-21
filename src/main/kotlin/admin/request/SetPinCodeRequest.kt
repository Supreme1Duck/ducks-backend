package com.ducks.admin.request

import kotlinx.serialization.Serializable

@Serializable
data class SetPinCodeRequest(
    val shopId: Long,
    val pin: String,
)
