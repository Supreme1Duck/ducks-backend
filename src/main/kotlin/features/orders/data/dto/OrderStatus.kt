package com.ducks.features.orders.data.dto

// Статус заказа. Общий для клиента и для продавца.
enum class OrderStatus(val value: Int) {
    // Заказ принят продавцом и готовится.
    IN_PROGRESS(0),
    COMPLETED(1),
    CANCELLED(2),
    EXPIRED(3),
    // Заказ готов к выдаче, но ещё не выдан.
    READY(4),
    // Заказ был готов, но клиент его не забрал.
    NOT_PICKED_UP(5),
    // Заказ создан, но продавец его ещё не принял.
    PENDING(6),
}
