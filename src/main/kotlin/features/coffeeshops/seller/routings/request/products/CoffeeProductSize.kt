package com.ducks.features.coffeeshops.seller.routings.request.products

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.util.DucksBadRequestError
import java.math.BigDecimal

interface CoffeeProductSize {
    val id: String
    val sizeName: String?
    val sizeValue: String
    val price: BigDecimal
    val priceWithoutDiscount: BigDecimal?
}

fun List<CoffeeProductSize>.validatedSizes(): List<CoffeeProductSizeDTO> {
    if (isEmpty()) {
        throw DucksBadRequestError("У товара должен быть хотя бы один размер")
    }

    val singleSize = size == 1

    return map { size ->
        val sizeName = size.sizeName?.takeUnless { it.isBlank() }

        if (!singleSize && sizeName == null) {
            throw DucksBadRequestError("У размера \"${size.sizeValue}\" не заполнено название")
        }

        if (size.sizeValue.isBlank()) {
            throw DucksBadRequestError("У размера \"${sizeName.orEmpty()}\" не заполнен объём")
        }

        if (size.price.signum() <= 0) {
            throw DucksBadRequestError("Цена размера \"${sizeName ?: size.sizeValue}\" должна быть больше нуля")
        }

        val priceWithoutDiscount = size.priceWithoutDiscount
        if (priceWithoutDiscount != null && priceWithoutDiscount <= size.price) {
            throw DucksBadRequestError(
                "Цена без скидки размера \"${sizeName ?: size.sizeValue}\" должна быть больше цены со скидкой"
            )
        }

        CoffeeProductSizeDTO(
            id = size.id,
            sizeName = if (singleSize) null else sizeName,
            sizeValue = size.sizeValue,
            price = size.price,
            priceWithoutDiscount = priceWithoutDiscount,
        )
    }
}

/** Цена, от которой показывается товар. */
fun List<CoffeeProductSizeDTO>.lowestPrice(): BigDecimal = minOf { it.price }
