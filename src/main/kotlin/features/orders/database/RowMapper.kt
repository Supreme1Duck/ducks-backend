package com.ducks.features.orders.database

import com.ducks.features.coffeeshops.client.data.model.dto.CoffeeProductSizeDTO
import com.ducks.features.orders.data.dto.OrderDTO
import com.ducks.features.orders.data.dto.OrderProductDTO
import com.ducks.features.orders.data.dto.OrderStatus
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
        isTakeaway = this[CoffeeOrdersTable.isTakeaway],
        products = products,
        isActive = (this[CoffeeOrdersTable.acceptedTime] != null) and (this[CoffeeOrdersTable.finishedTime] == null),
        isReady = (this[CoffeeOrdersTable.readyTime] != null) and (this[CoffeeOrdersTable.finishedTime] == null),
        estimatedFinishTime = this[CoffeeOrdersTable.estimatedFinishTime] ?: 0L,
        price = this[CoffeeOrdersTable.totalPrice],
    )
}

// Статус заказа по флагам и таймстампам. Один и тот же для клиента и для продавца.
// Требует, чтобы в строке были все колонки CoffeeOrdersTable.
fun ResultRow.toOrderStatus(): OrderStatus {
    return when {
        this[CoffeeOrdersTable.isExpired] -> OrderStatus.EXPIRED
        this[CoffeeOrdersTable.isCancelledByClient] || this[CoffeeOrdersTable.isCancelledBySeller] -> OrderStatus.CANCELLED
        this[CoffeeOrdersTable.isNotPickedUp] -> OrderStatus.NOT_PICKED_UP
        this[CoffeeOrdersTable.finishedTime] != null -> OrderStatus.COMPLETED
        this[CoffeeOrdersTable.readyTime] != null -> OrderStatus.READY
        this[CoffeeOrdersTable.acceptedTime] == null -> OrderStatus.PENDING
        else -> OrderStatus.IN_PROGRESS
    }
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