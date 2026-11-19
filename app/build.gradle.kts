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

val releaseSlot = providers.gradleProperty("luxReleaseSlot").orNull?.toInt()

android {
    namespace = "cn.lux.music.android"
    androidResources {
        // The upstream AAR includes its own development samples; do not ship them in Lux.
        ignoreAssetsPattern =
            listOf(
                ignoreAssetsPattern.orEmpty(),
                "test_assert_define.js",
                "test_base_module1.mjs",
                "test_base_module2.mjs",
                "test_module_import_dynamic.js",
                "test_polyfill_date.js",
            ).joinToString(":")
    }
    defaultConfig {
        applicationId = "cn.lux.music.android"
        versionCode = target.code(releaseSlot ?: 1)
        versionName =
            when (releaseSlot) {
                null -> "$target-bootstrap"
                999 -> target.toString()
                else -> "$target-dev.$releaseSlot"
            }
    }
    if (releaseSlot != null) {
        packaging.jniLibs.excludes += "lib/x86_64/**"
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

// The release job signs the verified build-tools output outside Gradle.
tasks.matching { it.name in setOf("assembleRelease", "bundleRelease", "packageRelease") }.configureEach {
    doFirst { requireNotNull(releaseSlot) { "Specify luxReleaseSlot for a release build" } }
}
