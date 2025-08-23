package com.ducks.features.orders.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object CoffeeOrderedProductsTable: LongIdTable("ducks_coffee_ordered_products_table") {

    val orderId = reference("order_id", CoffeeOrdersTable)

    val productName = text("product_name")
    val imageUrl = text("image_url").nullable()

    // Списки через запятую
    val constructors = text("constructors").nullable()
    val constructorIds = text("constructor_ids").nullable()

    val needToCook = bool("need_to_cook").default(false)

    val price = decimal("price", precision = 15, scale = 2)
}