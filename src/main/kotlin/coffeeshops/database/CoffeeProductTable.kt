package com.ducks.coffeeshops.database

import com.ducks.coffeeshops.common.data.model.dto.CoffeeShopSizeSerializer
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.json.json
import util.citext

object CoffeeProductTable: LongIdTable("ducks_coffee_shop_product_table") {

    val name = citext("name", 100)
    val description = text("description").nullable()
    val priceFrom = decimal("price", precision = 10, scale = 2)
    val categoryId = reference("category_id", CoffeeProductCategoryTable)

    val constructorsId = reference("constructors", CoffeeProductsWithConstructorsTable).nullable()

    val shopId = reference("shop_id", CoffeeShopTable)
    val sizes = json(
        "sizes",
        serialize = CoffeeShopSizeSerializer::serialize,
        deserialize = CoffeeShopSizeSerializer::deserialize,
    )

    val imageUrl = text("imageUrl")

    val carbohydrates = varchar("carbohydrates", 15).nullable()
    val protein = varchar("protein", 15).nullable()
    val fats = varchar("fats", 15).nullable()
    val calories = varchar("calories", 15).nullable()
}