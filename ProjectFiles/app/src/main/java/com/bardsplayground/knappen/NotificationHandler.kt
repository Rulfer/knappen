package com.bardsplayground.knappen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHandler(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "knappen_channel"
        private const val NOTIFICATION_ID = 1
    }

    /**
     * Posts the notification if the user has allowed notifications; silently does nothing otherwise.
     * This is safe to call from a background receiver (it never opens an activity).
     */
    fun createNotification(message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        if (!PermissionUtils.isNotificationPermissionGranted(context)) return

        showNotification(notificationManager, message)
    }

    /**
     * Opens the permission explanation screen if permission is missing and it has not been shown before.
     * Only call this as a result of a user action (e.g. tapping the widget).
     */
    fun promptForPermissionOnce(prefs: PrefsManager) {
        if (PermissionUtils.isNotificationPermissionGranted(context) || prefs.wasPermissionPrompted()) return

        val intent = Intent(context, PermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun showNotification(notificationManager: NotificationManager, message: String) {
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(AppLanguage.localized(context).getString(R.string.notification_title))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openApp)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    /** Re-creates the channel so its name (shown in system settings) follows a language change. */
    fun refreshChannel() {
        createNotificationChannel(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Creating an existing channel again only updates its name; the user's settings are kept.
            val channel = NotificationChannel(
                CHANNEL_ID,
                AppLanguage.localized(context).getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}
