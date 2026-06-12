package com.cutenotes.watch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Builds and shows the "you got a note" notification. */
object Notifications {
    const val CHANNEL_ID = "notes"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notes",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { enableVibration(true) }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun show(context: Context, title: String, body: String) {
        ensureChannel(context)

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = launch?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply { if (pending != null) setContentIntent(pending) }
            .build()

        // notify() can throw if POST_NOTIFICATIONS isn't granted (API 33+).
        runCatching {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notification)
        }
    }
}
