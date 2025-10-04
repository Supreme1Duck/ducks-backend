package com.ducks.features.coffeeshops.seller.domain

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable.closestTimeToTakeOrders
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import com.ducks.util.DucksBadRequestError
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.ktor.ext.inject

class SellerOrdersRepository(
    application: Application,
) {

    private val calculateCoffeeShopsOrdersTimeService by application.inject<CalculateCoffeeShopsOrdersTimeService>()

    suspend fun acceptOrder(orderId: Long, shopId: Long) {
        newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val (isOrderCorrect, closestTimeToTakeOrders) = getAdditionalInfo(orderId, shopId)
                ?: throw DucksBadRequestError("Попытка принять несуществующий заказ")

            if (!isOrderCorrect) {
                throw DucksBadRequestError("Попытка принять невалидный заказ!")
            }

            val secondsToCookAllProducts = CoffeeOrdersTable
                .join(
                    otherTable = CoffeeOrderedProductsTable,
                    joinType = JoinType.LEFT,
                    onColumn = CoffeeOrdersTable.id,
                    otherColumn = CoffeeOrderedProductsTable.orderId
                )
                .selectAll()
                .where {
                    CoffeeOrdersTable.id eq orderId
                }
                .map {
                    it[CoffeeOrderedProductsTable.secondsToCook]
                }.sumOf {
                    it ?: 0
                }

            val closestTimeToCook = closestTimeToTakeOrders ?: throw DucksBadRequestError("У вас нет свободного времени для принятия заказа!")

            val estimatedTimeToFinish = closestTimeToCook + secondsToCookAllProducts.times(1000)

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[acceptedTime] = currentTime
                it[estimatedFinishTime] = estimatedTimeToFinish
            }
        }

        calculateCoffeeShopsOrdersTimeService.invoke(shopId)
    }

    suspend fun cancelBySeller(orderId: Long, shopId: Long, message: String?) {
        newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val isOrderCancellable = CoffeeOrdersTable
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .map {
                    it[CoffeeOrdersTable.finishedTime] == null
                }
                .firstOrNull() ?: throw DucksBadRequestError("Попытка отменить несуществующий заказ!")

            if (!isOrderCancellable) {
                throw DucksBadRequestError("Попытка отменить невалидный заказ!")
            }

            CoffeeOrdersTable.update(
                where = {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
            ) {
                it[finishedTime] = currentTime
                it[isCancelledBySeller] = true
                it[cancelledMessage] = message
            }
        }

        calculateCoffeeShopsOrdersTimeService.invoke(shopId)
    }

    suspend fun finishOrder(orderId: Long, shopId: Long) {
        newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val isOrderCanBeFinished = CoffeeOrdersTable
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .map {
                    it[CoffeeOrdersTable.finishedTime] == null
                }
                .firstOrNull() ?: throw DucksBadRequestError("Попытка завершить несуществующий заказ!")

            if (!isOrderCanBeFinished) {
                throw DucksBadRequestError("Попытка завершить уже завершенный заказ")
            }

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[finishedTime] = currentTime
            }
        }

        calculateCoffeeShopsOrdersTimeService.invoke(shopId)
    }

    private fun getAdditionalInfo(orderId: Long, shopId: Long): Pair<Boolean, Long?>? {
        return CoffeeOrdersTable
            .join(CoffeeShopTable, JoinType.LEFT, CoffeeShopTable.id, CoffeeOrdersTable.coffeeShop)
            .select(CoffeeOrdersTable.acceptedTime, CoffeeShopTable.closestTimeToTakeOrders, CoffeeOrdersTable.finishedTime)
            .where {
                (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
            }
            .map {
                val isOrderCorrect =
                    it[CoffeeOrdersTable.acceptedTime] == null && it[CoffeeOrdersTable.finishedTime] == null

                isOrderCorrect to it[closestTimeToTakeOrders]
            }
            .firstOrNull()
    }
}