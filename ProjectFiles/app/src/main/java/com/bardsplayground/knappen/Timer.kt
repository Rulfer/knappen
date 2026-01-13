package com.bardsplayground.knappen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class Timer : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("Timer", "Timer finished!")
        // We can assume its time to re-activate the button!
        val handler = MainButtonHandler(context)
        handler.onTimerTriggered()
    }
}