plugins {
    id("lux.android.library")
    id("lux.android.compose")
}
android { namespace = "cn.lux.music.designsystem" }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
}
