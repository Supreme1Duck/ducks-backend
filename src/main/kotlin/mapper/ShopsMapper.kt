package com.ducks.mapper

import com.ducks.database.entity.ShopEntity
import com.ducks.database.entity.ShopProductEntity
import com.ducks.dto.ColorDTO
import com.ducks.dto.ShopDTO
import com.ducks.dto.ShopProductCategoryDTO
import com.ducks.dto.ShopProductDTO
import com.ducks.model.SeasonModel

fun ShopEntity.entityToDto(limit: Int? = null): ShopDTO {
    return ShopDTO(
        id = id.value,
        name = name,
        description = description,
        address = address,
        photoUrls = photoUrls,
        products = getProducts(limit).map { product ->
            product.toDto()
        }.toList(),
    )
}

fun ShopProductEntity.toDto(): ShopProductDTO {
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
            ColorDTO(
                color.id.value,
                color.name
            )
        },
        season = seasonId?.let{ SeasonModel.entries[it] }
    )
}