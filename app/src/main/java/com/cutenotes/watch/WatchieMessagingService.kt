package com.cutenotes.watch

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives pushes from Firebase Cloud Messaging so notes can arrive even when
 * Watchie is closed. The Cloud Function (see /functions) sends one of these when
 * a note lands in your inbox.
 */
class WatchieMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Keep the user's delivery token fresh in Firestore.
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val from = message.data["fromName"] ?: message.notification?.title
        val body = if (from != null) "@$from sent you a note" else (message.notification?.body ?: "You got a note")
        Notifications.show(applicationContext, "Watchie 💌", body)
    }
}
