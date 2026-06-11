# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic type signatures (required for Kotlin reflection, coroutines, DataStore)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod

# Google Play Billing
-keep class com.android.billingclient.** { *; }

# Unity Ads
-keep class com.unity3d.ads.** { *; }
-keep interface com.unity3d.ads.** { *; }

# Konfetti
-keep class nl.dionsegijn.konfetti.** { *; }

# App data classes used with JSON / SQLite
-keep class com.futurewatch.truthorlietv.database.** { *; }
-keep class com.futurewatch.truthorlietv.Player { *; }

# Keep enum names (used in billing response comparisons)
-keepclassmembers enum * { *; }

# Lifecycle (ProcessLifecycleOwner + anonymous DefaultLifecycleObserver subclasses in app code)
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }
-keep class * implements androidx.lifecycle.DefaultLifecycleObserver { *; }
-keep class * implements androidx.lifecycle.LifecycleObserver { *; }

# Keep app classes (Application, Activities, anonymous listeners)
-keep class com.futurewatch.truthorlietv.** { *; }

# Kotlin coroutines — R8 full mode strips coroutine internals without these
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# DataStore Preferences
-keep class androidx.datastore.** { *; }
-keep class androidx.datastore.preferences.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-dontwarn androidx.datastore.**

# Kotlin serialization / reflection used by coroutines
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**

# Keep font resources referenced from themes
-keep class androidx.core.content.res.** { *; }
