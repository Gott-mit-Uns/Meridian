import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Machine-local signing configuration keeps preview upgrades compatible across shells.
val localSettings=Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader(Charsets.UTF_8)?.use { load(it) }
}

android {
    namespace = "app.meridian.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.meridian.mobile"
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "app.meridian.mobile.VisualInspection"
        versionCode = 53
        versionName = "0.53.0"
    }

    signingConfigs.getByName("debug") {
        localSettings.getProperty("meridian.debugKeystore")?.let { storeFile=rootProject.file(it) }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
        }
        create("performance") {
            initWith(getByName("debug"))
            isDebuggable = false
            isJniDebuggable = false
            matchingFallbacks += listOf("debug")
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isDebuggable = false
            isJniDebuggable = false
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    androidResources { ignoreAssetsPattern = "vehicles:models" }
    splits { abi { isEnable = true; reset(); include("arm64-v8a", "armeabi-v7a"); isUniversalApk = true } }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("dev.chrisbanes.haze:haze:1.5.4")
    implementation("io.agora.rtc:full-rtc-basic:4.5.2")
    implementation("io.agora.rtc:full-video-codec-dec:4.5.2")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tasklist:4.6.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
