package com.ducks.admin.repository

import com.ducks.admin.database.ShopCredentialsTable
import com.ducks.admin.repository.result.CreateShopResult
import com.ducks.admin.request.CreateShopRequest
import com.ducks.features.shops.database.table.ShopTable
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class AdminShopsRepository {

    suspend fun createNewShop(
        data: CreateShopRequest,
        createdByAdminID: Long,
    ): CreateShopResult {
        return newSuspendedTransaction {
            try {
                val shopId = ShopTable.insertAndGetId {
                    it[name] = data.name
                    it[address] = data.address
                    it[description] = data.description
                    it[photoUrls] = data.photoUrls
                }

                ShopCredentialsTable.insert {
                    it[login] = data.unp
                    it[password] = data.initialPass
                    it[createdBy] = createdByAdminID
                    it[ShopCredentialsTable.shopId] = shopId
                }

                CreateShopResult.Success
            } catch (e: ExposedSQLException) {
                if (e.message?.contains("unique constraint") == true) {
                    CreateShopResult.AlreadyExists
                } else {
                    throw e
                }
            }
        }
    }
}