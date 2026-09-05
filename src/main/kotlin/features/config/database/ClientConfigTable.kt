package com.ducks.features.config.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * Переключённые флаги клиентского приложения. Хранятся только отличия от дефолтов
 * из [com.ducks.features.config.model.ClientFeature]: нет строки — фича работает так,
 * как задумано в коде. Пустая таблица штатна, чистить её безопасно.
 */
object ClientConfigTable : LongIdTable("ducks_client_config_table") {

    val featureKey = varchar("feature_key", 64)

    val isEnabled = bool("is_enabled")

    val updatedAt = long("updated_at")

    init {
        uniqueIndex(featureKey)
    }
}
