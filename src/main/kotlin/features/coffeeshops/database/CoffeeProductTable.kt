package com.ducks.features.coffeeshops.database

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeShopSizeSerializer
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.json.json
import util.citext

object CoffeeProductTable: LongIdTable("ducks_coffee_shop_product_table") {

    val name = citext("name", 100)
    val description = text("description").nullable()
    val priceFrom = decimal("price", precision = 10, scale = 2)
    val categoryId = reference("category_id", CoffeeProductCategoryTable)

    val shopId = reference("shop_id", CoffeeShopTable)
    val sizes = json(
        name = "sizes",
        serialize = CoffeeShopSizeSerializer::serialize,
        deserialize = CoffeeShopSizeSerializer::deserialize,
    )

    val imageUrl = text("imageUrl")

    val minutesToCook = integer("minutes_to_cook").nullable()

    val inStock = bool("in_stock").default(true)

    val cooksInParallel = bool("cooks_in_parallel").default(false)

    val carbohydrates = integer("carbohydrates").nullable()
    val protein = integer("protein").nullable()
    val fats = integer("fats").nullable()
    val calories = integer("calories").nullable()
}