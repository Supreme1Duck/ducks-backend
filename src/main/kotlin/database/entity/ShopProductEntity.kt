package com.ducks.database.entity

import com.ducks.database.table.ShopProductTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ShopProductEntity(id: EntityID<Long>): LongEntity(id) {

    companion object : LongEntityClass<ShopProductEntity>(ShopProductTable)

    var name by ShopProductTable.name
    var description by ShopProductTable.description
    var price by ShopProductTable.price
    var imageUrls by ShopProductTable.imageUrls
    var brandName by ShopProductTable.brandName

    var shop by ShopEntity referencedOn ShopProductTable.shop
    var category by ShopProductCategoryEntity referencedOn ShopProductTable.category
}