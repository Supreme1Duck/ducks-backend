package com.ducks.shops.common.database.entity

import com.ducks.shops.common.database.table.ShopProductCategoryTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class ShopProductCategoryEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ShopProductCategoryEntity>(ShopProductCategoryTable)

    var name by ShopProductCategoryTable.name
    var description by ShopProductCategoryTable.description

    var isSuperCategory by ShopProductCategoryTable.isSuperCategory
    var superCategory by ShopProductCategoryEntity optionalReferencedOn ShopProductCategoryTable.superCategoryId // Top-level категории

    var parent by ShopProductCategoryEntity optionalReferencedOn ShopProductCategoryTable.parent // Родительская категория
    val children by ShopProductCategoryEntity optionalReferrersOn ShopProductCategoryTable.parent // Дочерние категории
}