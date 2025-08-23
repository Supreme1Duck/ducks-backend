package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.database.CoffeeProductCategoryTable
import com.ducks.features.coffeeshops.database.mappers.mapToCategoryDTO
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryDTO
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class SellerCoffeeCategoriesRepository {

    suspend fun getCategories(): List<CoffeeCategoryDTO> {
        return newSuspendedTransaction {
            CoffeeProductCategoryTable
                .selectAll()
                .map {
                    it.mapToCategoryDTO()
                }
        }
    }
}