package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.view.ViewGroup

class ResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.results)

        MusicManager.resumeMusic()

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
                setPadding(8, 0, 8, 0)
            }


            val icon = ImageView(this).apply {

                if (isCorrect) {
                    setImageResource(R.drawable.right)
                } else {
                    setImageResource(R.drawable.wrong)
                }
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(52),
                    dpToPx(52)
                )

                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            }

            val name = TextView(this).apply {
                text = player.name
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, dpToPx(8), 0, dpToPx(4))
            }

            val score = TextView(this).apply {
                val gained = if (isCorrect) "+1" else "+0"
                text = gained
                textSize = 14f
                setTextColor(if (isCorrect) Color.parseColor("#00FF88") else Color.LTGRAY)
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
                startActivity(Intent(this, FinalResultsActivity::class.java))
                finish()
            } else {
                if (GameSession.currRound >= GameSession.totalRounds) {
                    //final round to result
                    val finalIntent = Intent(this, ResultsActivity::class.java)
                    finalIntent.putExtra("STATEMENT", statement)
                    finalIntent.putExtra("ANSWER", correctAnswer)
                    finalIntent.putExtra("IS_FINAL", true)
                    startActivity(finalIntent)
                } else {
                    GameSession.currRound++
                    GameSession.currPlayerTurn = 0
                    startActivity(Intent(this, FactsActivity::class.java))
                }
                finish()
            }
        }
    }


    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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