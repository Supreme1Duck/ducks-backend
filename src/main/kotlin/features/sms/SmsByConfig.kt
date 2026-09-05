package com.ducks.features.sms

data class SmsByConfig(
    val token: String,
    val alphanameId: String,
) {
    companion object {
        const val SYSTEM_ALPHANAME_ID = "0"
    }
}
