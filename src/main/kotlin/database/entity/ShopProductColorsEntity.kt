package com.ducks.database.entity

import com.ducks.database.table.ShopProductColorsTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ShopProductColorsEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ShopProductColorsEntity>(ShopProductColorsTable)

    var name by ShopProductColorsTable.name
}