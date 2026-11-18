import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins { id("org.jetbrains.kotlin.jvm") }
java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    explicitApi()
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}
