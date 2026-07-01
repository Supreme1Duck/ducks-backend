package com.ducks.features.orders.database

import com.ducks.features.orders.database.model.ConstructorListSerializer
import com.ducks.features.orders.database.model.SelectedSizeSerializer
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

    val selectedSize = json(
        name = "selected_size",
        serialize = SelectedSizeSerializer::serialize,
        deserialize = SelectedSizeSerializer::deserialize,
    )

    val price = decimal("price", precision = 15, scale = 2).nullable()

    val minutesToCook = integer("minutes_to_cook").nullable()
    val quantity = integer("quantity")
}