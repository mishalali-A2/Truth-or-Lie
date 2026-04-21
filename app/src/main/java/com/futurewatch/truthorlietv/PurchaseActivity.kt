package com.futurewatch.truthorlietv

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class PurchaseActivity : AppCompatActivity() {

    private lateinit var btnPurchase: Button
    private lateinit var btnCancel: Button
    private lateinit var txtRestore: TextView
    private lateinit var titlePremium: TextView
    private lateinit var description: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.purchase)

        btnPurchase = findViewById(R.id.btnPurchase)
        btnCancel = findViewById(R.id.btnCancel)
        txtRestore = findViewById(R.id.txtRestore)
        titlePremium = findViewById(R.id.titlePremium)
        description = findViewById(R.id.description)

        // Check if already purchased
        val prefs = TruthOrLieApplication.prefs
        val allCategoriesUnlocked = prefs.getBoolean("all_categories_unlocked", false)

        if (allCategoriesUnlocked) {
            // Show already purchased state
            titlePremium.text = "ALREADY UNLOCKED!"
            description.text = "You already have full access to all categories!"
            btnPurchase.text = "Unlocked ✓"
            btnPurchase.isEnabled = false
            btnPurchase.alpha = 0.5f
        }

        // Purchase button click
        btnPurchase.setOnClickListener {
            if (allCategoriesUnlocked) {
                Toast.makeText(this, "Already unlocked!", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }
            purchaseProduct("unlock_all_categories")
        }

        // Cancel button
        btnCancel.setOnClickListener {
            finish()
        }

        // Restore purchases
        txtRestore.setOnClickListener {
            restorePurchases()
        }

        // Handle back press - close overlay
        btnCancel.requestFocus()

        setupFocusAnimation()
    }

    private fun purchaseProduct(productId: String) {
        val prefs = TruthOrLieApplication.prefs

        // Check if already owned
        if (prefs.getBoolean("all_categories_unlocked", false)) {
            Toast.makeText(this, "Already unlocked!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("PurchaseActivity", "Initiating purchase for: $productId")
        try {
            TruthOrLieApplication.billingRepository.purchaseProduct(this, productId)
        } catch (e: Exception) {
            Log.e("PurchaseActivity", "Error launching purchase flow", e)
            Toast.makeText(this, "Billing error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restorePurchases() {
        Log.d("PurchaseActivity", "Restoring purchases...")
        Toast.makeText(this, "Checking for previous purchases...", Toast.LENGTH_SHORT).show()
        TruthOrLieApplication.billingRepository.restorePurchases()

        // Check after restore
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val prefs = TruthOrLieApplication.prefs
            if (prefs.getBoolean("all_categories_unlocked", false)) {
                Toast.makeText(this, "Purchase restored! All categories unlocked.", Toast.LENGTH_LONG).show()
                finish()
            } else {
                Toast.makeText(this, "No previous purchases found.", Toast.LENGTH_SHORT).show()
            }
        }, 2000)
    }

    private fun setupFocusAnimation() {
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).start()
                v.translationZ = 20f
            } else {
                v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                v.translationZ = 0f
            }
        }

        btnPurchase.onFocusChangeListener = focusListener
        btnCancel.onFocusChangeListener = focusListener
        txtRestore.onFocusChangeListener = focusListener
    }

    override fun onResume() {
        super.onResume()
        // Update UI in case purchase completed while away
        val prefs = TruthOrLieApplication.prefs
        if (prefs.getBoolean("all_categories_unlocked", false)) {
            finish() // Close if now unlocked
        }
    }
}