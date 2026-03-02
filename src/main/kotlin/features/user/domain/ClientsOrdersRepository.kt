package com.ducks.features.user.domain

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import com.ducks.features.user.data.dto.ActiveOrderDTO
import com.ducks.util.DucksBadRequestError
import io.ktor.server.application.*
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.koin.ktor.ext.inject

class ClientsOrdersRepository(
    application: Application,
) {
    private val calculateCoffeeShopsOrdersTimeService by application.inject<CalculateCoffeeShopsOrdersTimeService>()

    suspend fun getActiveOrder(userId: Long): ActiveOrderDTO {
        return newSuspendedTransaction {
            val activeOrder = CoffeeOrdersTable
                .join(
                    otherTable = CoffeeShopTable,
                    joinType = JoinType.LEFT,
                    onColumn = CoffeeShopTable.id,
                    otherColumn = CoffeeOrdersTable.coffeeShop
                )
                .select(CoffeeOrdersTable.columns + CoffeeShopTable.name)
                .where {
                    (CoffeeOrdersTable.userId eq userId) and
                            (CoffeeOrdersTable.finishedTime eq null)
                }
                .map {
                    ActiveOrderDTO(
                        id = it[CoffeeOrdersTable.id].value,
                        shopName = it[CoffeeShopTable.name],
                        isAccepted = it[CoffeeOrdersTable.acceptedTime] != null,
                        estimatedFinishTime = it[CoffeeOrdersTable.estimatedFinishTime]!!,
                        products = "",
                        price = it[CoffeeOrdersTable.price],
                    )
                }
                .firstOrNull()

            val products = CoffeeOrdersTable
                .join(
                    otherTable = CoffeeOrderedProductsTable,
                    joinType = JoinType.LEFT,
                    onColumn = CoffeeOrderedProductsTable.orderId,
                    otherColumn = CoffeeOrdersTable.id
                )
                .select(CoffeeOrderedProductsTable.productName)
                .joinToString {
                    it[CoffeeOrderedProductsTable.productName]
                }

            activeOrder?.copy(products = products) ?: throw DucksBadRequestError(message = "Активных заказов нет")
        }
    }

    suspend fun cancelOrder(orderId: Long, clientId: Long) {
        newSuspendedTransaction {
            val isOrderAccepted = CoffeeOrdersTable
                .selectAll()
                .where {
                    (CoffeeOrdersTable.id eq orderId) and
                            (CoffeeOrdersTable.userId eq clientId)
                }
                .map {
                    it[CoffeeOrdersTable.acceptedTime] != null
                }
                .firstOrNull()

            if (isOrderAccepted == null) {
                throw DucksBadRequestError("Попытка отменить невалидный заказ")
            } else if (isOrderAccepted) {
                throw DucksBadRequestError("Заказ уже принят!")
            } else {
                val currentTime = Clock.System.now().toEpochMilliseconds()

                CoffeeOrdersTable
                    .update(
                        where = {
                            (CoffeeOrdersTable.id eq orderId) and
                                    (CoffeeOrdersTable.userId eq clientId)
                        }
                    ) {
                        it[isCancelledByClient] = true
                        it[finishedTime] = currentTime
                    }
            }

            val coffeeShopId = CoffeeOrdersTable
                .select(CoffeeOrdersTable.coffeeShop)
                .where { CoffeeOrdersTable.id eq orderId }
                .map { it[CoffeeOrdersTable.coffeeShop].value }
                .first()

            calculateCoffeeShopsOrdersTimeService.invoke(coffeeShopId)
        }
    }
}