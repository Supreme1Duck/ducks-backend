package com.ducks.database.entity

import com.ducks.database.table.ShopProductTable
import com.ducks.database.table.ShopTable
import com.ducks.util.optionalLimit
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ShopEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ShopEntity>(ShopTable)

    var name by ShopTable.name
    var description by ShopTable.description
    var address by ShopTable.address
    var photoUrls by ShopTable.photoUrls

    fun getProducts(limit: Int? = null): List<ShopProductEntity> {
        return ShopProductEntity.find { ShopProductTable.shop eq this@ShopEntity.id }
            .optionalLimit(limit)
            .toList()
    }
}