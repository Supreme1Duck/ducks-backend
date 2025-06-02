package com.ducks.repository

import com.ducks.database.entity.ShopEntity
import com.ducks.database.entity.ShopProductCategoryEntity
import com.ducks.database.entity.ShopProductEntity
import com.ducks.model.SeasonModel
import com.ducks.model.ShopProductModel

class ShopProductsRepository {

    fun insertProduct(
        shopEntity: ShopEntity,
        categoryEntity: ShopProductCategoryEntity,
        seasonModel: SeasonModel? = null,
        shopProductModel: ShopProductModel,
    ): ShopProductEntity {
        return ShopProductEntity.new {
            name = shopProductModel.name
            description = shopProductModel.description
            brandName = shopProductModel.brandName
            price = shopProductModel.price
            imageUrls = shopProductModel.imageUrls
            shop = shopEntity
            category = categoryEntity

            seasonModel?.let {
                this.seasonId = it.id
            }
        }
    }
}