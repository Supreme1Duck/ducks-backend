package com.ducks.features.orders.service

import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.service.MinuteChangeNotifierService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.neq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ActualizeOrdersService(
    private val changeNotifierService: MinuteChangeNotifierService,
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // Закрывает активные заказы которые старше 7 минут
    operator fun invoke() {
        changeNotifierService.observe()
            .onEach {
                newSuspendedTransaction {
                    val sevenMinInMs = 7 * 60_000
                    val currentTime = Clock.System.now().toEpochMilliseconds()
                    val nonActualizedOrderTime = currentTime - sevenMinInMs

                    CoffeeOrdersTable.update(
                        where = {
                            isNotAccepted(nonActualizedOrderTime)
                        }
                    ) {
                        it[finishedTime] = currentTime
                        it[isExpired] = true
                    }

                    CoffeeOrdersTable.update(
                        where = {
                            isAcceptedNotFinished(currentTime)
                        }
                    ) {
                        it[estimatedFinishTime] = currentTime
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun isNotAccepted(
        nonActualizedOrderTime: Long,
    ): Op<Boolean> {
        return (CoffeeOrdersTable.createdTime less nonActualizedOrderTime) and
                (CoffeeOrdersTable.acceptedTime eq null) and
                (CoffeeOrdersTable.finishedTime eq null)
    }

    private fun isAcceptedNotFinished(currentTime: Long): Op<Boolean> {
        return (CoffeeOrdersTable.acceptedTime neq null) and
                (CoffeeOrdersTable.finishedTime eq null) and
                (CoffeeOrdersTable.estimatedFinishTime less currentTime)
    }
}