package com.ducks.database.entity

import com.ducks.database.table.ShopProductCategoryTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ShopProductCategoryEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ShopProductCategoryEntity>(ShopProductCategoryTable)

    var name by ShopProductCategoryTable.name
    var description by ShopProductCategoryTable.description

    var isSuperCategory by ShopProductCategoryTable.isSuperCategory
    var superCategory by ShopProductCategoryEntity optionalReferencedOn ShopProductCategoryTable.superCategoryId // Top-level категории

    var parent by ShopProductCategoryEntity optionalReferencedOn ShopProductCategoryTable.parent // Родительская категория
    val children by ShopProductCategoryEntity optionalReferrersOn ShopProductCategoryTable.parent // Дочерние категории
}