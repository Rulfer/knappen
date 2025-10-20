package com.bardsplayground.knappen

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

class ToastActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val message = intent.getStringExtra("message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        finish() // immediately close the activity
    }
}