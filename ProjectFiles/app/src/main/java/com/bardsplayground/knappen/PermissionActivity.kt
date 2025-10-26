package com.bardsplayground.knappen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.RemoteViews
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionActivity : AppCompatActivity() {


    private var messageToShow: String = ""
//    companion object {
//        fun verifyPermission(){
//
//        }
//
//        private val requestPermissionLauncher =
//            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                NotificationHandler.createNotification("Notifications enabled ✅")
//                finish()
//            } else {
//                // Open system notification settings if denied
//                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
//                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
//                }
//                startActivity(intent)
//                finish()
//            }
//            }
//    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Permission granted → Show the original message
                NotificationHandler(this).createNotification(messageToShow)
            } else {
                // Permission denied → Redirect to app settings
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivity(intent)
            }
            finish()
        }

//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
////                NotificationHandler.createNotification("Notifications enabled ✅")
//                finish()
//            } else {
//                // Open system notification settings if denied
//                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
//                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
//                }
//                startActivity(intent)
//                finish()
//            }
//        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("PermissionActivity", "onCreate")

        messageToShow = intent.getStringExtra("message") ?: "Notification permission granted."

        if (PermissionUtils.isNotificationPermissionGranted(this)) {
            NotificationHandler(this).createNotification(messageToShow)
            finish()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setContentView(R.layout.activity_permission)
            val myButton: Button = findViewById(R.id.request_button)
            myButton.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Just in case: older versions (won’t trigger)
            NotificationHandler(this).createNotification(messageToShow)
            finish()
        }
    }
}
