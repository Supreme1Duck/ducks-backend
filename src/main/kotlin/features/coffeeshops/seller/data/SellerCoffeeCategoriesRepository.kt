package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.database.CoffeeProductCategoryTable
import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.mappers.mapToCategoryDTO
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryDTO
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryWithCountDTO
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class SellerCoffeeCategoriesRepository {

    suspend fun getCategories(): List<CoffeeCategoryDTO> {
        return newSuspendedTransaction {
            CoffeeProductCategoryTable
                .selectAll()
                .map { it.mapToCategoryDTO() }
        }
    }

    suspend fun getCategoriesWithProductCount(shopId: Long): List<CoffeeCategoryWithCountDTO> {
        return newSuspendedTransaction {
            CoffeeProductCategoryTable
                .join(
                    otherTable = CoffeeProductTable,
                    joinType = JoinType.LEFT,
                    onColumn = CoffeeProductCategoryTable.id,
                    otherColumn = CoffeeProductTable.categoryId,
                    additionalConstraint = { CoffeeProductTable.shopId eq shopId }
                )
                .select(
                    CoffeeProductCategoryTable.id,
                    CoffeeProductCategoryTable.name,
                    CoffeeProductTable.id.count()
                )
                .groupBy(CoffeeProductCategoryTable.id, CoffeeProductCategoryTable.name)
                .map {
                    CoffeeCategoryWithCountDTO(
                        id = it[CoffeeProductCategoryTable.id].value,
                        name = it[CoffeeProductCategoryTable.name],
                        productCount = it[CoffeeProductTable.id.count()],
                    )
                }
        }
    }
}