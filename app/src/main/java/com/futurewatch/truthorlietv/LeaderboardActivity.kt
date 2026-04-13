package com.futurewatch.truthorlietv

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.futurewatch.truthorlietv.database.PlayerEntity
import com.futurewatch.truthorlietv.database.PlayerRepository

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var backBtn: Button
    private lateinit var messageContainer: LinearLayout
    private lateinit var leaderboardContainer: LinearLayout
    private lateinit var leaderboardScroll: ScrollView
    private lateinit var playerRepository: PlayerRepository
    private val rowViews = mutableListOf<View>()
    private var currentFocusIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.leaderboard)

        MusicManager.resumeMusic()
        MusicManager.resumeMusic()

        backBtn = findViewById(R.id.btnBack)
        messageContainer = findViewById(R.id.messageContainer)
        leaderboardContainer = findViewById(R.id.leaderboardContainer)
        leaderboardScroll = findViewById(R.id.leaderboardScroll)
        playerRepository = PlayerRepository(this)

        backBtn.setOnClickListener {
            finish()
        }
        setupFocusAnimations()
        loadLeaderboard()
    }

    private fun setupFocusAnimations() {
        val scaleUp = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        scaleUp.duration = 150

        val scaleDown = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        scaleDown.duration = 150

        backBtn.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .translationZ(20f)
                    .setDuration(150)
                    .start()

                v.setBackgroundResource(R.drawable.tv_edittext_bg) // glow
            } else {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(150)
                    .start()

                v.setBackgroundResource(R.drawable.text_input)
            }
        }
    }

    private fun setupRowFocus(row: View, player: PlayerEntity, position: Int) {
        row.isFocusable = true
        row.isFocusableInTouchMode = true

        if (position > 0) {
            row.nextFocusUpId = rowViews[position - 1].id
        } else {
            row.nextFocusUpId = backBtn.id
        }

        if (position < rowViews.size - 1) {
            row.nextFocusDownId = rowViews[position + 1].id
        } else {
            row.nextFocusDownId = backBtn.id
        }

        row.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                currentFocusIndex = position

                view.animate()
                    .scaleX(1.01f)
                    .scaleY(1.01f)
                    .translationZ(20f)
                    .setDuration(150)
                    .start()

                view.setBackgroundResource(R.drawable.row_focused)

                leaderboardScroll.smoothScrollTo(0, view.top - leaderboardScroll.top - 100)

                updateRowTextColors(view, player, position, focused = true)

                Log.d("LeaderboardActivity", "Row focused: ${player.name} at position $position")
            } else {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(150)
                    .start()

                // Reset bg
                if (position % 2 == 0) {
                    view.setBackgroundResource(R.drawable.row_normal)
                } else {
                    view.setBackgroundResource(R.drawable.row_normal_dark)
                }

                updateRowTextColors(view, player, position, focused = false)
            }
        }
    }

    private fun updateRowTextColors(row: View, player: PlayerEntity, position: Int, focused: Boolean) {

        val leftText = (row as LinearLayout).getChildAt(0) as TextView
        val rightText = row.getChildAt(1) as TextView

        if (focused) {
            leftText.setTextColor(when (position) {
                0 -> Color.parseColor("#FFE44D")
                1 -> Color.parseColor("#E0E0E0")
                2 -> Color.parseColor("#FFA04D")
                else -> Color.parseColor("#FFFFFF")
            })
            rightText.setTextColor(Color.parseColor("#B87CFF"))
            leftText.textSize = 22f
            rightText.textSize = 22f
        } else {
            leftText.setTextColor(when (position) {
                0 -> Color.parseColor("#FFD700")
                1 -> Color.parseColor("#C0C0C0")
                2 -> Color.parseColor("#CD7F32")
                else -> Color.parseColor("#AAAAAA")
            })
            rightText.setTextColor(Color.parseColor("#7F3FFF"))
            leftText.textSize = 20f
            rightText.textSize = 20f
        }
    }

    private fun loadLeaderboard() {
        Log.d("LeaderboardActivity", "Loading leaderboard from database")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val players = playerRepository.getAllPlayersSorted()
                Log.d("LeaderboardActivity", "Loaded ${players.size} players from database")

                withContext(Dispatchers.Main) {
                    if (players.isEmpty()) {
                        leaderboardScroll.visibility = View.GONE
                        messageContainer.visibility = View.VISIBLE
                    } else {
                        messageContainer.visibility = View.GONE
                        leaderboardScroll.visibility = View.VISIBLE
                        displayLeaderboard(players)

                        if (rowViews.isNotEmpty()) {
                            rowViews[0].requestFocus()
                        } else {
                            backBtn.requestFocus()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LeaderboardActivity", "Error loading leaderboard", e)
                withContext(Dispatchers.Main) {
                    leaderboardScroll.visibility = View.GONE
                    messageContainer.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.tvEmptyTitle)?.text = "Error Loading Data"
                    findViewById<TextView>(R.id.tvEmptySubtitle)?.text = "Please try again later."
                }
            }
        }
    }

    private fun displayLeaderboard(players: List<PlayerEntity>) {
        leaderboardContainer.removeAllViews()
        rowViews.clear()

        players.forEachIndexed { index, player ->
            val row = createLeaderboardRow(player, index)
            leaderboardContainer.addView(row)
            rowViews.add(row)
            setupRowFocus(row, player, index)
        }

        Log.d("LeaderboardActivity", "Displayed ${players.size} players in leaderboard")
    }

    private fun createLeaderboardRow(player: PlayerEntity, position: Int): LinearLayout {
        val row = LinearLayout(this)
        row.id = View.generateViewId()
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        row.setPadding(48, 24, 48, 24)

        if (position % 2 == 0) {
            row.setBackgroundResource(R.drawable.row_normal)
        } else {
            row.setBackgroundResource(R.drawable.row_normal_dark)
        }

        val leftText = TextView(this)
        val rankIcon = when (position) {
            0 -> "🥇 "
            1 -> "🥈 "
            2 -> "🥉 "
            else -> "${position + 1}  "
        }
        leftText.text = "$rankIcon${player.name}"
        leftText.textSize = 20f
        leftText.setTextColor(when (position) {
            0 -> Color.parseColor("#FFD700")
            1 -> Color.parseColor("#C0C0C0")
            2 -> Color.parseColor("#CD7F32")
            else -> Color.parseColor("#AAAAAA")
        })
        leftText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val rightText = TextView(this)
        rightText.text = "${player.points} pts"
        rightText.textSize = 20f
        rightText.setTextColor(Color.parseColor("#7F3FFF"))

        row.addView(leftText)
        row.addView(rightText)

        return row
    }
}