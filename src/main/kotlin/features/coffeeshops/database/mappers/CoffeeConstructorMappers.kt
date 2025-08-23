package com.ducks.features.coffeeshops.database.mappers

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeConstructorCategoryDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeConstructorDTO
import com.ducks.features.coffeeshops.database.CoffeeConstructorCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeConstructorsTable
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.mapToConstructorCategoryDTO(): CoffeeConstructorCategoryDTO {
    return CoffeeConstructorCategoryDTO(
        id = this[CoffeeConstructorCategoryTable.id].value,
        name = this[CoffeeConstructorCategoryTable.name],
        defaultConstructorId = this[CoffeeConstructorCategoryTable.defaultConstructorId],
        maxSelection = this[CoffeeConstructorCategoryTable.maxSelection],
    )
}

fun ResultRow.mapToConstructorDTO(): CoffeeConstructorDTO {
    return CoffeeConstructorDTO(
        id = this[CoffeeConstructorsTable.id].value,
        name = this[CoffeeConstructorsTable.name],
        categoryId = this[CoffeeConstructorsTable.categoryId].value,
        price = this[CoffeeConstructorsTable.price],
        isInStock = this[CoffeeConstructorsTable.isInStock]
    )
}