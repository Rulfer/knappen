package com.bardsplayground.knappen

import android.content.Context
import android.content.SharedPreferences

class PrefsManager (context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    private val KEY_TIMER_ACTIVE = "timer_active"
    private val KEY_TIMER_TRIGGER = "timer_trigger"
    private val KEY_TIMER_DURATION = "timer_duration"

    /**
     * Store the current 'is timer active' state. It will also remove the 'trigger at' value when 'active' is false, and call setTriggerTime to -1L.
     */
    fun setTimerActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_TIMER_ACTIVE, active).apply()

        if(!active){
            prefs.edit().remove(KEY_TIMER_TRIGGER)
            setTriggerTime(-1L)
        }
    }

    /**
     * Returns true if both KEY_TIMER_ACTIVE is true and KEY_TIMER_TRIGGER isn't -1L
     */
    fun isTimerActive(): Boolean {
        return prefs.getBoolean(KEY_TIMER_ACTIVE, false) && getTriggerTime() != -1L
    }

    fun setTriggerTime(triggerAt: Long) {
        prefs.edit().putLong(KEY_TIMER_TRIGGER, triggerAt).apply()
    }

    fun getTriggerTime(): Long {
        return prefs.getLong(KEY_TIMER_TRIGGER, -1L)
    }

    /**
     * This is how long the button should be disabled after being clicked on, as defined by the user.
     * Default value is 4 hours.
     */
    fun getTimerDuration(): Long{
//        return prefs.getLong(KEY_TIMER_DURATION, 1000 * 60 * 60 * 4)
        return prefs.getLong(KEY_TIMER_DURATION, 1000 * 5)
    }
}