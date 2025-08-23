package com.ducks.features.coffeeshops.client.domain

import com.ducks.features.coffeeshops.client.data.CoffeeShopsDataSource
import com.ducks.features.coffeeshops.client.data.CoffeeProductsDataSource
import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeShopWithProductsDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopPreviewDTO
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