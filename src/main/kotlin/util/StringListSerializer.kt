package com.ducks.util

import kotlinx.serialization.json.Json

object StringListSerializer {
    fun serialize(list: List<String>): String {
        return Json.encodeToString(list)
    }

    fun deserialize(serializedString: String): List<String> {
        return Json.decodeFromString(serializedString)
    }
}