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

class ResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.results)

        MusicManager.resumeMusic()

        val tvResult = findViewById<TextView>(R.id.tvResult)
        val tvResultGlow1 = findViewById<TextView>(R.id.tvResultGlow1)
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
            tvRound.text = ""
            btnNext.text = "See Final Scores"
        }

        if (correctAnswer) {
            // TRUTH - Green glow
            tvResult.text = "TRUTH"
            tvResult.setTextColor(Color.parseColor("#00FF88"))
            tvResultGlow1.text = "TRUTH"
            tvResultGlow1.setTextColor(Color.parseColor("#AAFFDF"))

            tvResult.setShadowLayer(12f, 0f, 0f, Color.parseColor("#00FF88"))

            tvResultGlow1.setShadowLayer(25f, 0f, 0f, Color.parseColor("#AAFFDF"))
            tvResultGlow1.alpha = 0.4f

        } else {
            // LIE - Red glow
            tvResult.text = "LIE"
            tvResult.setTextColor(Color.parseColor("#EF4444"))
            tvResultGlow1.text = "LIE"
            tvResultGlow1.setTextColor(Color.parseColor("#EF4444"))

            tvResult.setShadowLayer(12f, 0f, 0f, Color.parseColor("#66FF0000"))

            tvResultGlow1.setShadowLayer(25f, 0f, 0f, Color.parseColor("#66FF0000"))
            tvResultGlow1.alpha = 0.4f
        }

        container.removeAllViews()

        container.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        container.gravity = Gravity.CENTER

        GameSession.players.forEach { player ->

            val isCorrect = player.lastAnswer == correctAnswer

            val playerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 0, 16, 0)
            }

            val icon = ImageView(this).apply {
                if (isCorrect) {
                    setImageResource(R.drawable.right)
                } else {
                    setImageResource(R.drawable.wrong)
                }
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(56),
                    dpToPx(56)
                )
                setBackgroundResource(R.drawable.circle_bg)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }

            val name = TextView(this).apply {
                text = player.name
                textSize = 14f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, dpToPx(8), 0, dpToPx(4))
                maxWidth = dpToPx(80)
            }

            val score = TextView(this).apply {
                val gained = if (isCorrect) "+100" else "+0"
                text = gained
                textSize = 13f
                setTextColor(if (isCorrect) Color.parseColor("#00FF88") else Color.LTGRAY)
                gravity = Gravity.CENTER
            }

            playerLayout.addView(icon)
            playerLayout.addView(name)
            playerLayout.addView(score)

            container.addView(playerLayout)
        }

        container.requestLayout()

        // next round/ finish
        btnNext.setOnClickListener {
            if (isFinal) {
                startActivity(Intent(this, FinalResultsActivity::class.java))
                finish()
            } else {
                if (GameSession.currRound >= GameSession.totalRounds) {
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
}