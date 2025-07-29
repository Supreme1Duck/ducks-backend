package com.ducks.coffeeshops.seller.domain

import com.ducks.coffeeshops.database.CoffeeShopScheduleTable
import com.ducks.coffeeshops.database.CoffeeShopTable
import com.ducks.coffeeshops.seller.data.SellerCoffeeShopsDataSource
import com.ducks.coffeeshops.seller.data.UPDATE_MAP_COFFEE_SHOP_IMAGES
import com.ducks.coffeeshops.seller.routings.request.*
import com.ducks.common.data.UpdateMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

class SellerCoffeeShopRepository(
    private val sellerCoffeeShopDataSource: SellerCoffeeShopsDataSource,
    private val coffeeShopImageRepository: CoffeeShopImageRepository,
) {

    suspend fun updateShop(
        shopId: Long,
        updateMap: UpdateMap,
    ) {
        return newSuspendedTransaction {
            sellerCoffeeShopDataSource.update(shopId = shopId, updateMap = updateMap)

            if (updateMap.containsKey(UPDATE_MAP_COFFEE_SHOP_IMAGES)) {
                val newImages = Json.decodeFromJsonElement<List<String>>(updateMap[UPDATE_MAP_COFFEE_SHOP_IMAGES]!!)
                deleteUnusedImages(shopId, newImages)
            }
        }
    }

    private fun deleteUnusedImages(shopId: Long, imageUrls: List<String>): List<String> {
        val existingImageUrls = CoffeeShopTable
            .select(CoffeeShopTable.imageUrls)
            .where { CoffeeShopTable.id eq shopId }
            .map { it[CoffeeShopTable.imageUrls] }
            .first()

        val listToDelete = existingImageUrls?.mapNotNull {
            if (imageUrls.contains(it)) {
                null
            } else {
                it
            }
        }

        listToDelete?.forEach {
            coffeeShopImageRepository.deleteImage(shopId, it)
        }

        return imageUrls
    }

    suspend fun setSchedule(shopId: Long, schedule: SetCoffeeShopScheduleRequest) {
        newSuspendedTransaction {
            CoffeeShopScheduleTable.deleteWhere {
                CoffeeShopScheduleTable.shopId eq shopId
            }

            val scheduleMap = schedule.schedule.map {
                mapDayOfWeek(it.key) to it.value
            }

            CoffeeShopScheduleTable.batchInsert(scheduleMap) { (dayOfWeek, scheduleData) ->
                this[CoffeeShopScheduleTable.shopId] = shopId
                this[CoffeeShopScheduleTable.dayOfWeek] = dayOfWeek

                this[CoffeeShopScheduleTable.isClosed] = scheduleData == null

                this[CoffeeShopScheduleTable.startTime] = scheduleData?.startTime
                this[CoffeeShopScheduleTable.endTime] = scheduleData?.endTime
            }
        }
    }

    private fun mapDayOfWeek(entry: String): Int {
        return when (entry) {
            MONDAY_KEY -> 1
            TUESDAY_KEY -> 2
            WEDNESDAY_KEY -> 3
            THURSDAY_KEY -> 4
            FRIDAY_KEY -> 5
            SATURDAY_KEY -> 6
            SUNDAY_KEY -> 7
            else -> throw IllegalArgumentException()
        }
    }
}