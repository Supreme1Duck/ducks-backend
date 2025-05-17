package com.ducks.database.repository

import com.ducks.database.entity.ShopEntity
import com.ducks.database.table.ShopTable
import com.ducks.mapper.entityToModel
import com.ducks.dto.ShopDTO
import com.ducks.model.ShopModel
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ShopsRepository {

    /**
     * Здесь я в первый раз столкнулся с проблемой "ленивой инициализации".
     * Если обратиться к полю-ссылке вне транзакции - получим исключение. Теперь маппинг также внутри транзакции.
     */
    suspend fun getAll(productsLimit: Int? = null): List<ShopDTO> {
        return newSuspendedTransaction {
            val list = ShopEntity.all().toList()

             list.map {
                it.entityToModel(limit = productsLimit)
            }
        }
    }

    suspend fun getById(id: Long) : ShopEntity {
        return newSuspendedTransaction {
            ShopEntity[id]
        }
    }

    fun deleteAll() {
        transaction {
            ShopTable.deleteAll()
        }
    }

    fun insert(shop: ShopModel): ShopEntity {
        return transaction {
            ShopEntity.new {
                name = shop.name
                description = shop.description
                address = shop.address
                photoUrls = shop.photoUrls
            }
        }
    }
}