package com.bardsplayground.knappen

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission

class MainButtonHandler(private val context: Context) {

    private val SECOND: Long = 1000L
    private val MINUTE: Long = 60 * SECOND
    private val HOUR: Long = 60 * MINUTE

    val prefs = PrefsManager(context)
    private val notificationHandler = NotificationHandler(context)

    fun onMainButtonClicked(intent: Intent) {
        Log.d("Knappen", "onMainButtonClicked")

        if(prefs.isTimerActive() && timeUntilTriggerMs() > 0)
        {
            // Display 'not clickable' with extra fail safe
//            toast("Knappen is clickable in ${timeUntilTriggerString()}.")
            Log.d("Button handler", "Should be disabled.")
            refreshAllWidgets()

//            setIsInteractable(false)
            return;
        }

//        setIsInteractable(isInteractable = false)
        Log.d("Button handler", "Done.")

        startTimerTryCatch(intent)

    }

    fun onResetButtonClicked(intent: Intent) {
        Log.d("Button handler", "onResetButtonClicked")
        prefs.setTimerActive(false)
        cancelAlarm(intent)
        refreshAllWidgets()
//        onMainButtonClicked(intent)
    }

    fun timeUntilTriggerMs():Long {
        return prefs.getTriggerTime() - System.currentTimeMillis()
    }

    fun timeUntilTriggerString(): String {
        val diff = prefs.getTriggerTime() - System.currentTimeMillis()
        if (diff <=0)
            return "now"

        val hours = diff / HOUR
        val minutes = (diff % HOUR) / MINUTE
        val seconds = (diff % MINUTE) / SECOND
        return when {
            hours >0 ->"${hours}h${minutes}m"
            minutes >0 ->"${minutes}m${seconds}s"
            else ->"${seconds}s"
        }
    }

    /**
     * Resets the text on the button and makes it interactable again.
     */
    fun onTimerTriggered() {
        notificationHandler.createNotification("Knappen er klikkbar igjen!")
        prefs.setTimerActive(active = false)
        refreshAllWidgets()
    }

    fun onBoot(intent: Intent){
        if (prefs.isTimerActive() && prefs.getTriggerTime() > System.currentTimeMillis()) {
            startTimerTryCatch(intent, prefs.getTriggerTime())
        }else {
            prefs.setTimerActive(false)
        }

        // schedule a small delayed refresh to ensure widgets exist
        Handler(Looper.getMainLooper()).postDelayed({
            refreshAllWidgets()
        },500)
    }

    private fun startTimerTryCatch(intent: Intent, triggerAt:Long = -1)
    {
        try {
            startTimer(intent, triggerAt)
        }
        catch (e: SecurityException)
        {
            Log.d("Error", e.message.toString())
        }
    }

    @RequiresPermission(value = "android.permission.SCHEDULE_EXACT_ALARM", conditional = true)
    private fun startTimer(intent: Intent, triggerAt:Long = -1) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime: Long = if (triggerAt != (-1).toLong()) triggerAt else System.currentTimeMillis() + prefs.getTimerDuration()

        val packageContext = Intent(context, Timer::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getAlarmRequestCode(intent),
            packageContext,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        }
        else{
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent)
        }

        prefs.setTriggerTime(triggerTime)
        prefs.setTimerActive(active = true)
        refreshAllWidgets()
    }

    private fun cancelAlarm(intent: Intent){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val packageContext = Intent(context, Timer::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getAlarmRequestCode(intent),// MUST be the same requestCode as startTimer
            packageContext,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)// cancels the alarm

        prefs.setTimerActive(active = false)
        refreshAllWidgets()
    }

    fun refreshAllWidgets() {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, MainWidget::class.java)
        val ids = manager.getAppWidgetIds(component)
        for (id in ids) {
            updateAppWidget(context, manager, id)
        }
    }

    fun getAlarmRequestCode(intent: Intent): Int{
        return intent.getIntExtra("appWidgetId", -1) * 10 + 1;

    }
}