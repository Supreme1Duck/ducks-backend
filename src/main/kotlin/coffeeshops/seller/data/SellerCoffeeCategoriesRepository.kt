package com.ducks.coffeeshops.seller.data

import com.ducks.coffeeshops.database.CoffeeProductCategoryTable
import com.ducks.coffeeshops.database.mappers.mapToCategoryDTO
import com.ducks.coffeeshops.seller.data.model.CoffeeCategoryDTO
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