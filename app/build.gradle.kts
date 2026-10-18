plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.drivelite.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.drivelite.launcher"
        minSdk = 21          // Android 5+ : many head units report a fake (newer) Android version
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"
    }

    // Fixed key committed to the repo so every CI build has the SAME signature
    // (updates install over each other). For personal/sideload use only — not for Play Store.
    signingConfigs {
        create("drivelite") {
            storeFile = rootProject.file("keystore/drivelite.jks")
            storePassword = "drivelite"
            keyAlias = "drivelite"
            keyPassword = "drivelite"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("drivelite")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("drivelite")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = false      // report is uploaded by CI; review it, don't block the APK
        checkReleaseBuilds = false
        htmlReport = true
    }
}

// No AndroidX / no UI libraries on purpose: pure framework Views keep the APK tiny and fast.
dependencies {
    testImplementation("junit:junit:4.13.2")
}
