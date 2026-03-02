package com.ducks.features.coffeeshops.client.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopProductPreviewDTO
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeProductPreviewDTO
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.database.*
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryDTO
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.jdbc.selectAll

class CoffeeProductsDataSource {

    fun fetchByShop(shopId: Long): Map<CoffeeCategoryDTO, CoffeeShopProductPreviewDTO> {
        return CoffeeProductTable
            .join(CoffeeProductCategoryTable, joinType = JoinType.LEFT, CoffeeProductTable.categoryId, CoffeeProductCategoryTable.id)
            .selectAll()
            .where {
                CoffeeProductTable.shopId eq shopId
            }
            .associate {
                CoffeeCategoryDTO(
                    it[CoffeeProductCategoryTable.id].value,
                    it[CoffeeProductCategoryTable.name]
                ) to it.mapToCoffeeProductPreviewDTO()
            }
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