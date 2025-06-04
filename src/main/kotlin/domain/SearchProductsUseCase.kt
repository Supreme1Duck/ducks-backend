package com.ducks.domain

import com.ducks.database.entity.ShopProductEntity
import com.ducks.database.entity.ShopProductsWithSizesEntity
import com.ducks.database.table.ShopProductSizeTable
import com.ducks.database.table.ShopProductTable
import com.ducks.database.table.ShopProductsWithSizesTable
import com.ducks.dto.*
import com.ducks.mapper.toDto
import com.ducks.model.SeasonModel
import dto.SearchResultDTO
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class SearchProductsUseCase {

    suspend fun invoke(
        query: String?,
        categoriesIds: List<Long>?,
        sizeIds: List<Long>?,
        colorId: Int?,
        seasonModelId: Int?,
    ): SearchResultDTO = searchWithQuery(
        query = query,
        categoriesIds = categoriesIds,
        sizeIds = sizeIds,
        colorIds = colorId,
        seasonModelId = seasonModelId
    )

    private suspend fun searchWithQuery(
        query: String?,
        categoriesIds: List<Long>?,
        sizeIds: List<Long>?,
        colorIds: Int?,
        seasonModelId: Int?
    ): SearchResultDTO {
        return newSuspendedTransaction {
            val foundProducts = searchProductsWithQuery(
                query = query,
                categoriesIds = categoriesIds,
                colorIds = colorIds,
                seasonModelId = seasonModelId,
                sizeIds = sizeIds,
            )

            val availableSizes = getAvailableSizes(products = foundProducts)

            val (categories, seasons, colors) = getFilters(products = foundProducts)

            SearchResultDTO(
                products = foundProducts,
                sizes = availableSizes,
                categories = categories,
                seasons = seasons,
                colors = colors,
            )
        }
    }

    private fun searchProductsWithQuery(
        query: String?,
        categoriesIds: List<Long>?,
        colorIds: Int?,
        seasonModelId: Int?,
        sizeIds: List<Long>?,
    ): List<ShopProductDTO> {
        val productsFilteredBySizes = sizeIds?.let {
            it.map { size ->
                ShopProductsWithSizesEntity[size].product
            }
        }

        val queryFilteredOp = query?.let { (ShopProductTable.name like "%$query%") or (ShopProductTable.description like "%$query%") } ?: Op.TRUE
        val sizedFilteredOp = productsFilteredBySizes?.let { (ShopProductTable.id inList productsFilteredBySizes.map { it.id }.asIterable()) } ?: Op.TRUE
        val categoryFilteredOp = categoriesIds?.let { ShopProductTable.category inList it } ?: Op.TRUE
        val colorFilteredOp = colorIds?.let { ShopProductTable.color eq it } ?: Op.TRUE
        val seasonFilteredOp = seasonModelId?.let { ShopProductTable.seasonId eq seasonModelId } ?: Op.TRUE

        val products = ShopProductEntity.find {
            queryFilteredOp and
                    sizedFilteredOp and
                    categoryFilteredOp and
                    colorFilteredOp and
                    seasonFilteredOp
        }.map {
            it.toDto()
        }

        return products
    }

    private fun getAvailableSizes(products: List<ShopProductDTO>): List<ShopProductChildSizeDTO> {
        return ShopProductsWithSizesTable.join(
            otherTable = ShopProductSizeTable,
            joinType = JoinType.INNER,
            onColumn = ShopProductsWithSizesTable.size,
            otherColumn = ShopProductSizeTable.id
        ).select(
            ShopProductSizeTable.id, ShopProductSizeTable.name
        ).where {
            ShopProductsWithSizesTable.product inList products.map { it.id }
        }.map {
            ShopProductChildSizeDTO(
                id = it[ShopProductSizeTable.id].value,
                name = it[ShopProductSizeTable.name],
                parentId = 0,
            )
        }.distinct()
    }

    private fun getFilters(products: List<ShopProductDTO>): Filters {
        val categories = mutableSetOf<ShopProductCategoryDTO>()
        val seasons = mutableSetOf<SeasonModel>()
        val colors = mutableSetOf<ColorDTO>()

        products.forEach {
            categories.add(it.categoryDTO)
            it.season?.let(seasons::add)
            it.color?.let(colors::add)
        }

        return Filters(
            categories = categories.toList(),
            seasons = seasons.toList(),
            colors = colors.toList(),
        )
    }

    private data class Filters(
        val categories: List<ShopProductCategoryDTO>,
        val seasons: List<SeasonModel>,
        val colors: List<ColorDTO>,
    )
}