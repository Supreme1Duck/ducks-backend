package com.ducks.mapper

import com.ducks.database.entity.ShopEntity
import com.ducks.database.entity.ShopProductEntity
import com.ducks.dto.ShopDTO
import com.ducks.dto.ShopProductDTO

fun ShopEntity.entityToModel(limit: Int? = null): ShopDTO {
    return ShopDTO(
        id = id.value,
        name = name,
        description = description,
        address = address,
        photoUrls = photoUrls,
        products = getProducts(limit).map { product ->
            product.toModel()
        }.toList(),
    )
}

fun ShopProductEntity.toModel() : ShopProductDTO {
    return ShopProductDTO(
        id = id.value,
        name = name,
        description = description,
        price = price,
        shopId = shop.id.value,
        shopName = shop.name,
        imageUrls = imageUrls,
        brandName = brandName,
    )
}