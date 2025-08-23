package com.ducks.features.coffeeshops.client.domain

import com.ducks.features.coffeeshops.client.routings.request.CreateOrderRequest
import com.ducks.features.orders.database.CoffeeOrdersTable
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class ClientOrdersRepository {

    suspend fun createOrder(request: CreateOrderRequest) {
        newSuspendedTransaction {
            val currentTime = System.currentTimeMillis()

            val estimatedTimeToFinish = 

            CoffeeOrdersTable.insertAndGetId {
                it[createdTime] = currentTime
                it[coffeeShop] = request.shopId
                it[userId] = request.clientId
                it[comment] = request.comment
            }
        }
    }
}