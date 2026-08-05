package com.futurewatch.truthorlietv.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests the screen-enter/exit dedup STATE MACHINE that [ScreenTracker] implements, without
 * exercising [ScreenTracker] itself. [ScreenTracker] calls `android.os.SystemClock.elapsedRealtime()`
 * and posts through `androidx.lifecycle.ProcessLifecycleOwner`, both of which require a real (or
 * Robolectric-shadowed) Android runtime to avoid "Method ... not mocked" failures under the
 * plain-JVM unit test setup used in this project (no Robolectric dependency was added, per the
 * project's "avoid heavy new test deps" constraint).
 *
 * This test instead validates the same dedup RULES as a standalone, Android-free reproduction:
 *  - onStart while already active does not start a second duration timer or log a second enter.
 *  - onStop while not active is a no-op (no double exit).
 *  - a fresh onStart after a completed onStop starts tracking again and logs enter exactly once
 *    for that new active window, but the SCREEN_ENTER event itself only fires the very first
 *    time (hasLoggedEnter latch) — mirroring ScreenTracker's actual field semantics exactly.
 *
 * If ScreenTracker's dedup fields (isActive / hasLoggedEnter) are ever refactored, this test's
 * FakeScreenDedupState should be updated in lockstep so it keeps testing the real contract.
 */
class ScreenTrackerDedupTest {

    /** Mirrors ScreenTracker's exact dedup fields and onStart/onStop guard logic. */
    private class FakeScreenDedupState {
        var isActive = false
            private set
        var hasLoggedEnter = false
            private set
        var enterLogCount = 0
            private set
        var exitLogCount = 0
            private set

        fun onStart() {
            if (isActive) return // dedupe: already tracking, ignore a spurious re-entrant onStart
            isActive = true
            if (!hasLoggedEnter) {
                hasLoggedEnter = true
                enterLogCount++
            }
        }

        fun onStop() {
            if (!isActive) return
            isActive = false
            exitLogCount++
        }
    }

    @Test
    fun `single onStart logs exactly one enter event`() {
        val state = FakeScreenDedupState()
        state.onStart()
        assertEquals(1, state.enterLogCount)
    }

    @Test
    fun `repeated onStart without intervening onStop does not double-log enter`() {
        val state = FakeScreenDedupState()
        state.onStart()
        state.onStart() // e.g. a transient dialog re-resuming the Activity
        state.onStart()
        assertEquals(1, state.enterLogCount)
    }

    @Test
    fun `onStop without a prior onStart is a no-op and does not log exit`() {
        val state = FakeScreenDedupState()
        state.onStop()
        assertEquals(0, state.exitLogCount)
    }

    @Test
    fun `onStop after onStart logs exactly one exit`() {
        val state = FakeScreenDedupState()
        state.onStart()
        state.onStop()
        assertEquals(1, state.exitLogCount)
    }

    @Test
    fun `repeated onStop without an intervening onStart does not double-log exit`() {
        val state = FakeScreenDedupState()
        state.onStart()
        state.onStop()
        state.onStop() // e.g. both the Activity's own onPause and the app-background observer firing
        assertEquals(1, state.exitLogCount)
    }

    @Test
    fun `enter is only logged once per ScreenTracker instance even across multiple start-stop cycles`() {
        // Mirrors ScreenTracker's actual semantics: hasLoggedEnter is a one-time latch for the
        // life of one ScreenTracker instance (i.e. one Activity instance) — NOT reset per cycle.
        val state = FakeScreenDedupState()
        state.onStart()
        state.onStop()
        state.onStart()
        state.onStop()
        assertEquals(1, state.enterLogCount)
        assertEquals(2, state.exitLogCount)
    }
}
