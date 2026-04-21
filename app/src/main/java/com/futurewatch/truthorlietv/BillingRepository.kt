package com.futurewatch.truthorlietv

import android.app.Activity
import com.android.billingclient.api.ProductDetails

class BillingRepository private constructor(private val billingManager: BillingManager) {

    companion object {
        @Volatile
        private var instance: BillingRepository? = null

        fun getInstance(context: android.content.Context, listener: BillingManager.BillingListener): BillingRepository {
            return instance ?: synchronized(this) {
                val billingManager = BillingManager(context, listener)
                instance ?: BillingRepository(billingManager).also { instance = it }
            }
        }
    }

    fun initialize() {
        billingManager.initialize()
    }

    fun getProducts(): List<ProductDetails> {
        return emptyList()
    }

    fun purchaseProduct(activity: Activity, productId: String) {
        billingManager.launchPurchaseFlow(activity, productId)
    }

    fun clearPurchaseCache() {
        billingManager.clearPurchaseCache()
    }

    fun resetBillingState() {
        billingManager.resetBillingState()
    }
    fun restorePurchases() {
        billingManager.restorePurchases()
    }

//    fun isPremium(): Boolean {
//        return billingManager.getHasPremiumAccess()
//    }

    fun destroy() {
        billingManager.destroy()
    }
}