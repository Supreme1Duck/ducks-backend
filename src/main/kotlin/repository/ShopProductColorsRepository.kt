package com.ducks.repository

import com.ducks.database.entity.ShopProductColorsEntity
import com.ducks.model.ShopProductColorModel
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

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