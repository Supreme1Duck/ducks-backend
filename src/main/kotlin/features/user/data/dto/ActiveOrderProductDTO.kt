package com.ducks.features.user.data.dto

import kotlinx.serialization.Serializable
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class ActiveOrderProductDTO(
    val id: Long,
    val name: String,
    val quantity: Int,
    val constructors: List<Constructor>?,
    val size: Size,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal,
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
