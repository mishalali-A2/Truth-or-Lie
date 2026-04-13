package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class PlayerNamesActivity : AppCompatActivity() {

    private val playerInputs = mutableListOf<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.player_names)

        MusicManager.resumeMusic()

        val container = findViewById<LinearLayout>(R.id.containerPlayers)
        val playerCount = GameSession.playerCount
        val startBtn = findViewById<Button>(R.id.btnStart)

        startBtn.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.06f).scaleY(1.06f).translationZ(20f).setDuration(150).start()
            } else {
                v.animate().scaleX(1f).scaleY(1f).translationZ(0f).setDuration(150).start()
            }
        }

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

                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        v.animate().scaleX(1.06f).scaleY(1.06f).translationZ(20f).setDuration(150).start()
                        v.setBackgroundResource(R.drawable.tv_edittext_bg)
                    } else {
                        v.animate().scaleX(1f).scaleY(1f).translationZ(0f).setDuration(150).start()
                        v.setBackgroundResource(R.drawable.text_input)
                    }
                }
            }

            playerInputs.add(input)

            container.addView(label, buttonIndex + (i - 1) * 2)
            container.addView(input, buttonIndex + (i - 1) * 2 + 1)
        }


        startBtn.setOnClickListener {

            GameSession.players.clear()

            playerInputs.forEachIndexed { index, editText ->

                val name = editText.text.toString().trim()

                // fallback if no name
                if (name.isEmpty()) {
                    editText.error = "Enter name"
                    return@setOnClickListener
                }

                GameSession.players.add(Player(name))
            }

            GameSession.reset()

            val intent = Intent(this, FactsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        MusicManager.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
    }
}