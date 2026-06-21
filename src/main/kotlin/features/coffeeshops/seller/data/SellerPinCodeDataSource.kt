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

    suspend fun verifyPin(shopId: Long, pin: String) {
        newSuspendedTransaction {
            val row = CoffeeShopCredentialsTable
                .selectAll()
                .where { CoffeeShopCredentialsTable.shopId eq shopId }
                .firstOrNull() ?: throw DucksBadRequestError("Кофейня не найдена")

            val now = System.currentTimeMillis()

            val lockedUntil = row[CoffeeShopCredentialsTable.pinLockedUntil]
            if (lockedUntil != null && now < lockedUntil) {
                val secondsLeft = (lockedUntil - now) / 1000
                throw DucksBadRequestError("Слишком много попыток. Попробуйте через $secondsLeft секунд")
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

                if (attemptsInWindow >= 3) {
                    CoffeeShopCredentialsTable.update({ CoffeeShopCredentialsTable.shopId eq shopId }) {
                        it[pinFailedAttempts] = 0
                        it[pinLastFailedAt] = null
                        it[pinLockedUntil] = now + 10 * 60 * 1000L
                    }
                    throw DucksBadRequestError("Слишком много попыток. Попробуйте через 10 минут")
                } else {
                    CoffeeShopCredentialsTable.update({ CoffeeShopCredentialsTable.shopId eq shopId }) {
                        it[pinFailedAttempts] = attemptsInWindow
                        it[pinLastFailedAt] = now
                        it[pinLockedUntil] = null
                    }
                    throw DucksBadRequestError("Неверный пин-код")
                }
            }

            CoffeeShopCredentialsTable.update({ CoffeeShopCredentialsTable.shopId eq shopId }) {
                it[pinFailedAttempts] = 0
                it[pinLastFailedAt] = null
                it[pinLockedUntil] = null
            }
        }
    }
}
