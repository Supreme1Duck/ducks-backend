package com.ducks.shops.common.data

import com.ducks.shops.common.dto.*
import com.ducks.shops.common.mapper.toDto
import com.ducks.shops.common.model.SeasonModel
import com.ducks.shops.common.model.getSeasonModelById
import com.ducks.shops.database.entity.ShopProductEntity
import com.ducks.shops.database.entity.ShopProductsWithSizesEntity
import com.ducks.shops.database.rowmappers.mapToCategoryDTO
import com.ducks.shops.database.rowmappers.mapToChildSizeDTO
import com.ducks.shops.database.rowmappers.mapToColorDTO
import com.ducks.shops.database.rowmappers.mapToSearchShopProductDTO
import com.ducks.shops.database.table.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.like
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select

class ShopProductsDataSource {

    fun searchWithQuery(
        query: String?,
        shopId: Long?,
        categoriesIds: List<Long>?,
        colorIds: List<Int>?,
        seasonModelIds: List<Int>?,
        sizeIds: List<Long>?,
        lastId: Long?,
        limit: Int?,
    ): List<SearchShopProductDTO> {
        val productsFilteredBySizes = sizeIds?.let {
            it.map { size ->
                ShopProductsWithSizesEntity[size].product
            }
        }

        val shopQuery = shopId?.let { ShopProductTable.id eq it } ?: Op.TRUE
        val queryFilteredOp =
            query?.let { (ShopProductTable.name like "%$query%") or (ShopProductTable.description like "%$query%") }
                ?: Op.TRUE
        val sizedFilteredOp = productsFilteredBySizes?.let {
            (ShopProductTable.id inList productsFilteredBySizes.map { it.id }.asIterable())
        } ?: Op.TRUE
        val categoryFilteredOp = categoriesIds?.let { ShopProductTable.category inList it } ?: Op.TRUE
        val colorFilteredOp = colorIds?.let { ShopProductTable.color inList it } ?: Op.TRUE
        val seasonFilteredOp = seasonModelIds?.let { ShopProductTable.seasonId inList it } ?: Op.TRUE
        val lastIdQuery = lastId?.let { ShopProductTable.id less it } ?: Op.TRUE

        val products = ShopProductTable
            .join(ShopTable, JoinType.LEFT, ShopTable.id, ShopProductTable.shop)
            .select(ShopProductTable.columns + ShopTable.name)
            .where(
                shopQuery and
                        queryFilteredOp and
                        sizedFilteredOp and
                        categoryFilteredOp and
                        colorFilteredOp and
                        seasonFilteredOp and
                        lastIdQuery
            )
            .orderBy(ShopProductTable.id, SortOrder.DESC)
            .also { q ->
                limit?.let {
                    q.limit(it)
                }
            }
            .map {
                it.mapToSearchShopProductDTO()
            }

        return products
    }

    fun requestProductsFromDB(
        shopId: Long?,
        lastId: Long?,
        limit: Int?,
    ): List<SearchShopProductDTO> {
        return ShopProductTable
            .join(ShopTable, JoinType.INNER, ShopProductTable.shop, ShopTable.id)
            .select(
                ShopProductTable.columns +
                        ShopTable.columns
            )
            .also {
                if (shopId != null) {
                    it.where {
                        ShopProductTable.shop eq shopId
                    }
                }
            }
            .orderBy(ShopProductTable.id, SortOrder.DESC)
            .andWhere { ShopProductTable.id less (lastId ?: Long.MAX_VALUE) }
            .limit(limit ?: Int.MAX_VALUE)
            .map {
                it.mapToSearchShopProductDTO()
            }
    }

    fun requestAvailableFiltersFromDB(
        productIds: List<Long>?,
        shopId: Long?,
    ): ShopProductFiltersDTO {
        val sizes = mutableSetOf<ShopProductChildSizeDTO>()
        val colors = mutableSetOf<ShopProductColorDTO>()
        val categories = mutableSetOf<ShopProductCategoryDTO>()
        val seasons = mutableSetOf<SeasonModel>()

        ShopProductTable
            .join(ShopTable, JoinType.LEFT, ShopTable.id, ShopProductTable.shop)
            .join(ShopProductsWithSizesTable, JoinType.LEFT, ShopProductTable.id, ShopProductsWithSizesTable.product)
            .join(ShopProductSizeTable, JoinType.INNER, ShopProductsWithSizesTable.size, ShopProductSizeTable.id)
            .join(ShopProductCategoryTable, JoinType.INNER, ShopProductTable.category, ShopProductCategoryTable.id)
            .join(ShopProductColorsTable, JoinType.INNER, ShopProductTable.color, ShopProductColorsTable.id)
            .select(
                ShopProductSizeTable.columns +
                        ShopProductColorsTable.columns +
                        ShopProductCategoryTable.columns +
                        ShopProductTable.seasonId +
                        ShopTable.name +
                        ShopProductTable.id
            )
            .where(
                predicate = (shopId?.let { ShopProductTable.shop eq it } ?: Op.TRUE) and
                        (productIds?.let { ShopProductTable.id inList it } ?: Op.TRUE)
            )
            .map {
                categories.add(it.mapToCategoryDTO())
                sizes.add(it.mapToChildSizeDTO())
                colors.add(it.mapToColorDTO())
                it[ShopProductTable.seasonId]?.let { seasons.add(getSeasonModelById(it)) }
            }

        return ShopProductFiltersDTO(
            sizes = sizes.toList(),
            categories = categories.toList(),
            colors = colors.toList(),
            seasons = seasons.toList(),
        )
    }

    fun getProductsPreviewByShop(
        shopId: Long?,
        lastId: Long?,
        limit: Int?,
    ): List<ShopProductPreviewDTO> {
        return ShopProductTable
            .select(
                ShopProductTable.id, ShopProductTable.mainImageUrl,
            )
            .where(ShopProductTable.shop eq shopId)
            .orderBy(ShopProductTable.id, SortOrder.DESC)
            .andWhere { ShopProductTable.id less (lastId ?: Long.MAX_VALUE) }
            .limit(limit ?: Int.MAX_VALUE)
            .map {
                ShopProductPreviewDTO(
                    id = it[ShopProductTable.id].value,
                    imgUrl = it[ShopProductTable.mainImageUrl]
                )
            }
    }

    fun getShopProduct(
        productId: Long,
        sizes: List<ShopProductChildSizeDTO>,
    ): ShopProductDTO {
        return ShopProductEntity[productId].toDto(sizes)
    }

    fun requestSizesByProduct(
        productId: Long
    ): List<ShopProductChildSizeDTO> {
        return ShopProductsWithSizesTable
            .join(ShopProductSizeTable, JoinType.INNER, ShopProductsWithSizesTable.size, ShopProductSizeTable.id)
            .select(ShopProductSizeTable.columns)
            .where(ShopProductsWithSizesTable.product eq productId)
            .map {
                it.mapToChildSizeDTO()
            }
    }
}