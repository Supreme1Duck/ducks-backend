package com.ducks.features.orders.data.dto

// Статус заказа. Общий для клиента и для продавца.
enum class OrderStatus(val value: Int) {
    // Заказ принят продавцом и готовится.
    IN_PROGRESS(0),
    COMPLETED(1),
    CANCELLED_BY_SELLER(2),
    // Заказ отменён самим клиентом, пока продавец его не принял.
    CANCELLED_BY_CLIENT(3),
    EXPIRED(4),
    // Заказ готов к выдаче, но ещё не выдан.
    READY(5),
    // Заказ был готов, но клиент его не забрал.
    NOT_PICKED_UP(6),
    // Заказ создан, но продавец его ещё не принял.
    PENDING(7),
}
