package com.example.knappen

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

class MainButtonHandler(private val context: Context) {

    private val prefs = PrefsManager(context)

    fun onMainButtonClicked() {
        Log.d("Button handler", "onMainButtonClicked")
        toast("MainButtonHandler triggered!")

        if(prefs.isTimerActive())
        {
            // Display 'not clickable'
            toast("Not yet clickable.")
            return;
        }

        setIsInteractable(isInteractable = false)
        Log.d("Button handler", "Done.")

        try {
            startTimer()
        }
        catch (e: SecurityException)
        {
            Log.d("Error", e.message.toString())
            toast("Can't create timer. Ensure permission is given.")
        }
    }

    /**
     * Resets the text on the button and makes it interactable again.
     */
    fun resumeButtonInteractive() {
        toast("Button can be clicked on again!")
        prefs.setTimerActive(active = false)

        setIsInteractable(isInteractable = true)
    }

    fun refresh() {
        val timerActive = prefs.isTimerActive()
        setIsInteractable(isInteractable = !timerActive)
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

    @RequiresPermission(value = "android.permission.SCHEDULE_EXACT_ALARM", conditional = true)
    fun startTimer() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager


        val intent = Intent(context, Timer::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // ELAPSED_REALTIME uses uptimeMillis, so add SystemClock.elapsedRealtime()
        val triggerTime = SystemClock.elapsedRealtime() + 5  // 5 seconds

        try {
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 5000,
                pendingIntent
            )
            prefs.setTimerActive(active = true)
            toast("Button is clickable in $triggerTime seconds.")
        }
        catch (e: SecurityException)
        {
            Log.d("Error", e.message.toString())
            toast("Can't create timer. Ensure permission is given.")
        }
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