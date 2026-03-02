package com.ducks.util

import kotlinx.serialization.json.Json

object LongListSerializer {
    fun serialize(list: List<Long>): String {
        return Json.encodeToString(list)
    }

    fun deserialize(serializedString: String): List<Long> {
        return Json.decodeFromString(serializedString)
    }
}