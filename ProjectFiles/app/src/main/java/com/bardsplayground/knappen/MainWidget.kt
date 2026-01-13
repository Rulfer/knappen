package com.bardsplayground.knappen

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [MainWidgetConfigureActivity]
 */
class MainWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_BUTTON_CLICK = "com.bardsplayground.knappen.BUTTON_CLICKED"
        const val ACTION_BUTTON_RESET_CLICK = "com.bardsplayground.knappen.RESET_BUTTON"
        const val ACTION_OPEN_SETTINGS = "com.bardsplayground.knappen.OPEN_SETTINGS"
        const val ACTION_REQUEST_PERMISSION = "com.bardsplayground.knappen.REQUEST_PERMISSION"

    }

//    var context: Context? = null

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            deleteTitlePref(context, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
//        this.context = context

    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val handler = MainButtonHandler(context)
        when (intent.action) {
            ACTION_BUTTON_CLICK -> handler.onMainButtonClicked(intent)
            ACTION_BUTTON_RESET_CLICK -> handler.onResetButtonClicked(intent)
        }
    //        if (intent.action == ACTION_BUTTON_CLICK) {
//            // 👉 Delegate to your handler
//            val handler = MainButtonHandler(context)
//            handler.onMainButtonClicked()
//            return
//        }
//        if(intent.action == ACTION_BUTTON_RESET_CLICK){
//            val handler = MainButtonHandler(context);
//            handler.onResetButtonClicked()
//            return;
//        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.main_widget)
    val handler = MainButtonHandler(context)
    val isInteractable = !handler.prefs.isTimerActive()
    val buttonColor =if (isInteractable) {
        ContextCompat.getColor(context, R.color.button_interactable)
    }else {
        ContextCompat.getColor(context, R.color.button_disabled)
    }
    val textColor =if (isInteractable) {
        ContextCompat.getColor(context, R.color.button_text_interactable)
    }else {
        ContextCompat.getColor(context, R.color.button_text_disabled)
    }

    val buttonText =if (isInteractable) {
        "Knappen"
    }else {
        handler.timeUntilTriggerString()
    }

    views.setTextViewText(R.id.main_button, buttonText)
    views.setTextColor(R.id.main_button, textColor)
    views.setInt(R.id.main_button,"setBackgroundColor", buttonColor)

    val clickIntent = Intent(context, MainWidget::class.java).apply {
        action = MainWidget.ACTION_BUTTON_CLICK
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val resetIntent = Intent(context, MainWidget::class.java).apply {
        action = MainWidget.ACTION_BUTTON_RESET_CLICK
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

    val clickPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId *10 +1,
        clickIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val resetPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId *10 +2,
        resetIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setOnClickPendingIntent(R.id.main_button, clickPendingIntent)
    views.setOnClickPendingIntent(R.id.reset_button, resetPendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}