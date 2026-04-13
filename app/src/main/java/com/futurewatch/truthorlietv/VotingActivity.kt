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

    private val totalTime = 20000L
    private var timeLeft = totalTime
    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.voting)

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

//        val currentRound   = GameSession.getActualRound()
//        val totalRounds   = GameSession.totalRounds
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
                tvTimer.text = "${millisUntilFinished / 1000}s"
                barTimer.scaleX = millisUntilFinished.toFloat() / totalTime
            }

            override fun onFinish() {
                if (isLocked || isPaused) return
//                if (selectedAnswer == null) selectAnswer(true)
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
//                btnPause.requestFocus()
//                highlightPauseButton(true)
                pauseGame() //pause on up
                true
            }

//            KeyEvent.KEYCODE_DPAD_DOWN -> {
//                // Move focus back to game area
//                focusAnchor.requestFocus()
//                highlightPauseButton(false)
//                true
//            }

//            KeyEvent.KEYCODE_DPAD_CENTER,
//            KeyEvent.KEYCODE_BUTTON_A -> {
//                if (btnResume.hasFocus()) {
//                    resumeGame()
//                } else {
//                    lockAnswer()
//                }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_A ->{
                lockAnswer()
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
    }

    private fun lockAnswer() {
        isLocked = true
        if (::timer.isInitialized) timer.cancel()
//visual foc
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
        if (isCorrect) player.score += 1

        val totalPlayers = GameSession.players.size
        val nextTurn = GameSession.currPlayerTurn + 1
        val isLast = nextTurn % totalPlayers == 0
        GameSession.currPlayerTurn = nextTurn

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

    override fun onDestroy() {
        super.onDestroy()
        if (::timer.isInitialized) timer.cancel()
    }
}