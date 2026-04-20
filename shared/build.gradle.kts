import com.codingfeline.buildkonfig.compiler.FieldSpec
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)

    alias(libs.plugins.sqldelight)
    alias(libs.plugins.serialization)

    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.buildKonfig)
}

kotlin {
    // Android target configured via androidLibrary block (replaces androidTarget + android{})
    android {
        namespace = "at.techbee.spectacled.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Required for Compose Multiplatform resources to be bundled into the AAR
        androidResources {
            enable = true
        }
    }

    iosArm64()
    iosX64()
    iosSimulatorArm64()
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.material.icons.extended)
            implementation(libs.navigation.compose)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation) // For request body
            implementation(libs.ktor.serialization.kotlinx.xml) // If using kotlinx-serialization with XML support
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.napier)

            //image loading
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)

            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)

            implementation(libs.compose.pipette)

            implementation(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.multiplatform.settings.no.arg)

            //koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            implementation(libs.kotlinx.datetime)

            //implementation(libs.coroutines.extensions)

            implementation(libs.reorderable)

            implementation(libs.sql.delight.coroutines.extensions)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            //implementation(libs.dav4jvm)
            implementation(libs.sql.delight.android.driver)
            implementation(libs.androidx.work.runtime.ktx)
            // safe storage
            implementation(libs.ksafe)
            
            // Required for rendering Compose Previews in Android Studio
            implementation(libs.compose.uiTooling)
        }

        jvmMain.dependencies {
            //implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sql.delight.sqlite.driver)
            // safe storage
            implementation(libs.ksafe)
            //implementation(libs.dav4jvm)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sql.delight.native.driver)
            // safe storage
            implementation(libs.ksafe)
        }

        webMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.web.worker.driver)
            implementation(devNpm("copy-webpack-plugin", libs.versions.webPackPlugin.get()))
            implementation(npm("@cashapp/sqldelight-sqljs-worker", libs.versions.sqldelight.get()))
            implementation(npm("sql.js", libs.versions.sqlJs.get()))
        }
    }
}

buildkonfig {
    packageName = "at.techbee.spectacled.shared"
    defaultConfigs {
        buildConfigField(STRING, "APP_VERSION_STRING", libs.versions.appVersionString.get())
        buildConfigField(FieldSpec.Type.INT, "APP_BUILD_NUMBER", libs.versions.appBuildNumber.get())
        buildConfigField(STRING, "APP_VERSION_CODENAME", libs.versions.appVersionCodename.get())
    }
}


sqldelight {
    databases {
        create("SpectacledDatabase") {
            packageName.set("at.techbee.spectacled.db")
            generateAsync.set(true)
        }
    }
}


// Task to sync version strings to iOS xcconfig
tasks.register("syncIosVersion") {
    val iosApps = listOf("iosApp", "iosJournalsApp", "iosNotesApp", "iosTasksApp")
    val versionInt = libs.versions.appBuildNumber.get()
    val versionString = libs.versions.appVersionString.get()

    doLast {
        iosApps.forEach { appDir ->
            val configFile = file("../$appDir/Configuration/Config.xcconfig")
            if (configFile.exists()) {
                val lines = configFile.readLines().map { line ->
                    when {
                        line.startsWith("CURRENT_PROJECT_VERSION=") -> "CURRENT_PROJECT_VERSION=$versionInt"
                        line.startsWith("MARKETING_VERSION=") -> "MARKETING_VERSION=$versionString"
                        else -> line
                    }
                }
                configFile.writeText(lines.joinToString("\n"))
                println("iOS versions synced for $appDir: Build $versionInt, Marketing $versionString")
            } else {
                println("Warning: iOS config file not found for $appDir at ${configFile.absolutePath}")
            }
        }
    }
}

// Ensure version sync runs during build
tasks.matching { it.name.contains("build", ignoreCase = true) }.configureEach {
    dependsOn("syncIosVersion")
}
