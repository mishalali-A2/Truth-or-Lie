package com.futurewatch.truthorlietv

import android.app.Activity
import android.util.Log
import com.unity3d.ads.UnityAds
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener

object AdManager {
    private const val REWARDED_ID = "Rewarded_Android"
    private const val INTERSTITIAL_ID = "Interstitial_Android"
    private const val GAMEID= "6069840"
    private var lastInterstitialTime = 0L
    private const val INTERSTITIAL_COOLDOWN = 90 * 1000L // 90 seconds
    private var isInterstitialPreloaded = false
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false
    private var isInitialized = false

  //initialize unity
    fun initialize(activity: Activity, testMode: Boolean = true, onComplete: () -> Unit = {}) {
        if (isInitialized) {
            onComplete()
            return
        }

        UnityAds.initialize(activity, GAMEID, testMode, object : IUnityAdsInitializationListener {
            override fun onInitializationComplete() {
                Log.d("AdManager", "Unity Ads initialized successfully")
                isInitialized = true
                preloadInterstitial()
                onComplete()
            }

            override fun onInitializationFailed(
                error: UnityAds.UnityAdsInitializationError,
                message: String
            ) {
                Log.e("AdManager", " Unity Ads init failed: $error - $message")
                isInitialized = false
            }
        })
    }

    // Preload interstitial -> faster loading
    fun preloadInterstitial() {
        if (!isInitialized) return
        if (isLoadingInterstitial || isInterstitialPreloaded) return

        isLoadingInterstitial = true
        Log.d("AdManager", "Preloading interstitial ad...")

        UnityAds.load(INTERSTITIAL_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", " Interstitial preloaded successfully")
                isInterstitialPreloaded = true
                isLoadingInterstitial = false
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e("AdManager", " Failed to preload interstitial: $error - $message")
                isLoadingInterstitial = false
                isInterstitialPreloaded = false
            }
        })
    }

    //cooldown
    private fun canShowInterstitial(): Boolean {
        if (lastInterstitialTime == 0L) return true
        val diff = System.currentTimeMillis() - lastInterstitialTime
        return diff >= INTERSTITIAL_COOLDOWN
    }

    // Show Interstitial Ad
    fun showInterstitial(
        activity: Activity,
        onComplete: () -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        if (!isInitialized) {
            Log.e("AdManager", "Unity Ads not initialized")
            onFailed()
            return
        }

        if (!canShowInterstitial()) {
            val remainingSeconds = (INTERSTITIAL_COOLDOWN - (System.currentTimeMillis() - lastInterstitialTime)) / 1000
            Log.d("AdManager", "Interstitial on cooldown: ${remainingSeconds}s remaining")
            onFailed()
            return
        }
        //pause music on ads
        MusicManager.pauseMusic()
        if (isLoadingInterstitial) {
            Log.d("AdManager", "Interstitial still loading, waiting...")
            onFailed()
            return
        }
//on preload
        if (isInterstitialPreloaded) {
            isInterstitialPreloaded = false
            showLoadedInterstitial(activity, onComplete, onFailed)
            return
        }

        //else load first
        isLoadingInterstitial = true
        Log.d("AdManager", "Loading interstitial on demand...")

        UnityAds.load(INTERSTITIAL_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "Interstitial loaded, showing...")
                isLoadingInterstitial = false
                showLoadedInterstitial(activity, onComplete, onFailed)
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e("AdManager", " Failed to load interstitial: $error - $message")
                isLoadingInterstitial = false
                //resume on failure
                MusicManager.resumeMusic()
                onFailed()
            }
        })
    }

    private fun showLoadedInterstitial(
        activity: Activity,
        onComplete: () -> Unit,
        onFailed: () -> Unit
    ) {
        UnityAds.show(activity, INTERSTITIAL_ID, object : IUnityAdsShowListener {
            override fun onUnityAdsShowComplete(
                placementId: String,
                state: UnityAds.UnityAdsShowCompletionState
            ) {
                Log.d("AdManager", "Interstitial complete: $state")
                lastInterstitialTime = System.currentTimeMillis()
                preloadInterstitial() // Preload next one
                //resume on ad completion
                MusicManager.resumeMusic()
                onComplete()
            }

            override fun onUnityAdsShowFailure(
                placementId: String,
                error: UnityAds.UnityAdsShowError,
                message: String
            ) {
                Log.e("AdManager", "Interstitial show failed: $error - $message")
                preloadInterstitial()
                //resume on failure
                MusicManager.resumeMusic()
                onFailed()
            }

            override fun onUnityAdsShowStart(placementId: String) {
                Log.d("AdManager", "Interstitial started")
            }

            override fun onUnityAdsShowClick(placementId: String) {
                Log.d("AdManager", "Interstitial clicked")
            }
        })
    }

    // Show Rewarded Ad -> reward on complete
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onFailed: () -> Unit = {}
    ) {
        if (!isInitialized) {
            Log.e("AdManager", "Unity Ads not initialized")
            onFailed()
            return
        }
        // Pause music before showing ad
        MusicManager.pauseMusic()

        if (isLoadingRewarded) {
            Log.d("AdManager", "Rewarded ad already loading")
            onFailed()
            return
        }

        isLoadingRewarded = true
        Log.d("AdManager", "Loading rewarded ad...")

        UnityAds.load(REWARDED_ID, object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                Log.d("AdManager", "Rewarded ad loaded, showing...")
                isLoadingRewarded = false

                UnityAds.show(activity, REWARDED_ID, object : IUnityAdsShowListener {
                    override fun onUnityAdsShowComplete(
                        placementId: String,
                        state: UnityAds.UnityAdsShowCompletionState
                    ) {
                        Log.d("AdManager", "Rewarded ad complete: $state")
                        //resume on ad completion
                        MusicManager.resumeMusic()

                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            Log.d("AdManager", "Reward earned!")
                            onRewardEarned()
                        } else {
                            Log.d("AdManager", " Reward NOT earned - ad was skipped or incomplete")
                        }
                    }

                    override fun onUnityAdsShowFailure(
                        placementId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String
                    ) {
                        Log.e("AdManager", "Rewarded ad show failed: $error - $message")
                        isLoadingRewarded = false
                        //resume on failure
                        MusicManager.resumeMusic()

                        onFailed()
                    }

                    override fun onUnityAdsShowStart(placementId: String) {
                        Log.d("AdManager", "Rewarded ad started")
                    }

                    override fun onUnityAdsShowClick(placementId: String) {
                        Log.d("AdManager", "Rewarded ad clicked")
                    }
                })
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                Log.e("AdManager", "Failed to load rewarded ad: $error - $message")
                isLoadingRewarded = false
                //resume on failure
                MusicManager.resumeMusic()

                onFailed()
            }
        })
    }

    //check if ads r ready
    fun isRewardedReady(): Boolean {
        return isInitialized && !isLoadingRewarded
    }

    fun isInterstitialReady(): Boolean {
        return isInitialized && isInterstitialPreloaded && !isLoadingInterstitial
    }

    fun isInitialized(): Boolean = isInitialized
}