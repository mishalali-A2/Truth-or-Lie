package com.futurewatch.truthorlietv

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import android.app.ActivityManager
import android.os.Process
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.futurewatch.truthorlietv.analytics.AnalyticsParams
import com.futurewatch.truthorlietv.analytics.AnalyticsService
import com.futurewatch.truthorlietv.analytics.SessionTracker
class TruthOrLieApplication : Application() {

    companion object {
        lateinit var instance: TruthOrLieApplication
            private set
        lateinit var prefs: SharedPreferences
        lateinit var billingRepository: BillingRepository
    }

    override fun onCreate() {
        super.onCreate()
        //make sure no other processes are running only main
        if (!isMainProcess()) {
            Log.w("TruthOrLieApp", "Skipping init in non-main process")
            return
        }
        instance = this
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        initializeDefaultSettings()

        // Analytics init happens early so every downstream manager/activity can safely log events.
        AnalyticsService.init(this)
        SessionTracker.init()
        setAnalyticsUserProperties()

        MusicManager.init(this)

        TimerManager.init(this)

        CategoryManager.initialize(this)

        initializeBilling()

        // Re-verify subscription status every time the app comes to the foreground
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                billingRepository.restorePurchases()
            }
        })
    }

    private fun setAnalyticsUserProperties() {
        AnalyticsService.setUserProperty(AnalyticsParams.DEVICE_CATEGORY, "android_tv")
        AnalyticsService.setUserProperty(AnalyticsParams.APP_VERSION, BuildConfig.VERSION_NAME)
        updatePremiumStatusUserProperty()
    }

    /** Coarse boolean-ish premium status only — never the specific product purchased. */
    fun updatePremiumStatusUserProperty() {
        val hasPremium = prefs.getBoolean("all_categories_unlocked", false) ||
            prefs.getBoolean("ads_removed", false) ||
            prefs.getBoolean("premium_access", false)
        AnalyticsService.setUserProperty(
            AnalyticsParams.PREMIUM_STATUS,
            if (hasPremium) "has_premium" else "free"
        )
    }

    private fun isMainProcess(): Boolean {
        return try {
            val myPid = Process.myPid()
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            activityManager.runningAppProcesses?.forEach { processInfo ->
                if (processInfo.pid == myPid) {
                    return processInfo.processName == packageName
                }
            }
            true
        } catch (e: Exception) {
            Log.e("TruthOrLieApp", "Error checking process: ${e.message}")
            true
        }
    }

    private fun initializeDefaultSettings() {
        // Check if we have already initialized settings
        if (!prefs.contains("first_run_initialized")) {
            Log.d("TruthOrLieApp", "First run detected! Initializing default settings...")

            prefs.edit().apply {
                // Audio Defaults
                putBoolean("music_enabled", true)
                putString("music_genre", "Chill Lounge")

                // Gameplay Defaults
                putInt("timer_seconds", 20)

                // Mark as initialized so this doesn't run again
                putBoolean("first_run_initialized", true)

                apply()
            }
        }
    }

    private fun initializeBilling() {
        billingRepository = BillingRepository.getInstance(this, object : BillingManager.BillingListener {
            override fun onBillingSetupFinished() {
                Log.d("Billing", "Billing setup finished")
                AnalyticsService.logEvent(com.futurewatch.truthorlietv.analytics.AnalyticsEvents.BILLING_SETUP_FINISHED)
            }

            override fun onBillingDisconnected() {
                Log.d("Billing", "Billing disconnected")
                AnalyticsService.logEvent(com.futurewatch.truthorlietv.analytics.AnalyticsEvents.BILLING_DISCONNECTED)
            }

            override fun onProductsUpdated(products: List<com.android.billingclient.api.ProductDetails>) {
                Log.d("Billing", "Products updated: ${products.size}")
                products.forEach { product ->
                    Log.d("Billing", "Product: ${product.productId} - ${product.name}")
                }
            }

            override fun onPurchaseSuccess(productId: String) {
                Log.d("Billing", "Purchase success: $productId")
                AnalyticsService.logEvent(
                    com.futurewatch.truthorlietv.analytics.AnalyticsEvents.PURCHASE_SUCCEEDED,
                    mapOf(AnalyticsParams.PRODUCT_ID to productId)
                )
                if (productId in BillingManager.PRODUCT_TO_CATEGORY.keys) {
                    AnalyticsService.logEvent(
                        com.futurewatch.truthorlietv.analytics.AnalyticsEvents.CATEGORY_UNLOCK_PURCHASED,
                        mapOf(AnalyticsParams.PRODUCT_ID to productId)
                    )
                }
                when {
                    productId == "premium.access" -> {
                        prefs.edit().putBoolean("all_categories_unlocked", true).apply()
                        Log.d("Billing", "Premium access purchased!")
                    }
                    productId in setOf("premium_monthly", "premium_yearly") -> {
                        Log.d("Billing", "Premium subscription active!")
                    }
                    productId.startsWith("buy.") -> {
                        Log.d("Billing", "Category purchased: $productId - unlocked for 24h")
                    }
                    productId == "remove_ads" -> {
                        prefs.edit().putBoolean("ads_removed", true).apply()
                        Log.d("Billing", "Ads removed permanently!")
                    }
                    productId == "unlock_all_categories" -> {
                        prefs.edit().putBoolean("all_categories_unlocked", true).apply()
                        Log.d("Billing", "All categories unlocked!")
                    }
                }
                updatePremiumStatusUserProperty()
            }

            override fun onPurchaseError(responseCode: Int, message: String?) {
                Log.e("Billing", "Purchase error: $responseCode - $message")
                AnalyticsService.logEvent(
                    com.futurewatch.truthorlietv.analytics.AnalyticsEvents.PURCHASE_FAILED,
                    mapOf(AnalyticsParams.ERROR_CATEGORY to "billing_code_$responseCode")
                )
                AnalyticsService.logEvent(
                    com.futurewatch.truthorlietv.analytics.AnalyticsEvents.ERROR_BILLING,
                    mapOf(AnalyticsParams.ERROR_CATEGORY to "response_code_$responseCode")
                )
            }

            override fun onPurchaseCanceled() {
                Log.d("Billing", "Purchase canceled")
                AnalyticsService.logEvent(com.futurewatch.truthorlietv.analytics.AnalyticsEvents.PURCHASE_CANCELED)
            }

            override fun onPurchaseAlreadyOwned() {
                Log.d("Billing", "Purchase already owned")
                AnalyticsService.logEvent(com.futurewatch.truthorlietv.analytics.AnalyticsEvents.PURCHASE_ALREADY_OWNED)
            }

            override fun onPurchasePending(productId: String) {
                Log.d("Billing", "Purchase pending: $productId")
                AnalyticsService.logEvent(
                    com.futurewatch.truthorlietv.analytics.AnalyticsEvents.PURCHASE_PENDING,
                    mapOf(AnalyticsParams.PRODUCT_ID to productId)
                )
            }

            override fun onAcknowledgeFailed(debugMessage: String?) {
                Log.e("Billing", "Acknowledge failed: $debugMessage")
                AnalyticsService.logEvent(
                    com.futurewatch.truthorlietv.analytics.AnalyticsEvents.ERROR_BILLING,
                    mapOf(AnalyticsParams.ERROR_CATEGORY to "acknowledge_failed")
                )
            }

            override fun onRestoreCompleted(hasPremium: Boolean) {
                Log.d("Billing", "Restore completed - Premium: $hasPremium")
                AnalyticsService.logEvent(
                    com.futurewatch.truthorlietv.analytics.AnalyticsEvents.RESTORE_PURCHASES_COMPLETED,
                    mapOf(AnalyticsParams.FEATURE_OUTCOME to if (hasPremium) "has_premium" else "no_premium")
                )
                updatePremiumStatusUserProperty()
                if (hasPremium) {

                }
            }
        })

        billingRepository.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        MusicManager.onAppDestroy()
    }
}