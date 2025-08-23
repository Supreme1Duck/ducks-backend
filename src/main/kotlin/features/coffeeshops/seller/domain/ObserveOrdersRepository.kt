package com.ducks.features.coffeeshops.seller.domain

import com.ducks.features.orders.data.dto.CurrentOrdersDTO
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.orders.database.mapToOrderDTO
import com.ducks.features.orders.database.toOrderProductDTO
import com.ducks.features.user.database.UserTable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class ObserveOrdersRepository {

    suspend fun getCurrentOrders(shopId: Long): CurrentOrdersDTO {
        return newSuspendedTransaction {
            val orders = CoffeeOrdersTable
                .join(
                    UserTable,
                    joinType = JoinType.LEFT,
                    CoffeeOrdersTable.userId,
                    UserTable.id,
                )
                .selectAll()
                .where {
                    (isActive() or isPending()) and (CoffeeOrdersTable.coffeeShop eq shopId)
                }
                .map {
                    it.mapToOrderDTO(emptyList())
                }

            val products = CoffeeOrderedProductsTable
                .selectAll()
                .where { CoffeeOrderedProductsTable.orderId inList orders.map { it.id } }
                .map { it.toOrderProductDTO() }

            val ordersWithProducts = orders.map { order ->
                order.copy(
                    products = products.filter {
                        it.orderId == order.id
                    }
                )
            }

            val (activeOrders, pendingOrders) = ordersWithProducts.partition {
                it.isActive
            }

            CurrentOrdersDTO(
                activeOrdersDTO = activeOrders,
                pendingOrders = pendingOrders,
            )
        }
    }

    private fun isActive(): Op<Boolean> {
        return (CoffeeOrdersTable.acceptedTime.isNotNull() and CoffeeOrdersTable.finishedTime.isNull())
    }

    private fun isPending(): Op<Boolean> {
        return (CoffeeOrdersTable.acceptedTime.isNull()
                and CoffeeOrdersTable.finishedTime.isNull()
                and CoffeeOrdersTable.cancelledByClientTime.isNull()
                and CoffeeOrdersTable.cancelledBySellerTime.isNull())
    }
}