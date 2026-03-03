package com.bardsplayground.knappen.helpers

class LongValues {
    companion object {
        const val SECOND: Long = 1000L
        val MINUTE: Long = 60 * SECOND
        val HOUR: Long = 60 * MINUTE

        data class Time(val hours: Long, val minutes: Long, val seconds: Long)

        fun convertLongToStrings(longValue: Long): Time{
            val hours = longValue / HOUR
            val minutes = (longValue % HOUR) / MINUTE
            val seconds = (longValue % MINUTE) / SECOND
            return Time(hours, minutes, seconds)
        }
    }
}