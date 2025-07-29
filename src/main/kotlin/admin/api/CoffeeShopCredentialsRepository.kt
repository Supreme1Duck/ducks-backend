package com.ducks.admin.api

import com.ducks.admin.database.CoffeeShopCredentialsTable
import com.ducks.admin.repository.result.LoginResult
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class CoffeeShopCredentialsRepository {

    suspend fun login(login: String, password: String): LoginResult {
        val shopId = newSuspendedTransaction {
            CoffeeShopCredentialsTable
                .select(CoffeeShopCredentialsTable.shopId)
                .where {
                    (CoffeeShopCredentialsTable.login eq login) and (CoffeeShopCredentialsTable.password eq password)
                }
                .map {
                    it[CoffeeShopCredentialsTable.shopId].value
                }
                .firstOrNull()
        }

        return shopId?.let {
            LoginResult.Success(it)
        } ?: LoginResult.InvalidCredentials
    }
}