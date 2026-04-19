package com.futurewatch.truthorlietv

import android.app.Application
import android.content.SharedPreferences
import com.unity3d.ads.UnityAds
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import android.app.ActivityManager
import android.os.Process
class TruthOrLieApplication : Application() {

    companion object {
        lateinit var instance: TruthOrLieApplication
            private set
        lateinit var prefs: SharedPreferences
        lateinit var billingRepository: BillingRepository
    }

    override fun onCreate() {
        super.onCreate()
        if (!isMainProcess()) {
            Log.w("TruthOrLieApp", "Skipping init in non-main process")
            return
        }
        instance = this
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        UnityAds.initialize(this, "6069840", true, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d("UnityAds", "SDK Initialized Successfully")
            }

            override fun onInitializationFailed(
                error: UnityAds.UnityAdsInitializationError,
                message: String
            ) {
                Log.e("UnityAds", "SDK Initialization Failed: $error - $message")
            }
        })
        MusicManager.init(this)

        TimerManager.init(this)

        initializeBilling()

        initializeInfaticaSafely()

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
            true // Default to true can't determine
        } catch (e: Exception) {
            Log.e("TruthOrLieApp", "Error checking process: ${e.message}")
            true // Default to true on error
        }
    }
    private fun initializeInfaticaSafely() {
        try {
            // Check if we have storage permissions/storage available
            val storageAvailable = isStorageAvailable()
            if (!storageAvailable) {
                Log.w("TruthOrLieApp", "Storage not available, Infatica SDK may not work properly")
                return
            }

            // Check if Infatica was previously enabled
            val infaticaEnabled = prefs.getBoolean("network_sdk_enabled", false)
            if (infaticaEnabled) {
                Log.d("TruthOrLieApp", "Infatica was previously enabled, but will start from Settings")
                // Don't auto-start here - let user control via settings
            }
        } catch (e: Exception) {
            Log.e("TruthOrLieApp", "Failed to initialize Infatica: ${e.message}", e)
        }
    }

    private fun isStorageAvailable(): Boolean {
        return try {
            val cacheDir = cacheDir
            cacheDir.exists() && cacheDir.canWrite()
        } catch (e: Exception) {
            false
        }
    }

    private fun initializeBilling() {
        billingRepository = BillingRepository.getInstance(this, object : BillingManager.BillingListener {
            override fun onBillingSetupFinished() {
                Log.d("Billing", "Billing setup finished")
            }

            override fun onBillingDisconnected() {
                Log.d("Billing", "Billing disconnected")
            }

            override fun onProductsUpdated(products: List<com.android.billingclient.api.ProductDetails>) {
                Log.d("Billing", "Products updated: ${products.size}")
                products.forEach { product ->
                    Log.d("Billing", "Product: ${product.productId} - ${product.name}")
                }
            }

            override fun onPurchaseSuccess(productId: String) {
                Log.d("Billing", "Purchase success: $productId")
                // Handle different purchase types
                when (productId) {
                    "remove_ads" -> {
                        prefs.edit().putBoolean("ads_removed", true).apply()
                        Log.d("Billing", "Ads removed permanently!")
                    }
                    "unlock_all_categories" -> {
                        prefs.edit().putBoolean("all_categories_unlocked", true).apply()
                        Log.d("Billing", "All categories unlocked!")
                    }
                    "premium_monthly", "premium_yearly" -> {
                        // Premium subscription - handled via getHasPremiumAccess()
                        Log.d("Billing", "Premium subscription active!")
                    }
                }
            }

            override fun onPurchaseError(responseCode: Int, message: String?) {
                Log.e("Billing", "Purchase error: $responseCode - $message")
            }

            override fun onPurchaseCanceled() {
                Log.d("Billing", "Purchase canceled")
            }

            override fun onRestoreCompleted(hasPremium: Boolean) {
                Log.d("Billing", "Restore completed - Premium: $hasPremium")
                if (hasPremium) {
                    // Update UI to show premium features
                }
            }
        })

        billingRepository.initialize()
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            InfaticaManager.stop(this)
        } catch (e: Exception) {
            Log.e("TruthOrLieApp", "Error stopping Infatica: ${e.message}")
        }
        MusicManager.onAppDestroy()
    }
}