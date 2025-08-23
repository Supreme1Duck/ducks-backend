package com.ducks.features.shops.database.entity

import com.ducks.features.shops.database.table.ShopTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class ShopEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ShopEntity>(ShopTable)

    var name by ShopTable.name
    var description by ShopTable.description
    var address by ShopTable.address
    var photoUrls by ShopTable.photoUrls
}