package com.bardsplayground.knappen

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

class MainButtonHandler(private val context: Context) {

    private val SECOND =1_000L
    private val MINUTE = 60 * SECOND
    private val HOUR = 60 * MINUTE

    val prefs = PrefsManager(context)
    private val notificationHandler = NotificationHandler(context)

    fun onMainButtonClicked() {
        Log.d("Knappen", "onMainButtonClicked")

        if(prefs.isTimerActive() && timeUntilTriggerMs() > 0)
        {
            // Display 'not clickable' with extra fail safe
//            toast("Knappen is clickable in ${timeUntilTriggerString()}.")
            Log.d("Button handler", "Should be disabled.")
            setIsInteractable(false)
            return;
        }

        setIsInteractable(isInteractable = false)
        Log.d("Button handler", "Done.")

        startTimerTryCatch()

    }

    fun onResetButtonClicked() {
        Log.d("Button handler", "onResetButtonClicked")
        prefs.setTimerActive(false)
        onMainButtonClicked()
    }

    fun longHours(): Long{
//        return  1000 * 60 * 60
        return 1000 * 5;
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

//    fun timeUntilTriggerString(): String {
//        val diff = prefs.getTriggerTime() - System.currentTimeMillis()
//        if (diff <= 0) return "now"
//
//        val hours = diff / longHours()
//        val minutes = (diff % longHours()) / (1000 * 60)
//        return if (hours > 0)
//            "${hours}h ${minutes}m"
//        else
//            "${minutes}m"
//    }

    /**
     * Resets the text on the button and makes it interactable again.
     */
    fun onTimerTriggered() {
//        toast("Button can be clicked on again!")
        notificationHandler.createNotification("Knappen er klikkbar igjen!")
        setButtonText("Knappen")
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
//            toast("Knappen failed to verify when it should become active again.")
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
        val buttonText = if (isInteractable) "Knappen" else timeUntilTriggerString()


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
//            toast("Can't create timer. Ensure permission is given.")
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

//        notificationHandler.createNotification("Button is clicked on!")
//        toast("Knappen is clickable in ${timeUntilTriggerString()}.")
    }

    private fun setButtonText(message: String) {
        val views = RemoteViews(context.packageName, R.layout.main_widget)
        views.setTextViewText(R.id.main_button, message)
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