package com.futurewatch.truthorlietv

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Intent
import com.futurewatch.truthorlietv.analytics.AnalyticsEvents
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsScreens
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.ScreenTracker

class FactsActivity : AppCompatActivity() {
    private lateinit var factsList: MutableList<Facts>
    private var currentIndex = 0

    companion object {
        private val usedFactIds = mutableSetOf<String>()

        fun resetUsedFacts() {
            usedFactIds.clear()
        }

        fun hasBeenUsed(factId: String): Boolean {
            return usedFactIds.contains(factId)
        }

        fun markAsUsed(factId: String) {
            usedFactIds.add(factId)
        }
    }

    data class Facts(
        val id: String,
        val statement: String,
        val answer: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.display_facts)

        MusicManager.resumeMusic()

        ScreenTracker.attach(this, AnalyticsScreens.FACTS, previousScreen = AnalyticsScreens.PLAYER_NAMES)

        // Reset used facts only at the start of the first round
        if (GameSession.currRound == 1) {
            resetUsedFacts()
        }

        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        val tvRound = findViewById<TextView>(R.id.tvRound)
        val tvStatement = findViewById<TextView>(R.id.tvStatement)
        val btnStartVoting = findViewById<Button>(R.id.btnStartVoting)

        val allFacts = loadFacts(GameSession.category)

        // Separate truths and lies, excluding already used facts
        var truths = allFacts.filter { it.answer && !hasBeenUsed(it.id) }.toMutableList()
        var lies = allFacts.filter { !it.answer && !hasBeenUsed(it.id) }.toMutableList()

        android.util.Log.d("FactsActivity", "Available - Truths: ${truths.size}, Lies: ${lies.size}")

        // If we run out of either truths or lies, reset and use all facts
        if (truths.isEmpty() || lies.isEmpty()) {
            android.util.Log.w("FactsActivity", "Ran out of ${if(truths.isEmpty()) "truths" else "lies"}! Resetting used facts.")
            resetUsedFacts()
            val allFactsRefresh = loadFacts(GameSession.category)
            truths = allFactsRefresh.filter { it.answer }.toMutableList()
            lies = allFactsRefresh.filter { !it.answer }.toMutableList()
            android.util.Log.d("FactsActivity", "Refreshed - Truths: ${truths.size}, Lies: ${lies.size}")
        }

        // Combine all available facts
        val availableFacts = (truths + lies).toMutableList()

        // Create fact sequence for all remaining rounds
        factsList = createFactSequence(availableFacts, GameSession.totalRounds - GameSession.currRound + 1)
        currentIndex = 0

        tvCategory.text = GameSession.category.replace("_", " ").uppercase()
        tvRound.text = "ROUND ${GameSession.currRound}/${GameSession.totalRounds}"

        val currentFact = getNextFact()
        tvStatement.text = currentFact.statement
        markAsUsed(currentFact.id)

        android.util.Log.d("FactsActivity", "Fact ${GameSession.currRound}: Answer = ${if(currentFact.answer) "TRUTH" else "LIE"}, Statement: ${currentFact.statement}")

        AnalyticsService.logEvent(
            AnalyticsEvents.ROUND_STARTED,
            mapOf(
                AnalyticsParams.CATEGORY_ID to GameSession.category,
                AnalyticsParams.ROUND_NUMBER to GameSession.currRound
            )
        )

