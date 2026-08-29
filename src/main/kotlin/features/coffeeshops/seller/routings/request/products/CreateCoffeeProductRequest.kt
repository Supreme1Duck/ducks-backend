package com.ducks.features.coffeeshops.seller.routings.request.products

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class CreateCoffeeProductRequest(
    val name: String,
    val description: String? = null,

    val sizes: List<CoffeeProductSizeRequest>,
    val categoryId: Long,

    val imageUrl: String,

    val minutesToCook: Int?,

    /**
     * Товар греется или ждёт готовым и не занимает руки баристы — его время
     * не складывается с остальным заказом. По умолчанию false: обычную позицию
     * безопаснее считать дольше, чем обещать заказ раньше, чем он будет готов.
     */
    val cooksInParallel: Boolean = false,

    @SerialName("inStock")
    val isInStock: Boolean = true,

    val constructors: List<CoffeeCreateConstructorRequest>? = null,

    val carbohydrates: Int? = null,
    val protein: Int? = null,
    val fats: Int? = null,
)

@Serializable
data class CoffeeProductSizeRequest(
    val id: String,
    val sizeName: String? = null,
    val sizeValue: String,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)

@Serializable
data class CoffeeCreateConstructorRequest(
    val category: CoffeeCategoryConstructorRequest,
    val constructors: List<CoffeeConstructorRequest>,
)

@Serializable
data class CoffeeCategoryConstructorRequest(
    val id: Long,
    val defaultConstructorIds: List<Long>? = null,
    val maxSelection: Int? = null,
    val minSelection: Int? = null,
)

/**
 * [id] < 0 — добавка ещё не сохранена, приложение прислало временный id (та же конвенция,
 * что в /constructor/save). Такая добавка заводится при сохранении продукта, и [name]
 * для неё обязателен — у уже существующих добавок поле не нужно и приходить не будет.
 */
@Serializable
data class CoffeeConstructorRequest(
    val id: Long,
    val name: String? = null,
)
