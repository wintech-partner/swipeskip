plugins { alias(libs.plugins.kotlin.jvm) }

dependencies {
    api(project(":billing:api"))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
