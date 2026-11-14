import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

/** Shared platform policy; application-only targetSdk stays in the app convention. */
fun configureLuxAndroid(android: CommonExtension<*, *, *, *, *, *>) {
    android.apply {
        compileSdk = 36
        buildToolsVersion = "36.0.0"
        defaultConfig { minSdk = 33 }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        lint {
            abortOnError = true
            warningsAsErrors = true
            checkDependencies = true
            // Update availability is handled by dependency PRs, not a changing CI gate.
            disable += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
        }
    }
}
