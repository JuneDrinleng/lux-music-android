import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
configureLuxAndroid(android)
android { defaultConfig { targetSdk = 36 } }
kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}
