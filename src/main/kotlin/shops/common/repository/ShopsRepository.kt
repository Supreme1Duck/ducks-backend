package com.ducks.shops.common.repository

import com.ducks.shops.common.data.ShopDataSource
import com.ducks.shops.database.entity.ShopEntity
import com.ducks.shops.database.table.ShopTable
import com.ducks.shops.common.dto.ShopDTO
import com.ducks.shops.common.dto.ShopPreviewDTO
import com.ducks.shops.common.model.ShopModel
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class ShopsRepository(
    private val shopsDataSource: ShopDataSource,
) {

    /**
     * Здесь я в первый раз столкнулся с проблемой "ленивой инициализации".
     * Если обратиться к полю-ссылке exposed вне транзакции - получим исключение. Теперь маппинг также внутри транзакции.
     */
    suspend fun getAll(
        lastId: Long?,
        limit: Int?,
        productsPreviewLimit: Int?,
    ): List<ShopPreviewDTO> {
        return newSuspendedTransaction {
            shopsDataSource.getAllShops(
                lastId = lastId,
                limit = limit,
                productsPreviewLimit = productsPreviewLimit,
            )
        }
    }

    suspend fun getById(id: Long, productsPreviewLimit: Int?): ShopDTO {
        return newSuspendedTransaction {
            shopsDataSource.getById(id, productsPreviewLimit)
        }
    }

    suspend fun searchShops(query: String, lastId: Long?, limit: Int?, productsPreviewLimit: Int?): List<ShopPreviewDTO> {
        return newSuspendedTransaction {
            shopsDataSource.search(query, lastId, limit, productsPreviewLimit)
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

    suspend fun exists(shopId: Long): Boolean {
        return newSuspendedTransaction {
            try {
                ShopEntity[shopId]
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}