package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HowToPlayActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.how_to_play)

        // Back to Menu Button
        val backBtn = findViewById<Button>(R.id.btnBacktoMenu)
        backBtn.isFocusable = true
        backBtn.isFocusableInTouchMode = true

        backBtn.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .translationZ(20f)
                    .setDuration(150)
                    .start()

                v.setBackgroundResource(R.drawable.tv_edittext_bg) // glow
            } else {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(150)
                    .start()

                v.setBackgroundResource(R.drawable.text_input)
            }
        }

        backBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}