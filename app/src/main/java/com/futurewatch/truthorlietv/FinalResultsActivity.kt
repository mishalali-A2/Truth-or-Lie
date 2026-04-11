package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import nl.dionsegijn.konfetti.xml.KonfettiView
import nl.dionsegijn.konfetti.core.models.Shape
import nl.dionsegijn.konfetti.core.models.Size

class FinalResultsActivity : AppCompatActivity() {

    private lateinit var tvWinnerName: TextView
    private lateinit var tvWinnerPoints: TextView
    private lateinit var leaderboardContainer: LinearLayout
    private lateinit var btnPlayAgain: Button
    private lateinit var konfettiView: KonfettiView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.final_results)

        tvWinnerName = findViewById(R.id.tvWinnerName)
        tvWinnerPoints = findViewById(R.id.tvWinnerPoints)
        leaderboardContainer = findViewById(R.id.leaderboardContainer)
        btnPlayAgain = findViewById(R.id.btnPlayAgain)
        konfettiView = findViewById(R.id.konfettiView)

        showResults()
        startConfetti()

        btnPlayAgain.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnPlayAgain.requestFocus()
    }

    private fun showResults() {
        val sortedPlayers = GameSession.players.sortedByDescending { it.score }
        val winner = sortedPlayers.first()

        tvWinnerName.text = winner.name
        tvWinnerPoints.text = "${winner.score} pts"

        leaderboardContainer.removeAllViews()

        sortedPlayers.forEachIndexed { index, player ->

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            row.setPadding(0, 20, 0, 20)

            val left = TextView(this)
            left.text = "#${index + 1}  ${player.name}"
            left.textSize = 20f
            left.setTextColor(
                if (index == 0) Color.parseColor("#FFA500")
                else Color.parseColor("#AAAAAA")
            )
            left.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val right = TextView(this)
            right.text = "${player.score}"
            right.textSize = 20f
            right.setTextColor(Color.parseColor("#7F3FFF"))

            row.addView(left)
            row.addView(right)

            leaderboardContainer.addView(row)
        }
    }

    private fun startConfetti() {
        konfettiView.post {

            val positions = listOf(0.0, 0.25, 0.5, 0.75, 1.0)

            positions.forEach { xPos ->

                val party = nl.dionsegijn.konfetti.core.Party(
                    speed = 2f,
                    maxSpeed = 6f,
                    damping = 0.5f,
                    spread = 360,
                    colors = listOf(
                        Color.YELLOW,
                        Color.GREEN,
                        Color.MAGENTA,
                        Color.BLUE,
                        Color.CYAN
                    ),
                    emitter = nl.dionsegijn.konfetti.core.emitter.Emitter(
                        duration = 8000
                    ).perSecond(60),

                    position = nl.dionsegijn.konfetti.core.Position.Relative(xPos, 0.0)
                )

                konfettiView.start(party)
            }
        }
    }
}