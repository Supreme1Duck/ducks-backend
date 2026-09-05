package com.ducks.features.config.domain

import com.ducks.features.config.data.ClientConfigDataSource
import com.ducks.features.config.model.ClientConfigResponse
import com.ducks.features.config.model.ClientFeature
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/**
 * Конфиг клиентского приложения: дефолты из [ClientFeature], поверх них — переключения
 * из базы.
 *
 * Значения держатся в памяти [CACHE_TTL_MILLIS]: /config дёргает каждый запуск приложения,
 * а меняется он раз в месяц. Переключение из админки сбрасывает кэш сразу, поэтому TTL —
 * это задержка только для соседних инстансов сервера, а не для самого рубильника.
 */
class ClientConfigRepository(
    private val dataSource: ClientConfigDataSource,
) {

    @Volatile
    private var cachedFlags: Map<String, Boolean>? = null

    @Volatile
    private var cachedAt: Long = 0

    suspend fun getConfig(): ClientConfigResponse = ClientConfigResponse(features = flags())

    suspend fun isEnabled(feature: ClientFeature): Boolean =
        flags()[feature.key] ?: feature.enabledByDefault

    suspend fun setEnabled(feature: ClientFeature, isEnabled: Boolean): ClientConfigResponse {
        newSuspendedTransaction {
            dataSource.saveOverride(
                key = feature.key,
                isEnabled = isEnabled,
                updatedAt = System.currentTimeMillis(),
            )
        }

        cachedFlags = null

        return getConfig()
    }

    private suspend fun flags(): Map<String, Boolean> {
        val cached = cachedFlags
        if (cached != null && System.currentTimeMillis() - cachedAt < CACHE_TTL_MILLIS) return cached

        // База тут не обязательна: конфиг — это дефолты плюс правки. Упавший запрос
        // (код уехал вперёд миграции, база прилегла) не должен ронять старт приложения,
        // поэтому отдаём дефолты и пробуем снова на следующем запросе, не запоминая их.
        val overrides = try {
            newSuspendedTransaction { dataSource.fetchOverrides() }
        } catch (e: Exception) {
            println("Конфиг клиента недоступен, отдаём значения по умолчанию: $e")
            return defaults()
        }

        val flags = ClientFeature.entries.associate { feature ->
            feature.key to (overrides[feature.key] ?: feature.enabledByDefault)
        }

        cachedFlags = flags
        cachedAt = System.currentTimeMillis()

        return flags
    }

    private fun defaults(): Map<String, Boolean> =
        ClientFeature.entries.associate { it.key to it.enabledByDefault }

    private companion object {
        const val CACHE_TTL_MILLIS = 60_000L
    }
}
