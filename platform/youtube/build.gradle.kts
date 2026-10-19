plugins { alias(libs.plugins.kotlin.jvm) }

dependencies {
    api(project(":core:platform-api"))
    testImplementation(libs.junit)
}
