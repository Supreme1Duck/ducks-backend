package com.ducks.features.coffeeshops.seller.domain

import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.seller.data.SellerCoffeeProductDataSource
import com.ducks.features.coffeeshops.seller.data.UPDATE_MAP_COFFEE_PRODUCT_IMAGE
import com.ducks.features.coffeeshops.seller.routings.request.products.CreateCoffeeProductRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.UpdateCoffeeProductRequest
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class SellerCoffeeProductRepository(
    private val dataSource: SellerCoffeeProductDataSource,
    private val imageRepository: CoffeeShopImageRepository,
) {

    suspend fun insert(
        shopId: Long,
        data: CreateCoffeeProductRequest,
    ) {
        dataSource.insertProduct(
            shopId = shopId,
            productRequest = data,
        )
    }

    suspend fun update(
        shopId: Long,
        data: UpdateCoffeeProductRequest,
    ) {
        return newSuspendedTransaction {
            dataSource.updateProduct(
                shopId = shopId,
                productId = data.productId,
                updateMap = data.updateMap
            )

            if (data.updateMap.containsKey(UPDATE_MAP_COFFEE_PRODUCT_IMAGE)) {
                deleteUnusedImage(shopId, data.productId)
            }
        }
    }

    private fun deleteUnusedImage(shopId: Long, productId: Long) {
        val imageUrl = CoffeeProductTable
            .select(CoffeeProductTable.imageUrl)
            .where {
                (CoffeeProductTable.shopId eq shopId) and (CoffeeProductTable.id eq productId)
            }
            .map {
                it[CoffeeProductTable.imageUrl]
            }
            .first()

        imageRepository.deleteProductImage(
            imageUrl = imageUrl,
        )
    }

    suspend fun delete(
        shopId: Long,
        productId: Long,
    ) {
        return newSuspendedTransaction {
            dataSource.deleteProduct(shopId, productId)
        }
    }
}