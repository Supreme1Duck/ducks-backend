package com.ducks.repository

import com.ducks.database.entity.ShopProductEntity
import com.ducks.database.entity.ShopProductSizeEntity
import com.ducks.database.entity.ShopProductsWithSizesEntity

class ShopProductsWithSizesRepository {

    fun insert(
        product: ShopProductEntity,
        sizes: List<ShopProductSizeEntity>
    ) {
        sizes.forEach {
            ShopProductsWithSizesEntity.new {
                this.product = product
                this.size = it
            }
        }
    }

    fun insert(
        product: ShopProductEntity,
        size: ShopProductSizeEntity
    ) {
        ShopProductsWithSizesEntity.new {
            this.product = product
            this.size = size
        }
    }
}