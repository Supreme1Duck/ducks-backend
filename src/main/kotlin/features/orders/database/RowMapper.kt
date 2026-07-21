package com.ducks.features.orders.database

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.orders.data.dto.OrderDTO
import com.ducks.features.orders.data.dto.OrderProductDTO
import com.ducks.features.orders.database.model.OrderedProductConstructorDBModel
import com.ducks.features.user.database.UserTable
import org.jetbrains.exposed.v1.core.ResultRow
import java.math.BigDecimal

fun ResultRow.mapToOrderDTO(products: List<OrderProductDTO>): OrderDTO {

    return OrderDTO(
        id = this[CoffeeOrdersTable.id].value,
        createdAt = this[CoffeeOrdersTable.createdTime],
        userPhoneNumber = this[UserTable.phoneNumber],
        comment = this[CoffeeOrdersTable.comment],
        products = products,
        isActive = (this[CoffeeOrdersTable.acceptedTime] != null) and (this[CoffeeOrdersTable.finishedTime] == null),
        isReady = (this[CoffeeOrdersTable.readyTime] != null) and (this[CoffeeOrdersTable.finishedTime] == null),
        estimatedFinishTime = this[CoffeeOrdersTable.estimatedFinishTime] ?: 0L,
        price = this[CoffeeOrdersTable.totalPrice],
    )
}

fun ResultRow.toOrderProductDTO(): OrderProductDTO {
    return OrderProductDTO(
        id = this[CoffeeOrderedProductsTable.id].value,
        orderId = this[CoffeeOrderedProductsTable.orderId].value,
        name = this[CoffeeOrderedProductsTable.productName],
        constructors = this[CoffeeOrderedProductsTable.constructors]?.toOrderProductConstructors(),
        imageUrl = this[CoffeeOrderedProductsTable.imageUrl],
        size = this[CoffeeOrderedProductsTable.selectedSize].toOrderProductSize(),
        unitPrice = orderedProductUnitPrice(),
        price = this[CoffeeOrderedProductsTable.price] ?: 0.toBigDecimal(),
        quantity = this[CoffeeOrderedProductsTable.quantity],
    )
}

// Цена за одну штуку. Считается так же, как при создании заказа: размер + конструкторы.
// В колонке price лежит стоимость всей позиции, то есть unitPrice * quantity.
fun ResultRow.orderedProductUnitPrice(): BigDecimal {
    val sizePrice = this[CoffeeOrderedProductsTable.selectedSize].price
    val constructorsPrice = this[CoffeeOrderedProductsTable.constructors]
        ?.sumOf { it.price ?: BigDecimal.ZERO }
        ?: BigDecimal.ZERO

    return sizePrice + constructorsPrice
}

private fun CoffeeProductSizeDTO.toOrderProductSize() = OrderProductDTO.Size(
    id = id,
    sizeName = sizeName,
    sizeValue = sizeValue,
    price = price,
)

private fun List<OrderedProductConstructorDBModel>.toOrderProductConstructors() = map {
    OrderProductDTO.Constructor(id = it.id, name = it.name, price = it.price)
}