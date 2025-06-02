package com.ducks.domain

import com.ducks.database.entity.ShopEntity
import com.ducks.database.entity.ShopProductCategoryEntity
import com.ducks.database.entity.ShopProductSizeEntity
import com.ducks.repository.ShopProductsRepository
import com.ducks.repository.ShopProductsWithSizesRepository
import com.ducks.model.ShopProductModel
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class InsertShopProductUseCase(
    private val shopProductsRepository: ShopProductsRepository,
    private val shopProductsWithSizesRepository: ShopProductsWithSizesRepository,
) {

    suspend operator fun invoke(
        shopId: Long,
        sizeIds: List<Long>,
        categoryId: Long,
        productModel: ShopProductModel
    ) {
        newSuspendedTransaction {
            val shopEntity = ShopEntity[shopId]
            val categoryEntity = ShopProductCategoryEntity[categoryId]
            val sizes = sizeIds.map {
                ShopProductSizeEntity[it]
            }

            val insertedProduct = shopProductsRepository.insertProduct(
                shopEntity = shopEntity,
                categoryEntity = categoryEntity,
                shopProductModel = productModel
            )

            shopProductsWithSizesRepository.insert(
                product = insertedProduct,
                sizes = sizes
            )
        }
    }
}