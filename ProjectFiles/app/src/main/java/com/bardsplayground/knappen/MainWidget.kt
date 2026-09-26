package com.bardsplayground.knappen

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [MainWidgetConfigureActivity]
 */
class MainWidget : AppWidgetProvider() {
    companion object {
        const val ACTION_BUTTON_CLICK = "com.bardsplayground.knappen.BUTTON_CLICKED"

        /** Only handled for widgets that still hold an intent from an older version; the gear now opens the activity directly. */
        const val ACTION_OPEN_SETTINGS = "com.bardsplayground.knappen.OPEN_SETTINGS"
    }

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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_BUTTON_CLICK -> MainButtonHandler(context).onMainButtonClicked()
            ACTION_OPEN_SETTINGS -> context.startActivity(
                settingsIntent(
                    context,
                    intent.getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID
                    )
                )
            )
        }
    }
}

private fun settingsIntent(context: Context, appWidgetId: Int): Intent {
    return Intent(context, SettingsActivity::class.java).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.main_widget)
    val prefs = PrefsManager(context)
    // The launcher resolves @string in the widget layout with the phone locale, so set language-dependent text here.
    val text = AppLanguage.localized(context)
    views.setContentDescription(R.id.btn_settings, text.getString(R.string.widget_settings_description))

    if (prefs.isTimerActive()) {
        // Locked: show a live countdown. The Chronometer ticks by itself, no wake-ups or refresh loop needed.
        val remainingMs = prefs.getTriggerTime() - System.currentTimeMillis()
        views.setViewVisibility(R.id.main_button, View.GONE)
        views.setViewVisibility(R.id.main_countdown, View.VISIBLE)
        views.setChronometerCountDown(R.id.main_countdown, true)
        views.setChronometer(
            R.id.main_countdown,
            SystemClock.elapsedRealtime() + remainingMs,
            text.getString(R.string.widget_countdown_format),
            true
        )
    } else {
        views.setChronometer(R.id.main_countdown, SystemClock.elapsedRealtime(), null, false)
        views.setViewVisibility(R.id.main_countdown, View.GONE)
        views.setViewVisibility(R.id.main_button, View.VISIBLE)
    }

    val clickIntent = Intent(context, MainWidget::class.java).apply {
        action = MainWidget.ACTION_BUTTON_CLICK
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val clickPendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId * 10 + 1,
        clickIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val settingsPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId * 10 + 2,
        settingsIntent(context, appWidgetId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setOnClickPendingIntent(R.id.main_button, clickPendingIntent)
    views.setOnClickPendingIntent(R.id.main_countdown, clickPendingIntent)
    views.setOnClickPendingIntent(R.id.btn_settings, settingsPendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
