package com.ducks.features.orders.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class OrderProductDTO(
    val id: Long,
    val orderId: Long,
    val name: String,
    val constructors: List<Constructor>?,
    val imageUrl: String?,
    val size: Size,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
    val quantity: Int,
) {
    @Serializable
    data class Size(
        val id: String,
        val sizeName: String?,
        val sizeValue: String,
        @Serializable(with = BigDecimalSerializer::class)
        val price: BigDecimal,
    )

    @Serializable
    data class Constructor(
        val id: Long,
        val name: String,
        @Serializable(with = BigDecimalSerializer::class)
        val price: BigDecimal?,
    )
}
