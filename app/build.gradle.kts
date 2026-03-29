plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
    jacoco
}

// Derive versionCode from CI environment or fall back to a git-based count.
// Google Play requires each upload to have a strictly increasing versionCode.
fun resolvedVersionCode(): Int {
    // Prefer GITHUB_RUN_NUMBER when running in CI
    val runNumber = System.getenv("GITHUB_RUN_NUMBER")
    if (!runNumber.isNullOrBlank()) {
        return runNumber.toIntOrNull() ?: 1
    }
    // Fall back to counting git commits on the current branch
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output.toIntOrNull() ?: 1
    } catch (_: Exception) {
        1
    }
}

fun resolvedVersionName(): String {
    return System.getenv("VERSION_NAME") ?: "1.0.0"
}

android {
    namespace = "com.artifactkeeper.android"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.artifactkeeper.android"
        minSdk = 28
        targetSdk = 35
        versionCode = resolvedVersionCode()
        versionName = resolvedVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // All signing values come from environment variables set in CI.
            // Local dev builds fall through to the debug signing key.
            // See .gitguardian.yml for false-positive suppression on this file.
            fun env(name: String): String? = System.getenv(name)

            val storePath = env("ANDROID_KEYSTORE_PATH")
            val storeCred = env("ANDROID_KEYSTORE_CRED")
            val alias     = env("ANDROID_KEY_ALIAS")
            val aliasCred = env("ANDROID_KEY_CRED")

            if (listOf(storePath, storeCred, alias, aliasCred).all { it != null }) {
                storeFile = file(storePath!!)
                storePassword = storeCred
                keyAlias = alias
                keyPassword = aliasCred
            } else {
                // Release signing will use debug key in development; CI must set env vars.
                // The isMinifyEnabled=true build will still work for local testing, but
                // the APK will not be suitable for Play Store upload.
                storeFile = null
            }
        }
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.all {
            it.extensions.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) releaseSigning else signingConfigs.getByName("debug")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // SDK
    implementation(project(":sdk"))

    // Networking
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // QR code generation
    implementation(libs.zxing.core)

    // Security
    implementation(libs.security.crypto)

    // Lifecycle
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.android.test.ext.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(false)
    }
    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude("**/R.class", "**/R\$*.class", "**/BuildConfig.*", "**/Manifest*.*",
            "**/*_Hilt*.class", "**/Hilt_*.class", "**/*_Factory.class",
            "**/*_MembersInjector.class", "**/*Module_*.class")
    }
    val mainSrc = "${project.projectDir}/src/main/java"
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory) {
        include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    })
}
