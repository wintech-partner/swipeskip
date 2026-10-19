plugins { alias(libs.plugins.kotlin.jvm) }

dependencies {
    api(project(":core:model"))
    api(project(":core:platform-api"))
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
