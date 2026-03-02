package com.ducks.features.shops.seller.domain

import com.ducks.features.shops.common.model.ShopProductModel
import com.ducks.features.shops.common.repository.ShopProductsRepository
import com.ducks.features.shops.common.repository.ShopProductsWithSizesRepository
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class ShopProductInteractor(
    private val shopProductsRepository: ShopProductsRepository,
    private val shopProductsWithSizesRepository: ShopProductsWithSizesRepository,
) {

    suspend fun insert(
        shopId: Long,
        sizeIds: List<Long>,
        categoryId: Long,
        productModel: ShopProductModel
    ) {
        newSuspendedTransaction {
            val insertedProduct = shopProductsRepository.insertProduct(
                shopId = shopId,
                categoryId = categoryId,
                seasonModel = null,
                shopProductModel = productModel,
            )

            shopProductsWithSizesRepository.insert(
                productId = insertedProduct,
                sizeIds = sizeIds,
            )
        }
    }

    suspend fun update(
        shopId: Long,
        productId: Long,
    ) {
        newSuspendedTransaction {
            shopProductsRepository.updateProduct(
                shopId = shopId,
                productId = productId,
            )
        }
    }

    suspend fun delete(productId: Long, shopId: Long) {
        newSuspendedTransaction {
            shopProductsRepository.deleteProduct(
                shopId = shopId,
                productId = productId
            )
        }
    }
}