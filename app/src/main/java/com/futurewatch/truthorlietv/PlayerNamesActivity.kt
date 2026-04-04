package com.futurewatch.truthorlietv

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class PlayerNamesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.player_names)

        //num of players from players screen
        val container = findViewById<LinearLayout>(R.id.containerPlayers)
        val playerCount = intent.getIntExtra("PLAYER_COUNT", 2)

        // btn anim
        val startBtn = findViewById<Button>(R.id.btnStart)
        startBtn.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .translationZ(20f)
                    .setDuration(150)
                    .start()
            } else {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(150)
                    .start()
            }
        }

// index of button (so we insert above it)
        val buttonIndex = container.indexOfChild(startBtn)

        for (i in 1..playerCount) {

            val label = TextView(this).apply {
                text = "PLAYER $i"
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 14f
                letterSpacing = 0.1f

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = if (i == 1) 0 else 30
                }

                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
            }

            val input = EditText(this).apply {
                hint = "Player $i"
                setTextColor(Color.WHITE)
                setHintTextColor(Color.parseColor("#888888"))
                textSize = 18f
                background = getDrawable(R.drawable.text_input)
                setPadding(30, 25, 30, 25)

                layoutParams = LinearLayout.LayoutParams(
                    600,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = android.view.Gravity.CENTER_HORIZONTAL
                    topMargin = 10
                }

                isFocusable = true
                isFocusableInTouchMode = true

                //
                setOnFocusChangeListener { v, hasFocus ->
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
            }

            // b4 the let's go btn
            container.addView(label, buttonIndex + (i - 1) * 2)
            container.addView(input, buttonIndex + (i - 1) * 2 + 1)
        }
    }
}