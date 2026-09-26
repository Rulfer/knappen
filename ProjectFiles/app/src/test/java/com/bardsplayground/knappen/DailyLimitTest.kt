package com.bardsplayground.knappen

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DailyLimitTest {

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test
    fun clicksLeft_countsOnlyToday() {
        assertEquals(4, PrefsManager.clicksLeft(4, storedDay = 0, storedCount = 0, today = 20260926))
        assertEquals(1, PrefsManager.clicksLeft(4, storedDay = 20260926, storedCount = 3, today = 20260926))
        assertEquals(0, PrefsManager.clicksLeft(4, storedDay = 20260926, storedCount = 4, today = 20260926))
        // Yesterday's taps do not count any more (reset at midnight).
        assertEquals(4, PrefsManager.clicksLeft(4, storedDay = 20260925, storedCount = 4, today = 20260926))
    }

    @Test
    fun clicksLeft_lowerLimitMidDay_neverNegative() {
        assertEquals(0, PrefsManager.clicksLeft(2, storedDay = 20260926, storedCount = 3, today = 20260926))
    }

    @Test
    fun clicksLeft_noLimit() {
        assertEquals(
            Int.MAX_VALUE,
            PrefsManager.clicksLeft(PrefsManager.NO_DAILY_LIMIT, storedDay = 20260926, storedCount = 50, today = 20260926)
        )
    }

    @Test
    fun dayKey_changesAtLocalMidnight() {
        assertEquals(20260926, PrefsManager.dayKey(millis(2026, 9, 26, 23, 59)))
        assertEquals(20260927, PrefsManager.dayKey(millis(2026, 9, 27, 0, 0)))
        assertEquals(20261231, PrefsManager.dayKey(millis(2026, 12, 31, 12)))
    }

    @Test
    fun nextMidnight_isStartOfNextDay_alsoOverDstChange() {
        assertEquals(millis(2026, 9, 27, 0), PrefsManager.nextMidnight(millis(2026, 9, 26, 22, 30)))
        assertEquals(millis(2026, 9, 27, 0), PrefsManager.nextMidnight(millis(2026, 9, 26, 0, 0)))
        // Night of 24 -> 25 Oct 2026: clocks go back in Norway; midnight is still 00:00 local.
        assertEquals(millis(2026, 10, 26, 0), PrefsManager.nextMidnight(millis(2026, 10, 25, 12)))
    }
}
