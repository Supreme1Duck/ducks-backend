package com.ducks.features.coffeeshops.database.mappers

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeShopDetailsDTO
import com.ducks.features.coffeeshops.client.data.model.preview.CoffeeShopPreviewDTO
import com.ducks.features.coffeeshops.database.*
import com.ducks.features.coffeeshops.seller.data.model.CoffeeCategoryDTO
import com.ducks.features.coffeeshops.seller.data.model.SellerCoffeeShopActivePauseDTO
import com.ducks.features.coffeeshops.seller.data.model.SellerCoffeeShopDetailsDTO
import com.ducks.features.orders.data.model.WorkTimeModel
import features.coffeeshops.seller.data.model.CoffeeShopProductSellerPreviewDTO
import org.jetbrains.exposed.v1.core.ResultRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun ResultRow.mapToCoffeeShopPreview(): CoffeeShopPreviewDTO {
    return CoffeeShopPreviewDTO(
        id = this[CoffeeShopTable.id].value,
        name = this[CoffeeShopTable.name],
        address = this[CoffeeShopTable.address],
        tags = this[CoffeeShopTable.tags],
        isTemporaryClosed = this[CoffeeShopTable.isTemporaryClosed],
        pricesStartsFrom = this[CoffeeShopTable.lowestPrice],
        images = this[CoffeeShopTable.imageUrls].orEmpty()
    )
}


fun ResultRow.mapToSellerCoffeeProductPreviewDTO(): CoffeeShopProductSellerPreviewDTO {
    return CoffeeShopProductSellerPreviewDTO(
        id = this[CoffeeProductTable.id].value,
        name = this[CoffeeProductTable.name],
        imageUrl = this[CoffeeProductTable.imageUrl],
        price = this[CoffeeProductTable.priceFrom],
        inStock = this[CoffeeProductTable.inStock],
        categoryId = this[CoffeeProductTable.categoryId].value,
        minutesToCook = this[CoffeeProductTable.minutesToCook],
        shopId = this[CoffeeProductTable.shopId].value,
    )
}

fun ResultRow.mapToCoffeeShopDetailsDTO(): CoffeeShopDetailsDTO {
    val isClosed = this[CoffeeShopScheduleTable.isClosed]
    val workTime = if (isClosed) {
        "закрыто"
    } else {
        "${this[CoffeeShopScheduleTable.startTime]} - ${this[CoffeeShopScheduleTable.endTime]}"
    }

    return CoffeeShopDetailsDTO(
        id = this[CoffeeShopTable.id].value,
        name = this[CoffeeShopTable.name],
        address = this[CoffeeShopTable.address],
        imageUrls = this[CoffeeShopTable.imageUrls],
        tags = this[CoffeeShopTable.tags],
        lowestPrice = this[CoffeeShopTable.lowestPrice],
        workTime = workTime,
        tablesCapacity = this[CoffeeShopTable.tablesCapacity],
        isTemporaryClosed = this[CoffeeShopTable.isTemporaryClosed],
        freeTables = this[CoffeeShopTable.freeTables],
        closestTime = this[CoffeeShopTable.closestTimeToTakeOrders]?.plus(6 * 60 * 1000L),
        closestTimeReason = this[CoffeeShopTable.canTakeOrdersReason] ?: 0,
    )
}

fun ResultRow.mapToSellerCoffeeShopDetailsDTO(
    workTimeModel: WorkTimeModel?,
    schedule: List<SellerCoffeeShopDetailsDTO.Schedule>,
): SellerCoffeeShopDetailsDTO {
    val isClosed = workTimeModel?.isClosed ?: true

    val workTime = if (isClosed) {
        "закрыто"
    } else {
        formatWorkTime(workTimeModel)
    }

    // Не ошибка, hasValue не работает
    @Suppress("CONSTANT_CONDITION")
    val hasActivePause = this[CoffeeShopTechnicalPausesTable.startsAt] != null

    val activePause = if (hasActivePause) {
        SellerCoffeeShopActivePauseDTO(
            startsAt = this[CoffeeShopTechnicalPausesTable.startsAt],
            endsAt = this[CoffeeShopTechnicalPausesTable.endsAt],
        )
    } else {
        null
    }

    return SellerCoffeeShopDetailsDTO(
        id = this[CoffeeShopTable.id].value,
        name = this[CoffeeShopTable.name],
        address = this[CoffeeShopTable.address],
        description = this[CoffeeShopTable.description],
        imageUrls = this[CoffeeShopTable.imageUrls],
        schedule = schedule,
        tags = this[CoffeeShopTable.tags],
        lowestPrice = this[CoffeeShopTable.lowestPrice],
        isTemporaryClosed = this[CoffeeShopTable.isTemporaryClosed],
        workTime = workTime,
        tablesCapacity = this[CoffeeShopTable.tablesCapacity],
        freeTables = this[CoffeeShopTable.freeTables],
        closestTimeToTakeOrder = this[CoffeeShopTable.closestTimeToTakeOrders],
        activePause = activePause,
        canTakeOrdersReason = this[CoffeeShopTable.canTakeOrdersReason],
    )
}

fun ResultRow.mapToCategoryDTO(): CoffeeCategoryDTO {
    return CoffeeCategoryDTO(
        id = this[CoffeeProductCategoryTable.id].value,
        name = this[CoffeeProductCategoryTable.name]
    )
}

private fun formatWorkTime(workTimeModel: WorkTimeModel?) : String {
    if (workTimeModel == null)
        return "неизвестно"

    val timeZone = ZoneId.of("Europe/Moscow") // UTC+3, без DST
    val formatter = DateTimeFormatter.ofPattern("HH:mm")

    val startTime = Instant.ofEpochMilli(workTimeModel.startTime).atZone(timeZone).toLocalTime()
    val endTime = Instant.ofEpochMilli(workTimeModel.endTime).atZone(timeZone).toLocalTime()

    return "${startTime.format(formatter)} - ${endTime.format(formatter)}"
}