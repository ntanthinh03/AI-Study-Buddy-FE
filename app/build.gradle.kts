import java.io.File
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Keep module outputs outside app/build to avoid recurring Windows locks in IDE-managed folders.
layout.buildDirectory.set(rootProject.layout.buildDirectory.dir("app-module"))


android {
    namespace = "com.thinh.aistudybuddy"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.thinh.aistudybuddy"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("io.socket:socket.io-client:2.1.0")
}

fun File.makeWritableRecursively() {
    if (!exists()) return
    walkTopDown().forEach { path ->
        // Some generated resource folders on Windows are read-only; make them writable before cleanup.
        path.setWritable(true)
    }
}

fun unlockBuildDirOnWindows() {
    val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
    if (isWindows) {
        layout.buildDirectory.asFile.get().makeWritableRecursively()
    }
}

fun forceDeleteDirWithCmd(targetDir: File, maxAttempts: Int = 8) {
    if (!targetDir.exists()) return

    repeat(maxAttempts) { attempt ->
        targetDir.makeWritableRecursively()

        ProcessBuilder(
            "cmd", "/c", "attrib", "-R", "/S", "/D", "${targetDir.absolutePath}\\*"
        ).start().waitFor()
        ProcessBuilder(
            "cmd", "/c", "rmdir", "/s", "/q", targetDir.absolutePath
        ).start().waitFor()

        if (!targetDir.exists()) return
        Thread.sleep((attempt + 1) * 300L)
    }

    if (targetDir.exists()) {
        logger.warn(
            "Could not delete ${targetDir.absolutePath}; build will continue using the existing directory. " +
                "Close apps/processes locking the build directory and clean it manually if stale outputs persist."
        )
    }
}

val prepareFreshModuleBuildDir by tasks.registering {
    doLast {
        // Keep the module build dir stable so IDE/runtime artifact loading does not break on Windows.
        // Cleanup is intentionally handled by the standard clean task instead of deleting the active build dir here.
    }
}


tasks.withType<Delete>().configureEach {
    doFirst {
        unlockBuildDirOnWindows()
    }
}

tasks.named<Delete>("clean").configure {
    // Use explicit cleanup with retries to avoid Windows file-lock races in Gradle's default deleter.
    setDelete(emptySet<Any>())

    doLast {
        val buildDirFile = layout.buildDirectory.asFile.get()
        if (!buildDirFile.exists()) return@doLast

        unlockBuildDirOnWindows()
        repeat(6) { attempt ->
            exec {
                isIgnoreExitValue = true
                commandLine("cmd", "/c", "rmdir", "/s", "/q", buildDirFile.absolutePath)
            }
            if (!buildDirFile.exists()) return@doLast
            Thread.sleep((attempt + 1) * 250L)
        }

        if (buildDirFile.exists()) {
            throw GradleException(
                "Could not delete ${buildDirFile.absolutePath}. Close apps locking app/build and retry."
            )
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareFreshModuleBuildDir)
}

tasks.matching {
    it.name.contains("merge", ignoreCase = true) && it.name.endsWith("Resources")
}.configureEach {
    doFirst {
        unlockBuildDirOnWindows()
    }
}
