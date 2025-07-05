package com.ducks.shops.seller.domain

import com.ducks.shops.common.model.ShopProductModel
import com.ducks.shops.common.repository.ShopProductsRepository
import com.ducks.shops.common.repository.ShopProductsWithSizesRepository
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class InsertOrUpdateShopProductUseCase(
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
}