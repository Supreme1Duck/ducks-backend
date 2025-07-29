package com.ducks.shops.common.repository

import com.ducks.shops.database.entity.ShopProductColorsEntity
import com.ducks.shops.common.model.ShopProductColorModel
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class ShopProductColorsRepository {

    suspend fun insertColor(colorName: String) {
        newSuspendedTransaction {
            ShopProductColorsEntity.new {
                name = colorName
            }
        }
    }

    suspend fun getAll(): List<ShopProductColorModel> {
        return newSuspendedTransaction {
            ShopProductColorsEntity.all().toList()
                .map {
                    ShopProductColorModel(
                        it.id.value,
                        it.name
                    )
                }
        }
    }
}