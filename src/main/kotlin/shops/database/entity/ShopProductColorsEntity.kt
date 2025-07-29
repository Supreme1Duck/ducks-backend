package com.ducks.shops.database.entity

import com.ducks.shops.database.table.ShopProductColorsTable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class ShopProductColorsEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ShopProductColorsEntity>(ShopProductColorsTable)

    var name by ShopProductColorsTable.name
}