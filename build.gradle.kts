// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

tasks.register("updateReadmeVersions") {
    group = "documentation"
    description = "Updates version badges in README.md"

    doLast {
        try {
            val readmeFile = File(rootDir, "README.md")
            val versionsTomlFile = File(rootDir, "gradle/libs.versions.toml")
            val gradleWrapperFile = File(rootDir, "gradle/wrapper/gradle-wrapper.properties")
            val appBuildFile = File(rootDir, "app/build.gradle.kts")

            if (!readmeFile.exists()) {
                throw GradleException("README.md not found at ${readmeFile.absolutePath}")
            }

            val kotlinVersion = versionsTomlFile.readText()
                .findAfter("kotlin = \"", "\"")
                ?: throw GradleException("Kotlin version not found in libs.versions.toml")

            val agpVersion = versionsTomlFile.readText()
                .findAfter("agp = \"", "\"")
                ?: throw GradleException("AGP version not found in libs.versions.toml")

            val gradleVersion = gradleWrapperFile.readText()
                .findAfter("gradle-", "-bin.zip")
                ?: throw GradleException("Gradle version not found in gradle-wrapper.properties")

            val compileSdk = appBuildFile.readText()
                .findAfter("compileSdk = ", "\n")
                ?.trim()
                ?: throw GradleException("compileSdk not found in app/build.gradle.kts")

            val minSdk = appBuildFile.readText()
                .findAfter("minSdk = ", "\n")
                ?.trim()
                ?: throw GradleException("minSdk not found in app/build.gradle.kts")

            val newBadges = """
[<img alt="Kotlin Version" src="https://img.shields.io/badge/Kotlin_Version-$kotlinVersion-purple">](https://kotlinlang.org/)
[<img alt="Android SDK" src="https://img.shields.io/badge/Android_SDK-$compileSdk-green">](https://developer.android.com/)
[<img alt="Min SDK" src="https://img.shields.io/badge/Min_SDK-$minSdk-informational">](https://developer.android.com/)
[<img alt="Gradle" src="https://img.shields.io/badge/Gradle-$gradleVersion-blue">](https://gradle.org/)
[<img alt="AGP" src="https://img.shields.io/badge/AGP-$agpVersion-green">](https://developer.android.com/tools/releases/gradle-plugin)
""".trimIndent()

            val readmeContent = readmeFile.readText()
            val beginMarker = "<!-- BEGIN_VERSIONS -->"
            val endMarker = "<!-- END_VERSIONS -->"

            val beginIndex = readmeContent.indexOf(beginMarker)
            val endIndex = readmeContent.indexOf(endMarker)

            if (beginIndex == -1 || endIndex == -1) {
                throw GradleException("Version markers not found in README.md")
            }

            val updatedContent = readmeContent.substring(0, beginIndex + beginMarker.length) +
                "\n" + newBadges + "\n" +
                readmeContent.substring(endIndex)

            readmeFile.writeText(updatedContent)
            logger.lifecycle("✓ README.md versions updated successfully")
        } catch (e: Exception) {
            logger.error("Failed to update README.md: ${e.message}")
            throw e
        }
    }
}

fun String.findAfter(prefix: String, suffix: String): String? {
    val startIndex = indexOf(prefix)
    if (startIndex == -1) return null
    val valueStart = startIndex + prefix.length
    val endIndex = indexOf(suffix, valueStart)
    if (endIndex == -1) return null
    return substring(valueStart, endIndex)
}
