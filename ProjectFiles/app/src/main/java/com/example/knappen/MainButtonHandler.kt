package com.example.knappen

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.collection.emptyLongSet
import androidx.core.content.ContextCompat

class MainButtonHandler(private val context: Context) {

    private val prefs = PrefsManager(context)

    fun onMainButtonClicked() {
        Log.d("Knappen", "onMainButtonClicked")

        if(prefs.isTimerActive() && timeUntilTrigger() > 0)
        {
            // Display 'not clickable' with extra fail safe
            val triggersInXHours = timeUntilTrigger()
            val systemCurrentTime = System.currentTimeMillis()
            val prefsTime = prefs.getTriggerTime()
            Log.d("MainButtonHandler", "1 - Triggers in $triggersInXHours")
            Log.d("MainButtonHandler", "1 - System.currentTimeMillis() is $systemCurrentTime")
            Log.d("MainButtonHandler", "1 - prefs.getTriggerTime() is $prefsTime")

            toast("Knappen is clickable in $triggersInXHours hours.")
            setIsInteractable(false)
            return;
        }

        val isTriggerActive = prefs.isTimerActive()
        val triggersInXHours = timeUntilTrigger()
        val systemCurrentTime = System.currentTimeMillis()
        val prefsTime = prefs.getTriggerTime()
        Log.d("MainButtonHandler", "2 - prefs.isTimerActive() is $isTriggerActive")
        Log.d("MainButtonHandler", "2 - Triggers in $triggersInXHours")
        Log.d("MainButtonHandler", "2 - System.currentTimeMillis() is $systemCurrentTime")
        Log.d("MainButtonHandler", "2 - prefs.getTriggerTime() is $prefsTime")

        setIsInteractable(isInteractable = false)
        Log.d("Button handler", "Done.")

        startTimerTryCatch()

    }

    fun longHours(): Long{
        return  1000 * 60 * 60
    }

    fun timeUntilTrigger(): Long{

        return (prefs.getTriggerTime() - System.currentTimeMillis()) / (longHours())
    }

    /**
     * Resets the text on the button and makes it interactable again.
     */
    fun resumeButtonInteractive() {
        toast("Button can be clicked on again!")
        prefs.setTimerActive(active = false)

        setIsInteractable(isInteractable = true)
    }

    /**
     * Check if the button should be disabled or active.
     */
    fun refresh() {
        val timerActive = prefs.isTimerActive()
        setIsInteractable(isInteractable = !timerActive)
    }

    fun onBoot(){
        val wasTimerActive = prefs.isTimerActive()
        val triggerTimerAt = prefs.getTriggerTime()

        val defaultValue: Long = -1
        if(wasTimerActive && triggerTimerAt == defaultValue) {
            // Shit. We were unable to check when the timer was expected to trigger.
            toast("Knappen failed to verify when it should become active again.")
            return;
        }
        if(!wasTimerActive){
            // Knappen wasn't disabled when the phone was turned off
            setIsInteractable(isInteractable = true)
        }

        val isTimerActive = triggerTimerAt > System.currentTimeMillis()
        setIsInteractable(isInteractable = !isTimerActive)

        if(isTimerActive)
            startTimerTryCatch(prefs.getTriggerTime())
        else
            prefs.setTimerActive(false) // Reset values
    }



    private fun setIsInteractable(isInteractable: Boolean)
    {
        val views = RemoteViews(context.packageName, R.layout.main_widget)
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, MainWidget::class.java)

        val buttonColor = if (isInteractable)  ContextCompat.getColor(context, R.color.button_interactable) else ContextCompat.getColor(context, R.color.button_disabled)
        val textColor = if (isInteractable)  ContextCompat.getColor(context, R.color.button_text_interactable) else ContextCompat.getColor(context, R.color.button_text_disabled)
        val buttonText = if (isInteractable) "Click" else "Waiting..."

        views.setTextColor(R.id.main_button, textColor)
        views.setInt(R.id.main_button, "setBackgroundColor", buttonColor)
        views.setTextViewText(R.id.main_button, buttonText)

        manager.updateAppWidget(component, views)
    }

    private fun startTimerTryCatch(triggerAt:Long = -1)
    {
        try {
            startTimer(triggerAt)
        }
        catch (e: SecurityException)
        {
            Log.d("Error", e.message.toString())
            toast("Can't create timer. Ensure permission is given.")
        }
    }

    @RequiresPermission(value = "android.permission.SCHEDULE_EXACT_ALARM", conditional = true)
    private fun startTimer(triggerAt:Long = -1) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime: Long = if (triggerAt != (-1).toLong()) triggerAt else System.currentTimeMillis() + prefs.getTimerDuration()

        val intent = Intent(context, Timer::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
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
        val timeUntilTrigger = timeUntilTrigger()
        toast("Knappen is enabled in $timeUntilTrigger hours.")
    }

    private fun toast(message: String)
    {
        val intent = Intent(context, ToastActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("message", message)
        }

        context.startActivity(intent)
    }
}