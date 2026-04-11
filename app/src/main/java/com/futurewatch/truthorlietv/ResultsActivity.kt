package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class ResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.results)

        val tvResult = findViewById<TextView>(R.id.tvResult)
        val tvStatement = findViewById<TextView>(R.id.tvStatement)
        val container = findViewById<LinearLayout>(R.id.playersContainer)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val tvRound = findViewById<TextView>(R.id.tvRound)

        val statement = intent.getStringExtra("STATEMENT") ?: ""
        val correctAnswer = intent.getBooleanExtra("ANSWER", false)

        tvStatement.text = statement

        //final result display
        val isFinal = GameSession.currRound >= GameSession.totalRounds

        if (!isFinal) {
            val round = GameSession.currRound
            tvRound.text = "ROUND $round/${GameSession.totalRounds}"
            btnNext.text = "Next Round"
        } else {
            tvRound.text = "FINAL RESULTS"
            btnNext.text = "Finish Game"
        }

        if (correctAnswer) {
            tvResult.text = "TRUTH"
            tvResult.setTextColor(Color.parseColor("#00FF88"))
        } else {
            tvResult.text = "LIE"
            tvResult.setTextColor(Color.parseColor("#FF4444"))
        }

        container.removeAllViews()

        GameSession.players.forEach { player ->

            val isCorrect = player.lastAnswer == correctAnswer

            val playerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val icon = TextView(this).apply {
                text = if (isCorrect) "✔" else "✖"
                textSize = 28f
                setTextColor(
                    if (isCorrect)
                        Color.parseColor("#00FF88")
                    else
                        Color.parseColor("#FF4444")
                )
                gravity = Gravity.CENTER
            }

            val name = TextView(this).apply {
                text = player.name
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
            }

            val score = TextView(this).apply {
                val gained = if (isCorrect) "+100" else "+0"
                text = gained
                textSize = 14f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
            }

            playerLayout.addView(icon)
            playerLayout.addView(name)
            playerLayout.addView(score)

            container.addView(playerLayout)
        }

        // next round/ finish
        btnNext.setOnClickListener {

            if (isFinal) {
                // Game finished - go to final scores screen
                startActivity(Intent(this, FinalResultsActivity::class.java))
                finish()
            } else {
                // Move to next round
                if (GameSession.currRound >= GameSession.totalRounds) {
                    // All rounds done, show final results
                    val finalIntent = Intent(this, ResultsActivity::class.java)
                    finalIntent.putExtra("STATEMENT", statement)
                    finalIntent.putExtra("ANSWER", correctAnswer)
                    finalIntent.putExtra("IS_FINAL", true)
                    startActivity(finalIntent)
                } else {
                    // Continue to next round
                    GameSession.currRound++
                    GameSession.currPlayerTurn = 0
                    startActivity(Intent(this, FactsActivity::class.java))
                }
                finish()
            }
        }
    }
}