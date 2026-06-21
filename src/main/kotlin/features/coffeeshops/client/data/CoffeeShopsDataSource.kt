package com.ducks.features.coffeeshops.client.data

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopPreviewDTO
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.database.mappers.mapToCoffeeShopPreview
import com.ducks.features.orders.data.model.WorkTimeModel
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.jdbc.selectAll

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
            .where(CoffeeShopTable.id less (lastId ?: Long.MAX_VALUE))
            .orderBy(CoffeeShopTable.id, SortOrder.DESC)
            .limit(limit ?: Int.MAX_VALUE)
            .map {
                it.mapToCoffeeShopPreview()
            }
    }

    fun getShopDetails(
        shopId: Long,
        workTime: WorkTimeModel?,
    ) : CoffeeShopDetailsDTO {
        return CoffeeShopTable
            .selectAll()
            .where {
                CoffeeShopTable.id eq shopId
            }.map {
                it.mapToCoffeeShopDetailsDTO(workTime)
            }.first()
    }
}