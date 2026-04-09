plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.swparks.screenshots"
    compileSdk = 36

    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 26
        targetSdk = 35

        testInstrumentationRunner = "com.swparks.screenshots.ScreenshotTestRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":app"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.test.rules)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.ui.test.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.kotlinx.serialization.json)
    implementation("tools.fastlane:screengrab:2.1.1")
}
