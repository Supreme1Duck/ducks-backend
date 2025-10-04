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

    val secondsToCook = integer("seconds_to_cook").nullable()
}