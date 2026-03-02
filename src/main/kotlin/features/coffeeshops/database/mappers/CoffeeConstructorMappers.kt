package com.ducks.features.coffeeshops.database.mappers

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeConstructorDTO
import com.ducks.features.coffeeshops.database.CoffeeConstructorCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeConstructorsTable
import com.ducks.features.coffeeshops.seller.data.model.SellerCoffeeConstructorCategoryDTO
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.mapToConstructorCategoryDTO(): SellerCoffeeConstructorCategoryDTO {
    return SellerCoffeeConstructorCategoryDTO(
        id = this[CoffeeConstructorCategoryTable.id].value,
        name = this[CoffeeConstructorCategoryTable.name],
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