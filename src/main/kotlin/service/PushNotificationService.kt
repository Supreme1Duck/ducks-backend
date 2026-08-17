package com.ducks.service

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.ApsAlert
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Платформы разведены намеренно, верхнеуровневый блок `notification` не заполняется:
 *
 *  - Android получает чистый data-only с `priority: HIGH` — уведомление рисует
 *    само приложение в `onMessageReceived`, иначе пуш не будет показан;
 *  - iOS получает обычный alert-пуш через `ApnsConfig.aps.alert`, потому что
 *    data-only на iOS — это silent push: система его троттлит, не доставляет
 *    вовсе после force-quit и глушит в Low Power Mode.
 *
 * Блок `data` едет в обоих случаях, поэтому навигация по `orderId` работает везде.
 *
 * Контракт payload (все значения — строки, так требует FCM):
 *  - `title`   — заголовок уведомления;
 *  - `body`    — текст уведомления;
 *  - `type`    — событие, см. [PushType];
 *  - `orderId` — id заказа, к которому относится событие (может отсутствовать).
 */
class PushNotificationService {

    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)

    // Отправка не должна держать вызывающий поток: FCM отвечает 100–500 мс, а на
    // сетевых проблемах Admin SDK ретраит и упирается в таймауты порядка 10 секунд.
    // SupervisorJob — чтобы падение одной отправки не убивало scope целиком.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("push-notifications"))

    // Приложение клиента (проект по умолчанию)
    private val clientMessaging: FirebaseMessaging = initMessaging(
        appName = "[DEFAULT]",
        resource = "/firebase-adminsdk.json",
    ) ?: error("firebase-adminsdk.json not found in resources")

    // Приложение продавца (отдельный Firebase проект)
    private val sellerMessaging: FirebaseMessaging? = initMessaging(
        appName = "seller",
        resource = "/firebase-adminsdk-seller.json",
    ).also {
        if (it == null) {
            logger.warn("firebase-adminsdk-seller.json not found in resources — пуши продавцам отключены")
        }
    }

    private fun initMessaging(appName: String, resource: String): FirebaseMessaging? {
        val serviceAccount = PushNotificationService::class.java
            .getResourceAsStream(resource)
            ?: return null

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        val app = FirebaseApp.getApps()
            .find { it.name == appName }
            ?: if (appName == "[DEFAULT]") {
                FirebaseApp.initializeApp(options)
            } else {
                FirebaseApp.initializeApp(options, appName)
            }

        return FirebaseMessaging.getInstance(app)
    }

    /** Пуш клиенту. */
    fun send(fcmToken: String, title: String, body: String, type: PushType, orderId: Long? = null) {
        send(clientMessaging, fcmToken, title, body, type, orderId)
    }

    /** Пуш продавцу (отдельный Firebase проект). */
    fun sendToSeller(fcmToken: String, title: String, body: String, type: PushType, orderId: Long? = null) {
        val messaging = sellerMessaging
        if (messaging == null) {
            logger.error("Попытка отправить пуш продавцу, но seller Firebase проект не сконфигурирован")
            return
        }
        send(messaging, fcmToken, title, body, type, orderId)
    }

    /**
     * Ставит отправку в фон и сразу возвращает управление: пуш не должен задерживать
     * ответ на создание/смену статуса заказа.
     */
    private fun send(
        messaging: FirebaseMessaging,
        fcmToken: String,
        title: String,
        body: String,
        type: PushType,
        orderId: Long?,
    ) {
        // Сообщение собираем на вызывающем потоке: apns-expiration должен считаться
        // от момента события, а не от момента, когда до отправки дойдёт очередь.
        val message = buildMessage(fcmToken, title, body, type, orderId)

        scope.launch {
            sendWithRetry(messaging, message, fcmToken)
        }
    }

    private suspend fun sendWithRetry(messaging: FirebaseMessaging, message: Message, fcmToken: String) {
        var delayMs = INITIAL_RETRY_DELAY_MS

        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                messaging.sendAsync(message).await()
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseMessagingException) {
                when (e.messagingErrorCode) {
                    // Токен мёртв: переустановка, очистка данных, ротация. Ретраить бесполезно.
                    MessagingErrorCode.UNREGISTERED,
                    MessagingErrorCode.INVALID_ARGUMENT,
                    MessagingErrorCode.SENDER_ID_MISMATCH -> {
                        logger.warn(
                            "FCM token is dead ({}), push dropped: token=$fcmToken",
                            e.messagingErrorCode,
                            e,
                        )
                        return
                    }

                    // Временный сбой на стороне FCM — имеет смысл повторить.
                    MessagingErrorCode.UNAVAILABLE,
                    MessagingErrorCode.INTERNAL,
                    MessagingErrorCode.QUOTA_EXCEEDED -> {
                        if (attempt == MAX_ATTEMPTS - 1) {
                            logger.error(
                                "Failed to send push after $MAX_ATTEMPTS attempts ({}): token=$fcmToken",
                                e.messagingErrorCode,
                                e,
                            )
                            return
                        }
                        logger.warn(
                            "Transient FCM error ({}), retry in $delayMs ms: token=$fcmToken",
                            e.messagingErrorCode,
                        )
                        delay(delayMs)
                        delayMs *= RETRY_BACKOFF_FACTOR
                    }

                    else -> {
                        logger.error("Failed to send push notification to token $fcmToken", e)
                        return
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to send push notification to token $fcmToken", e)
                return
            }
        }
    }

    private fun buildMessage(
        fcmToken: String,
        title: String,
        body: String,
        type: PushType,
        orderId: Long?,
    ): Message {
        return Message.builder()
            .setToken(fcmToken)
            .putData("title", title)
            .putData("body", body)
            .putData("type", type.value)
            .apply {
                orderId?.let { putData("orderId", it.toString()) }
            }
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setTtl(TTL_MS)
                    .build()
            )
            .setApnsConfig(
                ApnsConfig.builder()
                    .putHeader("apns-push-type", "alert")
                    .putHeader("apns-priority", "10")
                    .putHeader("apns-expiration", ((System.currentTimeMillis() + TTL_MS) / 1000).toString())
                    .setAps(
                        Aps.builder()
                            .setAlert(
                                ApsAlert.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                            )
                            .setSound("default")
                            // Разбудить приложение, если система даст такую возможность.
                            // Показ уведомления от этого не зависит — его рисует iOS.
                            .setContentAvailable(true)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    /**
     * Ждёт [ApiFuture] без блокировки потока. Колбэк отдаёт исходное исключение,
     * поэтому [FirebaseMessagingException] не приходится доставать из `ExecutionException`.
     */
    private suspend fun <T> ApiFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
        ApiFutures.addCallback(
            this,
            object : ApiFutureCallback<T> {
                override fun onSuccess(result: T) {
                    continuation.resume(result)
                }

                override fun onFailure(t: Throwable) {
                    continuation.resumeWithException(t)
                }
            },
            DIRECT_EXECUTOR,
        )

        continuation.invokeOnCancellation { cancel(false) }
    }

    private companion object {
        // Заказ живёт минуты — протухший пуш («заказ готов» через час) только мешает.
        const val TTL_MS = 60 * 60 * 1000L

        const val MAX_ATTEMPTS = 3
        const val INITIAL_RETRY_DELAY_MS = 500L
        const val RETRY_BACKOFF_FACTOR = 3

        val DIRECT_EXECUTOR = Executor { it.run() }
    }
}

enum class PushType(val value: String) {
    ORDER_ACCEPTED("order_accepted"),
    ORDER_READY("order_ready"),
    ORDER_GIVEN_OUT("order_given_out"),
    ORDER_NOT_PICKED_UP("order_not_picked_up"),
    ORDER_CANCELLED_BY_SELLER("order_cancelled_by_seller"),
    // Клиент сам отменил заказ, который продавец ещё не принял. Уходит продавцу.
    ORDER_CANCELLED_BY_CLIENT("order_cancelled_by_client"),
    ORDER_EXPIRED("order_expired"),
    NEW_ORDER("new_order"),
}
