package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable

/**
 * Результат удаления аккаунта. Аккаунт недоступен уже с [requestedAt], а [dataRemovalAt] —
 * момент, когда из базы вычистят остатки персональных данных.
 */
@Serializable
data class AccountDeletionDTO(
    val requestedAt: Long,
    val dataRemovalAt: Long,
)
