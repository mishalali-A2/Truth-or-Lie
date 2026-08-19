package com.futurewatch.truthorlietv

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.InputTracker
import com.futurewatch.truthorlietv.analytics.ScreenTracker

class VotingActivity : AppCompatActivity() {

    private lateinit var tvStatement: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvRound: TextView
    private lateinit var tvPlayer: TextView

    private lateinit var btnPause: ImageView
    private lateinit var btnResume: Button
    private lateinit var btnEndGame: Button
    private lateinit var pauseOverlay: FrameLayout

    private lateinit var barTimer: View
    private lateinit var frameTruth: FrameLayout
    private lateinit var frameLie: FrameLayout
    private lateinit var focusAnchor: View

    // Overlay for next player's turn
    private lateinit var nextPlayerOverlay: FrameLayout
    private lateinit var nextPlayerText: TextView
    private lateinit var btnNextPlayerContinue: Button

    private var selectedAnswer: Boolean? = null
    private var isLocked = false
    private var isPaused = false
    private var isSelectionAnimating = false

    private var currentStatement: String = ""
    private var correctAnswer = false
    private var wasTimeout = false

    // Dynamic timer values - read from settings
    private var totalTime: Long = 20000L
    private var timeLeft: Long = 20000L
    private lateinit var timer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.voting)

        MusicManager.resumeMusic()

        ScreenTracker.attach(this, AnalyticsScreens.VOTING, previousScreen = AnalyticsScreens.FACTS)

        // Get timer duration from settings
        totalTime = TimerManager.getTimerMillis()
        timeLeft = totalTime

        android.util.Log.d("VotingActivity", "Timer duration: ${totalTime / 1000} seconds")

        tvStatement = findViewById(R.id.tvStatement)
        tvTimer = findViewById(R.id.tvTimer)
        tvRound = findViewById(R.id.tvRound)
        tvPlayer = findViewById(R.id.tvPlayer)
        btnPause = findViewById(R.id.btnPause)
        btnResume = findViewById(R.id.btnResume)
        btnEndGame = findViewById(R.id.btnEndGame)
        pauseOverlay = findViewById(R.id.pauseOverlay)
        barTimer = findViewById(R.id.barTimer)
        frameTruth = findViewById(R.id.frameTruth)
        frameLie = findViewById(R.id.frameLie)
        focusAnchor = findViewById(R.id.focusAnchor)

        // Next player overlay
        nextPlayerOverlay = findViewById(R.id.nextPlayerOverlay)
        nextPlayerText = findViewById(R.id.nextPlayerText)
        btnNextPlayerContinue = findViewById(R.id.btnNextPlayerContinue)
        nextPlayerOverlay.visibility = View.GONE
        btnNextPlayerContinue.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.VOTING, AnalyticsParams.CONTROL_ID to "voting_next_player_continue")
            )
            nextPlayerOverlay.visibility = View.GONE
            startNextPlayerTurn()
        }

        frameTruth.isFocusable = false
        frameTruth.isFocusableInTouchMode = false
        frameLie.isFocusable = false
        frameLie.isFocusableInTouchMode = false

        pauseOverlay.isFocusable = true
        pauseOverlay.isFocusableInTouchMode = true

        // Make pause overlay buttons focusable for DPAD navigation
        btnResume.isFocusable = true
        btnResume.isFocusableInTouchMode = true
        btnEndGame.isFocusable = true
        btnEndGame.isFocusableInTouchMode = true

        // Set DPAD navigation order
        btnResume.nextFocusDownId = R.id.btnEndGame
        btnEndGame.nextFocusUpId = R.id.btnResume
        btnResume.nextFocusUpId = R.id.btnEndGame
        btnEndGame.nextFocusDownId = R.id.btnResume

        btnPause.isFocusable = true
        btnPause.isFocusableInTouchMode = false

        currentStatement = intent.getStringExtra("STATEMENT") ?: ""
        correctAnswer = intent.getBooleanExtra("ANSWER", false)

        // safety check -> for logging
        if (GameSession.players.isEmpty()) {
            android.util.Log.e("VotingActivity", "ERROR: GameSession.players is empty!")
            AnalyticsService.logEvent(
                AnalyticsEvents.ERROR_DATA_LOAD,
                mapOf(
                    AnalyticsParams.ERROR_CATEGORY to "game_session_players_empty",
                    AnalyticsParams.ERROR_SOURCE to "voting_activity_oncreate"
                )
            )
            finish()
            return
        }

        val currentPlayer = GameSession.getCurrentPlayer()

        tvStatement.text = currentStatement
        tvRound.text = "ROUND ${GameSession.currRound}/${GameSession.totalRounds}"
        tvPlayer.text = "${currentPlayer.name}, what do you think?"

        AnalyticsService.logEvent(
            AnalyticsEvents.PLAYER_TURN_STARTED,
            mapOf(
                AnalyticsParams.CATEGORY_ID to GameSession.category,
                AnalyticsParams.ROUND_NUMBER to GameSession.currRound,
                AnalyticsParams.PLAYER_INDEX to (GameSession.currPlayerTurn % GameSession.players.size)
            )
        )

        btnResume.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.VOTING, AnalyticsParams.CONTROL_ID to "voting_resume")
            )
            resumeGame()
        }
        btnEndGame.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.VOTING, AnalyticsParams.CONTROL_ID to "voting_end_game")
            )
            AnalyticsService.logEvent(
                AnalyticsEvents.GAME_ABANDONED,
                mapOf(
                    AnalyticsParams.CATEGORY_ID to GameSession.category,
                    AnalyticsParams.ROUND_NUMBER to GameSession.currRound,
                    AnalyticsParams.ROUND_COUNT to GameSession.totalRounds,
                    AnalyticsParams.PLAYER_COUNT to GameSession.players.size
                )
            )
            endGame()
        }

        focusAnchor.requestFocus()

        startTimer(timeLeft)

        //callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isPaused) InputTracker.logVotingKeyAction("voting_pause", "back")
                pauseGame()
            }
        })
    }

    private fun startTimer(duration: Long) {
        if (duration <= 0L) return

        AnalyticsService.logEvent(
            AnalyticsEvents.VOTING_TIMER_STARTED,
            mapOf(
                AnalyticsParams.CATEGORY_ID to GameSession.category,
                AnalyticsParams.ROUND_NUMBER to GameSession.currRound
            )
        )

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
                if (selectedAnswer == null) {
                    wasTimeout = true
                    selectAnswer(false)
                }
                lockAnswer()
            }
        }.start()
    }

    // Pause on navigating up
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
        focusAnchor.requestFocus()
        startTimer(timeLeft)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isPaused) {
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_BUTTON_A -> {
                    val focused = currentFocus
                    when (focused?.id) {
                        R.id.btnResume -> {
                            InputTracker.logVotingKeyAction("voting_pause_resume", "select")
                            resumeGame()
                        }
                        R.id.btnEndGame -> {
                            InputTracker.logVotingKeyAction("voting_pause_end_game", "select")
                            endGame()
                        }
                    }
                    true
                }
                else -> super.onKeyDown(keyCode, event)
            }
        }

        if (isLocked || isSelectionAnimating) return true

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (focusAnchor.hasFocus() && !isLocked && !isSelectionAnimating) {
                    InputTracker.logVotingKeyAction("voting_answer_truth", "dpad_left")
                    selectAnswer(true)  // Truth on LEFT
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (focusAnchor.hasFocus() && !isLocked && !isSelectionAnimating) {
                    InputTracker.logVotingKeyAction("voting_answer_lie", "dpad_right")
                    selectAnswer(false)  // Lie on RIGHT
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (!isPaused) InputTracker.logVotingKeyAction("voting_pause", "pause")
                pauseGame()
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_A -> {
                // When OK/Enter is pressed, submit the selected answer
                if (selectedAnswer != null && !isLocked && !isSelectionAnimating) {
                    InputTracker.logVotingKeyAction("voting_lock_answer", "lock")
                    lockAnswer()
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun selectAnswer(answer: Boolean) {
        if (isSelectionAnimating) return

        selectedAnswer = answer
        isSelectionAnimating = true

        val truthParams = frameTruth.layoutParams as LinearLayout.LayoutParams
        val lieParams = frameLie.layoutParams as LinearLayout.LayoutParams
        val targetTruthWeight: Float
        val targetLieWeight: Float

        if (answer) {
            targetTruthWeight = 0.85f
            targetLieWeight = 0.35f
        } else {
            targetTruthWeight = 0.35f
            targetLieWeight = 0.85f
        }

        // Animate the weight change smoothly
        animateWeightChange(truthParams, targetTruthWeight, lieParams, targetLieWeight)
    }

    private fun animateWeightChange(
        truthParams: LinearLayout.LayoutParams,
        targetTruthWeight: Float,
        lieParams: LinearLayout.LayoutParams,
        targetLieWeight: Float
    ) {
        val startTruthWeight = truthParams.weight
        val startLieWeight = lieParams.weight

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val fraction = animation.animatedFraction

                truthParams.weight =
                    startTruthWeight + (targetTruthWeight - startTruthWeight) * fraction

                lieParams.weight =
                    startLieWeight + (targetLieWeight - startLieWeight) * fraction

                frameTruth.layoutParams = truthParams
                frameLie.layoutParams = lieParams
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isSelectionAnimating = false

                }
            })

            start()
        }
    }

    private fun lockAnswer() {
        if (isLocked) return

        isLocked = true
        if (::timer.isInitialized) timer.cancel()

        // Animate the alpha for visual feedback
        val targetTruthAlpha = if (selectedAnswer == true) 1.0f else 0.3f
        val targetLieAlpha = if (selectedAnswer == false) 1.0f else 0.3f

        frameTruth.animate()
            .alpha(targetTruthAlpha)
            .setDuration(200)
            .start()
        frameLie.animate()
            .alpha(targetLieAlpha)
            .setDuration(200)
            .start()

        val player = GameSession.getCurrentPlayer()
        val isCorrect = selectedAnswer == correctAnswer
        player.lastAnswer = selectedAnswer
        if (isCorrect) player.score += 100

        val totalPlayers = GameSession.players.size
        val currentTurnIndex = GameSession.currPlayerTurn
        val nextTurn = GameSession.currPlayerTurn + 1
        val isLast = nextTurn % totalPlayers == 0
        GameSession.currPlayerTurn = nextTurn

        android.util.Log.d("VotingActivity", "Answer: ${selectedAnswer}, Correct: $correctAnswer, Score: ${player.score}")

        // player_turn_index only — NEVER the player's name.
        AnalyticsService.logEvent(
            AnalyticsEvents.ANSWER_SUBMITTED,
            mapOf(
                AnalyticsParams.CATEGORY_ID to GameSession.category,
                AnalyticsParams.ROUND_NUMBER to GameSession.currRound,
                AnalyticsParams.IS_CORRECT to isCorrect,
                AnalyticsParams.IS_TIMEOUT to wasTimeout,
                AnalyticsParams.PLAYER_INDEX to (currentTurnIndex % totalPlayers)
            )
        )
        wasTimeout = false

        if (isLast) {
            // Last player, go to results
            val intent = Intent(this, ResultsActivity::class.java).apply {
                putExtra("STATEMENT", currentStatement)
                putExtra("ANSWER", correctAnswer)
            }
            startActivity(intent)
            finish()
        } else {
            // Show next player overlay
            val nextPlayer = GameSession.players[nextTurn % totalPlayers]
            nextPlayerText.text = "Next player's turn: ${nextPlayer.name}"
            nextPlayerOverlay.visibility = View.VISIBLE
            btnNextPlayerContinue.requestFocus()
        }
    }

    private fun startNextPlayerTurn() {
        val intent = Intent(this, VotingActivity::class.java).apply {
            putExtra("STATEMENT", currentStatement)
            putExtra("ANSWER", correctAnswer)
            putExtra("ROUND", intent.getIntExtra("ROUND", 1))
            putExtra("TOTAL_ROUNDS", intent.getIntExtra("TOTAL_ROUNDS", 1))
            putExtra("CATEGORY", intent.getStringExtra("CATEGORY") ?: "science")
            putExtra("FACT_ID", intent.getStringExtra("FACT_ID") ?: "")
        }
        startActivity(intent)
        finish()
    }

    private fun endGame() {
        val intent = Intent(this, FinalResultsActivity::class.java)
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