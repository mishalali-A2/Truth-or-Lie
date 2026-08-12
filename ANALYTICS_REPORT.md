# Firebase Analytics (GA4) Instrumentation Report — Truth or Lie TV

Package: `com.futurewatch.truthorlietv` · Platform: native Kotlin Android TV (Leanback), no Flutter, no Compose UI.

## 1. App audit summary

Truth or Lie TV is a turn-based party trivia game for Android TV. The user flow is linear:
`Splash → Main → Categories → Rounds → PlayerCount → PlayerNames → Facts → Voting → Results → (loop Facts, or) FinalResults`,
with `Leaderboard`, `HowToPlay`, `Settings`, and `Purchase` as satellite screens reachable from `Main`/`Settings`/`Categories`.

- 14 Activities, all XML layouts, no RecyclerView/GridView (plain `LinearLayout`/`GridLayout` with
  individually created child views), no Room (raw `SQLiteOpenHelper` via `PlayerRepository`/`PlayerEntity`).
- Only `VotingActivity` overrides `onKeyDown` for direct D-pad handling (Truth/Lie/pause/lock); every
  other screen relies on default Android focus traversal + `OnClickListener` (fires identically for
  touch and D-pad-center).
- Unity Ads is the only ad SDK, wired in `AdManager.kt`, with exactly two reachable placements
  (`results_interstitial`, `category_unlock_rewarded`) — see §7.
- **Existing analytics before this work: none.** No Firebase, no other analytics SDK, no custom
  event logging of any kind existed anywhere in the codebase prior to this instrumentation pass.

## 2. New architecture

New package: `app/src/main/java/com/futurewatch/truthorlietv/analytics/`

| File | Responsibility |
|---|---|
| `AnalyticsEvents.kt` | `const val` event name catalogue, grouped by category. |
| `AnalyticsParams.kt` | `const val` parameter name catalogue. |
| `AnalyticsValidator.kt` | Pure Kotlin (no Android types) validation/sanitization of event names, param names, param values, per GA4 limits. Independently unit-testable. |
| `AnalyticsService.kt` | Singleton wrapper around `FirebaseAnalytics`. The ONLY file that touches the Firebase SDK directly. Every public method validates via `AnalyticsValidator`, catches all exceptions, never throws. |
| `ScreenTracker.kt` | Per-Activity `DefaultLifecycleObserver` for screen enter/exit + duration, registered via `ScreenTracker.attach(activity, screenName, previousScreen)` in each Activity's `onCreate`. Also defines `AnalyticsScreens` (stable screen-name constants). |
| `SessionTracker.kt` | App-level foreground/background/session tracking via `ProcessLifecycleOwner`, mirroring `MusicManager`'s existing lifecycle pattern. |
| `InputTracker.kt` | D-pad key-action throttling (`KeyThrottle`) and focus idle-gap aggregation (`FocusIdleAggregator`) for high-frequency grids/lists. |
| `AdAnalyticsTracker.kt` | Thin wrapper functions for every Unity Ads callback in `AdManager.kt`, with raw SDK error/message strings sanitized into a small controlled bucket set. |

`AnalyticsService.init(context)` and `SessionTracker.init()` are called once from
`TruthOrLieApplication.onCreate()`, immediately after `initializeDefaultSettings()` and before
`MusicManager.init(this)` — early enough that every downstream manager/activity can safely log
events for the rest of the process lifetime.

## 3. Event catalogue

All custom event names live in `AnalyticsEvents.kt`. Lifecycle/session events are deliberately
prefixed `app_` (`app_session_start`, `app_foreground`, `app_background`, `app_session_end`) to
avoid colliding with GA4's own auto-collected `session_start`.

### Lifecycle / session
| Event | Trigger | Params | Fires |
|---|---|---|---|
| `app_session_start` | First app foreground of the process | — | Once per process |
| `app_foreground` | Every app foreground transition | — | Per foreground |
| `app_background` | Every app background transition | `session_duration_ms` | Per background |
| `app_session_end` | Same as `app_background` | `session_duration_ms` | Per background |

### Screen
| Event | Trigger | Params |
|---|---|---|
| `screen_enter` | `ScreenTracker` `onStart`, first activation only per instance | `screen_name`, `previous_screen` |
| `screen_exit` | `ScreenTracker` `onStart`→`onStop`, or app backgrounded mid-screen | `screen_name`, `screen_duration_ms`, `exit_reason` (`navigation`\|`background`) |

Screens instrumented (all 14 activities): `splash`, `main`, `categories`, `rounds`, `player_count`,
`player_names`, `facts`, `voting`, `results`, `final_results`, `leaderboard`, `how_to_play`,
`settings`, `purchase`.

### Controls / focus / input
| Event | Trigger | Params |
|---|---|---|
| `control_click` | Any standalone button tap (Main menu, Rounds, pause overlay, Settings rows/chips, Purchase, unlock overlay, etc.) | `screen_name`, `control_id` |
| `focus_changed` | Standalone-control focus gained (buttons everywhere except the two high-frequency areas) | `screen_name`, `control_id` |
| `focus_settled` | Idle-gap-debounced settle event for CategoriesActivity's 15-card grid and LeaderboardActivity's row list | `screen_name`, `control_id` |
| `dpad_key_action` | VotingActivity D-pad key press (Truth/Lie select, pause, lock), throttled | `screen_name`, `control_id`, `input_action` |

