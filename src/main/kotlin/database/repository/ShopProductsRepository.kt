package com.ducks.database.repository

import com.ducks.database.entity.ShopEntity
import com.ducks.database.entity.ShopProductCategoryEntity
import com.ducks.database.entity.ShopProductEntity
import com.ducks.model.ShopProductModel
import org.jetbrains.exposed.sql.transactions.transaction

class ShopProductsRepository {

    fun insertProduct(
        shopEntity: ShopEntity,
        categoryEntity: ShopProductCategoryEntity,
        shopProductModel: ShopProductModel,
    ) {
        transaction {
            ShopProductEntity.new {
                name = shopProductModel.name
                description = shopProductModel.description
                price = shopProductModel.price
                imageUrls = shopProductModel.imageUrls
                shop = shopEntity
                category = categoryEntity
            }
        }
    }
}