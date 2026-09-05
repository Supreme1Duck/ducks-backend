package com.ducks.features.config.data

import com.ducks.features.config.database.ClientConfigTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.update

class ClientConfigDataSource {

    /** Все переключённые флаги: ключ фичи — включена ли она. */
    fun fetchOverrides(): Map<String, Boolean> {
        return ClientConfigTable
            .select(ClientConfigTable.featureKey, ClientConfigTable.isEnabled)
            .associate { it[ClientConfigTable.featureKey] to it[ClientConfigTable.isEnabled] }
    }

    /**
     * Флаг переключается редко и всегда по одному, поэтому обходимся update + insert
     * вместо upsert: ключ уникальный, гонка двух админов упрётся в индекс, а не
     * размножит строки.
     */
    fun saveOverride(key: String, isEnabled: Boolean, updatedAt: Long) {
        val updatedRows = ClientConfigTable.update({ ClientConfigTable.featureKey eq key }) { row ->
            row[ClientConfigTable.isEnabled] = isEnabled
            row[ClientConfigTable.updatedAt] = updatedAt
        }

        if (updatedRows > 0) return

        ClientConfigTable.insert { row ->
            row[featureKey] = key
            row[ClientConfigTable.isEnabled] = isEnabled
            row[ClientConfigTable.updatedAt] = updatedAt
        }
    }
}
