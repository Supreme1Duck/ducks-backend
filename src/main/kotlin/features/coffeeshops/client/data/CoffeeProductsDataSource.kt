package com.ducks.features.coffeeshops.client.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CookingTimeEstimateDTO
import com.ducks.features.coffeeshops.client.data.model.dto.ProductsByCategoryDTO
import com.ducks.features.coffeeshops.database.*
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.database.mappers.mapToProductPreviewDTO
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryDTO
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.jdbc.selectAll

class CoffeeProductsDataSource {

    fun fetchByShop(shopId: Long): List<ProductsByCategoryDTO> {
        return CoffeeProductTable
            .join(CoffeeProductCategoryTable, joinType = JoinType.LEFT, CoffeeProductTable.categoryId, CoffeeProductCategoryTable.id)
            .join(CoffeeProductsWithConstructorsTable, joinType = JoinType.LEFT, CoffeeProductsWithConstructorsTable.product, CoffeeProductTable.id)
            .join(CoffeeConstructorsTable, joinType = JoinType.LEFT, CoffeeConstructorsTable.id, CoffeeProductsWithConstructorsTable.constructor)
            .join(CoffeeModifiedConstructorCategoryTable, joinType = JoinType.LEFT, CoffeeProductsWithConstructorsTable.modifiedCategory, CoffeeModifiedConstructorCategoryTable.id)
            .join(CoffeeConstructorCategoryTable, joinType = JoinType.LEFT, CoffeeModifiedConstructorCategoryTable.categoryId, CoffeeConstructorCategoryTable.id)
            .selectAll()
            .where { CoffeeProductTable.shopId eq shopId }
            .groupBy { CoffeeCategoryDTO(it[CoffeeProductCategoryTable.id].value, it[CoffeeProductCategoryTable.name]) }
            .map { (category, categoryRows) ->
                ProductsByCategoryDTO(
                    category = category,
                    products = categoryRows
                        .groupBy { it[CoffeeProductTable.id].value }
                        .map { (_, productRows) -> productRows.mapToProductPreviewDTO() },
                )
            }
    }

    fun estimateCookingTime(productIds: List<Long>): CookingTimeEstimateDTO {
        val quantityById = productIds.groupingBy { it }.eachCount()

        val rows = CoffeeProductTable
            .selectAll()
            .where { CoffeeProductTable.id inList productIds }
            .toList()

        val totalMinutes = rows.sumOf {
            val quantity = quantityById[it[CoffeeProductTable.id].value] ?: 1
            (it[CoffeeProductTable.minutesToCook] ?: 0) * quantity
        }
        return CookingTimeEstimateDTO(
            minutesToCook = totalMinutes + 1,
        )
    }

    fun getProductDetails(productId: Long): CoffeeProductWithDetailsDTO {
        return CoffeeProductTable
            .join(CoffeeProductsWithConstructorsTable, joinType = JoinType.LEFT, CoffeeProductsWithConstructorsTable.product, CoffeeProductTable.id)
            .join(CoffeeConstructorsTable, joinType = JoinType.LEFT, CoffeeConstructorsTable.id, CoffeeProductsWithConstructorsTable.constructor)
            // Категория с модификациями
            .join(CoffeeModifiedConstructorCategoryTable, joinType = JoinType.LEFT, CoffeeProductsWithConstructorsTable.modifiedCategory, CoffeeModifiedConstructorCategoryTable.categoryId)
            // Категория
            .join(CoffeeConstructorCategoryTable, joinType = JoinType.LEFT, CoffeeModifiedConstructorCategoryTable.categoryId, CoffeeConstructorCategoryTable.id)
            .selectAll()
            .where {
                CoffeeProductTable.id eq productId
            }
            .toList()
            .mapToCoffeeProductWithDetailsDTO()
    }
}