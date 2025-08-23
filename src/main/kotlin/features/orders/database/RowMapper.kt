package com.ducks.features.orders.database

import com.ducks.features.orders.data.dto.OrderDTO
import com.ducks.features.orders.data.dto.OrderProductDTO
import com.ducks.features.user.database.UserTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNull

fun ResultRow.mapToOrderDTO(products: List<OrderProductDTO>): OrderDTO {

    return OrderDTO(
        id = this[CoffeeOrdersTable.id].value,
        createdAt = this[CoffeeOrdersTable.createdTime],
        userPhoneNumber = this[UserTable.phoneNumber],
        comment = this[CoffeeOrdersTable.comment],
        products = products,
        isActive =  this[CoffeeOrdersTable.acceptedTime.isNotNull()] and this[CoffeeOrdersTable.finishedTime.isNull()]
    )
}

fun ResultRow.toOrderProductDTO(): OrderProductDTO {

    return OrderProductDTO(
        id = this[CoffeeOrderedProductsTable.id].value,
        orderId = this[CoffeeOrderedProductsTable.orderId].value,
        name = this[CoffeeOrderedProductsTable.productName],
        constructors = this[CoffeeOrderedProductsTable.constructors],
        imageUrl = this[CoffeeOrderedProductsTable.imageUrl],
        price = this[CoffeeOrderedProductsTable.price],
    )
}