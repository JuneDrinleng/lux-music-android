plugins { id("lux.jvm.library") }
dependencies {
    api(project(":core:model"))
    api(libs.coroutines.core)
}
