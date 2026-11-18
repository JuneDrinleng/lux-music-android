plugins {
    id("lux.android.library")
    id("lux.android.hilt")
}
android {
    namespace = "cn.lux.music.data"
    defaultConfig { testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    implementation(project(":core:api"))
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    implementation(libs.room.runtime)
    implementation(libs.okhttp)
    implementation(libs.hilt.android)
    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.coroutines.test)
}

// Only test code invokes the pinned Node oracle; production has no Node dependency.
tasks.withType<Test>().configureEach {
    systemProperty("lux.root", rootProject.projectDir.absolutePath)
    val oracle = providers.environmentVariable("LUX_LX_SOURCE")
    inputs.property("lxOracleEnabled", oracle.isPresent)
    if (oracle.isPresent) {
        inputs.files(rootProject.fileTree("scripts/lx-contract") { include("*.mjs", "*.cjs", "*.py") })
        inputs.file(rootProject.file(".contract-upstream/message2call.cjs"))
        inputs.file(rootProject.file(".contract-upstream/lx-deployment/server/modules/list/listDataManage.js"))
        inputs
            .files(fileTree(oracle.get() + "/src") { include("**/*.ts") })
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
