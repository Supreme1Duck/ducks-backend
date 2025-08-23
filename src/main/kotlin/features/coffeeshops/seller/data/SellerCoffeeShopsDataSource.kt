package com.ducks.features.coffeeshops.seller.data

import com.ducks.features.coffeeshops.database.CoffeeProductTable
import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.common.data.UpdateMap
import com.ducks.features.shops.database.table.ShopProductTable
import com.ducks.features.shops.database.table.ShopTable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class SellerCoffeeShopsDataSource {

    fun update(
        shopId: Long,
        updateMap: UpdateMap,
    ) {
        CoffeeShopTable.update(
            where = {
                ShopTable.id eq shopId
            }
        ) { table ->
            updateMap.forEach {
                when (it.key) {
                    UPDATE_MAP_COFFEE_SHOP_NAME -> {
                        table[name] = Json.decodeFromJsonElement<String>(it.value)
                    }

                    UPDATE_MAP_COFFEE_SHOP_ADDRESS -> {
                        table[address] = Json.decodeFromJsonElement<String>(it.value)
                    }

                    UPDATE_MAP_COFFEE_SHOP_DESCRIPTION -> {
                        table[description] = Json.decodeFromJsonElement<String?>(it.value)
                    }

                    UPDATE_MAP_COFFEE_SHOP_COOK_TIME -> {
                        table[secondsToCook] = Json.decodeFromJsonElement<Int>(it.value)
                    }

                    UPDATE_MAP_COFFEE_SHOP_CLOSEST_COOK_TIME -> {
                        table[closestTimeToCookInMinutes] = Json.decodeFromJsonElement<Int>(it.value)
                    }

                    UPDATE_MAP_COFFEE_SHOP_CLOSED -> {
                        table[isTemporaryClosed] = Json.decodeFromJsonElement<Boolean>(it.value)
                    }

                    UPDATE_MAP_COFFEE_SHOP_TAGS -> {
                        table[tags] = Json.decodeFromJsonElement<List<String>?>(it.value)
                    }

                    UPDATE_MAP_COFFEE_SHOP_IMAGES -> {
                        val imageUrls = Json.decodeFromJsonElement<List<String>>(it.value)
                        table[ShopProductTable.imageUrls] = imageUrls
                    }

                    UPDATE_MAP_COFFEE_SHOP_SEATS -> {
                        table[seatsCapacity] = Json.decodeFromJsonElement<Int>(it.value)
                    }
                }
            }
        }

        CoffeeShopTable.update(
            where = { CoffeeShopTable.id eq shopId }
        ) { table ->
            table[isShown] = isShopAvailableToShow(shopId)
        }
    }

    private fun isShopAvailableToShow(shopId: Long): Boolean {
        val hasAllInfo = CoffeeShopTable
            .selectAll()
            .where {
                CoffeeShopTable.id eq shopId and
                        CoffeeShopTable.name.notLike("") and
                        CoffeeShopTable.address.notLike("") and
                        CoffeeShopTable.imageUrls.isNotNull() and
                        CoffeeShopTable.lowestPrice.isNotNull()
            }
            .empty()
            .not()

        val hasProducts = CoffeeProductTable
            .selectAll()
            .where {
                CoffeeProductTable.shopId eq shopId
            }
            .empty()
            .not()

        return hasProducts && hasAllInfo
    }
}