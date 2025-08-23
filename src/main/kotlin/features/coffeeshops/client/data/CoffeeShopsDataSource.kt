package com.ducks.features.coffeeshops.client.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopPreviewDTO
import com.ducks.features.coffeeshops.database.CoffeeShopScheduleTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeShopPreview
import com.ducks.features.shops.database.table.ShopTable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.LocalDate

class CoffeeShopsDataSource {

    fun exists(
        shopId: Long,
    ) : Boolean {
        return CoffeeShopTable.selectAll()
            .where { CoffeeShopTable.id eq shopId }
            .empty()
            .not()
    }

    fun getAllShops(
        lastId: Long?,
        limit: Int?,
    ): List<CoffeeShopPreviewDTO> {
         return CoffeeShopTable
            .selectAll()
            .where(ShopTable.id less (lastId ?: Long.MAX_VALUE))
            .orderBy(ShopTable.id, SortOrder.DESC)
            .limit(limit ?: Int.MAX_VALUE)
            .map {
                it.mapToCoffeeShopPreview()
            }
    }

    fun getShopDetails(
        shopId: Long,
    ) : CoffeeShopDetailsDTO {
        val currentDayOfWeek = LocalDate.now().dayOfWeek

        return CoffeeShopTable
            .join(CoffeeShopScheduleTable, joinType = JoinType.LEFT, CoffeeShopTable.id, CoffeeShopScheduleTable.shopId)
            .selectAll()
            .where {
                (CoffeeShopTable.id eq shopId) and (CoffeeShopScheduleTable.dayOfWeek eq currentDayOfWeek.value)
            }.map {
                it.mapToCoffeeShopDetailsDTO()
            }.first()
    }
}