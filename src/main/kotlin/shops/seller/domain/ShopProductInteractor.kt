package com.ducks.shops.seller.domain

import com.ducks.common.data.UpdateMap
import com.ducks.shops.common.model.ShopProductModel
import com.ducks.shops.common.repository.ShopProductsRepository
import com.ducks.shops.common.repository.ShopProductsWithSizesRepository
import com.ducks.shops.seller.data.model.SELLER_PRODUCT_MAP_SIZE_KEY
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
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
        updateParametersMap: UpdateMap,
    ) {
        newSuspendedTransaction {
            shopProductsRepository.updateProduct(
                shopId = shopId,
                productId = productId,
                updateMap = updateParametersMap,
            )

            if (updateParametersMap.containsKey(SELLER_PRODUCT_MAP_SIZE_KEY)) {
                shopProductsWithSizesRepository.insert(
                    productId = productId,
                    sizeIds = Json.decodeFromJsonElement<List<Long>>(updateParametersMap.getValue(SELLER_PRODUCT_MAP_SIZE_KEY))
                )
            }
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