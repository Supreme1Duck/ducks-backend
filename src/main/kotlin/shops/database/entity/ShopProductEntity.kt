package com.ducks.shops.database.entity

import com.ducks.shops.database.table.ShopProductTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class ShopProductEntity(id: EntityID<Long>): LongEntity(id) {

    companion object : LongEntityClass<ShopProductEntity>(ShopProductTable)

    var name by ShopProductTable.name
    var description by ShopProductTable.description
    var price by ShopProductTable.price
    var imageUrls by ShopProductTable.imageUrls
    var brandName by ShopProductTable.brandName

    var shop by ShopEntity referencedOn ShopProductTable.shop
    var category by ShopProductCategoryEntity referencedOn ShopProductTable.category

    // Могут не быть
    var color by ShopProductColorsEntity optionalReferencedOn ShopProductTable.color
    var seasonId by ShopProductTable.seasonId
}