package com.futurewatch.truthorlietv

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Intent

class FactsActivity : AppCompatActivity() {
    private lateinit var factsList: MutableList<Facts>
    private var currentIndex = 0

    data class Facts(
        val id: String,
        val statement: String,
        val answer: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.display_facts)

        MusicManager.resumeMusic()

        val tvCategory = findViewById<TextView>(R.id.tvCategory)
        val tvRound = findViewById<TextView>(R.id.tvRound)
        val tvStatement = findViewById<TextView>(R.id.tvStatement)
        val btnStartVoting = findViewById<Button>(R.id.btnStartVoting)

        // Load facts and create a properly randomized list for this game session
        val allFacts = loadFacts(GameSession.category)
        factsList = createFactSequence(allFacts, GameSession.totalRounds)
        currentIndex = 0

        tvCategory.text = GameSession.category.replace("_", " ").uppercase()
        tvRound.text = "ROUND ${GameSession.currRound}/${GameSession.totalRounds}"

        val currentFact = getNextFact()
        tvStatement.text = currentFact.statement

        // Log the answer for debugging
        android.util.Log.d("FactsActivity", "Fact ${GameSession.currRound}: Answer = ${if(currentFact.answer) "TRUTH" else "LIE"}")

        btnStartVoting.setOnFocusChangeListener { v, hasFocus ->
            v.animate()
                .scaleX(if (hasFocus) 1.05f else 1f)
                .scaleY(if (hasFocus) 1.05f else 1f)
                .setDuration(150)
                .start()
        }

        // goto to Voting Screen
        btnStartVoting.setOnClickListener {

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
        if (allFacts.isEmpty()) return mutableListOf()

        android.util.Log.d("FactsActivity", "Creating fact sequence with ${allFacts.size} available facts for $totalRounds rounds")

        val sequence = mutableListOf<Facts>()
        val availableFacts = allFacts.toMutableList()

        // If we have enough unique facts, use them all and shuffle for randomness
        if (availableFacts.size >= totalRounds) {
            // Shuffle and take exactly what we need
            availableFacts.shuffle()
            sequence.addAll(availableFacts.take(totalRounds))
        } else {
            // If we don't have enough unique facts, distribute them and fill gaps
            val factsNeeded = totalRounds
            val cycles = (factsNeeded / availableFacts.size) + 1

            // Create a pool by repeating all facts enough times
            val factsPool = mutableListOf<Facts>()
            repeat(cycles) {
                factsPool.addAll(availableFacts)
            }

            // Shuffle the pool and take exactly what we need
            factsPool.shuffle()
            sequence.addAll(factsPool.take(factsNeeded))
        }

        android.util.Log.d("FactsActivity", "Fact sequence created with ${sequence.size} facts")
        return sequence
    }

    private fun ensureBalancedFacts(facts: MutableList<Facts>, totalRounds: Int): MutableList<Facts> {
        if (facts.isEmpty()) return facts

        val truths = facts.filter { it.answer }.toMutableList()
        val lies = facts.filter { !it.answer }.toMutableList()

        android.util.Log.d("FactsActivity", "Original - Truths: ${truths.size}, Lies: ${lies.size}, Total: ${facts.size}")

        // If we have no lies at all, we need to generate some
        if (lies.isEmpty()) {
            android.util.Log.w("FactsActivity", "No lies found in database! Converting some truths to lies for balance.")
            return generateMixedFacts(facts, totalRounds)
        }

        // Calculate required balance
        val requiredLies = (totalRounds / 2) + (totalRounds % 2)
        val requiredTruths = totalRounds / 2

        // Shuffle both lists for better randomization
        truths.shuffle()
        lies.shuffle()

        val balancedList = mutableListOf<Facts>()

        // Try to use unique facts first, then repeat if necessary
        val usedTruths = mutableListOf<Facts>()
        val usedLies = mutableListOf<Facts>()

        // Add truths - use all unique ones first, then recycle if needed
        for (i in 0 until requiredTruths) {
            val truthIndex = i % truths.size
            balancedList.add(truths[truthIndex])
            if (i < truths.size && usedTruths.size < truths.size) {
                usedTruths.add(truths[truthIndex])
            }
        }

        // Add lies - use all unique ones first, then recycle if needed
        for (i in 0 until requiredLies) {
            val lieIndex = i % lies.size
            balancedList.add(lies[lieIndex])
            if (i < lies.size && usedLies.size < lies.size) {
                usedLies.add(lies[lieIndex])
            }
        }

        android.util.Log.d("FactsActivity", "Balanced - Required Truths: $requiredTruths (Available: ${truths.size}), Required Lies: $requiredLies (Available: ${lies.size})")
        return balancedList
    }
    private fun generateMixedFacts(facts: MutableList<Facts>, totalRounds: Int): MutableList<Facts> {
        val mixedFacts = mutableListOf<Facts>()
        val halfRounds = totalRounds / 2

        for (i in 0 until totalRounds) {
            val originalFact = facts[i % facts.size]
            // Make half of them lies (every other round starting from round 1)
            val isLie = if (i < halfRounds) i % 2 == 1 else (i + halfRounds) % 2 == 1

            val mixedFact = Facts(
                id = "${originalFact.id}_${if(isLie) "lie" else "truth"}",
                statement = originalFact.statement,
                answer = !isLie // If it's a lie, answer = false
            )
            mixedFacts.add(mixedFact)
        }

        return mixedFacts
    }


    //  facts from JSON
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

                if (list.isNotEmpty()) break // Use the first one that works

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return list
    }

    //next fact
    private fun getNextFact(): Facts {

        // If all facts used → reshuffle
        if (currentIndex >= factsList.size) {
            factsList.shuffle()
            currentIndex = 0
        }

        val fact = factsList[currentIndex]
        currentIndex++

        return fact
    }
}