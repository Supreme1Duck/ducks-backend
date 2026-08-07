package com.ducks.features.coffeeshops.seller.data.model

import com.ducks.features.orders.data.dto.OrderProductDTO
import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

// Заказ в списке заказов за день. Состав заказа приходит отдельным запросом деталей.
@Serializable
data class SellerDayOrderDTO(
    val id: Long,
    val createdAt: Long,
    val userPhoneNumber: String,
    // Значения из OrderStatus.
    val status: Int,
    // Суммарное количество товаров в заказе, с учётом quantity каждой позиции.
    val productsCount: Int,
    val comment: String?,
    // true — заказ с собой, false — на месте.
    val isTakeaway: Boolean,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
)

@Serializable
data class SellerOrderDetailsDTO(
    val id: Long,
    val createdAt: Long,
    val acceptedAt: Long?,
    val readyAt: Long?,
    val finishedAt: Long?,
    val estimatedFinishTime: Long?,
    val userPhoneNumber: String,
    // Значения из OrderStatus.
    val status: Int,
    val comment: String?,
    val cancelledMessage: String?,
    val isToTime: Boolean,
    // true — заказ с собой, false — на месте.
    val isTakeaway: Boolean,
    val timeToCookInMinutes: Int,
    val products: List<OrderProductDTO>,
    // Стоимость позиций без чаевых.
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val tips: BigDecimal?,
    @Serializable(with = BigDecimalSerializer::class)
    val totalPrice: BigDecimal,
)
