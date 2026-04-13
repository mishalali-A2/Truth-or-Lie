package com.futurewatch.truthorlietv

import android.app.Activity
import com.unity3d.ads.*

object AdManager {
    private const val REWARDED_ID = "Rewarded_Android"
    private const val INTERSTITIAL_ID = "Interstitial_Android"

    private var lastInterstitialTime = 0L

    // Rewarded Ads
    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        UnityAds.show(activity, REWARDED_ID, object : IUnityAdsShowListener {

            override fun onUnityAdsShowComplete(
                placementId: String,
                state: UnityAds.UnityAdsShowCompletionState
            ) {
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    onRewardEarned()
                }
            }

            override fun onUnityAdsShowFailure(
                placementId: String,
                error: UnityAds.UnityAdsShowError,
                message: String
            ) {}

            override fun onUnityAdsShowStart(placementId: String) {}
            override fun onUnityAdsShowClick(placementId: String) {}
        })
    }

    // Interstitial Ad w/ 3 min freq
    fun showInterstitial(activity: Activity) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastInterstitialTime > 3 * 60 * 1000) {
            UnityAds.show(activity, INTERSTITIAL_ID)
            lastInterstitialTime = currentTime
        }
    }
}