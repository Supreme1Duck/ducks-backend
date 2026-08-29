package com.ducks.admin.response

import kotlinx.serialization.Serializable

/**
 * Ответ на создание товара вместе с картинкой. Ссылка возвращается, чтобы админка
 * показала карточку сразу, не перезапрашивая товар ради одного поля.
 */
@Serializable
data class CreatedCoffeeProductResponse(
    val id: Long,
    val imageUrl: String,
)
