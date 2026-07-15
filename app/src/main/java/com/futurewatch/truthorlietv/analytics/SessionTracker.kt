package com.futurewatch.truthorlietv.analytics

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * App-level foreground/background/session tracker. Hooks [ProcessLifecycleOwner] exactly like
 * [com.futurewatch.truthorlietv.MusicManager.init] already does for music — this reuses the same
 * established lifecycle-observation pattern rather than introducing a new mechanism.
 *
 * Emits:
 *  - [AnalyticsEvents.APP_SESSION_START] once per process, the first time the app enters the
 *    foreground (custom event, deliberately NOT named "session_start" — GA4 auto-logs its own
 *    `session_start` event and colliding with that name would corrupt Firebase's built-in
 *    session reporting).
 *  - [AnalyticsEvents.APP_FOREGROUND] every time the app returns to the foreground (including the
 *    first time).
 *  - [AnalyticsEvents.APP_BACKGROUND] / [AnalyticsEvents.APP_SESSION_END] every time the app is
 *    backgrounded, with session_duration_ms measured from the most recent foreground transition.
 *
 * Duplicate-prevention: [onStart]/[onStop] pairs from ProcessLifecycleOwner are already
 * deduplicated by the platform (they only fire on true foreground/background transitions, not on
 * every Activity's own onResume/onPause), but this class additionally guards with [isForeground]
 * so a spurious double-call can never emit two consecutive foreground or background events.
 */
object SessionTracker {

    private var isForeground = false
    private var hasStartedSession = false
    private var foregroundTimeMs = 0L
    private var initialized = false

    fun init() {
        if (initialized) return
        initialized = true

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (isForeground) return
                isForeground = true
                foregroundTimeMs = SystemClock.elapsedRealtime()

                if (!hasStartedSession) {
                    hasStartedSession = true
                    AnalyticsService.logEvent(AnalyticsEvents.APP_SESSION_START)
                }
                AnalyticsService.logEvent(AnalyticsEvents.APP_FOREGROUND)
            }

            override fun onStop(owner: LifecycleOwner) {
                if (!isForeground) return
                isForeground = false
                val durationMs = SystemClock.elapsedRealtime() - foregroundTimeMs

                AnalyticsService.logEvent(
                    AnalyticsEvents.APP_BACKGROUND,
                    mapOf(AnalyticsParams.SESSION_DURATION_MS to durationMs)
                )
                AnalyticsService.logEvent(
                    AnalyticsEvents.APP_SESSION_END,
                    mapOf(AnalyticsParams.SESSION_DURATION_MS to durationMs)
                )
            }
        })
    }
}
