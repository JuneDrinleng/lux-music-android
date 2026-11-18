plugins { id("lux.android.library") }
android { namespace = "cn.lux.music.playback" }
dependencies {
    implementation(project(":core:api"))
}
