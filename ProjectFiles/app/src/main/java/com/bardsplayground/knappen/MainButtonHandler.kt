package com.bardsplayground.knappen

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission

class MainButtonHandler(private val context: Context) {

    companion object {
        private const val TAG = "Knappen"

        /**
         * One fixed request code for the single, shared timer alarm. Must not end in 1
         * (older versions used `widgetId * 10 + 1`, see [cancelLegacyAlarms]).
         */
        const val ALARM_REQUEST_CODE = 42

        /** Request code of the (non-waking) midnight alarm that redraws the widgets when the daily count resets. */
        private const val DAY_CHANGE_REQUEST_CODE = 43
    }

    val prefs = PrefsManager(context)
    private val notificationHandler = NotificationHandler(context)

    fun onMainButtonClicked() {
        Log.d(TAG, "onMainButtonClicked")

        if (prefs.isTimerActive() || prefs.isDailyLimitReached()) {
            // Still locked, or all of today's taps are used - just make sure the widgets show the right state.
            refreshAllWidgets()
            return
        }

        // Ask for notification permission once, from a user-initiated tap.
        notificationHandler.promptForPermissionOnce(prefs)
        prefs.recordClick()
        startTimerTryCatch()
    }

    fun onResetButtonClicked() {
        Log.d(TAG, "onResetButtonClicked")
        cancelAlarm()
    }

    /**
     * Called by the alarm. Notifies the user and makes the button clickable again.
     * The timer also runs after the last tap of the day (so the lock still holds across midnight), but then the
     * button is not really clickable until midnight, so no notification is sent.
     */
    fun onTimerTriggered() {
        if (!prefs.isDailyLimitReached()) {
            notificationHandler.createNotification(
                AppLanguage.localized(context).getString(R.string.notification_ready)
            )
        }
        prefs.setTimerActive(active = false)
        refreshAllWidgets()
    }

    /**
     * Called after boot and after the app has been updated (both drop scheduled alarms).
     * Re-schedules a running timer, or finishes one that ran out while the phone was off.
     */
    fun onBootOrUpdate() {
        if (prefs.hasStoredTimer()) {
            val triggerAt = prefs.getTriggerTime()
            if (triggerAt > System.currentTimeMillis()) {
                startTimerTryCatch(triggerAt)
            } else {
                onTimerTriggered()
            }
        }
        refreshAllWidgets()
    }

    private fun startTimerTryCatch(triggerAt: Long = -1L) {
        try {
            startTimer(triggerAt)
        } catch (e: SecurityException) {
            Log.e(TAG, "Could not schedule alarm: ${e.message}")
        }
    }

    @RequiresPermission(value = "android.permission.SCHEDULE_EXACT_ALARM", conditional = true)
    private fun startTimer(triggerAt: Long = -1L) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime =
            if (triggerAt != -1L) triggerAt else System.currentTimeMillis() + prefs.getTimerDuration()
        val pendingIntent = alarmPendingIntent()

        cancelLegacyAlarms(alarmManager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Only possible on API 31-32 when the user revoked SCHEDULE_EXACT_ALARM (API 33+ has USE_EXACT_ALARM).
            // Inexact alarms can be very late, and the widget countdown then runs past 0:00.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }

        prefs.setTriggerTime(triggerTime)
        prefs.setTimerActive(active = true)
        refreshAllWidgets()
    }

    private fun cancelAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent())
        cancelLegacyAlarms(alarmManager)

        prefs.setTimerActive(active = false)
        refreshAllWidgets()
    }

    private fun alarmPendingIntent(): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, Timer::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * Versions up to 1.4 used `appWidgetId * 10 + 1` as request code (and -9 after a reboot).
     * Cancel any such alarms left over from an older version so they cannot fire a second time.
     */
    private fun cancelLegacyAlarms(alarmManager: AlarmManager) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, MainWidget::class.java))
        val legacyCodes = ids.map { it * 10 + 1 } + (-9)

        for (code in legacyCodes.filter { it != ALARM_REQUEST_CODE }) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                Intent(context, Timer::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun refreshAllWidgets() {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, MainWidget::class.java)
        val ids = manager.getAppWidgetIds(component)
        for (id in ids) {
            updateAppWidget(context, manager, id)
        }
        if (ids.isNotEmpty()) scheduleDayChangeRefresh()
    }

    /**
     * Redraw the widgets at the next local midnight, when the daily count resets ("x left today", and a capped
     * widget becomes tappable again). RTC (not _WAKEUP): delivered when the phone is next awake, which is enough
     * for a display update. Re-scheduling replaces the previous one (same PendingIntent).
     */
    fun scheduleDayChangeRefresh() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = PrefsManager.nextMidnight(System.currentTimeMillis())
        val pendingIntent = dayChangePendingIntent()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC, triggerAt, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC, triggerAt, pendingIntent)
        }
    }

    fun cancelDayChangeRefresh() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(dayChangePendingIntent())
    }

    private fun dayChangePendingIntent(): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            DAY_CHANGE_REQUEST_CODE,
            Intent(context, MainWidget::class.java).setAction(MainWidget.ACTION_DAY_CHANGED),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
