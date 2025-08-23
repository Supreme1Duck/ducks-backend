package com.ducks.features.shops.database.rowmappers

import com.ducks.features.shops.common.dto.*
import com.ducks.features.shops.common.model.getSeasonModelById
import com.ducks.features.shops.database.table.*
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.mapToShopDTO(
    products: List<SearchShopProductDTO>,
    filters: ShopProductFiltersDTO,
): ShopDTO {
    return ShopDTO(
        id = this[ShopTable.id].value,
        name = this[ShopTable.name],
        description = this[ShopTable.description],
        address = this[ShopTable.address],
        photoUrls = this[ShopTable.photoUrls],
        products = products,
        filters = filters
    )
}

fun ResultRow.mapToShopPreviewDTO(
    products: List<ShopProductPreviewDTO>
): ShopPreviewDTO {
    return ShopPreviewDTO(
        id = this[ShopTable.id].value,
        name = this[ShopTable.name],
        address = this[ShopTable.address],
        products = products,
    )
}

fun ResultRow.mapToShopProductDTO(
    sizes: List<ShopProductChildSizeDTO>,
): ShopProductDTO {

    return ShopProductDTO(
        id = this[ShopProductTable.id].value,
        name = this[ShopProductTable.name],
        description = this[ShopProductTable.description],
        price = this[ShopProductTable.price],
        shopId = this[ShopProductTable.shop].value,
        season = this[ShopProductTable.seasonId]?.let(::getSeasonModelById),
        brandName = this[ShopProductTable.brandName],
        imageUrls = this[ShopProductTable.imageUrls],
        shopName = this[ShopTable.name],
        categoryDTO = mapToCategoryDTO(),
        color = this.getOrNull(ShopProductColorsTable.id)?.let { mapToColorDTO() },
        sizes = sizes
    )
}

fun ResultRow.mapToSearchShopProductDTO(): SearchShopProductDTO {
    val mainImageUrl = this[ShopProductTable.mainImageUrl]
    val imageUrls = this[ShopProductTable.imageUrls]

    val allImages = imageUrls.toMutableList()
        .also {
            it.add(0, mainImageUrl)
        }

    return SearchShopProductDTO(
        id = this[ShopProductTable.id].value,
        name = this[ShopProductTable.name],
        price = this[ShopProductTable.price],
        brandName = this[ShopProductTable.brandName],
        shopName = this[ShopTable.name],
        imageUrls = allImages,
    )
}

fun ResultRow.mapToColorDTO(): ShopProductColorDTO {
    return ShopProductColorDTO(
        id = this[ShopProductColorsTable.id].value,
        name = this[ShopProductColorsTable.name]
    )
}

fun ResultRow.mapToCategoryDTO(): ShopProductCategoryDTO {
    return ShopProductCategoryDTO(
        id = this[ShopProductCategoryTable.id].value,
        name = this[ShopProductCategoryTable.name],
        isSuperCategory = this[ShopProductCategoryTable.isSuperCategory],
        parentCategoryId = this[ShopProductCategoryTable.parent]?.value,
        superCategoryId = this[ShopProductCategoryTable.superCategoryId]?.value,
        description = this[ShopProductCategoryTable.description],
    )
}

fun ResultRow.mapToChildSizeDTO(): ShopProductChildSizeDTO {
    return ShopProductChildSizeDTO(
        id = this[ShopProductSizeTable.id].value,
        name = this[ShopProductSizeTable.name],
        parentId = this[ShopProductSizeTable.parentId]?.value ?: -1,
    )
}

