package com.futurewatch.truthorlietv

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val splashDelay = 3500L

    private lateinit var dots: Array<TextView>
    private var dotIndex = 0
    private val dotHandler = Handler(Looper.getMainLooper())

    private val dotColors = intArrayOf(
        0xFF6C63FF.toInt(),
        0xFF8B7BFF.toInt(),
        0xFFB3A6FF.toInt()
    )

    private val dotRunnable = object : Runnable {
        override fun run() {
            dots.forEachIndexed { index, dot ->
                dot.setTextColor(dotColors[(index + dotIndex) % dotColors.size])
            }
            dotIndex++
            dotHandler.postDelayed(this, 400)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)

        val tvTruth = findViewById<View>(R.id.tvTruth)
        val tvLie = findViewById<View>(R.id.tvLie)
        val tvOr = findViewById<View>(R.id.tvOr)
        val subtitle = findViewById<View>(R.id.subtitle)

        dots = arrayOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3)
        )

        tvTruth.translationX = -1000f
        tvLie.translationX = 1000f

        tvOr.translationY = 200f
        tvOr.scaleX = 0f
        tvOr.scaleY = 0f

        subtitle.alpha = 0f

        val truthAnim = ObjectAnimator.ofFloat(tvTruth, "translationX", 0f).apply {
            duration = 800
            interpolator = OvershootInterpolator()
        }

        val lieAnim = ObjectAnimator.ofFloat(tvLie, "translationX", 0f).apply {
            duration = 800
            interpolator = OvershootInterpolator()
        }

        val orAnim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(tvOr, "translationY", 0f),
                ObjectAnimator.ofFloat(tvOr, "scaleX", 1f),
                ObjectAnimator.ofFloat(tvOr, "scaleY", 1f)
            )
            duration = 600
            interpolator = OvershootInterpolator()
        }

        val subtitleAnim = ObjectAnimator.ofFloat(subtitle, "alpha", 1f).apply {
            duration = 900
        }

        AnimatorSet().apply {
            playTogether(truthAnim, lieAnim)
            play(orAnim).after(truthAnim)
            play(subtitleAnim).after(orAnim)
            start()
        }

        dotHandler.post(dotRunnable)

        Handler(Looper.getMainLooper()).postDelayed({
            dotHandler.removeCallbacks(dotRunnable)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, splashDelay)
    }

    override fun onDestroy() {
        super.onDestroy()
        dotHandler.removeCallbacks(dotRunnable)
    }
}