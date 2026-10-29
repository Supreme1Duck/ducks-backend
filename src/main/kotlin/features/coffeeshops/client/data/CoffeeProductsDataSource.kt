package com.ducks.features.coffeeshops.client.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.dto.ProductsByCategoryDTO
import com.ducks.features.coffeeshops.client.data.model.dto.CheckProductsExistenceResponse
import com.ducks.features.coffeeshops.client.data.model.dto.ProductSnapshotDTO
import com.ducks.features.coffeeshops.client.data.model.dto.ShopProductPair
import com.ducks.features.coffeeshops.database.*
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.database.mappers.mapToProductPreviewDTO
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryDTO
import com.ducks.features.coffeeshops.service.CookingTimeCalculator
import com.ducks.features.coffeeshops.service.fetchCookingMode
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class CoffeeProductsDataSource(
    private val cookingTimeCalculator: CookingTimeCalculator = CookingTimeCalculator(),
) {

    fun fetchByShop(shopId: Long): List<ProductsByCategoryDTO> {
        return CoffeeProductTable
            .join(CoffeeProductCategoryTable, joinType = JoinType.LEFT, CoffeeProductTable.categoryId, CoffeeProductCategoryTable.id)
            .join(CoffeeShopTable, joinType = JoinType.INNER, CoffeeProductTable.shopId, CoffeeShopTable.id)
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

    fun calculateMinutesToCook(shopId: Long, productIds: List<Long>): Int {
        val quantityById = productIds.groupingBy { it }.eachCount()

        val items = CoffeeProductTable
            .selectAll()
            .where { CoffeeProductTable.id inList productIds }
            .map {
                CookingTimeCalculator.CookingItem(
                    minutesToCook = it[CoffeeProductTable.minutesToCook] ?: 0,
                    cooksInParallel = it[CoffeeProductTable.cooksInParallel],
                    quantity = quantityById[it[CoffeeProductTable.id].value] ?: 1,
                )
            }

        return cookingTimeCalculator.minutesToCook(items, fetchCookingMode(shopId))
    }

    /**
     * Сверяет пары (shopId, productId) с базой: в `missing` — те, которых нет
     * (продукт не существует или не принадлежит указанной кофейне),
     * в `products` — актуальные данные остальных.
     */
    fun checkExistence(pairs: List<ShopProductPair>): CheckProductsExistenceResponse {
        val distinctPairs = pairs.distinct()
        if (distinctPairs.isEmpty()) return CheckProductsExistenceResponse(missing = emptyList())

        val productIds = distinctPairs.map { it.productId }.distinct()
        val requested = distinctPairs.toSet()

        val products = CoffeeProductTable
            .join(CoffeeShopTable, joinType = JoinType.INNER, CoffeeProductTable.shopId, CoffeeShopTable.id)
            .select(
                CoffeeProductTable.id,
                CoffeeProductTable.shopId,
                CoffeeProductTable.name,
                CoffeeProductTable.imageUrl,
                CoffeeProductTable.sizes,
                CoffeeShopTable.name,
            )
            .where { CoffeeProductTable.id inList productIds }
            .map {
                ProductSnapshotDTO(
                    shopId = it[CoffeeProductTable.shopId].value,
                    productId = it[CoffeeProductTable.id].value,
                    name = it[CoffeeProductTable.name],
                    imageUrl = it[CoffeeProductTable.imageUrl],
                    shopName = it[CoffeeShopTable.name],
                    price = it[CoffeeProductTable.sizes].firstOrNull()?.price,
                )
            }
            .filter { ShopProductPair(shopId = it.shopId, productId = it.productId) in requested }

        val existingPairs = products.map { ShopProductPair(shopId = it.shopId, productId = it.productId) }.toSet()

        return CheckProductsExistenceResponse(
            missing = distinctPairs.filter { it !in existingPairs },
            products = products,
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