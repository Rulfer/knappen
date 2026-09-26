package com.bardsplayground.knappen

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bardsplayground.knappen.helpers.LongValues

/**
 * Persistent state for the (shared) timer. State is global for all widget instances.
 */
class PrefsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TIMER_ACTIVE = "timer_active"
        private const val KEY_TIMER_TRIGGER = "timer_trigger"
        private const val KEY_TIMER_DURATION = "timer_duration"
        private const val KEY_PERMISSION_PROMPTED = "notification_permission_prompted"

        /** Shortest allowed lock duration (also used by the "test" option in Settings). */
        const val MIN_TIMER_DURATION_MS = 60_000L
        const val TEST_TIMER_DURATION_MS = 60_000L
        const val DEFAULT_TIMER_DURATION_MS = 16_200_000L // 4.5 hours
    }

    /**
     * Store the current 'is timer active' state. When [active] is false the trigger time is reset to -1.
     */
    fun setTimerActive(active: Boolean) {
        prefs.edit {
            putBoolean(KEY_TIMER_ACTIVE, active)
            if (!active) putLong(KEY_TIMER_TRIGGER, -1L)
        }
    }

    /**
     * True if a timer is stored, even if its trigger time has already passed (e.g. while the phone was off).
     */
    fun hasStoredTimer(): Boolean {
        return prefs.getBoolean(KEY_TIMER_ACTIVE, false) && getTriggerTime() != -1L
    }

    /**
     * True while the button is locked: a timer is stored AND its trigger time is still in the future.
     * Derived from the clock so the widget never gets stuck if an alarm is late or missed.
     */
    fun isTimerActive(): Boolean {
        return hasStoredTimer() && getTriggerTime() > System.currentTimeMillis()
    }

    fun setTriggerTime(triggerAt: Long) {
        prefs.edit { putLong(KEY_TIMER_TRIGGER, triggerAt) }
    }

    fun getTriggerTime(): Long {
        return prefs.getLong(KEY_TIMER_TRIGGER, -1L)
    }

    /**
     * How long the button is locked after a tap, as chosen by the user. Default is 4.5 hours.
     */
    fun getTimerDuration(): Long {
        return prefs.getLong(KEY_TIMER_DURATION, DEFAULT_TIMER_DURATION_MS)
    }

    /**
     * Sets the lock duration. Values below [MIN_TIMER_DURATION_MS] are raised to the minimum.
     */
    fun setTimerDuration(hours: Int, minutes: Int) {
        setTimerDurationMs(hours * LongValues.HOUR + minutes * LongValues.MINUTE)
    }

    fun setTimerDurationMs(durationMs: Long) {
        prefs.edit { putLong(KEY_TIMER_DURATION, durationMs.coerceAtLeast(MIN_TIMER_DURATION_MS)) }
    }

    /** Whether the notification-permission explanation screen has already been shown to the user. */
    fun wasPermissionPrompted(): Boolean = prefs.getBoolean(KEY_PERMISSION_PROMPTED, false)

    fun setPermissionPrompted(prompted: Boolean) {
        prefs.edit { putBoolean(KEY_PERMISSION_PROMPTED, prompted) }
    }
}
