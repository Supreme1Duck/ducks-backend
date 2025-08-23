package com.ducks.features.shops.common.data

import com.ducks.features.shops.database.entity.ShopEntity
import com.ducks.features.shops.database.rowmappers.mapToShopPreviewDTO
import com.ducks.features.shops.database.table.ShopTable
import com.ducks.features.shops.common.dto.ShopDTO
import com.ducks.features.shops.common.dto.ShopPreviewDTO
import com.ducks.features.shops.common.mapper.entityToDto
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.like
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.selectAll

class ShopDataSource(
    private val productsDataSource: ShopProductsDataSource
) {

    fun getAllShops(
        lastId: Long?,
        limit: Int?,
        productsPreviewLimit: Int?,
    ): List<ShopPreviewDTO> {
        return ShopTable
            .selectAll()
            .where(ShopTable.id less (lastId ?: Long.MAX_VALUE))
            .orderBy(ShopTable.id, SortOrder.DESC)
            .limit(limit ?: Int.MAX_VALUE)
            .map {
                val products = productsDataSource.getProductsPreviewByShop(it[ShopTable.id].value, null, productsPreviewLimit)
                it.mapToShopPreviewDTO(products)
            }
    }

    fun search(
        query: String,
        lastId: Long?,
        limit: Int?,
        productsPreviewLimit: Int?,
    ): List<ShopPreviewDTO> {
        return ShopTable
            .selectAll()
            .where(
                ((ShopTable.name like "%$query%") or (ShopTable.address like "%$query%")) and
                        (lastId?.let { ShopTable.id less lastId } ?: Op.TRUE)
            )
            .orderBy(ShopTable.id, SortOrder.DESC)
            .also { q ->
                limit?.let {
                    q.limit(it)
                }
            }
            .map {
                val products = productsDataSource.getProductsPreviewByShop(shopId = it[ShopTable.id].value, null, productsPreviewLimit)

                it.mapToShopPreviewDTO(
                    products = products,
                )
            }
    }

    fun getById(
        id: Long,
        productsPreviewLimit: Int?,
    ): ShopDTO {
        val products = productsDataSource.requestProductsFromDB(shopId = id, lastId = null, limit = productsPreviewLimit)
        val filters = productsDataSource.requestAvailableFiltersFromDB(productIds = products.map { it.id }, shopId = id)

        return ShopEntity[id]
            .entityToDto(products, filters)
    }
}