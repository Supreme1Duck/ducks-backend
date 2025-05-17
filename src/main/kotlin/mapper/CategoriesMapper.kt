package com.ducks.mapper

import com.ducks.database.entity.ShopProductCategoryEntity
import com.ducks.model.ShopProductCategoryModel

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