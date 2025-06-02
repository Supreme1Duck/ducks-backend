package com.ducks.database.entity

import com.ducks.database.table.ShopProductsWithSizesTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ShopProductsWithSizesEntity(id: EntityID<Long>): LongEntity(id) {

    companion object : LongEntityClass<ShopProductsWithSizesEntity>(ShopProductsWithSizesTable)

    var product by ShopProductEntity referencedOn ShopProductsWithSizesTable.product
    var size by ShopProductSizeEntity referencedOn ShopProductsWithSizesTable.size
}