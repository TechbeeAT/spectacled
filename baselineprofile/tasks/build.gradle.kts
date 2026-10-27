plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "at.techbee.spectacled.baselineprofile.tasks"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 28 // Baseline Profile generation needs API 28+
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":androidTasksApp"

    // The generator is the same for all three apps, see ../src/
    sourceSets.getByName("main").kotlin.srcDir("../src/main/kotlin")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

baselineProfile {
    // Run on whatever emulator or device is connected (API 33+, or rooted API 28+).
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.testExt.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.benchmark.macro.junit4)
}

// Tells the shared generator which app to profile.
androidComponents {
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId }
        )
    }
}
