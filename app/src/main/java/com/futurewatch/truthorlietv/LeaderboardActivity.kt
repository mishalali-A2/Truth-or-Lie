package com.futurewatch.truthorlietv

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.InputTracker
import com.futurewatch.truthorlietv.analytics.ScreenTracker

class LeaderboardActivity : AppCompatActivity() {

    // Unbounded row list (grows every game played) — aggregate focus via idle-gap debounce
    // rather than one event per row stepped through, matching the CategoriesActivity strategy.
    private val rowFocusAggregator = InputTracker.FocusIdleAggregator(AnalyticsScreens.LEADERBOARD)

    private lateinit var backBtn: Button
    private lateinit var messageContainer: LinearLayout
    private lateinit var leaderboardContainer: LinearLayout
    private lateinit var rootScrollView: ScrollView
    private lateinit var playerRepository: PlayerRepository
    private val rowViews = mutableListOf<View>()
    private var currentFocusIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.leaderboard)

        MusicManager.resumeMusic()

        ScreenTracker.attach(this, AnalyticsScreens.LEADERBOARD, previousScreen = AnalyticsScreens.MAIN)

        backBtn = findViewById(R.id.btnBack)
        messageContainer = findViewById(R.id.messageContainer)
        leaderboardContainer = findViewById(R.id.leaderboardContainer)
        rootScrollView = findViewById(R.id.rootScrollView)
        playerRepository = PlayerRepository(this)

        backBtn.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.LEADERBOARD, AnalyticsParams.CONTROL_ID to "leaderboard_back")
            )
            finish()
        }

        setupFocusAnimations()
        loadLeaderboard()
    }

    private fun setupFocusAnimations() {
        backBtn.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .translationZ(20f)
                    .setDuration(150)
                    .start()
                //v.setBackgroundResource(R.drawable.tv_edittext_bg)

                // Scroll to show the button if needed
                rootScrollView.post {
                    rootScrollView.smoothScrollTo(0, rootScrollView.bottom)
                }
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

        // Setup DPAD navigation
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
                // Unbounded list: aggregate via idle-gap debounce (500ms), not one event per row.
                rowFocusAggregator.onFocusChanged("leaderboard_row_$position")

                view.animate()
                    .scaleX(1.02f)
                    .scaleY(1.02f)
                    .translationZ(20f)
                    .setDuration(150)
                    .start()

                view.setBackgroundResource(R.drawable.row_focused)

                // Smooth scroll to focused item within the main ScrollView
                rootScrollView.post {
                    val scrollAmount = view.top - rootScrollView.height / 2 + view.height / 2
                    rootScrollView.smoothScrollTo(0, maxOf(0, scrollAmount))
                }

                updateRowTextColors(view, player, position, focused = true)
            } else {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(150)
                    .start()

                // Reset background based on position
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
        val rowLayout = row as LinearLayout
        val leftText = rowLayout.getChildAt(0) as TextView
        val rightText = rowLayout.getChildAt(1) as TextView

        if (focused) {
            leftText.setTextColor(Color.parseColor("#FFFFFF"))
            rightText.setTextColor(Color.parseColor("#FFA500"))
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
                    AnalyticsService.logEvent(
                        AnalyticsEvents.LEADERBOARD_VIEWED,
                        mapOf(AnalyticsParams.FEATURE_OUTCOME to if (players.isEmpty()) "empty" else "populated")
                    )
                    if (players.isEmpty()) {
                        leaderboardContainer.visibility = View.GONE
                        messageContainer.visibility = View.VISIBLE
                        backBtn.requestFocus()
                    } else {
                        messageContainer.visibility = View.GONE
                        leaderboardContainer.visibility = View.VISIBLE
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
                AnalyticsService.logEvent(
                    AnalyticsEvents.ERROR_DATA_LOAD,
                    mapOf(
                        AnalyticsParams.ERROR_CATEGORY to "leaderboard_load_failed",
                        AnalyticsParams.ERROR_SOURCE to "leaderboard_load"
                    )
                )
                withContext(Dispatchers.Main) {
                    leaderboardContainer.visibility = View.GONE
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

        // Alternating background colors
        if (position % 2 == 0) {
            row.setBackgroundResource(R.drawable.row_normal)
        } else {
            row.setBackgroundResource(R.drawable.row_normal_dark)
        }

        // Left side: Rank icon + Player name
        val leftText = TextView(this)
        val rankDisplay = when (position) {
            0 -> "🥇  "
            1 -> "🥈  "
            2 -> "🥉  "
            else -> "${position + 1}.  "
        }
        leftText.text = "$rankDisplay${player.name}"
        leftText.textSize = 20f
        leftText.setTextColor(when (position) {
            0 -> Color.parseColor("#FFD700")
            1 -> Color.parseColor("#C0C0C0")
            2 -> Color.parseColor("#CD7F32")
            else -> Color.parseColor("#AAAAAA")
        })
        leftText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        // Right side: Points
        val rightText = TextView(this)
        rightText.text = "${player.points} pts"
        rightText.textSize = 20f
        rightText.setTextColor(Color.parseColor("#7F3FFF"))

        row.addView(leftText)
        row.addView(rightText)

        return row
    }

    override fun onResume() {
        super.onResume()
        MusicManager.resumeMusic()
        loadLeaderboard()
    }

    override fun onPause() {
        super.onPause()
        MusicManager.pauseMusic()
        rowFocusAggregator.cancel()
    }
}