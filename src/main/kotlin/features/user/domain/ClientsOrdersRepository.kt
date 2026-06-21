package com.ducks.features.user.domain

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.orders.service.CalculateCoffeeShopsOrdersTimeService
import com.ducks.features.user.data.dto.ActiveOrderDTO
import com.ducks.features.user.data.dto.ActiveOrderProductDTO
import com.ducks.features.user.data.dto.ClientOrderDTO
import com.ducks.features.user.data.dto.ClientOrderProductDTO
import com.ducks.features.user.data.dto.OrderStatus
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

    suspend fun getActiveOrder(userId: Long): ActiveOrderDTO? {
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
                        estimatedFinishTime = it[CoffeeOrdersTable.estimatedFinishTime] ?: 0L,
                        products = emptyList(),
                        price = it[CoffeeOrdersTable.totalPrice],
                        tips = it[CoffeeOrdersTable.tips],
                    )
                }
                .firstOrNull()

            if (activeOrder != null) {
                val products = CoffeeOrderedProductsTable
                    .select(
                        CoffeeOrderedProductsTable.productName,
                        CoffeeOrderedProductsTable.price,
                        CoffeeOrderedProductsTable.constructors,
                    )
                    .where { CoffeeOrderedProductsTable.orderId eq activeOrder.id }
                    .map {
                        ActiveOrderProductDTO(
                            name = it[CoffeeOrderedProductsTable.productName],
                            price = it[CoffeeOrderedProductsTable.price] ?: 0.toBigDecimal(),
                            constructors = it[CoffeeOrderedProductsTable.constructors]
                                ?.joinToString { constructor -> constructor.name }
                                ?: "",
                        )
                    }

                activeOrder.copy(products = products)
            } else {
                null
            }
        }
    }

    suspend fun getOrders(userId: Long): List<ClientOrderDTO> {
        return newSuspendedTransaction {
            val orders = CoffeeOrdersTable
                .join(CoffeeShopTable, joinType = JoinType.LEFT, CoffeeOrdersTable.coffeeShop, CoffeeShopTable.id)
                .select(CoffeeOrdersTable.columns + CoffeeShopTable.name)
                .where { CoffeeOrdersTable.userId eq userId }
                .map {
                    ClientOrderDTO(
                        id = it[CoffeeOrdersTable.id].value,
                        shopName = it[CoffeeShopTable.name],
                        finishedAt = it[CoffeeOrdersTable.estimatedFinishTime] ?: 0L,
                        products = emptyList(),
                        comment = it[CoffeeOrdersTable.comment],
                        status = when {
                            it[CoffeeOrdersTable.isCancelledByClient] -> OrderStatus.CANCELLED.value
                            it[CoffeeOrdersTable.finishedTime] != null -> OrderStatus.COMPLETED.value
                            else -> OrderStatus.IN_PROGRESS.value
                        },
                        price = it[CoffeeOrdersTable.totalPrice],
                    )
                }

            val products = CoffeeOrderedProductsTable
                .select(
                    CoffeeOrderedProductsTable.orderId,
                    CoffeeOrderedProductsTable.productId,
                    CoffeeOrderedProductsTable.productName,
                    CoffeeOrderedProductsTable.imageUrl,
                )
                .where { CoffeeOrderedProductsTable.orderId inList orders.map { it.id } }
                .groupBy { it[CoffeeOrderedProductsTable.orderId].value }

            orders.map { order ->
                order.copy(
                    products = products[order.id]?.map {
                        ClientOrderProductDTO(
                            id = it[CoffeeOrderedProductsTable.productId],
                            name = it[CoffeeOrderedProductsTable.productName],
                            imageUrl = it[CoffeeOrderedProductsTable.imageUrl],
                        )
                    } ?: emptyList()
                )
            }
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