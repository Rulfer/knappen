package com.bardsplayground.knappen

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startActivity

class NotificationHandler (private val context: Context) {
    val channelId = "knappen_channel"

    fun createNotification(message: String) {

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        // Android 13+ → Check permission
        if (!PermissionUtils.isNotificationPermissionGranted(context)) {
            // Permission not granted, so request it.
            val intent = Intent(context, PermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("message", message)
            }
            context.startActivity(intent)
            return
        }

        // Permission is granted or not required → Show notification
        showNotification(notificationManager, message)
    }

    private fun showNotification(
        notificationManager: NotificationManager,
//        channelId: String,
        message: String
    ) {
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Knappen")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    fun createNotificationChannel(notificationManager: NotificationManager){
        // Create notification channel (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Knappen Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}

//    companion object {
//
//        fun createNotification(message: String) {
//
//            val notificationManager =
//                _context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            val channelId = "knappen_channel"
//
//            // Create channel (for Android O+)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                val channel = NotificationChannel(
//                    channelId,
//                    "Knappen Notifications",
//                    NotificationManager.IMPORTANCE_DEFAULT
//                )
//                notificationManager.createNotificationChannel(channel)
//            }
//
//            // Check permission (Android 13+)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                if (ActivityCompat.checkSelfPermission(
//                        _context,
//                        Manifest.permission.POST_NOTIFICATIONS
//                    ) != PackageManager.PERMISSION_GRANTED
//                ) {
//                    // Permission missing → open settings/explanation screen
//                    val intent = Intent(_context, PermissionActivity::class.java).apply {
//                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    }
//                    _context.startActivity(intent)
//                    return
//                }
//            }
//
//            // Build & show notification
//            val builder = NotificationCompat.Builder(_context, channelId)
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .setContentTitle("Knappen")
//                .setContentText(message)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setAutoCancel(true)
//
////            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
//        }
//    }
//}
