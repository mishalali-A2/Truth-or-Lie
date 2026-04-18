package com.futurewatch.truthorlietv

import android.app.Application
import android.content.SharedPreferences
import com.unity3d.ads.UnityAds
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener

class TruthOrLieApplication : Application() {

    companion object {
        lateinit var instance: TruthOrLieApplication
            private set
        lateinit var prefs: SharedPreferences
    }

    override fun onCreate() {
        super.onCreate()
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
    }

    override fun onTerminate() {
        super.onTerminate()
        MusicManager.onAppDestroy()
    }
}