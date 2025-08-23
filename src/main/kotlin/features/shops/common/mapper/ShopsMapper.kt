package com.ducks.features.shops.common.mapper

import com.ducks.features.shops.common.dto.*
import com.ducks.features.shops.database.entity.ShopEntity
import com.ducks.features.shops.database.entity.ShopProductEntity
import com.ducks.features.shops.common.model.SeasonModel

fun ShopEntity.entityToDto(
    products: List<SearchShopProductDTO>,
    filters: ShopProductFiltersDTO
): ShopDTO {
    return ShopDTO(
        id = id.value,
        name = name,
        description = description,
        address = address,
        photoUrls = photoUrls,
        products = products,
        filters = filters
    )
}

fun ShopProductEntity.toDto(
    sizes: List<ShopProductChildSizeDTO>
): ShopProductDTO {
    return ShopProductDTO(
        id = id.value,
        name = name,
        description = description,
        price = price,
        shopId = shop.id.value,
        shopName = shop.name,
        brandName = brandName,
        imageUrls = imageUrls,
        categoryDTO = ShopProductCategoryDTO(
            category.id.value,
            category.name,
        ),
        color = color?.let { color ->
            ShopProductColorDTO(
                color.id.value,
                color.name
            )
        },
        season = seasonId?.let { SeasonModel.entries[it] },
        sizes = sizes
    )
}