package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PlayerCountActivity : AppCompatActivity() {

    private var playerCount = 2

    private val MIN_PLAYERS = 2
    private val MAX_PLAYERS = 6

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.player_count)

        val main = findViewById<android.view.View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnMinus = findViewById<Button>(R.id.btnMinus)
        val btnPlus = findViewById<Button>(R.id.btnPlus)
        val txtPlayerCount = findViewById<TextView>(R.id.txtPlayerCount)
        val btnPlayers = findViewById<Button>(R.id.btnPlayers)

        // def
        txtPlayerCount.text = playerCount.toString()

        btnMinus.setOnClickListener {
            if (playerCount > MIN_PLAYERS) {
                playerCount--
                txtPlayerCount.text = playerCount.toString()
            }
        }

        btnPlus.setOnClickListener {
            if (playerCount < MAX_PLAYERS) {
                playerCount++
                txtPlayerCount.text = playerCount.toString()
            }
        }
//set name screen
        btnPlayers.setOnClickListener {
            val intent = Intent(this, PlayerNamesActivity::class.java)
            intent.putExtra("PLAYER_COUNT", playerCount)
            startActivity(intent)
        }
    }
}