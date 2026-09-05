package com.ducks.features.config.model

import kotlinx.serialization.Serializable

/**
 * Ответ /config. Флаги отдаются мапой, а не полями: сервер добавляет новые ключи,
 * не ломая старые сборки приложения, а клиент читает только те, которые знает,
 * и подставляет свой дефолт для отсутствующих.
 */
@Serializable
data class ClientConfigResponse(
    val features: Map<String, Boolean>,
)
