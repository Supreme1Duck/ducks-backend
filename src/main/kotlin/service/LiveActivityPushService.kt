package com.ducks.service

import com.ducks.features.coffeeshops.database.CoffeeShopTable
import com.ducks.features.orders.database.CoffeeOrderedProductsTable
import com.ducks.features.orders.database.CoffeeOrdersTable
import com.ducks.features.user.database.UserTable
import com.google.auth.oauth2.ServiceAccountCredentials
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Updates and ends a client-created ActivityKit activity through FCM HTTP v1. */
class LiveActivityPushService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("live-activities"))
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    private val serviceAccount = ServiceAccountCredentials.fromStream(
        requireNotNull(javaClass.getResourceAsStream("/firebase-adminsdk.json"))
    )
    private val credentials = serviceAccount.createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))
    private val endpoint = URI.create("https://fcm.googleapis.com/v1/projects/${serviceAccount.projectId}/messages:send")

    /** Called after the order status has committed. Delivery does not delay the request. */
    fun update(orderId: Long) = send(orderId, "update")
    fun end(orderId: Long) = send(orderId, "end")

    private fun send(orderId: Long, event: String) {
        scope.launch {
            try {
                val target = loadTarget(orderId, event) ?: return@launch
                val now = System.currentTimeMillis() / 1000
                val payload = buildJsonObject {
                    putJsonObject("message") {
                        put("token", target.fcmToken)
                        putJsonObject("apns") {
                            put("live_activity_token", target.activityToken)
                            putJsonObject("headers") { put("apns-priority", if (event == "end") "10" else "5") }
                            putJsonObject("payload") {
                                putJsonObject("aps") {
                                    put("timestamp", now)
                                    put("event", event)
                                    if (event == "end") put("dismissal-date", now - 1)
                                    putJsonObject("content-state") {
                                        put("statusTitle", target.statusTitle)
                                        put("shopName", target.shopName)
                                        put("shopAddress", target.shopAddress)
                                        put("itemsLine", target.itemsLine)
                                        put("hint", target.hint)
                                        put("step", target.step)
                                        putJsonArray("stepLabels") {
                                            add(JsonPrimitive("Ожидает"))
                                            add(JsonPrimitive("Принят"))
                                            add(JsonPrimitive("Готовится"))
                                            add(JsonPrimitive("Готов"))
                                        }
                                        val readyAt = target.readyAtEpochMs
                                        // Swift Codable Date defaults to seconds since 2001-01-01.
                                        if (readyAt == null || readyAt <= 0) {
                                            put("readyAt", JsonNull)
                                        } else {
                                            put("readyAt", readyAt / 1000.0 - APPLE_REFERENCE_DATE_OFFSET_SECONDS)
                                        }
                                        put("isReady", target.isReady)
                                    }
                                }
                            }
                        }
                    }
                }
                val body = Json.encodeToString(JsonObject.serializer(), payload)
                sendWithRetry(orderId, event, body)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to {} Live Activity for order {}", event, orderId, e)
            }
        }
    }

    private suspend fun sendWithRetry(orderId: Long, event: String, body: String) {
        repeat(3) { attempt ->
            try {
                val status = withContext(Dispatchers.IO) {
                    credentials.refreshIfExpired()
                    val request = HttpRequest.newBuilder(endpoint)
                        .timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Bearer ${credentials.accessToken.tokenValue}")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build()
                    http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
                }
                if (status in 200..299) return
                if (status !in 500..599 && status != 429) {
                    logger.warn("Failed to {} Live Activity for order {}: FCM HTTP {}", event, orderId, status)
                    return
                }
                if (attempt == 2) {
                    logger.warn("Failed to {} Live Activity for order {} after retries: FCM HTTP {}", event, orderId, status)
                    return
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt == 2) throw e
            }
            delay(500L * (attempt + 1))
        }
    }

    private suspend fun loadTarget(orderId: Long, event: String): Target? = newSuspendedTransaction {
        val row = CoffeeOrdersTable
            .join(UserTable, JoinType.INNER, CoffeeOrdersTable.userId, UserTable.id)
            .join(CoffeeShopTable, JoinType.INNER, CoffeeOrdersTable.coffeeShop, CoffeeShopTable.id)
            .select(
                CoffeeOrdersTable.liveActivityToken,
                CoffeeOrdersTable.finishedTime,
                CoffeeOrdersTable.isCancelledByClient, CoffeeOrdersTable.isCancelledBySeller,
                CoffeeOrdersTable.isExpired, CoffeeOrdersTable.isNotPickedUp,
                CoffeeOrdersTable.acceptedTime, CoffeeOrdersTable.readyTime,
                CoffeeOrdersTable.estimatedFinishTime,
                CoffeeShopTable.name, CoffeeShopTable.address, UserTable.fcmToken,
            )
            .where { CoffeeOrdersTable.id eq orderId }
            .firstOrNull() ?: return@newSuspendedTransaction null

        val finished = row[CoffeeOrdersTable.finishedTime] != null
        val isEndEvent = event == "end"

        if (isEndEvent && !finished) {
            return@newSuspendedTransaction null
        }
        if (!isEndEvent && finished) {
            return@newSuspendedTransaction null
        }

        val activityToken = row[CoffeeOrdersTable.liveActivityToken] ?: return@newSuspendedTransaction null
        val fcmToken = row[UserTable.fcmToken] ?: return@newSuspendedTransaction null
        val products = CoffeeOrderedProductsTable
            .select(CoffeeOrderedProductsTable.productName, CoffeeOrderedProductsTable.quantity)
            .where { CoffeeOrderedProductsTable.orderId eq orderId }
            .map { item ->
                val name = item[CoffeeOrderedProductsTable.productName]
                val quantity = item[CoffeeOrderedProductsTable.quantity]
                if (quantity > 1) "$name ×$quantity" else name
            }

        val cancelled = row[CoffeeOrdersTable.isCancelledByClient] ||
            row[CoffeeOrdersTable.isCancelledBySeller] || row[CoffeeOrdersTable.isExpired]
        val notPickedUp = row[CoffeeOrdersTable.isNotPickedUp]
        val ready = row[CoffeeOrdersTable.readyTime] != null
        val accepted = row[CoffeeOrdersTable.acceptedTime] != null
        Target(
            activityToken = activityToken,
            fcmToken = fcmToken,
            shopName = row[CoffeeShopTable.name],
            shopAddress = row[CoffeeShopTable.address],
            itemsLine = products.joinToString(" · "),
            statusTitle = when {
                cancelled -> "Заказ отменён"
                notPickedUp -> "Заказ не забран"
                finished -> "Заказ выдан"
                ready -> "Готов — забирайте"
                accepted -> "Готовится"
                else -> "Ожидает принятия"
            },
            hint = when {
                ready || finished -> "номер заказа"
                accepted -> "до готовности"
                else -> "обычно до 2 минут"
            },
            step = when {
                ready || finished -> 3
                accepted -> 2
                else -> 0
            },
            readyAtEpochMs = row[CoffeeOrdersTable.estimatedFinishTime],
            isReady = ready && !cancelled && !notPickedUp,
        )
    }

    private data class Target(
        val activityToken: String,
        val fcmToken: String,
        val shopName: String,
        val shopAddress: String,
        val itemsLine: String,
        val statusTitle: String,
        val hint: String,
        val step: Int,
        val readyAtEpochMs: Long?,
        val isReady: Boolean,
    )

    private companion object {
        const val APPLE_REFERENCE_DATE_OFFSET_SECONDS = 978307200.0
    }
}
