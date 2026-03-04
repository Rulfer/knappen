package com.bardsplayground.knappen

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.NumberPicker
import android.widget.RemoteViews
import android.widget.TextView
import com.bardsplayground.knappen.helpers.LongValues
import java.util.Calendar
import kotlin.math.log

class SettingsActivity : Activity() {
    companion object {
        const val CHANGE_TIMER_INTERVALS_CLICK = "com.bardsplayground.knappen.SET_TIMER"
    }

    var _prefsManager: PrefsManager? = null
    var _mainButtonHandler : MainButtonHandler? = null

    val prefsManager: PrefsManager
        get() = _prefsManager ?: PrefsManager(this)

    val mainButtonHandler: MainButtonHandler
        get() = _mainButtonHandler ?: MainButtonHandler(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.settings_setTimerDuration)
            .setOnClickListener {onClickSetTimerDuration()  }

        findViewById<Button>(R.id.settings_resetTimer)
            .setOnClickListener { onClickResetTimer() }


        setTimerDurationHeader(prefsManager.getTimerDuration())
    }

    private fun onClickSetTimerDuration(){
        val timer = LongValues.convertLongToStrings(prefsManager.getTimerDuration())

        // Create a dialog view with two NumberPickers
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_duration_picker, null)
        val hoursPicker = dialogView.findViewById<NumberPicker>(R.id.hoursPicker)
        val minutesPicker = dialogView.findViewById<NumberPicker>(R.id.minutesPicker)

        // Set ranges
        hoursPicker.minValue = 0
        hoursPicker.maxValue = 23
        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59

        // Set current values
        hoursPicker.value = timer.hours.toInt()
        minutesPicker.value = timer.minutes.toInt()

        // Build and show the dialog
        AlertDialog.Builder(this)
            .setTitle("Velg varighet")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val selectedHours = hoursPicker.value
                val selectedMinutes = minutesPicker.value
                setTimerDurationHeader(selectedHours, selectedMinutes)
                prefsManager.setTimerDuration(selectedHours, selectedMinutes)
            }
            .setNegativeButton("Abnryt", null)
            .show()


//        val timer = LongValues.convertLongToStrings(prefsManager.getTimerDuration())
//        val timePickerDialog = TimePickerDialog(
//            this,
//            { _, selectedHour, selectedMinute ->
//                // Handle selected time
//                setTimerDurationHeader(selectedHour, selectedMinute)
//                prefsManager.setTimerDuration(selectedHour, selectedMinute)
//            },
//
//            timer.hours.toInt(),
//            timer.minutes.toInt(),
//            true // Set true for 24-hour view (no AM/PM)
//        )
//        timePickerDialog.show()
    }

    private fun onClickResetTimer(){
        mainButtonHandler.onResetButtonClicked(this.intent)
    }

    private fun setTimerDurationHeader(duration: Long){
        val currentDuration = LongValues.convertLongToStrings(duration)
        setTimerDurationHeader(currentDuration.hours.toInt(), currentDuration.minutes.toInt())
    }

    private fun setTimerDurationHeader(hours: Int, minutes: Int){
        findViewById<TextView>(R.id.settings_currentTimerDuration).text =  "Knappen låses i " + hours + "t" + minutes + "m";
    }
}