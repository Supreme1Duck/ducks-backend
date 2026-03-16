package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.CoffeeShopScheduleTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.coffeeshops.seller.data.model.SellerCoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.seller.routings.request.shop.UpdateCoffeeShopRequest
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.batchInsert

class SellerCoffeeShopsDataSource {

    fun fetchSchedule(shopId: Long): List<SellerCoffeeShopDetailsDTO.Schedule> {
        return CoffeeShopScheduleTable
            .selectAll()
            .where { CoffeeShopScheduleTable.shopId eq shopId }
            .sortedBy { it[CoffeeShopScheduleTable.dayOfWeek] }
            .map {
                SellerCoffeeShopDetailsDTO.Schedule(
                    dayOfWeek = getDayNameByIndex(it[CoffeeShopScheduleTable.dayOfWeek]),
                    startTime = it[CoffeeShopScheduleTable.startTime].orEmpty(),
                    endTime = it[CoffeeShopScheduleTable.endTime].orEmpty(),
                    isClosed = it[CoffeeShopScheduleTable.isClosed],
                )
            }
    }

    suspend fun update(
        shopId: Long,
        request: UpdateCoffeeShopRequest,
    ) {
        newSuspendedTransaction {
            CoffeeShopTable.update(
                where = {
                    CoffeeShopTable.id eq shopId
                }
            ) { table ->
                table[address] = request.address
                table[description] = request.description
                table[tags] = request.tags
                table[imageUrls] = request.photoUrls
                table[isShown] = isShopAvailableToShow(shopId)
            }

            CoffeeShopScheduleTable.deleteWhere {
                CoffeeShopScheduleTable.shopId eq shopId
            }

            CoffeeShopScheduleTable.batchInsert(request.schedule) {
                this[CoffeeShopScheduleTable.dayOfWeek] = getDayIndexByName(it.name)
                this[CoffeeShopScheduleTable.startTime] = it.openTime
                this[CoffeeShopScheduleTable.endTime] = it.closeTime
                this[CoffeeShopScheduleTable.isClosed] = it.isClosed
                this[CoffeeShopScheduleTable.shopId] = shopId
            }
        }
    }

    fun updateTemporaryClosed(shopId: Long, isClosed: Boolean, reason: String?) {
        CoffeeShopTable.update(where = { CoffeeShopTable.id eq shopId }) {
            it[isTemporaryClosed] = isClosed
            it[temporaryClosedReason] = if (isClosed) reason?.takeIf { r -> r.isNotBlank() } else null
        }
    }

    fun updateFreeTables(shopId: Long, freeTables: Int) {
        CoffeeShopTable.update(where = { CoffeeShopTable.id eq shopId }) {
            it[CoffeeShopTable.freeTables] = freeTables
        }
    }

    private fun isShopAvailableToShow(shopId: Long): Boolean {
        val hasAllInfo = CoffeeShopTable
            .selectAll()
            .where {
                CoffeeShopTable.id eq shopId and
                        CoffeeShopTable.name.notLike("") and
                        CoffeeShopTable.address.notLike("")
                // TODO вернуть в uncomment после дебага
//                and
//                        CoffeeShopTable.imageUrls.isNotNull() and
//                        CoffeeShopTable.lowestPrice.isNotNull()
            }
            .empty()
            .not()

        val hasProducts = CoffeeProductTable
            .selectAll()
            .where {
                CoffeeProductTable.shopId eq shopId
            }
            .empty()
            .not()

        return hasProducts && hasAllInfo
    }

    private fun getDayNameByIndex(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            1 -> "Понедельник"
            2 -> "Вторник"
            3 -> "Среда"
            4 -> "Четверг"
            5 -> "Пятница"
            6 -> "Суббота"
            7 -> "Воскресенье"
            else -> ""
        }
    }

    private fun getDayIndexByName(dayOfWeek: String): Int {
        return when (dayOfWeek) {
            "Понедельник" -> 1
            "Вторник" -> 2
            "Среда" -> 3
            "Четверг" -> 4
            "Пятница" -> 5
            "Суббота" -> 6
            "Воскресенье" -> 7
            else -> -1
        }
    }
}