### Game funnel
| Event | Trigger (Activity) | Params |
|---|---|---|
| `category_selected` | Category card tap, unlocked | `category_id` |
| `category_locked_viewed` | Category card tap, locked → overlay shown | `category_id` |
| `rounds_selected` | Round-count button tap | `round_count` |
| `player_count_selected` | "Continue" tap on PlayerCountActivity | `player_count` |
| `game_started` | PlayerNamesActivity valid submit | `category_id`, `round_count`, `player_count` (never names) |
| `round_started` | FactsActivity shows a new statement | `category_id`, `round_number` |
| `answer_submitted` | VotingActivity `lockAnswer()` | `category_id`, `round_number`, `is_correct`, `is_timeout`, `player_index` (never name) |
| `round_completed` | ResultsActivity shown | `category_id`, `round_number`, `round_count` |
| `game_completed` | FinalResultsActivity shown | `category_id`, `round_count`, `player_count`, `is_tie`, `score_bucket` |
| `game_abandoned` | VotingActivity pause-overlay "End Game" tap | `category_id`, `round_number`, `round_count`, `player_count` |

### Monetization
| Event | Trigger | Params |
|---|---|---|
| `category_unlock_ad_watched` | Rewarded-ad reward earned (`state==COMPLETED`) | `category_id` |
| `category_unlock_purchased` | `onPurchaseSuccess` for a `buy.*`/category product | `product_id` |
| `purchase_initiated` | Buy button tap (CategoriesActivity overlay, PurchaseActivity) | `product_id`, `category_id` (where applicable) |
| `purchase_succeeded` | `BillingManager.BillingListener.onPurchaseSuccess` | `product_id` |
| `purchase_failed` | `onPurchaseError` | `error_category` (billing response code bucket) |
| `purchase_canceled` | `onPurchaseCanceled` | — |
| `restore_purchases_requested` | Restore tap (Settings, PurchaseActivity) | — |
| `restore_purchases_completed` | `onRestoreCompleted` | `feature_outcome` (`has_premium`\|`no_premium`) |
| `billing_setup_finished` / `billing_disconnected` | Billing lifecycle callbacks | — |

`category_unlock_expired` is defined in the catalogue for the 24h-purchase-expiry gate in
`CategoryManager.isUnlocked()`; the expiry check there is a pure read-time computation with no
natural callback/event hook in the existing code (it silently clears the SharedPreferences flag
inline), so wiring it would require adding a new code path rather than an additive one-liner —
left as a documented gap, see §9.

### Settings
| Event | Trigger | Params |
|---|---|---|
| `music_toggled` | Music switch | `setting_value` (bool) |
| `music_genre_changed` | Genre chip tap | `setting_value` (genre string) |
| `timer_duration_changed` | Timer chip tap | `setting_value` (seconds) |
| `other_app_cross_promo_tapped` | Cross-promo chip tap | `app_id` (package name, never display name) |
| `rate_app_tapped` | Rate App row tap | — (app-side is currently a no-op Toast stub, not a gap in analytics) |
| `privacy_policy_tapped` / `terms_of_service_tapped` | Row tap | — (opens external browser; only the tap is observable) |

### Ads — see §7 for the full catalogue and funnels.

### Errors (sanitized categories only, never raw messages)
| Event | Source |
|---|---|
| `error_billing` | `BillingManager` purchase errors, `PurchaseActivity` launch-flow exceptions |
| `error_data_load` | `LeaderboardActivity` DB load failure, `FinalResultsActivity` score-save failure |
| `error_ad` | Defined for ad error paths; ad failures are logged via the dedicated `ad_load_failed`/`ad_show_failed` events instead (richer `ad_placement`/`ad_format` context), so this generic bucket is currently unused — kept for future non-ad-specific error routing. |

## 4. Parameter catalogue

Full source of truth: `AnalyticsParams.kt`. Highlights with cardinality/BigQuery notes:

| Param | Type | Cardinality | Custom dimension? |
|---|---|---|---|
| `screen_name` | string | 14 fixed values | Yes — high-value, low-cardinality |
| `control_id` | string | ~80 fixed values across the app | BigQuery-only (too many for a Console dimension, still fine at 40-char/event-scoped limits) |
| `category_id` | string | 15 fixed keys (internal enum-like, e.g. `science`, `history`) | Yes |
| `round_count` | int | {3,5,7,10,15} | Yes (as a metric or dimension) |
| `round_number` | int | 1..15 | BigQuery-only |
| `player_count` | int | 1..6 | Yes |
| `player_index` | int | 0..5 (turn ordinal, never a name) | BigQuery-only |
| `is_correct` / `is_timeout` / `is_tie` | bool (stored as string `"true"`/`"false"`) | 2 | BigQuery-only |
| `score_bucket` | string | 5 buckets (`0`,`1-200`,`201-500`,`501-1000`,`1000+`) | Yes |
| `product_id` | string | 16 fixed SKUs | Yes |
| `ad_placement` | string | 2 (`results_interstitial`, `category_unlock_rewarded`) | Yes |
| `ad_outcome` | string | Unity `UnityAdsShowCompletionState` values, lowercased | BigQuery-only |
| `error_category` | string | small controlled bucket set (see §7) | BigQuery-only |
| `device_category` | string (user property) | 1 value (`android_tv`) always | N/A — constant |
| `premium_status` | string (user property) | `has_premium`\|`free` | Yes |

