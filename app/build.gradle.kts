import java.util.Properties

plugins {
    id("lux.android.application")
    id("lux.android.compose")
    id("lux.android.hilt")
}
val versionProperties =
    Properties().apply {
        rootProject.file("version.properties").inputStream().use { load(it) }
    }
val target = LuxVersion.parse(versionProperties.getProperty("stableVersion"))

android {
    namespace = "cn.lux.music.android"
    defaultConfig {
        applicationId = "cn.lux.music.android"
        versionCode = target.code(1)
        versionName = "$target-bootstrap"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release { isMinifyEnabled = false }
    }
}
dependencies {
    implementation(project(":feature"))
    implementation(project(":data"))
    implementation(project(":playback"))
    implementation(project(":source-runtime"))
    implementation(libs.activity.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}

// Signing and public release allocation are intentionally not enabled in stage 1.
tasks.matching { it.name in setOf("assembleRelease", "bundleRelease", "packageRelease") }.configureEach {
    doFirst { error("Public release is not enabled; follow docs/RELEASE-FLOW.md") }
}
