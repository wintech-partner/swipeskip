plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.wintech_partner.swipeskip"
    compileSdk = 37

    defaultConfig {
        // Placeholder id: a release blocker, see docs/OPEN_ITEMS.md.
        applicationId = "io.github.wintech_partner.swipeskip"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("foss") {
            dimension = "distribution"
            // foss: no billing code, no network permission, auto timer allowed.
            buildConfigField("boolean", "AUTO_TIMER_ENABLED", "true")
            buildConfigField("String", "PREMIUM_PRODUCT_ID", "\"\"")
        }
        create("play") {
            dimension = "distribution"
            applicationIdSuffix = ""
            // play: manual tap only until the human decides whether AUTO_TIMER can be submitted.
            buildConfigField("boolean", "AUTO_TIMER_ENABLED", "false")
            // Blank keeps the store disabled. Supply the real id via -PpremiumProductId=... when it exists.
            buildConfigField(
                "String", "PREMIUM_PRODUCT_ID",
                "\"${providers.gradleProperty("premiumProductId").orElse("").get()}\"",
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signing config is injected by CI from secrets; nothing is stored in the repository.
            // AGP otherwise embeds the git revision (or NO_VALID_GIT_FOUND) in META-INF, so the
            // APK differs between checkouts and F-Droid's reproducible-build check fails.
            vcsInfo.include = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        // A missing translation is a build failure, not a warning.
        error += "MissingTranslation"
        warningsAsErrors = false
        abortOnError = true
    }

    androidResources {
        generateLocaleConfig = false
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:automation"))
    implementation(project(":core:platform-api"))
    implementation(project(":platform:youtube"))
    implementation(project(":data:settings"))
    implementation(project(":billing:api"))
    "fossImplementation"(project(":billing:noop"))
    "playImplementation"(project(":billing:play"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.uiautomator)
}
