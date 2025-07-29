package com.ducks.shops.database.entity

import com.ducks.shops.database.table.ShopProductsWithSizesTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class ShopProductsWithSizesEntity(id: EntityID<Long>): LongEntity(id) {

    companion object : LongEntityClass<ShopProductsWithSizesEntity>(ShopProductsWithSizesTable)

    var product by ShopProductEntity referencedOn ShopProductsWithSizesTable.product
    var size by ShopProductSizeEntity referencedOn ShopProductsWithSizesTable.size
}