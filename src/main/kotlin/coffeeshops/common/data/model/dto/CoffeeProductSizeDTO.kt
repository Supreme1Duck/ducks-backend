package com.ducks.coffeeshops.common.data.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import util.BigDecimalSerializer
import java.math.BigDecimal

@Serializable
data class CoffeeProductSizeDTO(
    val sizeName: String?,
    val sizeValue: String?,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal?,
)

object CoffeeShopSizeSerializer{
    private val json = Json { ignoreUnknownKeys = true }

    fun serialize(value: List<CoffeeProductSizeDTO>): String {
        return json.encodeToString(value)
    }

    fun deserialize(value: String): List<CoffeeProductSizeDTO> {
        return json.decodeFromString<List<CoffeeProductSizeDTO>>(value)
    }
}
