package com.ducks.admin.repository

import com.ducks.admin.database.CoffeeShopCredentialsTable
import com.ducks.admin.database.ShopCredentialsTable
import com.ducks.admin.repository.result.CreateShopResult
import com.ducks.admin.request.CreateCoffeeShopRequest
import com.ducks.features.coffeeshops.database.CoffeeProductCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.seller.domain.SellerConstructorsRepository
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class AdminCoffeeShopsRepository(
    private val sellersConstructorsRepository: SellerConstructorsRepository,
) {

    suspend fun createNewShop(
        data: CreateCoffeeShopRequest,
        createdByAdminID: Long,
    ): CreateShopResult {
        return try {
            val shopId = createShop(data, createdByAdminID)

            sellersConstructorsRepository.insertBasic(shopId = shopId)

            CreateShopResult.Success
        } catch (e: ExposedSQLException) {
            if (e.message?.contains("unique constraint") == true) {
                CreateShopResult.AlreadyExists
            } else {
                throw e
            }
        }
    }

    private suspend fun createShop(
        data: CreateCoffeeShopRequest,
        createdByAdminID: Long,
    ): Long {
        return newSuspendedTransaction {
            val shopId = CoffeeShopTable.insertAndGetId {
                it[name] = data.name
                it[address] = data.address
            }.value

            CoffeeShopCredentialsTable.insert {
                it[login] = data.unp
                it[password] = data.initialPass
                it[createdBy] = createdByAdminID
                it[ShopCredentialsTable.shopId] = shopId
            }

            shopId
        }
    }

    suspend fun insertNewCategories(
        categories: List<String>,
    ) {
        newSuspendedTransaction {
            CoffeeProductCategoryTable
                .batchInsert(categories) {
                    this[CoffeeProductCategoryTable.name] = it
                }
        }
    }
}