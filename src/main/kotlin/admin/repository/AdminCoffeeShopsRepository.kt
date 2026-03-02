package com.ducks.admin.repository

import com.ducks.admin.database.CoffeeShopCredentialsTable
import com.ducks.admin.database.ShopCredentialsTable
import com.ducks.admin.request.CreateCoffeeShopRequest
import com.ducks.features.coffeeshops.database.CoffeeProductCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.seller.domain.SellerCoffeeShopRepository
import com.ducks.features.coffeeshops.seller.domain.SellerConstructorsRepository
import com.ducks.features.coffeeshops.seller.routings.request.shop.*
import com.ducks.util.DucksBadRequestError
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class AdminCoffeeShopsRepository(
    private val sellersConstructorsRepository: SellerConstructorsRepository,
    private val repository: SellerCoffeeShopRepository,
) {

    suspend fun createNewShop(
        data: CreateCoffeeShopRequest,
        createdByAdminID: Long,
    ) {
        val shopId = createShop(data, createdByAdminID)

        addSchedules(shopId, data.workTime)

        sellersConstructorsRepository.insertBasic(shopId = shopId)
    }

    private suspend fun addSchedules(shopId: Long, request: SetCoffeeShopScheduleRequest) {
        repository.setSchedule(
            shopId = shopId,
            schedule = request,
        )
    }

    private suspend fun createShop(
        data: CreateCoffeeShopRequest,
        createdByAdminID: Long,
    ): Long {
        return try {
            newSuspendedTransaction {
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
        } catch (e: ExposedSQLException) {
            if (e.message?.contains("unique constraint") == true) {
                throw DucksBadRequestError("Кофешоп с таким унп уже существует")
            } else {
                throw DucksBadRequestError("Неизвестная SQL ошибка")
            }
        } catch (e: Exception) {
            throw Exception("Ошибка в процессе создания магазина, ${e.stackTrace}")
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