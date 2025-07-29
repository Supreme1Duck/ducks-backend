package com.ducks.util

import com.ducks.shops.database.dbmodels.ShopProductImageDTO
import kotlinx.serialization.json.Json

object ImageDBModelSerializer {
    fun serialize(list: List<ShopProductImageDTO>): String {
        return Json.encodeToString(list)
    }

    fun deserialize(serializedString: String): List<ShopProductImageDTO> {
        return Json.decodeFromString(serializedString)
    }
}