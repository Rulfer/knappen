package com.example.knappen

import android.content.Context
import android.content.SharedPreferences

class PrefsManager (context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    private val KEY_TIMER_ACTIVE = "timer_active"

    fun setTimerActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_TIMER_ACTIVE, active).apply()
    }

    fun isTimerActive(): Boolean {
        return prefs.getBoolean(KEY_TIMER_ACTIVE, false)
    }

    fun setLong(key: String, value: Long)
    {
        prefs.edit().putLong(key, value).apply()
    }

    /**
     * Returns -1 if the key doesn't exist.
     */
    fun getLong(key: String): Long
    {
        return prefs.getLong(key, -1)

    }
}