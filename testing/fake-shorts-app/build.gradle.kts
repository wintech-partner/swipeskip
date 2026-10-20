// Test-only app that mimics a vertical Shorts pager with an ad state and a Skip button.
// It lets the accessibility service be exercised on an emulator without touching YouTube or any real ad.
plugins { alias(libs.plugins.android.application) }

android {
    namespace = "io.github.wintech_partner.swipeskip.fakeshorts"
    compileSdk = 37
    defaultConfig {
        applicationId = "io.github.wintech_partner.swipeskip.fakeshorts"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
