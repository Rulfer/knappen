package com.bardsplayground.knappen

import android.app.Activity
import android.os.Bundle
import android.widget.RemoteViews

class SettingsActivity : Activity() {
    companion object {
        const val CHANGE_TIMER_INTERVALS_CLICK = "com.bardsplayground.knappen.SET_TIMER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
    }
}