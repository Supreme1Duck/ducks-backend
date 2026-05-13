package com.ducks.features.orders.database

import com.ducks.features.orders.data.dto.OrderDTO
import com.ducks.features.orders.data.dto.OrderProductDTO
import com.ducks.features.user.database.UserTable
import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.mapToOrderDTO(products: List<OrderProductDTO>): OrderDTO {

    return OrderDTO(
        id = this[CoffeeOrdersTable.id].value,
        createdAt = this[CoffeeOrdersTable.createdTime],
        userPhoneNumber = this[UserTable.phoneNumber],
        comment = this[CoffeeOrdersTable.comment],
        products = products,
        isActive = (this[CoffeeOrdersTable.acceptedTime] != null) and (this[CoffeeOrdersTable.finishedTime] == null),
        estimatedFinishTime = this[CoffeeOrdersTable.estimatedFinishTime] ?: 0L,
        price = this[CoffeeOrdersTable.totalPrice],
    )
}

fun ResultRow.toOrderProductDTO(): OrderProductDTO {
    return OrderProductDTO(
        id = this[CoffeeOrderedProductsTable.id].value,
        orderId = this[CoffeeOrderedProductsTable.orderId].value,
        name = this[CoffeeOrderedProductsTable.productName],
        constructors = this[CoffeeOrderedProductsTable.constructors],
        imageUrl = this[CoffeeOrderedProductsTable.imageUrl],
        size = this[CoffeeOrderedProductsTable.selectedSize],
        price = this[CoffeeOrderedProductsTable.price] ?: 0.toBigDecimal(),
        quantity = this[CoffeeOrderedProductsTable.quantity],
    )
}