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
            tvRound.text = ""
            btnNext.text = "See Final Scores"
        }

        if (correctAnswer) {
            tvResult.text = "TRUTH"
            tvResult.setTextColor(Color.parseColor("#22C55E"))

            tvResult.setShadowLayer(
                60f,
                0f,
                0f,
                Color.parseColor("#22C55E")
            )
        } else {
            tvResult.text = "LIE"
            tvResult.setTextColor(Color.parseColor("#EF4444"))

            tvResult.setShadowLayer(
                60f,
                0f,
                0f,
                Color.parseColor("#EF4444")
            )
        }

        container.removeAllViews()

        // Set container to wrap_content and center the players
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
                // Remove weight, use WRAP_CONTENT instead
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16, 0, 16, 0)  // Reduced padding between players
            }

            val icon = ImageView(this).apply {

                if (isCorrect) {
                    setImageResource(R.drawable.right)
                } else {
                    setImageResource(R.drawable.wrong)
                }
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(56),  // Slightly smaller icon
                    dpToPx(56)
                )
                setBackgroundResource(R.drawable.circle_bg)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
            }

            val name = TextView(this).apply {
                text = player.name
                textSize = 14f  // Slightly smaller font
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, dpToPx(8), 0, dpToPx(4))
                maxWidth = dpToPx(80)  // Limit name width to prevent text from being too wide
            }

            val score = TextView(this).apply {
                val gained = if (isCorrect) "+1" else "+0"
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

        // If there are fewer players, adjust container to not stretch
        container.requestLayout()

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