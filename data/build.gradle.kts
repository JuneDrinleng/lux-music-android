plugins { id("lux.android.library") }
android { namespace = "cn.lux.music.data" }
dependencies {
    implementation(project(":core:api"))
}
