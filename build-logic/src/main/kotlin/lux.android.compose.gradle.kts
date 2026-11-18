plugins { id("org.jetbrains.kotlin.plugin.compose") }
plugins.withId("com.android.library") {
    extensions.configure<com.android.build.api.dsl.LibraryExtension> { buildFeatures.compose = true }
}
plugins.withId("com.android.application") {
    extensions.configure<com.android.build.api.dsl.ApplicationExtension> { buildFeatures.compose = true }
}
