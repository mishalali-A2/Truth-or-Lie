package com.futurewatch.truthorlietv

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class CategoriesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        MusicManager.resumeMusic()
//        caused flickering -> parent in XML steals the focus
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
            R.id.card_animals to "animals",
            //locked
            R.id.card_history to "history",
            R.id.card_space to "space",
            R.id.card_technology to "technology",
            R.id.card_human_body to "human_body",
            R.id.card_crazy_facts to "crazy_facts",
            R.id.card_mixed_facts to "mixed_facts"
        )

        categoryMap.forEach { (id, categoryName) ->
            val view = findViewById<View>(id)

            view?.apply {
                isFocusable = true
                isClickable = true

                onFocusChangeListener = focusListener

                // to rounds screen
                setOnClickListener {
                    if (CategoryManager.isUnlocked(categoryName)) {
                        GameSession.category = categoryName
                        val intent = Intent(this@CategoriesActivity, RoundsActivity::class.java)
                        startActivity(intent)
                    } else {
                        showUnlockDialog(categoryName)
                    }
                }
            }
        }

        // Default
        findViewById<View>(R.id.card_general)?.requestFocus()
    }

    private fun showUnlockDialog(category: String) {

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Unlock Category")
            .setMessage("Watch an ad to unlock for this session or buy full access.")
            .setPositiveButton("Watch Ad") { _, _ ->

                AdManager.showRewardedAd(this) {
                    // Unlock after ad
                    CategoryManager.unlockTemporarily(category)

                    // Start game immediately
                    GameSession.category = category
                    startActivity(Intent(this, FactsActivity::class.java))
                }
            }
            .setNegativeButton("Buy ($2.99)") { _, _ ->
                // Google Play Billing
            }
            .setNeutralButton("Cancel", null)
            .create()

        dialog.setOnDismissListener {
            findViewById<View>(R.id.card_general)?.requestFocus()
        }

        dialog.show()
    }
}