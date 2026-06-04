# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# Unity Ads
-keep class com.unity3d.ads.** { *; }
-keep interface com.unity3d.ads.** { *; }

# Konfetti
-keep class nl.dionsegijn.konfetti.** { *; }

# App data classes used with JSON / SQLite
-keep class com.futurewatch.truthorlietv.database.PlayerEntity { *; }
-keep class com.futurewatch.truthorlietv.Player { *; }

# Keep enum names (used in billing response comparisons)
-keepclassmembers enum * { *; }

# Lifecycle (ProcessLifecycleOwner used in Application)
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }

# Keep app classes (Application, Activities, anonymous listeners)
-keep class com.futurewatch.truthorlietv.** { *; }

# Keep font resources referenced from themes
-keep class androidx.core.content.res.** { *; }
