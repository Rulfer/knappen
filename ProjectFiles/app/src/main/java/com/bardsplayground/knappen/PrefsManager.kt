package com.bardsplayground.knappen

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.bardsplayground.knappen.helpers.LongValues
import java.util.Calendar

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
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_CLICKS_DAY = "clicks_day"
        private const val KEY_CLICKS_COUNT = "clicks_count"

        /** Shortest allowed lock duration (also used by the "test" option in Settings). */
        const val MIN_TIMER_DURATION_MS = 60_000L
        const val TEST_TIMER_DURATION_MS = 60_000L
        const val DEFAULT_TIMER_DURATION_MS = 16_200_000L // 4.5 hours

        /** Max taps per calendar day. [NO_DAILY_LIMIT] turns the cap off. Keep UI wording generic (not "doses"). */
        const val DEFAULT_DAILY_LIMIT = 4
        const val NO_DAILY_LIMIT = 0
        const val MAX_DAILY_LIMIT = 24

        /** Local calendar day as yyyyMMdd, e.g. 20260926. Changes at local midnight. */
        fun dayKey(timeMillis: Long): Int {
            val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
            return calendar.get(Calendar.YEAR) * 10_000 +
                (calendar.get(Calendar.MONTH) + 1) * 100 +
                calendar.get(Calendar.DAY_OF_MONTH)
        }

        /** Start of the next local day (next midnight) after [timeMillis]. */
        fun nextMidnight(timeMillis: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = timeMillis
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        /**
         * Taps left today. A count stored for another day counts as 0 (it resets at midnight).
         * [Int.MAX_VALUE] when there is no limit.
         */
        fun clicksLeft(limit: Int, storedDay: Int, storedCount: Int, today: Int): Int {
            if (limit == NO_DAILY_LIMIT) return Int.MAX_VALUE
            val usedToday = if (storedDay == today) storedCount else 0
            return (limit - usedToday).coerceAtLeast(0)
        }
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

    fun getDailyLimit(): Int = prefs.getInt(KEY_DAILY_LIMIT, DEFAULT_DAILY_LIMIT)

    /** Max taps per day ([NO_DAILY_LIMIT] = off); clamped to 0..[MAX_DAILY_LIMIT]. Applies to today immediately. */
    fun setDailyLimit(limit: Int) {
        prefs.edit { putInt(KEY_DAILY_LIMIT, limit.coerceIn(NO_DAILY_LIMIT, MAX_DAILY_LIMIT)) }
    }

    fun hasDailyLimit(): Boolean = getDailyLimit() != NO_DAILY_LIMIT

    fun getClicksLeftToday(now: Long = System.currentTimeMillis()): Int {
        return clicksLeft(
            getDailyLimit(),
            prefs.getInt(KEY_CLICKS_DAY, 0),
            prefs.getInt(KEY_CLICKS_COUNT, 0),
            dayKey(now)
        )
    }

    fun isDailyLimitReached(): Boolean = getClicksLeftToday() == 0

    /** Counts one tap for today (starting from 0 if the stored count belongs to an earlier day). */
    fun recordClick(now: Long = System.currentTimeMillis()) {
        val today = dayKey(now)
        val usedToday = if (prefs.getInt(KEY_CLICKS_DAY, 0) == today) prefs.getInt(KEY_CLICKS_COUNT, 0) else 0
        prefs.edit {
            putInt(KEY_CLICKS_DAY, today)
            putInt(KEY_CLICKS_COUNT, usedToday + 1)
        }
    }

    /** Whether the notification-permission explanation screen has already been shown to the user. */
    fun wasPermissionPrompted(): Boolean = prefs.getBoolean(KEY_PERMISSION_PROMPTED, false)

    fun setPermissionPrompted(prompted: Boolean) {
        prefs.edit { putBoolean(KEY_PERMISSION_PROMPTED, prompted) }
    }
}
