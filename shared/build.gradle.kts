import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)

    alias(libs.plugins.sqldelight)
    alias(libs.plugins.serialization)

    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.buildKonfig)
}

/*
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

val xxxKey = localProperties.getProperty("XXX_API_KEY") ?: System.getenv("XXX_API_KEY") ?: ""
 */

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

            // safe storage
            implementation(libs.ksafe)

            implementation(libs.material.kolor)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            //implementation(libs.dav4jvm)
            implementation(libs.sql.delight.android.driver)
            implementation(libs.androidx.work.runtime.ktx)

            // Required for rendering Compose Previews in Android Studio
            implementation(libs.compose.uiTooling)

            // Glance Widget
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance.material3)
        }

        jvmMain.dependencies {
            //implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sql.delight.sqlite.driver)
            //implementation(libs.dav4jvm)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sql.delight.native.driver)
        }

        webMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.web.worker.driver)
            implementation(devNpm("copy-webpack-plugin", libs.versions.webPackPlugin.get()))
            implementation(npm("@cashapp/sqldelight-sqljs-worker", libs.versions.sqldelight.get()))
            implementation(npm("sql.js", libs.versions.sqlJs.get()))
            // Provides the IANA time-zone database for kotlinx-datetime on js/wasmJs. Without it,
            // TimeZone.of("Europe/Vienna") and friends throw IllegalTimeZoneException in the browser
            // (the JVM/native targets get their zones from the platform). Needed both for the ICS
            // timezone tests and for correct wall-clock handling of zoned entries on the web target.
            implementation(npm("@js-joda/timezone", libs.versions.jsJodaTimezone.get()))
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

tasks.matching { it.name == "prepareAndroidMainArtProfile" }.configureEach {
    enabled = false
}
