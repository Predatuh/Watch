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

    // Notification messages are shown by the system when the app is in the
    // background/closed (buzz + wrist-raise, like a text). When the app is in
    // the foreground the in-app inbox listener plays the note directly, so we
    // don't post a duplicate here. Wear OS does not honor full-screen intents
    // or allow background activity launches, so a closed app can't auto-play
    // the animation without a tap — the notification carries the content and
    // tapping it opens the animated note.
    override fun onMessageReceived(message: RemoteMessage) {}
}
