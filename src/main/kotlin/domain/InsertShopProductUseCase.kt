package com.ducks.domain

import com.ducks.database.repository.ShopProductCategoryRepository
import com.ducks.database.repository.ShopProductsRepository
import com.ducks.database.repository.ShopsRepository
import com.ducks.model.ShopProductModel
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class InsertShopProductUseCase(
    private val shopsRepository: ShopsRepository,
    private val categoryRepository: ShopProductCategoryRepository,
    private val shopProductsRepository: ShopProductsRepository,
) {

    suspend operator fun invoke(shopId: Long, categoryId: Long, productModel: ShopProductModel) {
        newSuspendedTransaction {
            val shopEntity = shopsRepository.getById(shopId)
            val categoryEntity = categoryRepository.getById(categoryId)

            shopProductsRepository.insertProduct(
                shopEntity,
                categoryEntity,
                productModel
            )
        }
    }
}