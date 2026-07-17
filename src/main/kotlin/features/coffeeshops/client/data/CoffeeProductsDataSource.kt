package com.ducks.features.coffeeshops.client.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.dto.ProductsByCategoryDTO
import com.ducks.features.coffeeshops.client.data.model.dto.ShopProductPair
import com.ducks.features.coffeeshops.database.*
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.database.mappers.mapToProductPreviewDTO
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryDTO
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.jdbc.select
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

    fun calculateMinutesToCook(productIds: List<Long>): Int {
        val quantityById = productIds.groupingBy { it }.eachCount()

        val rows = CoffeeProductTable
            .selectAll()
            .where { CoffeeProductTable.id inList productIds }
            .toList()

        val totalMinutes = rows.sumOf {
            val quantity = quantityById[it[CoffeeProductTable.id].value] ?: 1
            (it[CoffeeProductTable.minutesToCook] ?: 0) * quantity
        }
        return totalMinutes + 1
    }

    /**
     * Возвращает те пары (shopId, productId), которых нет в базе:
     * продукт не существует или не принадлежит указанной кофейне.
     */
    fun findMissingPairs(pairs: List<ShopProductPair>): List<ShopProductPair> {
        val distinctPairs = pairs.distinct()
        if (distinctPairs.isEmpty()) return emptyList()

        val productIds = distinctPairs.map { it.productId }.distinct()

        val existingPairs = CoffeeProductTable
            .select(CoffeeProductTable.id, CoffeeProductTable.shopId)
            .where { CoffeeProductTable.id inList productIds }
            .map { ShopProductPair(shopId = it[CoffeeProductTable.shopId].value, productId = it[CoffeeProductTable.id].value) }
            .toSet()

        return distinctPairs.filter { it !in existingPairs }
    }

    fun getClosestTimeToTakeOrder(shopId: Long): Long? {
        return CoffeeShopTable
            .select(CoffeeShopTable.closestTimeToTakeOrders)
            .where { CoffeeShopTable.id eq shopId }
            .map { it[CoffeeShopTable.closestTimeToTakeOrders] }
            .firstOrNull()
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