        btnStartVoting.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.05f else 1f)
                .scaleY(if (hasFocus) 1.05f else 1f)
                .setDuration(150)
                .start()
        }

        // Go to Voting Screen
        btnStartVoting.setOnClickListener {
            AnalyticsService.logEvent(
                AnalyticsEvents.CONTROL_CLICK,
                mapOf(AnalyticsParams.SCREEN_NAME to AnalyticsScreens.FACTS, AnalyticsParams.CONTROL_ID to "facts_start_voting")
            )
            val intent = Intent(this, VotingActivity::class.java)
            intent.putExtra("ROUND", GameSession.currRound)
            intent.putExtra("TOTAL_ROUNDS", GameSession.totalRounds)
            intent.putExtra("CATEGORY", GameSession.category)
            intent.putExtra("FACT_ID", currentFact.id)
            intent.putExtra("STATEMENT", currentFact.statement)
            intent.putExtra("ANSWER", currentFact.answer)

            startActivity(intent)
        }
    }

    private fun createFactSequence(allFacts: MutableList<Facts>, totalRounds: Int): MutableList<Facts> {
        if (allFacts.isEmpty()) {
            android.util.Log.w("FactsActivity", "No facts available!")
            return mutableListOf()
        }

        android.util.Log.d("FactsActivity", "Creating fact sequence with ${allFacts.size} available facts for $totalRounds rounds")

        val sequence = mutableListOf<Facts>()
        val availableFacts = allFacts.toMutableList()

        // For single round, just pick randomly
        if (totalRounds == 1) {
            availableFacts.shuffle()
            sequence.add(availableFacts[0])
            return sequence
        }

        var lastWasTruth: Boolean? = null
        var sameTypeCount = 0

        repeat(totalRounds) {
            val currentAvailable = if (sequence.isNotEmpty()) {
                availableFacts.filter { it.id != sequence.last().id }.toMutableList()
            } else {
                availableFacts
            }

            if (currentAvailable.isEmpty()) {
                availableFacts.shuffle()
                sequence.add(availableFacts[0])
                lastWasTruth = availableFacts[0].answer
                sameTypeCount = 1
                return@repeat
            }

            //avoid 3+ of same type
            if (sameTypeCount >= 2 && lastWasTruth != null) {
                val oppositeTypeFacts = currentAvailable.filter { it.answer != lastWasTruth }
                if (oppositeTypeFacts.isNotEmpty()) {
                    // Force pick oppos type
                    val forcedFact = oppositeTypeFacts.random()
                    sequence.add(forcedFact)
                    lastWasTruth = forcedFact.answer
                    sameTypeCount = 1
                    android.util.Log.d("FactsActivity", "Forced ${if(forcedFact.answer) "TRUTH" else "LIE"} to break pattern")
                    return@repeat
                }
            }

            val selectedFact = if (sequence.size < 2) {
                currentAvailable.random()
            } else {
                currentAvailable.random()
            }

            sequence.add(selectedFact)

            if (lastWasTruth == selectedFact.answer) {
                sameTypeCount++
            } else {
                sameTypeCount = 1
            }
            lastWasTruth = selectedFact.answer
        }

        android.util.Log.d("FactsActivity", "Created sequence: ${sequence.map { if(it.answer) "T" else "L" }.joinToString(", ")}")
        return sequence
    }

    // Load facts from JSON
    private fun loadFacts(category: String): MutableList<Facts> {
        val list = mutableListOf<Facts>()

        val categoriesToTry = listOf(category, "mixed_facts")

        for (cat in categoriesToTry) {
            try {
                val inputStream = assets.open("$cat.json")
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonStr = reader.readText()

                val jsonObject = JSONObject(jsonStr)
                val factsArray = jsonObject.getJSONArray("questions")

                for (i in 0 until factsArray.length()) {
                    val obj = factsArray.getJSONObject(i)

                    list.add(
                        Facts(
                            id = obj.getString("id"),
                            statement = obj.getString("statement"),
                            answer = obj.getBoolean("answer")
                        )
                    )
                }

                if (list.isNotEmpty()) break // Use the first category that works

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return list
    }

    // Get next fact from the sequence
    private fun getNextFact(): Facts {
        // If we've used all facts in the sequence, reshuffle
        if (currentIndex >= factsList.size) {
            factsList.shuffle()
            currentIndex = 0
        }

        val fact = factsList[currentIndex]
        currentIndex++

        return fact
    }
}