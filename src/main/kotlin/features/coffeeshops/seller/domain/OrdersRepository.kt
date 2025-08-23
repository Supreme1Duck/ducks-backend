package com.ducks.features.coffeeshops.seller.domain

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.util.DucksBadRequestError
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class OrdersRepository {
    suspend fun acceptOrder(orderId: Long, shopId: Long) {
        newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val (isOrderCorrect, secondsToCook) = CoffeeOrdersTable
                .join(CoffeeShopTable, JoinType.LEFT, CoffeeShopTable.id, CoffeeOrdersTable.coffeeShop)
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .map {
                    val isOrderCorrect = (it[CoffeeOrdersTable.cancelledByClientTime] == null
                            && it[CoffeeOrdersTable.cancelledBySellerTime] == null
                            && it[CoffeeOrdersTable.acceptedTime] == null
                            && it[CoffeeOrdersTable.finishedTime] == null)

                    val secondsToCook = it[CoffeeShopTable.secondsToCook]

                    isOrderCorrect to secondsToCook
                }
                .firstOrNull() ?: throw DucksBadRequestError("Попытка принять несуществующий заказ")

            if (!isOrderCorrect) {
                throw DucksBadRequestError("Попытка принять невалидный заказ!")
            }

            val productsToCookCount = CoffeeOrdersTable
                .join(
                    CoffeeOrderedProductsTable,
                    joinType = JoinType.LEFT,
                    CoffeeOrdersTable.id,
                    CoffeeOrderedProductsTable.orderId
                )
                .selectAll()
                .where {
                    CoffeeOrdersTable.id eq orderId
                }
                .map {
                    it[CoffeeOrderedProductsTable.needToCook]
                }.count { it }

            val estimatedTimeToFinish = System.currentTimeMillis() + (productsToCookCount * secondsToCook).times(1000)

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[acceptedTime] = currentTime
                it[estimatedFinishTime] = estimatedTimeToFinish
            }
        }
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
                it[cancelledBySellerTime] = currentTime
                it[cancelledMessage] = message
            }
        }
    }

    suspend fun cancelByClient(orderId: Long) {
        newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val isOrderCancellable = CoffeeOrdersTable
                .selectAll()
                .where {
                    CoffeeOrdersTable.id eq orderId
                }
                .map {
                    it[CoffeeOrdersTable.acceptedTime] == null
                            && it[CoffeeOrdersTable.finishedTime] == null
                            && it[CoffeeOrdersTable.cancelledByClientTime] == null
                            && it[CoffeeOrdersTable.cancelledBySellerTime] == null
                }.firstOrNull() ?: throw DucksBadRequestError("Попытка завершить несуществующий заказ!")

            if (!isOrderCancellable) {
                throw DucksBadRequestError("Попытка завершить невалидный заказ!")
            }

            CoffeeOrdersTable.update(
                where = {
                    CoffeeOrdersTable.id eq orderId
                }
            ) {
                it[cancelledByClientTime] = currentTime
            }
        }
    }

    suspend fun finishOrder(orderId: Long, shopId: Long) {
        newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val isOrderFinishable = CoffeeOrdersTable
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .map {
                    it[CoffeeOrdersTable.finishedTime] == null
                            && it[CoffeeOrdersTable.cancelledByClientTime] == null
                            && it[CoffeeOrdersTable.cancelledBySellerTime] == null
                }
                .firstOrNull() ?: throw DucksBadRequestError("Попытка завершить несуществующий заказ!")

            if (!isOrderFinishable) {
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
    }
}