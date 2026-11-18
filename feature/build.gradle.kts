plugins {
    id("lux.android.library")
    id("lux.android.compose")
}
android { namespace = "cn.lux.music.feature" }
dependencies {
    implementation(project(":core:api"))
    implementation(project(":core:designsystem"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
}
