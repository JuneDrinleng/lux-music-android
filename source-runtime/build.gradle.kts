plugins { id("lux.android.library") }
android { namespace = "cn.lux.music.sources" }
dependencies {
    implementation(project(":core:api"))
}
