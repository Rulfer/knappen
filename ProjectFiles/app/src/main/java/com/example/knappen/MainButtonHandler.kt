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

class MainButtonHandler(private val context: Context) {

    fun onMainButtonClicked() {
        Log.d("Button handler", "onMainButtonClicked")
        toast("MainButtonHandler triggered!")


        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, MainWidget::class.java)
        val views = RemoteViews(context.packageName, R.layout.main_widget)
        views.setTextViewText(R.id.main_button, "Clicked!")
        manager.updateAppWidget(component, views)
        Log.d("Button handler", "Done.")

        try {
            startTimer()
        }
        catch (e: SecurityException)
        {
            Log.d("Error", e.message.toString())
        }    }

    /**
     * Resets the text on the button and makes it interactable again.
     */
    fun resumeButtonInteractive() {
        toast("Timer was triggered!")

    }

    fun refresh() {

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
//        alarmManager.setExactAndAllowWhileIdle(
//            AlarmManager.ELAPSED_REALTIME_WAKEUP,
//            triggerTime,
//            pendingIntent
//        )
        }
        catch (e: SecurityException)
        {
            Log.d("Error", e.message.toString())
        }
        toast("Alarm set!")
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