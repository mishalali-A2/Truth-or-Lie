plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.futurewatch.truthorlietv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.futurewatch.truthorlietv"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "1.8"

    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(System.getenv("KEYSTORE_PATH") ?: "release-keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "fwQalandarr"
            keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "fwQalandarr"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            versionNameSuffix = "-DEBUG"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
        buildConfig = true
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.unity3d.ads:unity-ads:4.9.2")
    implementation("nl.dionsegijn:konfetti-xml:2.0.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}