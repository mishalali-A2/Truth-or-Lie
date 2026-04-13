package com.futurewatch.truthorlietv

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)

        val backBtn = findViewById<Button>(R.id.btnBack)

        // Back to main menu
        backBtn.setOnClickListener {
            finish()
        }

        // TV focus animation
        val focusListener = android.view.View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
            } else {
                v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
        }

        backBtn.onFocusChangeListener = focusListener

        // Default focus
        backBtn.requestFocus()
    }
}