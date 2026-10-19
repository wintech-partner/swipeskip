plugins { alias(libs.plugins.android.library) }

android {
    namespace = "org.example.autoshorts.billing.play"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":billing:api"))
    implementation(libs.play.billing.ktx)
}
