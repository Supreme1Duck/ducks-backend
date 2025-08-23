package com.ducks.features.coffeeshops.client.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopProductPreviewDTO
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeProductPreviewDTO
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.database.*
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.jdbc.selectAll

class CoffeeProductsDataSource {

    fun fetchByShop(shopId: Long): Map<String, CoffeeShopProductPreviewDTO> {
        return CoffeeProductTable
            .join(CoffeeProductCategoryTable, joinType = JoinType.LEFT, CoffeeProductTable.categoryId, CoffeeProductCategoryTable.id)
            .selectAll()
            .where {
                CoffeeProductTable.shopId eq shopId
            }
            .associate {
                it[CoffeeProductCategoryTable.name] to it.mapToCoffeeProductPreviewDTO()
            }
    }

    fun getProductDetails(productId: Long): CoffeeProductWithDetailsDTO {
        return CoffeeProductTable
            .join(CoffeeProductsWithConstructorsTable, joinType = JoinType.LEFT, CoffeeProductsWithConstructorsTable.id, CoffeeProductTable.constructorsId)
            .join(CoffeeConstructorsTable, joinType = JoinType.LEFT, CoffeeConstructorsTable.id, CoffeeProductsWithConstructorsTable.constructor)
            .join(CoffeeConstructorCategoryTable, joinType = JoinType.LEFT, CoffeeConstructorsTable.categoryId, CoffeeConstructorCategoryTable.id)
            .selectAll()
            .groupBy(CoffeeProductTable.id)
            .where {
                CoffeeProductTable.id eq productId
            }
            .map {
                it.mapToCoffeeProductWithDetailsDTO()
            }
            .first()
    }
}