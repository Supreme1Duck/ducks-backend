package com.ducks.coffeeshops.common.domain

import com.ducks.coffeeshops.common.data.CoffeeShopsDataSource
import com.ducks.coffeeshops.common.data.CoffeeProductsDataSource
import com.ducks.coffeeshops.common.data.model.dto.CoffeeShopWithProductsDTO
import com.ducks.coffeeshops.common.data.model.preview.CoffeeShopPreviewDTO
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class CoffeeShopsRepository(
    private val shopsDataSource: CoffeeShopsDataSource,
    private val productDataSource: CoffeeProductsDataSource,
) {

    suspend fun exists(shopId: Long): Boolean {
        return newSuspendedTransaction {
            shopsDataSource.exists(shopId)
        }
    }

    suspend fun getShop(shopId: Long): CoffeeShopWithProductsDTO {
        return newSuspendedTransaction {
            val shop = shopsDataSource.getShopDetails(shopId)

            val productsWithCategories = productDataSource.fetchByShop(shopId)

            CoffeeShopWithProductsDTO(
                shop = shop,
                products = productsWithCategories
            )
        }
    }

    suspend fun getShopsList(lastId: Long?, limit: Int?): List<CoffeeShopPreviewDTO> {
        return newSuspendedTransaction {
            shopsDataSource.getAllShops(lastId, limit)
        }
    }
}