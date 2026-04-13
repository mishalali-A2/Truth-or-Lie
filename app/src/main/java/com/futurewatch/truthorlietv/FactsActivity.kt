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

        // Load + shuffle facts -> no repeat
        factsList = loadFacts(GameSession.category).shuffled().toMutableList()
        currentIndex = 0

        tvCategory.text = GameSession.category.replace("_", " ").uppercase()
        tvRound.text = "ROUND ${GameSession.currRound}/${GameSession.totalRounds}"

        val currentFact = getNextFact()
        tvStatement.text = currentFact.statement

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