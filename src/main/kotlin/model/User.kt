package com.ducks.model

import java.util.UUID

data class User(
    val id: UUID,
    val phoneNumber: String,
    val firstName: String?,
    val lastName: String?
)