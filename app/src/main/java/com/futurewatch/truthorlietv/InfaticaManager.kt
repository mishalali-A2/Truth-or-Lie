package com.futurewatch.truthorlietv

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.infatica.agent.service.Service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object InfaticaManager {
    private const val TAG = "InfaticaManager"
    private const val PREFS_NAME = "infatica_prefs"
    private const val KEY_SDK_ENABLED = "sdk_enabled"
    private const val KEY_SDK_ID = "sdk_id"
    private const val KEY_CONSENT_GIVEN = "infatica_consent_given"
    private const val PARTNER_ID = "FutureWatch"

    private const val NOTIFICATION_CHANNEL_ID = "infatica_service_channel"
    private const val NOTIFICATION_ID = 1001

    private var sdkId: String = ""
    private var isConnected = false
    private var notificationManager: NotificationManager? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var isStarting = false

    // Service connection callback
    private val connection = object : Service.Companion.Connection() {
        override fun onServiceConnected(binding: Service.Companion.Binding) {
            coroutineScope.launch {
                try {
                    sdkId = binding.getId()
                    isConnected = true
                    isStarting = false
                    saveSdkId(sdkId)
                    Log.d(TAG, "✅ Infatica SDK connected successfully")
                    Log.d(TAG, "SDK ID: $sdkId")
                    Log.d(TAG, "Partner ID: $PARTNER_ID")
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting SDK ID: ${e.message}")
                    isConnected = false
                    isStarting = false
                }
            }
        }

        override fun onServiceDisconnected() {
            coroutineScope.launch {
                Log.d(TAG, "Infatica service disconnected")
                isConnected = false
                isStarting = false
            }
        }
    }

    fun hasNetworkConnection(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities != null && (
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        )
            } else {
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo != null && networkInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network: ${e.message}")
            false
        }
    }

    fun hasConsent(context: Context): Boolean {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_CONSENT_GIVEN, false)
        } catch (e: Exception) {
            false
        }
    }

    fun saveConsent(context: Context, granted: Boolean) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_CONSENT_GIVEN, granted)
                .apply()
            Log.d(TAG, "Consent saved: $granted")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving consent: ${e.message}")
        }
    }

    fun isEnabled(context: Context): Boolean {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SDK_ENABLED, false)
        } catch (e: Exception) {
            false
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        Log.d(TAG, "setEnabled called with enabled=$enabled")

        if (isStarting) {
            Log.d(TAG, "Already starting, ignoring request")
            return
        }

        try {
            val appContext = context.applicationContext

            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SDK_ENABLED, enabled)
                .apply()

            if (enabled) {
                start(context)
            } else {
                stop(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in setEnabled: ${e.message}")
        }
    }

    private fun canShowNotificationPermission(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Infatica Background Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Enables P2B network resource sharing when you opt-in"
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                }
                notificationManager?.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channel: ${e.message}")
        }
    }

    private fun showForegroundNotification(context: Context) {
        try {
            if (!canShowNotificationPermission(context)) {
                Log.w(TAG, "Notification permission not granted")
                return
            }

            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Network Sharing")
                .setContentText("Supporting the app - Partner: $PARTNER_ID")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setSound(null)
                .setVibrate(null)
                .setDefaults(0)
                .setOngoing(true)
                .build()

            Service.startForeground(context, NOTIFICATION_ID, notification, PARTNER_ID)
            Log.d(TAG, "✅ Foreground service started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground notification: ${e.message}")
        }
    }

    private fun hideForegroundNotification() {
        try {
            notificationManager?.cancel(NOTIFICATION_ID)
            Log.d(TAG, "Foreground notification hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding notification: ${e.message}")
        }
    }

    fun start(context: Context) {
        if (isStarting) {
            Log.d(TAG, "Already starting, skipping")
            return
        }

        Log.d(TAG, "Starting Infatica SDK...")
        isStarting = true

        try {
            val appContext = context.applicationContext

            // Check consent
            if (!hasConsent(context)) {
                Log.w(TAG, "User consent not given")
                isStarting = false
                return
            }

            // Check network connectivity
            if (!hasNetworkConnection(context)) {
                Log.w(TAG, "No network connection")
                isStarting = false
                return
            }

            if (isConnected) {
                Log.d(TAG, "Service already connected")
                isStarting = false
                return
            }

            createNotificationChannel(appContext)
            showForegroundNotification(appContext)
            Service.bind(appContext, connection, PARTNER_ID)
            Log.d(TAG, "✅ Service.bind() called")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Infatica: ${e.message}")
            isStarting = false
            isConnected = false
        }
    }

    fun stop(context: Context) {
        Log.d(TAG, "Stopping Infatica SDK...")
        isStarting = false

        try {
            val appContext = context.applicationContext

            if (!isConnected && sdkId.isEmpty()) {
                Log.d(TAG, "Infatica not connected")
                return
            }

            Service.stop(appContext)
            Service.unbind(appContext, connection)
        } catch (e: Exception) {
            Log.e(TAG, "Error during stop: ${e.message}")
        } finally {
            hideForegroundNotification()
            sdkId = ""
            isConnected = false
            Log.d(TAG, "Infatica stopped")
        }
    }

    fun isServiceRunning(): Boolean = isConnected

    fun getSdkId(): String = sdkId

    private fun saveSdkId(id: String) {
        try {
            val context = TruthOrLieApplication.instance
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SDK_ID, id)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving SDK ID: ${e.message}")
        }
    }
}