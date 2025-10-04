package com.ducks.features.orders.service

import com.ducks.features.orders.database.CoffeeOrdersTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ActualizeOrdersService {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Закрывает активные заказы которые старше 3 минут
    operator fun invoke() {
        coroutineScope.launch {
            while (true) {
                delay(5_000L)
                newSuspendedTransaction {
                    val threeMinInMs = 3 * 60 * 1_000
                    val currentTime = System.currentTimeMillis()
                    val nonActualizedOrderTime = currentTime - threeMinInMs

                    CoffeeOrdersTable.update(
                        where = {
                            isNotAccepted(nonActualizedOrderTime)
                        }
                    ) {
                        it[finishedTime] = currentTime
                        it[isExpired] = true
                    }
                }
            }
        }
    }

    private fun isNotAccepted(
        nonActualizedOrderTime: Long,
    ): Op<Boolean> {
        return (CoffeeOrdersTable.createdTime less nonActualizedOrderTime) and
                (CoffeeOrdersTable.acceptedTime eq null) and
                (CoffeeOrdersTable.finishedTime eq null)
    }
}