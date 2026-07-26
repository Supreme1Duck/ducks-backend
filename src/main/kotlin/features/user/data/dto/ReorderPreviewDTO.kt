package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

enum class ReorderChangeStatus(val value: Int) {
    PRODUCT_UNAVAILABLE(0),
    SIZE_UNAVAILABLE(1),
    SIZE_PRICE_INCREASED(2),
    SIZE_PRICE_DECREASED(3),
    CONSTRUCTOR_UNAVAILABLE(4),
    CONSTRUCTOR_PRICE_INCREASED(5),
    CONSTRUCTOR_PRICE_DECREASED(6),
}

@Serializable
data class ReorderSizeDTO(
    val id: String,
    val sizeName: String?,
    val sizeValue: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)

@Serializable
data class ReorderConstructorDTO(
    val id: Long,
    val name: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
)

/**
 * Одно атомарное отличие ранее заказанной позиции от актуального состояния.
 * Для одной позиции может быть несколько изменений (каждое со своим [status]).
 * Заполняются только поля, относящиеся к конкретному статусу, остальные null.
 */
@Serializable
data class ReorderChangeDTO(
    val productId: Long,
    val productName: String,
    val imageUrl: String?,
    val status: Int,
    // Цена изменившегося элемента (размера или добавки) до/после. null, если цена не менялась.
    @Serializable(with = BigDecimalSerializer::class)
    val oldPrice: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val newPrice: BigDecimal?,
    // Заполнены для изменений размера. null, если размер не менялся.
    val oldSize: ReorderSizeDTO?,
    val newSize: ReorderSizeDTO?,
    // Затронутая добавка для CONSTRUCTOR_PRICE_*. null для остальных статусов.
    val constructor: ReorderConstructorDTO?,
    // Недоступные добавки для CONSTRUCTOR_UNAVAILABLE. null, если ничего не удалено.
    val removedConstructors: List<ReorderConstructorDTO>?,
)

@Serializable
data class ReorderCartProductDTO(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val size: ReorderSizeDTO,
    val constructors: List<ReorderConstructorDTO>?,
    val quantity: Int,
    // Актуальная цена за одну штуку: размер + добавки.
    @Serializable(with = BigDecimalSerializer::class)
    val unitPrice: BigDecimal,
    // Актуальная цена всей позиции: unitPrice * quantity.
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)

@Serializable
data class ReorderPreviewDTO(
    val changes: List<ReorderChangeDTO>,
    // Разница итоговой цены корзины и старого заказа. null, если равна нулю.
    @Serializable(with = BigDecimalSerializer::class)
    val totalPriceDelta: BigDecimal?,
    val cart: List<ReorderCartProductDTO>,
)
