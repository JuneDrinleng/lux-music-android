plugins { id("lux.android.library") }
android { namespace = "cn.lux.music.data" }
dependencies {
    implementation(project(":core:api"))
    implementation(libs.serialization.json)
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}

// Only test code invokes the pinned Node oracle; production has no Node dependency.
tasks.withType<Test>().configureEach {
    systemProperty("lux.root", rootProject.projectDir.absolutePath)
    val oracle = providers.environmentVariable("LUX_LX_SOURCE")
    inputs.property("lxOracleEnabled", oracle.isPresent)
    if (oracle.isPresent) {
        inputs.files(rootProject.fileTree("scripts/lx-contract") { include("*.mjs") })
        inputs.file(rootProject.file(".contract-upstream/message2call.cjs"))
        inputs
            .files(fileTree(oracle.get() + "/src") { include("**/*.ts") })
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