## 5. Privacy exclusions (hard rules enforced throughout)

- **`Player.name`** (`gameSession.kt`) and **`PlayerEntity.name`** (SQLite `players` table) are
  NEVER sent to analytics, as an event param or a user property, anywhere in this instrumentation.
  Every event that needs to reference "which player" uses `player_index` (turn ordinal 0-based)
  instead — see `answer_submitted` in `VotingActivity.lockAnswer()`.
- Purchase token, order ID, and signature from `BillingClient.Purchase` are never read for
  analytics purposes — only `productId` is logged (`purchase_succeeded`, `purchase_initiated`,
  `category_unlock_purchased`).
- No raw exception messages or stack traces are sent; all error events use a small controlled
  `error_category` bucket string (see §7 for the ad-specific bucket list; billing/data-load errors
  use short fixed strings like `"purchase_flow_launch_failed"`, `"leaderboard_load_failed"`).
- `other_app_cross_promo_tapped` sends the cross-promo app's **package name** (`app_id`), never its
  display name string.

## 6. Screen-duration model

Implemented in `ScreenTracker.kt`, one instance per Activity via `ScreenTracker.attach(this, screenName, previousScreen)` called from `onCreate`.

- Registered as a `DefaultLifecycleObserver` on the Activity's own `lifecycle` — chosen over manual
  `onResume`/`onPause` overrides because it requires touching only `onCreate` (one line) in each of
  the 14 activities instead of duplicating lifecycle method overrides everywhere.
- **Duration** is measured with `SystemClock.elapsedRealtime()` (monotonic, immune to wall-clock/timezone changes), from the Activity's `onStart` to its `onStop`.
- **Dedup**: a re-entrant `onStart` (e.g. a transient system dialog causing the Activity to resume
  again without a real screen change) is a no-op if a tracking window is already active
  (`isActive` guard) — it does not restart the duration timer or re-log `screen_enter`.
  `screen_enter` itself is additionally latched (`hasLoggedEnter`) to fire at most once per
  Activity **instance**.
- **Backgrounding**: each `ScreenTracker` also registers a `ProcessLifecycleOwner` observer while
  active; if the whole app is backgrounded while that screen is on top, `screen_exit` is flushed
  with `exit_reason=background` and the correct elapsed duration (in addition to the Activity's own
  `onStop`, which fires anyway in that case — the dedup guard on `isActive` means only one
  `screen_exit` is ever logged regardless of which path triggers it first).
- **Activity relaunches are NOT coalesced.** `VotingActivity` relaunches itself as a new Activity
  instance per player turn, and `ResultsActivity`→`FactsActivity` loops per round. Each relaunch
  creates a fresh `ScreenTracker` and is treated as a legitimate, distinct screen view — this is
  architecturally correct for Android's Activity model and matches how GA4 screen tracking is
  meant to behave (each `screen_view` is a real navigation event).

## 7. D-pad / focus throttling model

Implemented in `InputTracker.kt`. Two independent strategies:

### 7.1 Key-action throttle (`InputTracker.KeyThrottle`, VotingActivity only)
- Window: **200ms**.
- An identical `(control_id, action)` pair repeated within 200ms of the previous identical pair is
  suppressed.
- Exceptions that always pass through regardless of timing: `select`, `lock`, `back`, `pause`,
  `resume` — the "final"/committing actions, which are inherently low-frequency and already
  rate-limited by `VotingActivity`'s own `isLocked`/`isSelectionAnimating` 300ms animation guards.
- A genuine control-to-control transition (e.g. Truth→Lie selection) always passes through even if
  it happens faster than the window, since it is not a *repeat* of the same event.
- 200ms was chosen deliberately shorter than the app's own 300ms selection-animation lock: in
  practice the app's existing state guards already prevent re-entrant key presses entirely, so this
  throttle almost never needs to fire — it exists as defense-in-depth against a future regression
  in those guards, not as the primary rate limiter.

### 7.2 Focus idle-gap aggregation (`InputTracker.FocusIdleAggregator`, two instances: CategoriesActivity's 15-card grid, LeaderboardActivity's unbounded row list)
- Window: **500ms** of inactivity after the last focus change.
- Each focus change resets a Handler-based debounce timer; only when 500ms elapses with no further
  focus change is a single `focus_settled` event emitted, for whichever control was focused last.
- This bounds emitted volume to at most one event per ~500ms of continuous D-pad panning through
  the grid/list, and exactly one event when the user stops on a card/row — instead of one event per
  transient focus step, which would otherwise flood analytics (15 cards, and an unbounded,
  ever-growing leaderboard row count).
