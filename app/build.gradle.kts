import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val signingPropertiesFile = rootProject.file("keystore.properties")
val signingProperties = Properties().apply {
    if (signingPropertiesFile.exists()) {
        signingPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.sipun.sonora"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.sipun.sonora"
        minSdk = 29
        targetSdk = 37
        versionCode = 300
        versionName = "3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            val keystorePath = signingProperties.getProperty("storeFile")
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv("ANDROID_KEYSTORE_FILE")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = rootProject.file(keystorePath)
            }

            storePassword = signingProperties.getProperty("storePassword")
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = signingProperties.getProperty("keyAlias")
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = signingProperties.getProperty("keyPassword")
                ?.takeIf { it.isNotBlank() }
                ?: System.getenv("ANDROID_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {}
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.coil.compose)
    implementation(libs.lottie.compose)
    implementation(libs.jaudiotagger)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
