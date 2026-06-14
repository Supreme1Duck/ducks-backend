package com.ducks.features.coffeeshops.service

import com.ducks.features.coffeeshops.seller.data.SellerCoffeeShopsDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ActualizeCoffeeShopsVisibilityService(
    private val sellerCoffeeShopsDataSource: SellerCoffeeShopsDataSource,
) {

    private val scope = CoroutineScope(Dispatchers.Default)

    // Актуализирует скрытые кофешопы
    fun initialize() {
        scope.launch {
            while (true) {
                actualize()
                delay(15 * 60 * 1000L)
            }
        }
    }

    private fun actualize() {
        transaction {
            sellerCoffeeShopsDataSource.fetchHiddenShopIds().forEach { shopId ->
                sellerCoffeeShopsDataSource.tryShowShop(shopId)
            }
        }
    }
}
