package com.futurewatch.truthorlietv.analytics

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Verifies [AnalyticsService]'s core safety contract: it must never throw, even when called
 * before [AnalyticsService.init] (i.e. with no FirebaseAnalytics instance backing it — exactly
 * the state it's in in this pure-JVM test environment, since Robolectric/a real Android runtime
 * is intentionally not used here). This exercises the same "swallow everything" code path that
 * protects the app if Firebase itself ever throws internally.
 *
 * Every call below would throw if the try/catch + null-guard contract in AnalyticsService were
 * removed — the test's only assertion is that these calls simply return without throwing.
 */
class AnalyticsServiceSafetyTest {

    @Test
    fun `logEvent before init does not throw`() {
        AnalyticsService.logEvent("some_event", mapOf("k" to "v"))
        // no exception means the safety contract held
    }

    @Test
    fun `logEvent with malformed name does not throw`() {
        AnalyticsService.logEvent("firebase_reserved_name", mapOf("k" to "v"))
        AnalyticsService.logEvent("", emptyMap())
        AnalyticsService.logEvent("!!!invalid###", emptyMap())
    }

    @Test
    fun `logEvent with oversized params does not throw`() {
        val manyParams = (1..100).associate { "param_$it" to "value_$it" }
        AnalyticsService.logEvent("large_event", manyParams)
    }

    @Test
    fun `logEvent with null and mixed-type param values does not throw`() {
        AnalyticsService.logEvent(
            "mixed_event",
            mapOf(
                "a_string" to "value",
                "a_null" to null,
                "a_bool" to true,
                "an_int" to 5,
                "a_double" to 1.5
            )
        )
    }

    @Test
    fun `setUserProperty before init does not throw`() {
        AnalyticsService.setUserProperty("device_category", "android_tv")
    }

    @Test
    fun `setUserProperty with invalid name does not throw`() {
        AnalyticsService.setUserProperty("firebase_reserved", "value")
        AnalyticsService.setUserProperty("", "value")
    }

    @Test
    fun `setScreen before init does not throw`() {
        AnalyticsService.setScreen("main", "MainActivity")
    }

    @Test
    fun `isInitializedForTest reflects uninitialized state in this JVM test environment`() {
        // In a pure JVM test with no Android runtime, FirebaseAnalytics.getInstance() is never
        // reachable, so AnalyticsService.init() is never called by these tests — confirming the
        // "safe to call before init" contract is genuinely being exercised above, not skipped.
        assertFalse(AnalyticsService.isInitializedForTest())
    }
}
