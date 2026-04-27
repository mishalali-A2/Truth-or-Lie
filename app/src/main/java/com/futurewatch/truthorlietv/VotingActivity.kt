package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class VotingActivity : AppCompatActivity() {

    private lateinit var tvStatement: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvRound: TextView
    private lateinit var tvPlayer: TextView

    private lateinit var btnPause: Button
    private lateinit var btnResume: Button
    private lateinit var pauseOverlay: FrameLayout

    private lateinit var barTimer: View
    private lateinit var frameTruth: FrameLayout
    private lateinit var frameLie: FrameLayout
    private lateinit var focusAnchor: View

    private var selectedAnswer: Boolean? = null
    private var isLocked = false
    private var isPaused = false
    private var isPauseFocused = false

    private var currentStatement: String = ""
    private var correctAnswer = false

    // Dynamic timer values - read from settings
    private var totalTime: Long = 20000L  // Will be set from TimerManager
    private var timeLeft: Long = 20000L
    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.voting)

        MusicManager.resumeMusic()

        // Get timer duration from settings
        totalTime = TimerManager.getTimerMillis()
        timeLeft = totalTime

        android.util.Log.d("VotingActivity", "Timer duration: ${totalTime / 1000} seconds")

        tvStatement  = findViewById(R.id.tvStatement)
        tvTimer      = findViewById(R.id.tvTimer)
        tvRound      = findViewById(R.id.tvRound)
        tvPlayer     = findViewById(R.id.tvPlayer)
        btnPause     = findViewById(R.id.btnPause)
        btnResume    = findViewById(R.id.btnResume)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        barTimer     = findViewById(R.id.barTimer)
        frameTruth   = findViewById(R.id.frameTruth)
        frameLie     = findViewById(R.id.frameLie)
        focusAnchor  = findViewById(R.id.focusAnchor)

        frameTruth.isFocusable = false
        frameTruth.isFocusableInTouchMode = false
        frameLie.isFocusable = false
        frameLie.isFocusableInTouchMode = false

        btnPause.isFocusable = true
        btnPause.isFocusableInTouchMode = false
        btnResume.isFocusable = true
        btnResume.isFocusableInTouchMode = false

        currentStatement = intent.getStringExtra("STATEMENT") ?: ""
        correctAnswer = intent.getBooleanExtra("ANSWER", false)

        // safety check -> for logging
        if (GameSession.players.isEmpty()) {
            android.util.Log.e("VotingActivity", "ERROR: GameSession.players is empty!")
            finish()
            return
        }

        val currentPlayer = GameSession.getCurrentPlayer()

        tvStatement.text = currentStatement
        tvRound.text = "ROUND ${GameSession.currRound}/${GameSession.totalRounds}"
        tvPlayer.text    = "${currentPlayer.name}, what do you think?"

        btnResume.setOnClickListener { resumeGame() }

        focusAnchor.requestFocus()

        startTimer(timeLeft)

        //callback
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                pauseGame()
            }
        })
    }

    private fun startTimer(duration: Long) {
        if (duration <= 0L) return

        timer = object : CountDownTimer(duration, 100) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                tvTimer.text = "${secondsRemaining}s"

                // Update progress bar
                barTimer.scaleX = millisUntilFinished.toFloat() / totalTime

                // Warning when time is low (last 3 seconds)
                if (secondsRemaining <= 3 && secondsRemaining > 0) {
                    tvTimer.setTextColor(resources.getColor(android.R.color.holo_red_light))
                } else {
                    tvTimer.setTextColor(resources.getColor(android.R.color.holo_orange_light))
                }
            }

            override fun onFinish() {
                if (isLocked || isPaused) return
                // Time's up - auto-lock with default answer
                if (selectedAnswer == null) {
                    // Default to false (Lie) when time runs out
                    selectAnswer(false)
                }
                lockAnswer()
            }
        }.start()
    }

    //pause on navigating up
    private fun pauseGame() {
        if (isPaused) return
        isPaused = true
        MusicManager.pauseMusic()
        if (::timer.isInitialized) timer.cancel()
        pauseOverlay.visibility = View.VISIBLE
        btnResume.requestFocus()
    }

    private fun resumeGame() {
        if (!isPaused) return
        isPaused = false
        MusicManager.resumeMusic()
        pauseOverlay.visibility = View.GONE
        isPauseFocused = false
        highlightPauseButton(false)
        focusAnchor.requestFocus()
        startTimer(timeLeft)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (isPaused) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                resumeGame()
            }
            return true
        }

        if (isLocked) return true

        return when (keyCode) {

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusAnchor.hasFocus()) selectAnswer(true)
                true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusAnchor.hasFocus()) selectAnswer(false)
                true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                pauseGame() //pause on up
                true
            }

            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_A ->{
                if (selectedAnswer != null) {
                    lockAnswer()
                }
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun highlightPauseButton(highlighted: Boolean) {
        btnPause.alpha = if (highlighted) 1.0f else 0.6f
    }

    private fun selectAnswer(answer: Boolean) {
        selectedAnswer = answer
        val truthParams = frameTruth.layoutParams as LinearLayout.LayoutParams
        val lieParams   = frameLie.layoutParams as LinearLayout.LayoutParams

        if (answer) {
            truthParams.weight = 0.85f
            lieParams.weight   = 0.35f
        } else {
            lieParams.weight   = 0.85f
            truthParams.weight = 0.35f
        }

        frameTruth.layoutParams = truthParams
        frameLie.layoutParams   = lieParams
        frameTruth.requestLayout()
        frameLie.requestLayout()

        // Auto-lock after selection with a small delay
        btnResume.postDelayed({
            if (!isLocked && selectedAnswer != null) {
                lockAnswer()
            }
        }, 500)
    }

    private fun lockAnswer() {
        isLocked = true
        if (::timer.isInitialized) timer.cancel()

        //visual feedback
        if (selectedAnswer == true) {
            frameTruth.alpha = 1.0f
            frameLie.alpha = 0.3f
        } else {
            frameLie.alpha = 1.0f
            frameTruth.alpha = 0.3f
        }

        val player = GameSession.getCurrentPlayer()
        val isCorrect = selectedAnswer == correctAnswer
        player.lastAnswer = selectedAnswer
        if (isCorrect) player.score += 100

        val totalPlayers = GameSession.players.size
        val nextTurn = GameSession.currPlayerTurn + 1
        val isLast = nextTurn % totalPlayers == 0
        GameSession.currPlayerTurn = nextTurn

        android.util.Log.d("VotingActivity", "Answer: ${selectedAnswer}, Correct: $correctAnswer, Score: ${player.score}")

        val intent = if (isLast) {
            Intent(this, ResultsActivity::class.java).apply {
                putExtra("STATEMENT", currentStatement)
                putExtra("ANSWER", correctAnswer)
            }
        } else {
            Intent(this, VotingActivity::class.java).apply {
                putExtra("STATEMENT", currentStatement)
                putExtra("ANSWER", correctAnswer)
                putExtra("ROUND", intent.getIntExtra("ROUND", 1))
                putExtra("TOTAL_ROUNDS", intent.getIntExtra("TOTAL_ROUNDS", 1))
                putExtra("CATEGORY", intent.getStringExtra("CATEGORY") ?: "science")
                putExtra("FACT_ID", intent.getStringExtra("FACT_ID") ?: "")
            }
        }

        startActivity(intent)
        finish()
    }

    override fun onPause() {
        super.onPause()
        MusicManager.pauseMusic()
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::timer.isInitialized) timer.cancel()
    }
}