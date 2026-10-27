
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "at.techbee.spectacled.notes"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "at.techbee.spectacled.notes"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.appBuildNumber.get().toInt()
        versionName = libs.versions.appVersionString.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("spectacled") {
            storeFile = file(System.getenv("ANDROID_KEYSTORE") ?: "/dev/null")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS_NOTES")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD_NOTES")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("spectacled")
            // Packs native debug symbols of the bundled .so libraries into the AAB, so
            // Play can symbolicate native crashes and ANRs without a separate upload.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

// Profiles come from :baselineprofileNotes, see README → Baseline Profiles.
baselineProfile {
    // Uses the startup profile to put startup code in the primary DEX file.
    dexLayoutOptimization = true
}

dependencies {
    implementation(projects.composeNotesApp)
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(projects.baselineprofileNotes)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose.viewmodel.navigation)

    implementation(libs.napier)
}