- All other focusable controls in the app (standalone buttons, PlayerNamesActivity's ≤6 inputs) use
  the direct, non-aggregated `InputTracker.logFocusChanged()` — safe individually since they are
  low-frequency and bounded.

## 8. Ad placement catalogue (Unity Ads — the only ad SDK in this project)

**No AdMob, no mediation, no other ad SDK exists or was added.** Exactly two reachable placements:

| Placement (`ad_placement`) | Format | Unity placement ID | Shown from |
|---|---|---|---|
| `results_interstitial` | `interstitial` | `Interstitial_Android` | `FinalResultsActivity.onCreate`, unconditional if `AdManager.isInterstitialReady()` |
| `category_unlock_rewarded` | `rewarded` | `Rewarded_Android` | `CategoriesActivity` unlock overlay `btnWatchAd` |

Three "test" ad buttons exist in `SettingsActivity.addDebugPanel()` but that method's call site is
commented out (`//addDebugPanel()`) — dead, unreachable code. Not instrumented, per the brief.

### Ad lifecycle events (`AdAnalyticsTracker.kt`, wrapping every existing `AdManager.kt` callback)
`ad_init_succeeded` / `ad_init_failed` → `ad_load_requested` → `ad_load_succeeded` /
`ad_load_failed` → `ad_show_requested` → `ad_show_started` → `ad_show_clicked` (optional) →
`ad_show_completed` / `ad_show_failed`. Rewarded adds `ad_reward_earned` (state==`COMPLETED`) or
`ad_reward_skipped` (any other completion state) after `ad_show_completed`.

Distinct outcomes specific to this app's ad-serving logic, tracked separately from a normal failure:
- `ad_show_skipped_cooldown` — `results_interstitial` blocked by the 3-minute
  `INTERSTITIAL_COOLDOWN`; the request never reaches the Unity SDK at all. Also emitted (as the
  closest available bucket) when `FinalResultsActivity` finds `AdManager.isInterstitialReady()`
  false for any reason — `AdManager` does not expose *why* it wasn't ready at that call site
  (cooldown vs. not-yet-preloaded are internal booleans not surfaced to callers), so this bucket is
  used as the general "no show attempt reached Unity" signal. Documented as a known imprecision,
  not silently hidden.
- `ad_show_fallback_on_demand` — `results_interstitial` wasn't preloaded when cooldown/init checks
  passed, so `AdManager.showInterstitial()` falls back to an on-demand load-then-show path
  (`ad_preloaded=false`).

### Error sanitization (`AdAnalyticsTracker.sanitizeLoadError` / `sanitizeShowError`)
Unity's raw `UnityAdsLoadError`/`UnityAdsShowError` enum name + message string are mapped into a
small controlled bucket set for `error_category`, never passed through raw:
`no_fill`, `network`, `timeout`, `invalid_request`, `not_ready`, `internal`, `callback`, `unknown`.

### Ad revenue analytics — NOT AVAILABLE
The existing Unity Ads integration in `AdManager.kt` does not wire an
`IUnityAdsImpressionListener` or any other paid-event/impression-level callback. There is no
mediation layer and no data source for revenue (eCPM, ad value, currency) anywhere in the current
SDK integration. Implementing ad revenue analytics would require adding a new Unity Ads listener
type to `AdManager.kt`, which was explicitly out of scope ("only instrument the EXISTING Unity Ads
calls... do not change ad show/load/timeout logic itself"). **This section is not implemented, and
should not be inferred from any other event** — documented here explicitly rather than fabricated.

## 9. Gaps / not implemented, with reasons

- **Ad revenue analytics**: not possible without adding a new Unity Ads callback type — see §8.
- **`category_unlock_expired`**: event constant exists but has no wired call site. The 24h-unlock
  expiry check in `CategoryManager.isUnlocked()` is a pure read-time computation (checks elapsed
  hours, clears SharedPreferences inline) with no existing callback/observer pattern to hook
  additively — wiring it cleanly would mean adding a new code path, which conflicts with the
  "additive 1-3 line hooks only, no refactoring" constraint. Left unwired; flagged here rather than
  silently dropped.
- **Firebase Crashlytics**: intentionally not added, per the task's explicit scope (Analytics only).
- **`google-services.json`**: does not exist in this repo and was not fabricated. See §11.
- One minor imprecision in ad "not ready" attribution is documented inline in §8
  (`ad_show_skipped_cooldown` reused as a general "not ready" bucket at one call site).
- A few `AnalyticsEvents`/`AnalyticsParams` constants are defined but not currently wired to any
  call site (`DIALOG_SHOWN`, `DIALOG_DISMISSED`, `NAVIGATION`, `BACK_PRESSED`, and a handful of
  parameter constants such as `NAVIGATION_SOURCE`, `FEATURE_ID`, `LOAD_DURATION_MS`). They exist as
  reserved, validator-safe names for future use and are harmless unused constants, not dead code
  paths — flagged here for completeness.

### Post-implementation review fixes

A final review pass (naming/privacy compliance, event-volume/performance, unrelated-changes
preservation — three independent read-only passes) found no PII leaks, no naming violations, and no
unrelated behavior changes. It surfaced four small event-correctness issues, all fixed:

1. **`CategoriesActivity`/`LeaderboardActivity` focus aggregators never cancelled on pause.**
   `InputTracker.FocusIdleAggregator` already had a `cancel()` method, but neither Activity called
   it. If a user panned the grid/list and navigated away within the 500ms idle window, a stale
   `focus_settled` event could fire ~500ms later, misattributed to a screen the user had already
   left. Fixed by calling `gridFocusAggregator.cancel()` / `rowFocusAggregator.cancel()` in each
   Activity's `onPause()`.
2. **`VotingActivity` pause action could double-log on a fast double-tap.** `KEYCODE_DPAD_UP`
   logged `voting_pause` unconditionally before calling `pauseGame()`, and `"pause"` is in
   `InputTracker.ALWAYS_PASS_ACTIONS` (bypasses the 200ms key throttle by design, since it's a
   committing action). A double-tap within 200ms could log the event twice even though
   `pauseGame()` itself is idempotent (`if (isPaused) return`). Fixed by guarding the analytics
   call with the same `!isPaused` check.
3. **`PlayerCountActivity`'s +/- buttons have no throttle.** Reviewed and left as-is: each click
   fires one `control_click` event with no debounce, but the range is hard-bounded to 1–6 players,
   so worst-case volume is ~10-12 events per screen visit even with rapid mashing — self-limiting
   by the existing business logic, not worth adding throttling machinery for.
4. **`LeaderboardActivity.onResume()` rebuilds all rows on every resume**, not just first entry.
   Confirmed this is pre-existing app behavior (unrelated to the analytics addition) and does not
   cause duplicate `screen_enter`/`screen_exit` (those are `onStart`/`onStop`-driven, correctly
   deduped by `ScreenTracker`). Left unchanged — fixing it would be a non-additive behavior change
   to code the analytics task didn't introduce.

## 10. Tests

`app/src/test/java/com/futurewatch/truthorlietv/analytics/` — 4 files, 55 JUnit4 tests, pure JVM
(no Robolectric, no real Firebase/network calls):

- **`AnalyticsValidatorTest.kt`** (29 tests) — valid/invalid event names, reserved-prefix rejection
  (`firebase_`/`google_`/`ga_`), length limits (event name, param name, param value — including
  exact-boundary and over-boundary cases), null/blank param handling, 25-param-per-event cap,
  numeric/boolean pass-through, `sanitizeEventName` best-effort fallback behavior.
- **`AnalyticsServiceSafetyTest.kt`** (8 tests) — proves `AnalyticsService.logEvent` /
  `setUserProperty` / `setScreen` never throw, including with malformed names, oversized param
  maps, null/mixed-type values, and when called before `init()` (the exact state these pure-JVM
  tests run in, since no Firebase/Robolectric runtime is present).
- **`InputTrackerTest.kt`** (12 tests) — `KeyThrottle`: identical repeat within 200ms window
  suppressed, repeat after window passes emits again, genuine control-to-control transition never
  suppressed even when fast, `select`/`lock`/`back`/`pause`/`resume` always pass through.
  `FocusIdleLogic`: idle/not-idle boundary behavior for the 500ms settle window.
- **`ScreenTrackerDedupTest.kt`** (6 tests) — reproduces `ScreenTracker`'s exact
  `isActive`/`hasLoggedEnter` state-machine (extracted as a standalone fake since the real class
  touches `SystemClock`/`ProcessLifecycleOwner`, which throw under plain JVM tests without
  Robolectric): repeated `onStart` doesn't double-log enter, repeated `onStop` doesn't double-log
  exit, enter is a one-time latch per instance across multiple start/stop cycles.

A minimal, standard (non-Robolectric) config addition was needed:
`android.testOptions.unitTests.isReturnDefaultValues = true` in `app/build.gradle.kts`, so that
`android.util.Log` calls inside the code under test return silently instead of throwing
`RuntimeException: Method ... not mocked` — the standard, lightweight fix for this exact situation,
not a new test framework dependency.

## 11. Validation — exact commands and output

All commands run from the repo root, `./gradlew <task> --console=plain`, JDK 21 daemon.

### `:app:compileDebugKotlin`
The Google Services Gradle plugin's own task (`processDebugGoogleServices`) sits ahead of
`compileDebugKotlin` in the task graph and fails immediately (at task-execution time, not
configuration time) because `app/google-services.json` does not exist:

```
> Task :app:processDebugGoogleServices FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:processDebugGoogleServices'.
> File google-services.json is missing.
  The Google Services Plugin cannot function without it.
  Searched locations: .../app/src/debug/google-services.json, .../app/src/google-services.json,
  .../app/src/Debug/google-services.json, .../app/google-services.json

BUILD FAILED in 23s
```

**This is expected and will resolve the instant a real `google-services.json` is added** — it is
not a code problem. To independently verify the Kotlin source itself is correct (syntax/types),
the `google-services` plugin application was **temporarily** commented out in
`app/build.gradle.kts`, `:app:compileDebugKotlin` was run, and the plugin line was restored
immediately afterward (confirmed via diff against a pre-edit backup — the restored file is
byte-identical to the instrumented version except for the intentional `testOptions` addition made
in the same session). With the plugin temporarily disabled:

```
> Task :app:compileDebugKotlin
w: .../SettingsActivity.kt:449:59 'field scaledDensity: Float' is deprecated. Deprecated in Java.
w: .../VotingActivity.kt:189:52 'fun getColor(p0: Int): Int' is deprecated. Deprecated in Java.
w: .../VotingActivity.kt:191:52 'fun getColor(p0: Int): Int' is deprecated. Deprecated in Java.

BUILD SUCCESSFUL in 8s
```

Zero errors. The three warnings are pre-existing deprecations in code untouched by this work
(`scaledDensity`, `getColor(Int)`), unrelated to the analytics instrumentation. One real compile
error was caught and fixed during this process: `TruthOrLieApplication.kt` called
`initializeAnalyticsUserProperties()` but the method was named `setAnalyticsUserProperties()` —
corrected to match.

### `:app:testDebugUnitTest`
Also blocked by the same `processDebugGoogleServices` failure when the plugin is active (identical
error to above, task fails before any test runs). With the plugin temporarily disabled (same
verification method as above):

```
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 8s
```

Per-class results (from `app/build/test-results/testDebugUnitTest/*.xml`):

```
AnalyticsServiceSafetyTest:  tests="8"  failures="0"  errors="0"
AnalyticsValidatorTest:      tests="29" failures="0"  errors="0"
InputTrackerTest:            tests="12" failures="0"  errors="0"
ScreenTrackerDedupTest:      tests="6"  failures="0"  errors="0"
```

**55 tests total, 0 failures, 0 errors.** Two real issues were found and fixed by these tests
during development: (1) `AnalyticsService`'s `Log.d`/`Log.e` calls threw under plain JVM unit tests
without `unitTests.isReturnDefaultValues = true` — fixed via the build config addition in §10;
(2) one test's own expected value for `sanitizeEventName`'s trailing-underscore-trim behavior was
initially wrong — fixed the test, not the implementation, after confirming the implementation's
`trimEnd('_')` behavior was correct.

### `assembleDebug` / `build`
```
> Task :app:processDebugGoogleServices FAILED
> File google-services.json is missing. ...
BUILD FAILED in 1s
```
Identical, expected failure — confirmed with the google-services plugin left in its normal
(enabled) state, i.e. the real state of the repo as delivered.

## 12. BigQuery SQL examples

Placeholders: `PROJECT_ID.DATASET_ID.events_*`. Adapted to this app's actual turn-based-trivia
event set — no continuous-gameplay/frame-level SQL, since there is no such state in this app.

### Sessions
```sql
-- 1. Daily active users and session count
SELECT
  event_date,
  COUNT(DISTINCT user_pseudo_id) AS dau,
  COUNTIF(event_name = 'app_session_start') AS sessions_started
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE _TABLE_SUFFIX BETWEEN '20260101' AND '20261231'
GROUP BY event_date
ORDER BY event_date;

-- 2. Average session duration (ms) per day
SELECT
  event_date,
  AVG((SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'session_duration_ms')) AS avg_session_ms
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'app_session_end'
GROUP BY event_date;

-- 3. Sessions per user distribution
SELECT sessions_per_user, COUNT(*) AS user_count
FROM (
  SELECT user_pseudo_id, COUNTIF(event_name = 'app_session_start') AS sessions_per_user
  FROM `PROJECT_ID.DATASET_ID.events_*`
  GROUP BY user_pseudo_id
)
GROUP BY sessions_per_user
ORDER BY sessions_per_user;
```

### Screens
```sql
-- 4. Top screens by enter count
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'screen_name') AS screen_name,
  COUNT(*) AS enters
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'screen_enter'
GROUP BY screen_name
ORDER BY enters DESC;

-- 5. Average screen duration per screen
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'screen_name') AS screen_name,
  AVG((SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'screen_duration_ms')) AS avg_duration_ms
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'screen_exit'
GROUP BY screen_name
ORDER BY avg_duration_ms DESC;

-- 6. Screen exits by reason (navigation vs. app backgrounded mid-screen)
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'screen_name') AS screen_name,
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'exit_reason') AS exit_reason,
  COUNT(*) AS exits
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'screen_exit'
GROUP BY screen_name, exit_reason
ORDER BY screen_name, exits DESC;
```

### Navigation / controls
```sql
-- 7. Most-tapped controls app-wide
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'screen_name') AS screen_name,
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'control_id') AS control_id,
  COUNT(*) AS taps
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'control_click'
GROUP BY screen_name, control_id
ORDER BY taps DESC
LIMIT 50;

-- 8. Main menu button click-through distribution
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'control_id') AS control_id,
  COUNT(*) AS taps
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'control_click'
  AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'screen_name') = 'main'
GROUP BY control_id
ORDER BY taps DESC;
```

### D-pad / focus
```sql
-- 9. VotingActivity D-pad action breakdown
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'input_action') AS input_action,
  COUNT(*) AS occurrences
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'dpad_key_action'
GROUP BY input_action
ORDER BY occurrences DESC;

-- 10. Categories grid focus_settled distribution (which cards users linger on)
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'control_id') AS control_id,
  COUNT(*) AS settles
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'focus_settled'
  AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'screen_name') = 'categories'
GROUP BY control_id
ORDER BY settles DESC;

-- 11. Leaderboard row focus_settled — how far users typically scroll
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'control_id') AS row_control_id,
  COUNT(*) AS settles
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'focus_settled'
  AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'screen_name') = 'leaderboard'
GROUP BY row_control_id
ORDER BY settles DESC;
```

### Game funnel: game_started → game_completed
```sql
-- 12. Funnel counts per stage
SELECT event_name, COUNT(*) AS event_count
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name IN (
  'category_selected','rounds_selected','player_count_selected',
  'game_started','round_started','answer_submitted','round_completed',
  'game_completed','game_abandoned'
)
GROUP BY event_name
ORDER BY event_count DESC;

-- 13. Game completion rate (completed vs abandoned, per category)
SELECT
  category_id,
  COUNTIF(event_name = 'game_completed') AS completed,
  COUNTIF(event_name = 'game_abandoned') AS abandoned,
  SAFE_DIVIDE(COUNTIF(event_name = 'game_completed'),
              COUNTIF(event_name = 'game_completed') + COUNTIF(event_name = 'game_abandoned')) AS completion_rate
FROM (
  SELECT event_name,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'category_id') AS category_id
  FROM `PROJECT_ID.DATASET_ID.events_*`
  WHERE event_name IN ('game_completed', 'game_abandoned')
)
GROUP BY category_id
ORDER BY completion_rate DESC;

-- 14. Average answer correctness rate per category
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'category_id') AS category_id,
  AVG(CAST((SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'is_correct') AS BOOL)::INT64) AS correct_rate
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'answer_submitted'
GROUP BY category_id
ORDER BY correct_rate DESC;

-- 15. Timeout rate per round number (does the timer feel too short late-game?)
SELECT
  (SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'round_number') AS round_number,
  COUNTIF((SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'is_timeout') = 'true') AS timeouts,
  COUNT(*) AS total_answers
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'answer_submitted'
GROUP BY round_number
ORDER BY round_number;

-- 16. Round count popularity (which game length players pick most)
SELECT
  (SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'round_count') AS round_count,
  COUNT(*) AS selections
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'rounds_selected'
GROUP BY round_count
ORDER BY selections DESC;

-- 17. Player count distribution
SELECT
  (SELECT value.int_value FROM UNNEST(event_params) WHERE key = 'player_count') AS player_count,
  COUNT(*) AS selections
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'player_count_selected'
GROUP BY player_count
ORDER BY player_count;

-- 18. Tie rate and winner score bucket distribution
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'score_bucket') AS score_bucket,
  COUNTIF((SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'is_tie') = 'true') AS ties,
  COUNT(*) AS games
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'game_completed'
GROUP BY score_bucket
ORDER BY score_bucket;
```

### Category unlock funnel
```sql
-- 19. Locked-category-viewed → unlock method breakdown
SELECT
  category_id,
  COUNTIF(event_name = 'category_locked_viewed') AS viewed,
  COUNTIF(event_name = 'category_unlock_ad_watched') AS unlocked_via_ad,
  COUNTIF(event_name = 'category_unlock_purchased') AS unlocked_via_purchase
FROM (
  SELECT event_name,
    COALESCE(
      (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'category_id'),
      'unknown'
    ) AS category_id
  FROM `PROJECT_ID.DATASET_ID.events_*`
  WHERE event_name IN ('category_locked_viewed','category_unlock_ad_watched','category_unlock_purchased')
)
GROUP BY category_id
ORDER BY viewed DESC;

-- 20. Overall unlock conversion rate (any method) from locked-view
SELECT
  SAFE_DIVIDE(
    COUNTIF(event_name IN ('category_unlock_ad_watched','category_unlock_purchased')),
    COUNTIF(event_name = 'category_locked_viewed')
  ) AS unlock_conversion_rate
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name IN ('category_locked_viewed','category_unlock_ad_watched','category_unlock_purchased');
```

### Ad funnels (per placement: load → show → impression → complete)
```sql
-- 21. Full ad funnel per placement
SELECT
  ad_placement,
  COUNTIF(event_name = 'ad_load_requested') AS load_requested,
  COUNTIF(event_name = 'ad_load_succeeded') AS load_succeeded,
  COUNTIF(event_name = 'ad_load_failed') AS load_failed,
  COUNTIF(event_name = 'ad_show_requested') AS show_requested,
  COUNTIF(event_name = 'ad_show_started') AS show_started,
  COUNTIF(event_name = 'ad_show_completed') AS show_completed,
  COUNTIF(event_name = 'ad_show_failed') AS show_failed,
  COUNTIF(event_name = 'ad_show_skipped_cooldown') AS skipped_cooldown
FROM (
  SELECT event_name,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'ad_placement') AS ad_placement
  FROM `PROJECT_ID.DATASET_ID.events_*`
  WHERE event_name LIKE 'ad_%'
)
GROUP BY ad_placement;

-- 22. Rewarded ad reward-earned vs reward-skipped rate
SELECT
  COUNTIF(event_name = 'ad_reward_earned') AS earned,
  COUNTIF(event_name = 'ad_reward_skipped') AS skipped,
  SAFE_DIVIDE(COUNTIF(event_name = 'ad_reward_earned'),
              COUNTIF(event_name = 'ad_reward_earned') + COUNTIF(event_name = 'ad_reward_skipped')) AS earn_rate
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name IN ('ad_reward_earned', 'ad_reward_skipped');

-- 23. Ad load failure reasons (sanitized buckets) by placement
SELECT
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'ad_placement') AS ad_placement,
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'error_category') AS error_category,
  COUNT(*) AS occurrences
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name = 'ad_load_failed'
GROUP BY ad_placement, error_category
ORDER BY occurrences DESC;

-- 24. Interstitial cooldown-skip rate vs. successful shows
SELECT
  COUNTIF(event_name = 'ad_show_skipped_cooldown') AS skipped_cooldown,
  COUNTIF(event_name = 'ad_show_completed'
    AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'ad_placement') = 'results_interstitial'
  ) AS shown_completed
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name IN ('ad_show_skipped_cooldown', 'ad_show_completed');

-- 25. On-demand fallback frequency (preload miss rate) for the interstitial
SELECT
  event_date,
  COUNTIF(event_name = 'ad_show_fallback_on_demand') AS fallback_loads,
  COUNTIF(event_name = 'ad_show_requested') AS total_show_requests
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name IN ('ad_show_fallback_on_demand', 'ad_show_requested')
GROUP BY event_date
ORDER BY event_date;
```

### Purchase funnel
```sql
-- 26. Purchase funnel by product
SELECT
  product_id,
  COUNTIF(event_name = 'purchase_initiated') AS initiated,
  COUNTIF(event_name = 'purchase_succeeded') AS succeeded,
  COUNTIF(event_name = 'purchase_failed') AS failed,
  COUNTIF(event_name = 'purchase_canceled') AS canceled
FROM (
  SELECT event_name,
    (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'product_id') AS product_id
  FROM `PROJECT_ID.DATASET_ID.events_*`
  WHERE event_name IN ('purchase_initiated','purchase_succeeded','purchase_failed','purchase_canceled')
)
GROUP BY product_id
ORDER BY succeeded DESC;

-- 27. Restore-purchases usage and outcome
SELECT
  COUNTIF(event_name = 'restore_purchases_requested') AS requested,
  COUNTIF(event_name = 'restore_purchases_completed'
    AND (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'feature_outcome') = 'has_premium'
  ) AS restored_with_premium
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name IN ('restore_purchases_requested', 'restore_purchases_completed');
```

### Error rates
```sql
-- 28. Error events by category and source
SELECT
  event_name,
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'error_category') AS error_category,
  (SELECT value.string_value FROM UNNEST(event_params) WHERE key = 'error_source') AS error_source,
  COUNT(*) AS occurrences
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name IN ('error_billing', 'error_data_load', 'error_ad')
GROUP BY event_name, error_category, error_source
ORDER BY occurrences DESC;

-- 29. Daily error rate relative to sessions (rough health signal)
SELECT
  event_date,
  COUNTIF(event_name IN ('error_billing','error_data_load','error_ad')) AS errors,
  COUNTIF(event_name = 'app_session_start') AS sessions,
  SAFE_DIVIDE(
    COUNTIF(event_name IN ('error_billing','error_data_load','error_ad')),
    COUNTIF(event_name = 'app_session_start')
  ) AS errors_per_session
FROM `PROJECT_ID.DATASET_ID.events_*`
GROUP BY event_date
ORDER BY event_date;

-- 30. Settings engagement — which rows/chips get touched most
SELECT event_name, COUNT(*) AS occurrences
FROM `PROJECT_ID.DATASET_ID.events_*`
WHERE event_name IN (
  'music_toggled','music_genre_changed','timer_duration_changed',
  'other_app_cross_promo_tapped','rate_app_tapped',
  'privacy_policy_tapped','terms_of_service_tapped'
)
GROUP BY event_name
ORDER BY occurrences DESC;
```

## 13. Recommendations

**Firebase Console custom dimensions (curated, not everything):**
`screen_name`, `category_id`, `round_count`, `player_count`, `product_id`, `ad_placement`,
`score_bucket`, `premium_status` (user property).

**Recommended custom metrics:** `screen_duration_ms` (average), `session_duration_ms` (average),
`round_number` (as a numeric metric for round-depth analysis).

**Params that should stay BigQuery-only** (too high-cardinality or too niche for a Console
dimension slot, but valuable in SQL): `control_id`, `player_index`, `error_category`,
`error_source`, `ad_outcome`, `previous_screen`, `input_action`, `exit_reason`.

## 14. Remaining manual steps (required before this builds)

1. Create a Firebase project for `com.futurewatch.truthorlietv` in the Firebase Console.
2. Register the Android app with that exact package name (and both debug/release signing
   fingerprints if using Dynamic Links/App Check later, though not required for Analytics alone).
3. Download the generated `google-services.json`.
4. Place it at `app/google-services.json` (repo root of the `app` module — NOT the project root).
5. Re-run `./gradlew build` — the `processDebugGoogleServices` failure documented in §11 will
   resolve immediately, and `compileDebugKotlin`/`testDebugUnitTest`/`assembleDebug` will all
   proceed normally (already verified independently in §11 that the Kotlin source itself compiles
   cleanly and all 55 tests pass).
