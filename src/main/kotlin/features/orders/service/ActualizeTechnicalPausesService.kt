package com.ducks.features.orders.service

import com.ducks.features.coffeeshops.database.CoffeeShopTechnicalPausesTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

class ActualizeTechnicalPausesService {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    // Закрывает неактивные тех паузы
    operator fun invoke() {
        coroutineScope.launch {
            while (true) {
                delay(5_000L)
                newSuspendedTransaction {
                    val currentTime = System.currentTimeMillis()

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
        }
    }
}