plugins { `kotlin-dsl` }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
dependencies {
    testImplementation(libs.junit)
    implementation(libs.android.gradle)
    implementation(libs.kotlin.gradle)
    implementation(libs.kotlin.compose.gradle)
    implementation(libs.hilt.gradle)
    implementation(libs.ksp.gradle)
}
