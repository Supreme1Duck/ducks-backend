package com.ducks.features.config.model

/**
 * Всё, что клиентское приложение спрашивает у сервера на старте.
 *
 * Список флагов и их значения по умолчанию живут здесь, а не в базе: приложение должно
 * стартовать с осмысленным конфигом, даже если /config не ответил. В базе лежат только
 * переключения — [com.ducks.features.config.database.ClientConfigTable].
 *
 * Новая фича — одна строка в этом enum. Ключ уезжает клиенту как есть, поэтому
 * переименовывать его после релиза нельзя: старые версии приложения перестанут
 * его находить и уедут на свой дефолт.
 */
enum class ClientFeature(val key: String, val enabledByDefault: Boolean) {

    /**
     * Подбор допродажи к корзине — POST /coffee-shops/cart/recommendations.
     * Выключенный флаг не только прячет карусель в приложении: сервер и сам начинает
     * отдавать пустой список, иначе старые клиенты продолжат её показывать.
     */
    CART_RECOMMENDATIONS("cartRecommendations", enabledByDefault = true);

    companion object {
        fun ofKey(key: String): ClientFeature? = entries.firstOrNull { it.key == key }
    }
}
