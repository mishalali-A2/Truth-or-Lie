package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
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
                hint = "Player $i (Optional)"
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

                //  char limit filter (10-12 characters) - only if user enters a name
                filters = arrayOf(InputFilter.LengthFilter(12))

                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        v.animate().scaleX(1.06f).scaleY(1.06f).translationZ(20f).setDuration(150).start()
                        v.setBackgroundResource(R.drawable.tv_edittext_bg)
                    } else {
                        v.animate().scaleX(1f).scaleY(1f).translationZ(0f).setDuration(150).start()
                        v.setBackgroundResource(R.drawable.text_input)
                    }
                }

                // Add text watcher to enforce character limit when user starts typing
                addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        if (s != null && s.isNotEmpty()) {
                            if (s.length in 10..12) {
                                error = null
                            } else if (s.length < 10 && s.length > 0) {
                                error = "Name must be 10-12 characters if provided (current: ${s.length})"
                            }
                        } else {
                            error = null
                        }
                    }
                })
            }

            playerInputs.add(input)

            container.addView(label, buttonIndex + (i - 1) * 2)
            container.addView(input, buttonIndex + (i - 1) * 2 + 1)
        }

        startBtn.setOnClickListener {

            GameSession.players.clear()

            playerInputs.forEachIndexed { index, editText ->

                val name = editText.text.toString().trim()

                //default "Player X"
                if (name.isEmpty()) {
                    GameSession.players.add(Player("Player ${index + 1}"))
                    return@forEachIndexed
                }

                if (name.length < 10) {
                    editText.error = "Name must be 10-12 characters if provided (current: ${name.length})"
                    editText.requestFocus()
                    return@setOnClickListener
                }

                if (name.length > 12) {
                    editText.error = "Name cannot exceed 12 characters"
                    editText.requestFocus()
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