package com.ducks.features.orders.service

import com.ducks.features.coffeeshops.database.CoffeeShopTechnicalPausesTable
import com.ducks.service.MinuteChangeNotifierService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ActualizeTechnicalPausesService(
    private val changeNotifierService: MinuteChangeNotifierService
) {

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // Закрывает неактивные тех паузы
    operator fun invoke() {
        changeNotifierService.observe()
            .onEach {
                newSuspendedTransaction {
                    val currentTime = it

                    CoffeeShopTechnicalPausesTable.update(
                        where = {
                            (CoffeeShopTechnicalPausesTable.isActive eq true) and
                                    (CoffeeShopTechnicalPausesTable.endsAt less currentTime)
                        }
                    ) {
                        it[isActive] = false
                    }
                }
            }
            .launchIn(coroutineScope)
    }
}