package com.ducks.domain

import com.ducks.database.entity.ShopProductColorsEntity
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class InsertShopProductColorUseCase {

    suspend operator fun invoke(colorName: String) {
        newSuspendedTransaction {
            ShopProductColorsEntity.new {
                name = colorName
            }
        }
    }
}