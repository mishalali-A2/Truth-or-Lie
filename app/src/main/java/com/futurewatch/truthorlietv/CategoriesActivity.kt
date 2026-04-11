package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity

class CategoriesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

//        caused flickering -> parent in xml steals the focus
//        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
//            if (hasFocus) {
//                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scaleup))
//                v.elevation = 20f
//            } else {
//                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.scaledown))
//                v.elevation = 0f
//            }
//        }

        val focusListener = View.OnFocusChangeListener { v, hasFocus ->

            v.clearAnimation()

            if (hasFocus) {
                v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .translationZ(20f)
                    .setDuration(150)
                    .start()
            } else {
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationZ(0f)
                    .setDuration(150)
                    .start()
            }
        }

        // Card -> Category
        val categoryMap = mapOf(
            R.id.card_general to "general_knowledge",
            R.id.card_science to "science",
            R.id.card_animals to "animals"
            // Add more ONLY if they are unlocked
        )

        categoryMap.forEach { (id, categoryName) ->
            val view = findViewById<View>(id)

            view?.apply {
                isFocusable = true
                isClickable = true

                onFocusChangeListener = focusListener

                // to rounds screen
                setOnClickListener {
                    GameSession.category = categoryName
                    val intent = Intent(this@CategoriesActivity, RoundsActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        // Default
        findViewById<View>(R.id.card_general)?.requestFocus()
    }
}