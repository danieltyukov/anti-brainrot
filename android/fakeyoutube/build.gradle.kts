// Test-only stand-in for the YouTube app: same package name, a "Shorts"
// screen whose root view carries the id the detector looks for. Never
// published; used to exercise the accessibility service on emulators where
// the real app cannot run.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "app.revanced.android.youtube"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.revanced.android.youtube"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "stand-in"
    }
    buildTypes {
        debug { }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
