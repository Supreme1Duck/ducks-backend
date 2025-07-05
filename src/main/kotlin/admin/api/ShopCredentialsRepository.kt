package com.ducks.admin.api

import com.ducks.admin.database.ShopCredentialsTable
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class ShopCredentialsRepository {

    suspend fun login(login: String, password: String): Long {
        return newSuspendedTransaction {
            ShopCredentialsTable
                .select(ShopCredentialsTable.shopId)
                .where {
                    (ShopCredentialsTable.login eq login) and (ShopCredentialsTable.password eq password)
                }
                .map {
                    it[ShopCredentialsTable.shopId].value
                }
                .first()
        }
    }
}