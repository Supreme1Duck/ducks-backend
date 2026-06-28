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

    init {
        val serviceAccount = PushNotificationService::class.java
            .getResourceAsStream("/firebase-adminsdk.json")
            ?: error("firebase-adminsdk.json not found in resources")

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        }
    }

    fun send(fcmToken: String, title: String, body: String) {
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

            FirebaseMessaging.getInstance().send(message)
        } catch (e: Exception) {
            logger.error("Failed to send push notification to token $fcmToken", e)
        }
    }
}
