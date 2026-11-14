import org.gradle.api.artifacts.ProjectDependency

plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

spotless {
    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(
            mapOf("ktlint_function_naming_ignore_when_annotated_with" to "Composable"),
        )
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get())
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt.yml"))
    source.setFrom(
        fileTree(rootDir) {
            include("**/src/**/*.kt")
            exclude("**/build/**")
        },
    )
}

val allowedEdges =
    mapOf(
        ":app" to setOf(":feature", ":data", ":playback", ":source-runtime"),
        ":core:model" to emptySet(),
        ":core:api" to setOf(":core:model"),
        ":core:designsystem" to emptySet(),
        ":feature" to setOf(":core:api", ":core:designsystem"),
        ":data" to setOf(":core:api"),
        ":playback" to setOf(":core:api"),
        ":source-runtime" to setOf(":core:api"),
    )

val architectureCheck by tasks.registering {
    group = "verification"
    description = "Checks actual project dependencies and pure Kotlin module plugins."
    doLast {
        check(subprojects.map { it.path }.toSet() == allowedEdges.keys + setOf(":core")) {
            "New modules require an explicit architecture boundary."
        }
        subprojects.filter { it.path in allowedEdges }.forEach { module ->
            val edges =
                module.configurations
                    .flatMap { config ->
                        // Android adds self-artifact dependencies for its test/lint configurations.
                        config.dependencies
                            .withType(ProjectDependency::class.java)
                            .map { it.path }
                            .filter { it != module.path }
                    }.toSet()
            check(edges.all { it in allowedEdges.getValue(module.path) }) {
                "Forbidden dependencies from ${module.path}: ${edges - allowedEdges.getValue(module.path)}"
            }
            if (module.path in setOf(":core:model", ":core:api")) {
                val allowedGroups =
                    setOf("org.jetbrains.kotlin", "org.jetbrains.kotlinx", "org.jetbrains", "junit", "org.hamcrest")
                val forbidden =
                    module.configurations.flatMap { it.dependencies }.filter {
                        it !is ProjectDependency && (it.group !in allowedGroups || it.name.endsWith("-android"))
                    }
                check(forbidden.isEmpty()) { "Core API/model cannot depend on Android or infrastructure libraries" }
                check(!module.plugins.hasPlugin("com.android.library")) { "Core API/model must remain JVM-only" }
                check(!module.plugins.hasPlugin("com.android.application")) { "Core API/model must remain JVM-only" }
            }
        }
    }
}

tasks.register("verify") {
    group = "verification"
    dependsOn(
        "spotlessCheck",
        "detekt",
        architectureCheck,
        ":core:model:test",
        ":core:api:test",
        ":app:lintDebug",
        ":app:assembleDebug",
    )
}
