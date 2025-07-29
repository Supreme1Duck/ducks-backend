package com.ducks.coffeeshops.database.mappers

import com.ducks.coffeeshops.common.data.model.preview.CoffeeShopPreviewDTO
import com.ducks.coffeeshops.common.data.model.dto.CoffeeProductWithDetailsDTO
import com.ducks.coffeeshops.common.data.model.dto.CoffeeShopDetailsDTO
import com.ducks.coffeeshops.common.data.model.dto.NutrientsDTO
import com.ducks.coffeeshops.common.data.model.preview.CoffeeShopProductPreviewDTO
import com.ducks.coffeeshops.database.*
import com.ducks.coffeeshops.seller.data.model.CoffeeCategoryDTO
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.mapToCoffeeShopPreview(): CoffeeShopPreviewDTO {
    return CoffeeShopPreviewDTO(
        id = this[CoffeeShopTable.id].value,
        name = this[CoffeeShopTable.name],
        address = this[CoffeeShopTable.address],
        tags = this[CoffeeShopTable.tags],
        pricesStartsFrom = this[CoffeeShopTable.lowestPrice],
    )
}

fun ResultRow.mapToCoffeeProductPreviewDTO(): CoffeeShopProductPreviewDTO {
    return CoffeeShopProductPreviewDTO(
        id = this[CoffeeProductTable.id].value,
        name = this[CoffeeProductTable.name],
        imageUrl = this[CoffeeProductTable.imageUrl],
        price = this[CoffeeProductTable.priceFrom],
        categoryId = this[CoffeeProductTable.categoryId].value,
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
        tags = this[CoffeeShopTable.tags],
        lowestPrice = this[CoffeeShopTable.lowestPrice],
        workTime = workTime,
        seatsCapacity = this[CoffeeShopTable.seatsCapacity],
        closestTime = this[CoffeeShopTable.closestTimeToCookInMinutes],
    )
}

fun ResultRow.mapToCoffeeProductWithDetailsDTO(): CoffeeProductWithDetailsDTO {
    val sizes = this[CoffeeProductTable.sizes]
    val minPrice = sizes.minOf {
        it.price ?: 0.toBigDecimal()
    }
    val minSize = sizes.first().sizeValue

    return CoffeeProductWithDetailsDTO(
        id = this[CoffeeProductTable.id].value,
        name = this[CoffeeProductTable.name],
        description = this[CoffeeProductTable.description],
        imageUrl = this[CoffeeProductTable.imageUrl],
        minPrice = minPrice,
        minSize = "от $minSize",
        nutrients = this[CoffeeProductTable.calories]?.let {
            NutrientsDTO(
                calories = it,
                carbohydrates = this[CoffeeProductTable.carbohydrates]!!,
                protein = this[CoffeeProductTable.protein]!!,
                fats = this[CoffeeProductTable.fats]!!,
            )
        }
    )
}

fun ResultRow.mapToCategoryDTO(): CoffeeCategoryDTO {
    return CoffeeCategoryDTO(
        id = this[CoffeeProductCategoryTable.id].value,
        name = this[CoffeeProductCategoryTable.name]
    )
}