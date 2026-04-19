plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.futurewatch.truthorlietv"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.futurewatch.truthorlietv"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        //infatica
        manifestPlaceholders += mapOf(
            "INFATICA_PARTNER_ID" to "FutureWatch"
        )

    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = false
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation("com.google.android.material:material:1.13.0")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation ("androidx.datastore:datastore-preferences:1.1.1")
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.unity3d.ads:unity-ads:4.9.2")
    implementation("nl.dionsegijn:konfetti-xml:2.0.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}