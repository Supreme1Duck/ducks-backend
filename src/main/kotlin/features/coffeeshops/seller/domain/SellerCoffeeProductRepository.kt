package com.ducks.features.coffeeshops.seller.domain

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.mappers.mapToSellerCoffeeProductPreviewDTO
import com.ducks.features.coffeeshops.seller.data.SellerCoffeeProductDataSource
import com.ducks.features.coffeeshops.seller.routings.request.products.CreateCoffeeProductRequest
import com.ducks.features.coffeeshops.seller.routings.request.products.UpdateCoffeeProductRequest
import features.coffeeshops.seller.data.model.CoffeeShopProductSellerPreviewDTO
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class SellerCoffeeProductRepository(
    private val dataSource: SellerCoffeeProductDataSource,
    private val imageRepository: CoffeeShopImageRepository,
) {

    suspend fun fetchProductsByShop(shopId: Long): List<CoffeeShopProductSellerPreviewDTO> {
        return newSuspendedTransaction {
            CoffeeProductTable
                .selectAll()
                .where {
                    CoffeeProductTable.shopId eq shopId
                }
                .map {
                    it.mapToSellerCoffeeProductPreviewDTO()
                }
        }
    }

    suspend fun getProductDetails(productId: Long): CoffeeProductWithDetailsDTO {
        return dataSource.getProductDetails(productId)
    }

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
            )
        }
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