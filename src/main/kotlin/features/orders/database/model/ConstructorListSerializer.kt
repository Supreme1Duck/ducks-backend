package com.ducks.features.orders.database.model

import kotlinx.serialization.json.Json

object ConstructorListSerializer {

    private val json = Json { ignoreUnknownKeys = true }

    fun serialize(value: List<OrderedProductConstructorDBModel>): String {
        return json.encodeToString(value)
    }

    fun deserialize(value: String): List<OrderedProductConstructorDBModel> {
        return json.decodeFromString<List<OrderedProductConstructorDBModel>>(value)
    }
}