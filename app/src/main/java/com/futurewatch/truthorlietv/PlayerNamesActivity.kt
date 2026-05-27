package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PlayerNamesActivity : AppCompatActivity() {

    private val playerInputs = mutableListOf<EditText>()
    private val errorTextViews = mutableListOf<TextView>()

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
                //text = "PLAYER $i"
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
                hint = "Player $i (Optional - 3-10 characters)"
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

                filters = arrayOf(InputFilter.LengthFilter(10))

                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        v.animate().scaleX(1.06f).scaleY(1.06f).translationZ(20f).setDuration(150).start()
                        // Clear error visual when focusing
                        clearErrorVisual(this)
                    } else {
                        v.animate().scaleX(1f).scaleY(1f).translationZ(0f).setDuration(150).start()
                    }
                }

                addTextChangedListener(object : android.text.TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: android.text.Editable?) {
                        validateInput(this@apply, i)
                    }
                })
            }

            // Create error TextView below each input
            val errorText = TextView(this).apply {
                text = ""
                setTextColor(Color.parseColor("#FF6B6B"))
                textSize = 12f
                visibility = android.view.View.GONE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 5
                    leftMargin = 20
                    rightMargin = 20
                }
            }

            playerInputs.add(input)
            errorTextViews.add(errorText)

            container.addView(label, buttonIndex + (i - 1) * 3)
            container.addView(input, buttonIndex + (i - 1) * 3 + 1)
            container.addView(errorText, buttonIndex + (i - 1) * 3 + 2)
        }

        startBtn.setOnClickListener {
            var hasError = false

            playerInputs.forEachIndexed { index, editText ->
                val name = editText.text.toString().trim()
                val isValid = validateInput(editText, index + 1)

                if (!isValid && name.isNotEmpty()) {
                    hasError = true
                    editText.requestFocus()
                    // Shake animation for error
                    val shakeAnim = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
                    editText.startAnimation(shakeAnim)
                }
            }

            if (hasError) {
                android.widget.Toast.makeText(
                    this,
                    "Please fix the errors above",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            GameSession.players.clear()

            playerInputs.forEachIndexed { index, editText ->
                val name = editText.text.toString().trim()

                if (name.isEmpty()) {
                    GameSession.players.add(Player("Player ${index + 1}"))
                } else {
                    GameSession.players.add(Player(name))
                }
            }

            GameSession.reset()

            val intent = Intent(this, FactsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateInput(editText: EditText, playerNumber: Int): Boolean {
        val name = editText.text.toString().trim()
        val errorTextView = errorTextViews[playerNumber - 1]

        return when {
            name.isEmpty() -> {
                clearErrorVisual(editText, errorTextView)
                true
            }
            name.length in 3..10 -> {
                clearErrorVisual(editText, errorTextView)
                true
            }
            name.length < 3 -> {
                showError(editText, errorTextView)
                false
            }
            else -> {
                showError(editText, errorTextView)
                false
            }
        }
    }

    private fun showError(editText: EditText, errorTextView: TextView) {
        // Set error border
        editText.background = getDrawable(R.drawable.text_input_error)
        editText.setPadding(30, 25, 30, 25)
        errorTextView.visibility = android.view.View.VISIBLE

        val pulseAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        errorTextView.startAnimation(pulseAnim)
    }

    private fun clearErrorVisual(editText: EditText, errorTextView: TextView? = null) {
        editText.background = getDrawable(R.drawable.text_input)
        editText.error = null
        editText.setPadding(30, 25, 30, 25)
        errorTextView?.let {
            it.text = ""
            it.visibility = android.view.View.GONE
        }
    }
}