plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.subburn.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.subburn.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // FFmpegKit ships large native libs; keep only the common ABIs.
        // x86_64 keeps emulators and ChromeOS working; x86 is dropped as dead weight.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

    // Release signing comes from env vars so CI can sign with a secret keystore;
    // without them the release build falls back to the debug signing config.
    val keystorePath = System.getenv("SUBBURN_KEYSTORE")
    signingConfigs {
        if (!keystorePath.isNullOrBlank()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SUBBURN_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SUBBURN_KEY_ALIAS")
                keyPassword = System.getenv("SUBBURN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // Per-ABI APKs (~35 MB each) plus one universal APK, since the bundled
    // FFmpeg native libs dominate the download size.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildFeatures { compose = true }

    lint {
        abortOnError = true
        checkDependencies = true
        // Dependency-upgrade nags would break CI on a schedule nobody controls.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        jniLibs.useLegacyPackaging = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.11.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // FFmpeg 8.1.1 full-gpl: libass (the `subtitles` filter) + libx265.
    // The original com.arthenica:ffmpeg-kit-* AARs were pulled from Maven
    // Central; this is the maintained republished build (same API, package
    // renamed to com.antonkarpenko.ffmpegkit). minSdk of the AAR is 24.
    implementation("com.antonkarpenko:ffmpeg-kit-full-gpl:2.2.1")
}
