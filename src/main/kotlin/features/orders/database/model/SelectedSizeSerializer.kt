package com.ducks.features.orders.database.model

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import kotlinx.serialization.json.Json

object SelectedSizeSerializer {

    private val json = Json { ignoreUnknownKeys = true }

    fun serialize(value: CoffeeProductSizeDTO): String {
        return json.encodeToString(value)
    }

    fun deserialize(value: String): CoffeeProductSizeDTO {
        return json.decodeFromString<CoffeeProductSizeDTO>(value)
    }
}
