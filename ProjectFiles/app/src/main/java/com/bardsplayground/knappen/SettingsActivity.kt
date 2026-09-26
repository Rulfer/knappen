package com.bardsplayground.knappen

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import com.bardsplayground.knappen.helpers.LongValues

class SettingsActivity : Activity() {

    companion object {
        private const val STATE_PICKER_HOURS = "picker_hours"
        private const val STATE_PICKER_MINUTES = "picker_minutes"
        private const val STATE_PICKER_LIMIT = "picker_limit"
    }

    private val prefsManager by lazy { PrefsManager(this) }
    private val mainButtonHandler by lazy { MainButtonHandler(this) }

    /**
     * The open picker (duration or daily limit), kept so it can survive a rotation (re-shown with the same values).
     * Only the pickers of the open dialog are non-null.
     */
    private var pickerDialog: AlertDialog? = null
    private var hoursPicker: NumberPicker? = null
    private var minutesPicker: NumberPicker? = null
    private var limitPicker: NumberPicker? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.localized(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.settings_setTimerDuration)
            .setOnClickListener { onClickSetTimerDuration() }

        findViewById<Button>(R.id.settings_setDailyLimit)
            .setOnClickListener { showDailyLimitPicker(prefsManager.getDailyLimit()) }

        findViewById<Button>(R.id.settings_testDuration)
            .setOnClickListener { onClickTestDuration() }

        findViewById<Button>(R.id.settings_resetTimer)
            .setOnClickListener { onClickResetTimer() }

        showCurrentDuration()
        showCurrentDailyLimit()

        if (savedInstanceState?.containsKey(STATE_PICKER_HOURS) == true) {
            showDurationPicker(
                savedInstanceState.getInt(STATE_PICKER_HOURS),
                savedInstanceState.getInt(STATE_PICKER_MINUTES)
            )
        } else if (savedInstanceState?.containsKey(STATE_PICKER_LIMIT) == true) {
            showDailyLimitPicker(savedInstanceState.getInt(STATE_PICKER_LIMIT))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (pickerDialog?.isShowing != true) return
        limitPicker?.let { outState.putInt(STATE_PICKER_LIMIT, it.value) }
        if (hoursPicker != null) {
            outState.putInt(STATE_PICKER_HOURS, hoursPicker?.value ?: 0)
            outState.putInt(STATE_PICKER_MINUTES, minutesPicker?.value ?: 0)
        }
    }

    override fun onDestroy() {
        // Dismiss explicitly so a rotation does not leak the dialog window.
        pickerDialog?.dismiss()
        pickerDialog = null
        super.onDestroy()
    }

    private fun onClickSetTimerDuration() {
        val timer = LongValues.convertLongToStrings(prefsManager.getTimerDuration())
        showDurationPicker(timer.hours.toInt(), timer.minutes.toInt())
    }

    private fun showDurationPicker(hours: Int, minutes: Int) {
        // Create a dialog view with two NumberPickers
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_duration_picker, null)
        val hoursPicker = dialogView.findViewById<NumberPicker>(R.id.hoursPicker)
        val minutesPicker = dialogView.findViewById<NumberPicker>(R.id.minutesPicker)

        hoursPicker.minValue = 0
        hoursPicker.maxValue = 23
        minutesPicker.minValue = 0
        minutesPicker.maxValue = 59

        hoursPicker.value = hours.coerceIn(0, 23)
        minutesPicker.value = minutes.coerceIn(0, 59)

        this.hoursPicker = hoursPicker
        this.minutesPicker = minutesPicker
        this.limitPicker = null
        pickerDialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_picker_title)
            .setView(dialogView)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                // PrefsManager raises anything below the minimum (1 minute), so show what was actually stored.
                prefsManager.setTimerDuration(hoursPicker.value, minutesPicker.value)
                showCurrentDuration()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    /** Picker values 0..MAX_DAILY_LIMIT, where 0 is shown as "No limit". */
    private fun showDailyLimitPicker(limit: Int) {
        val picker = NumberPicker(this).apply {
            minValue = PrefsManager.NO_DAILY_LIMIT
            maxValue = PrefsManager.MAX_DAILY_LIMIT
            displayedValues = Array(maxValue + 1) { n ->
                if (n == PrefsManager.NO_DAILY_LIMIT) getString(R.string.picker_no_limit) else n.toString()
            }
            wrapSelectorWheel = false
            value = limit.coerceIn(minValue, maxValue)
        }

        hoursPicker = null
        minutesPicker = null
        limitPicker = picker
        pickerDialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_daily_limit_picker_title)
            .setView(picker)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                prefsManager.setDailyLimit(picker.value)
                showCurrentDailyLimit()
                // "x of y left today" and a capped widget depend on the limit, so redraw right away.
                mainButtonHandler.refreshAllWidgets()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showCurrentDailyLimit() {
        val limit = prefsManager.getDailyLimit()
        findViewById<TextView>(R.id.settings_currentDailyLimit).text =
            if (limit == PrefsManager.NO_DAILY_LIMIT) {
                getString(R.string.settings_no_daily_limit)
            } else {
                resources.getQuantityString(R.plurals.settings_daily_limit, limit, limit)
            }
    }

    private fun onClickTestDuration() {
        prefsManager.setTimerDurationMs(PrefsManager.TEST_TIMER_DURATION_MS)
        showCurrentDuration()
    }

    private fun onClickResetTimer() {
        mainButtonHandler.onResetButtonClicked()
    }

    private fun showCurrentDuration() {
        val duration = LongValues.convertLongToStrings(prefsManager.getTimerDuration())
        findViewById<TextView>(R.id.settings_currentTimerDuration).text =
            getString(R.string.settings_lock_duration, duration.hours.toInt(), duration.minutes.toInt())
    }
}
