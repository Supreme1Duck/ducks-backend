package com.ducks.features.shops.common.repository

import com.ducks.features.shops.database.table.ShopProductsWithSizesTable
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere

class ShopProductsWithSizesRepository {

    fun insert(
        productId: Long,
        sizeIds: List<Long>
    ) {
        ShopProductsWithSizesTable.batchInsert(sizeIds) {
            this[ShopProductsWithSizesTable.product] = productId
            this[ShopProductsWithSizesTable.size] = it
        }
    }

    fun update(
        productId: Long,
        sizeIds: List<Long>,
    ) {
        ShopProductsWithSizesTable
            .deleteWhere { product eq productId }

        insert(productId, sizeIds)
    }
}