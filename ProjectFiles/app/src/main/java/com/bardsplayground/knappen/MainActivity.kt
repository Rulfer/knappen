package com.bardsplayground.knappen

import android.Manifest
import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Launcher screen: explains how to add the widget and shows / fixes the two things Knappen depends on
 * (notification permission and, on Android 12+, exact alarms).
 */
class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.localized(newBase))
    }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) openNotificationSettings()
            updateStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // targetSdk 35+ draws edge-to-edge, so pad the content by the system bars.
        val root = findViewById<View>(R.id.main_root)
        val padding = (24 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                padding + bars.left,
                padding + bars.top,
                padding + bars.right,
                padding + bars.bottom
            )
            insets
        }

        setupAddWidgetButton()

        findViewById<Button>(R.id.btn_notifications).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings()
            }
        }

        findViewById<Button>(R.id.btn_exact_alarm).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                )
            }
        }

        findViewById<Button>(R.id.btn_open_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun setupAddWidgetButton() {
        val button = findViewById<Button>(R.id.btn_add_widget)
        val manager = AppWidgetManager.getInstance(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
            button.visibility = View.VISIBLE
            button.setOnClickListener {
                manager.requestPinAppWidget(ComponentName(this, MainWidget::class.java), null, null)
            }
        }
    }

    private fun updateStatus() {
        // Notifications
        val notificationsOk = PermissionUtils.isNotificationPermissionGranted(this)
        findViewById<TextView>(R.id.status_notifications).setText(
            if (notificationsOk) R.string.main_notifications_on else R.string.main_notifications_off
        )
        findViewById<Button>(R.id.btn_notifications).visibility =
            if (notificationsOk) View.GONE else View.VISIBLE

        // Exact alarms only need a user decision on Android 12+
        val exactRow = findViewById<TextView>(R.id.status_exact)
        val exactButton = findViewById<Button>(R.id.btn_exact_alarm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            // Always allowed on API 33+ (USE_EXACT_ALARM); only API 31-32 users can switch it off.
            val exactOk = alarmManager.canScheduleExactAlarms()
            exactRow.visibility = if (exactOk) View.GONE else View.VISIBLE
            exactRow.setText(R.string.main_exact_off)
            exactButton.visibility = if (exactOk) View.GONE else View.VISIBLE
        } else {
            exactRow.visibility = View.GONE
            exactButton.visibility = View.GONE
        }
    }

    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            // API 24-25 has no per-app notification settings action; the app info screen links to it.
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        }
        startActivity(intent)
    }
}
