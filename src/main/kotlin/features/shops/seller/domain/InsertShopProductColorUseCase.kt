package com.ducks.features.shops.seller.domain

import com.ducks.features.shops.database.entity.ShopProductColorsEntity
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class InsertShopProductColorUseCase {

    suspend operator fun invoke(colorName: String) {
        newSuspendedTransaction {
            ShopProductColorsEntity.new {
                name = colorName
            }
        }
    }
}