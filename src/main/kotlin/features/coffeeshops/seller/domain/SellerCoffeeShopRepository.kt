package com.ducks.features.coffeeshops.seller.domain

import com.ducks.common.data.UpdateMap
import com.ducks.features.coffeeshops.database.CoffeeShopScheduleTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.database.CoffeeShopTechnicalPausesTable
import com.ducks.features.coffeeshops.seller.data.SellerCoffeeShopsDataSource
import com.ducks.features.coffeeshops.seller.data.UPDATE_MAP_COFFEE_SHOP_IMAGES
import com.ducks.features.coffeeshops.seller.routings.request.shop.*
import com.ducks.util.DucksBadRequestError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.*
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

    suspend fun addTechnicalPause(
        shopId: Long,
        startsAt: Long,
        endsAt: Long,
    ) {
        return newSuspendedTransaction {
            if (endsAt < System.currentTimeMillis() || endsAt <= startsAt) {
                throw DucksBadRequestError("Неверное время окончания паузы!")
            }

            val activePauseAlreadyExists = CoffeeShopTechnicalPausesTable
                .selectAll()
                .where { (CoffeeShopTechnicalPausesTable.coffeeShop eq shopId) and (CoffeeShopTechnicalPausesTable.isActive eq true) }
                .firstOrNull() != null

            if (activePauseAlreadyExists) {
                throw DucksBadRequestError("Мы пока не поддерживаем несколько активных пауз")
            }

            CoffeeShopTechnicalPausesTable.insert {
                it[CoffeeShopTechnicalPausesTable.startsAt] = startsAt
                it[CoffeeShopTechnicalPausesTable.endsAt] = endsAt
                it[coffeeShop] = shopId
            }
        }
    }

    suspend fun deletePause(
        pauseId: Long,
        shopId: Long,
    ) {
        return newSuspendedTransaction {
            CoffeeShopTechnicalPausesTable
                .update(
                    where = {
                        (CoffeeShopTechnicalPausesTable.id eq pauseId) and
                                (CoffeeShopTechnicalPausesTable.coffeeShop eq shopId)
                    }
                ) {
                    it[isActive] = false
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