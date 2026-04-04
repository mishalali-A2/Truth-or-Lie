package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 3500L // 3.5 seconds
    private lateinit var dots: Array<TextView>
    private var dotIndex = 0
    private val dotHandler = Handler(Looper.getMainLooper())

    private val dotRunnable = object : Runnable {
        override fun run() {
            // Reset all dots
            dots.forEach { it.setTextColor(0xFF9CA3AF.toInt()) } // gray
            // Highlight current dot
            dots[dotIndex].setTextColor(0xFFFFFFFF.toInt()) // white
            dotIndex = (dotIndex + 1) % dots.size
            dotHandler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)

        dots = arrayOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3)
        )

        // Start dot animation
        dotHandler.post(dotRunnable)

        // Delay to launch MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            dotHandler.removeCallbacks(dotRunnable)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, SPLASH_DELAY)
    }
}