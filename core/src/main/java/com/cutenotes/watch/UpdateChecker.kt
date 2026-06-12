package com.cutenotes.watch

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Result of checking whether a newer version is available. */
data class UpdateInfo(val available: Boolean, val message: String)

/**
 * Checks Firestore for the latest published version. Bump
 * `config/app.latestVersionCode` whenever you publish a new build; anyone on an
 * older versionCode then sees the update prompt.
 */
object UpdateChecker {
    suspend fun check(currentVersionCode: Int): UpdateInfo {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("config").document("app").get().await()
            val latest = (doc.getLong("latestVersionCode") ?: 0L).toInt()
            val message = doc.getString("updateMessage")
                ?: "A new version of Watchie is available."
            UpdateInfo(available = latest > currentVersionCode, message = message)
        } catch (e: Exception) {
            UpdateInfo(available = false, message = "")
        }
    }
}
