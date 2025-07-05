package com.ducks.shops.common.mapper

import com.ducks.shops.common.database.entity.ShopProductCategoryEntity
import com.ducks.shops.common.model.ShopProductCategoryModel

fun ShopProductCategoryEntity.mapToModel(): ShopProductCategoryModel {
    return ShopProductCategoryModel(
        id = id.value,
        name = name,
        parentId = parent?.id?.value,
        children = children.toList().map { it.mapToModel() },
        superCategoryId = superCategory?.id?.value,
        isSuperCategory = isSuperCategory,
    )
}