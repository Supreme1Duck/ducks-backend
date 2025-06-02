package com.ducks.database.entity

import com.ducks.database.table.ShopProductSizeTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ShopProductSizeEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ShopProductSizeEntity>(ShopProductSizeTable)

    var name by ShopProductSizeTable.name

    var parent by ShopProductSizeEntity optionalReferencedOn ShopProductSizeTable.parentId
    val children by ShopProductSizeEntity optionalReferrersOn ShopProductSizeTable.parentId

    val isParent by lazy { parent == null }
}