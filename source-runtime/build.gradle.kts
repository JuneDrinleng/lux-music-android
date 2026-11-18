plugins { id("lux.android.library") }
android {
    namespace = "cn.lux.music.sources"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}
dependencies {
    implementation(project(":core:api"))
    implementation(libs.quickjs.android)
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.junit)
}
