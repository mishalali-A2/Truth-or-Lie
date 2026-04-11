package com.futurewatch.truthorlietv

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RoundsActivity : AppCompatActivity() {

    private var selectedRounds = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.rounds)

        val main = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btn3 = findViewById<Button>(R.id.btn3)
        val btn5 = findViewById<Button>(R.id.btn5)
        val btn7 = findViewById<Button>(R.id.btn7)
        val btn10 = findViewById<Button>(R.id.btn10)
        val btn15 = findViewById<Button>(R.id.btn15)

        val buttons = listOf(btn3, btn5, btn7, btn10, btn15)

        //anim
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->

            if (hasFocus) {
                v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .translationZ(20f)
                    .setDuration(150)
                    .start()

               // v.setBackgroundColor(Color.parseColor("#6C5CE7")) // focused color
            } else {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(150)
                    .start()

              //  v.setBackgroundColor(Color.parseColor("#1A2238"))
            }
        }

        buttons.forEach { button ->

            button.onFocusChangeListener = focusListener

            button.setOnClickListener {

                selectedRounds = button.text.toString().toInt()

                buttons.forEach { it.isSelected = false }
                button.isSelected = true

               GameSession.totalRounds = selectedRounds

                val intent = Intent(this, PlayerCountActivity::class.java)
                startActivity(intent)
            }
        }

        // def
        btn5.requestFocus()
    }
}