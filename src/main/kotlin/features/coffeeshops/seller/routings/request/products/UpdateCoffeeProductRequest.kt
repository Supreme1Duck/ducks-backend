package com.ducks.features.coffeeshops.seller.routings.request.products

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class UpdateCoffeeProductRequest(
    val productId: Long,
    val name: String,
    val description: String? = null,
    val sizes: List<UpdateCoffeeProductSizeRequest>,
    val categoryId: Long,
    val imageUrl: String,
    val minutesToCook: Int?,
    val cooksInParallel: Boolean = false,
    @SerialName("inStock")
    val isInStock: Boolean = true,
    val constructors: List<UpdateCoffeeConstructorRequest>? = null,
    val carbohydrates: Int? = null,
    val protein: Int? = null,
    val fats: Int? = null,
)

@Serializable
data class UpdateCoffeeProductSizeRequest(
    override val id: String,
    override val sizeName: String? = null,
    override val sizeValue: String,
    @Serializable(with = BigDecimalSerializer::class)
    override val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    override val priceWithoutDiscount: BigDecimal? = null,
) : CoffeeProductSize

@Serializable
data class UpdateCoffeeConstructorRequest(
    val category: UpdateCoffeeCategoryConstructorRequest,
    val constructors: List<UpdateCoffeeConstructorItemRequest>,
)

@Serializable
data class UpdateCoffeeCategoryConstructorRequest(
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
data class UpdateCoffeeConstructorItemRequest(
    val id: Long,
    val name: String? = null,
)
