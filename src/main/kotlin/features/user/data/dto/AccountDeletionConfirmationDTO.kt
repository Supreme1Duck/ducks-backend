package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable

/**
 * Подтверждение, выданное за верный код из СМС. [confirmationToken] предъявляется при
 * удалении аккаунта и сгорает после первого успешного удаления.
 */
@Serializable
data class AccountDeletionConfirmationDTO(
    val confirmationToken: String,
    val expiresAt: Long,
)
