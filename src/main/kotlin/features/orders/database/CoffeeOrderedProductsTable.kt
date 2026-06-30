package com.ducks.features.orders.database

import com.ducks.features.orders.database.model.ConstructorListSerializer
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.json.json

object CoffeeOrderedProductsTable: LongIdTable("ducks_coffee_ordered_products_table") {

    val orderId = reference("order_id", CoffeeOrdersTable)

    val productName = text("product_name")
    val productId = long("product_id")
    val imageUrl = text("image_url").nullable()

    val constructors = json(
        name = "constructors",
        serialize = ConstructorListSerializer::serialize,
        deserialize = ConstructorListSerializer::deserialize,
    ).nullable()

    val selectedSize = text("size").nullable()

    val selectedSizeName = text("selected_size_name").nullable()
    val selectedSizeValue = text("selected_size_value")
    val selectedSizePrice = decimal("selected_size_price", precision = 15, scale = 2)

    val price = decimal("price", precision = 15, scale = 2).nullable()

    val minutesToCook = integer("minutes_to_cook").nullable()
    val quantity = integer("quantity")
}