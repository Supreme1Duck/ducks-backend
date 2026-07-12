package com.ducks.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory

class PushNotificationService {

    private val logger = LoggerFactory.getLogger(PushNotificationService::class.java)

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
    fun send(fcmToken: String, title: String, body: String) {
        send(clientMessaging, fcmToken, title, body)
    }

    /** Пуш продавцу (отдельный Firebase проект). */
    fun sendToSeller(fcmToken: String, title: String, body: String) {
        val messaging = sellerMessaging
        if (messaging == null) {
            logger.error("Попытка отправить пуш продавцу, но seller Firebase проект не сконфигурирован")
            return
        }
        send(messaging, fcmToken, title, body)
    }

    private fun send(messaging: FirebaseMessaging, fcmToken: String, title: String, body: String) {
        try {
            val message = Message.builder()
                .setToken(fcmToken)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .build()

            messaging.send(message)
        } catch (e: Exception) {
            logger.error("Failed to send push notification to token $fcmToken", e)
        }
    }
}
