package com.ducks.features.shops.database.entity

import com.ducks.features.shops.database.table.ShopProductSizeTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class ShopProductSizeEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ShopProductSizeEntity>(ShopProductSizeTable)

    var name by ShopProductSizeTable.name

    var parent by ShopProductSizeEntity optionalReferencedOn ShopProductSizeTable.parentId
    val children by ShopProductSizeEntity optionalReferrersOn ShopProductSizeTable.parentId

    val isParent by lazy { parent == null }
}