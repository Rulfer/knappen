package com.bardsplayground.knappen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Restores a running timer after a reboot or an app update (both remove scheduled alarms).
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d("Knappen", "Restoring timer after ${intent.action}")
                MainButtonHandler(context).onBootOrUpdate()
            }
        }
    }
}
