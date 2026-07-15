package com.ducks.features.coffeeshops.seller.data

import com.ducks.admin.database.CoffeeShopCredentialsTable
import com.ducks.util.DucksBadRequestError
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class SellerPinCodeDataSource {

    suspend fun setPin(shopId: Long, pin: String) {
        newSuspendedTransaction {
            CoffeeShopCredentialsTable.update({ CoffeeShopCredentialsTable.shopId eq shopId }) {
                it[pinCode] = pin
                it[pinFailedAttempts] = 0
                it[pinLastFailedAt] = null
                it[pinLockedUntil] = null
            }
        }
    }

    private sealed interface VerifyResult {
        data object Success : VerifyResult
        data class Locked(val secondsLeft: Long) : VerifyResult
        data object JustLocked : VerifyResult
        data object WrongPin : VerifyResult
    }

    suspend fun verifyPin(shopId: Long, pin: String) {
        val result = newSuspendedTransaction {
            val row = CoffeeShopCredentialsTable
                .selectAll()
                .where { CoffeeShopCredentialsTable.shopId eq shopId }
                .firstOrNull() ?: return@newSuspendedTransaction null

            val now = System.currentTimeMillis()

            val lockedUntil = row[CoffeeShopCredentialsTable.pinLockedUntil]
            if (lockedUntil != null && now < lockedUntil) {
                val secondsLeft = (lockedUntil - now) / 1000
                return@newSuspendedTransaction VerifyResult.Locked(secondsLeft)
            }

            val storedPin = row[CoffeeShopCredentialsTable.pinCode]

            if (pin != storedPin) {
                val lastFailedAt = row[CoffeeShopCredentialsTable.pinLastFailedAt]
                val failedAttempts = row[CoffeeShopCredentialsTable.pinFailedAttempts]

                val windowMs = 2 * 60 * 1000L
                val attemptsInWindow = if (lastFailedAt != null && now - lastFailedAt < windowMs) {
                    failedAttempts + 1
                } else {
                    1
                }

                return@newSuspendedTransaction if (attemptsInWindow >= 3) {
                    CoffeeShopCredentialsTable.update({ CoffeeShopCredentialsTable.shopId eq shopId }) {
                        it[pinFailedAttempts] = 0
                        it[pinLastFailedAt] = null
                        it[pinLockedUntil] = now + 10 * 60 * 1000L
                    }
                    VerifyResult.JustLocked
                } else {
                    CoffeeShopCredentialsTable.update({ CoffeeShopCredentialsTable.shopId eq shopId }) {
                        it[pinFailedAttempts] = attemptsInWindow
                        it[pinLastFailedAt] = now
                        it[pinLockedUntil] = null
                    }
                    VerifyResult.WrongPin
                }
            }

            CoffeeShopCredentialsTable.update({ CoffeeShopCredentialsTable.shopId eq shopId }) {
                it[pinFailedAttempts] = 0
                it[pinLastFailedAt] = null
                it[pinLockedUntil] = null
            }
            VerifyResult.Success
        }

        when (result) {
            null -> throw DucksBadRequestError("Кофейня не найдена")
            is VerifyResult.Locked ->
                throw DucksBadRequestError("Слишком много попыток. Попробуйте через ${result.secondsLeft} секунд")
            VerifyResult.JustLocked ->
                throw DucksBadRequestError("Слишком много попыток. Попробуйте через 10 минут")
            VerifyResult.WrongPin ->
                throw DucksBadRequestError("Неверный пин-код")
            VerifyResult.Success -> Unit
        }
    }
}
