import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    // Android target configured via androidLibrary block (replaces androidTarget + android{})
    android {
        namespace = "at.techbee.spectacled.journals.lib"  // namespace must be different from in androidJournalsApp, otherwise it will conflict
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }

        // Required for Compose Multiplatform resources to be bundled into the AAR
        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeJournalsApp"
            //isStatic = true
            export(projects.shared)
            linkerOpts.add("-lsqlite3")
            freeCompilerArgs += listOf("-Xbinary=bundleId=at.techbee.spectacled.journals.ios")
        }
    }

    jvm()
    
    js {
        outputModuleName = "composeJournalsApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeJournalsApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    // Serve sources to debug inside browser
                    static(rootDirPath)
                    static(projectDirPath)
                }
            }
        }
        binaries.executable()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeJournalsApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeJournalsApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    // Serve sources to debug inside browser
                    static(rootDirPath)
                    static(projectDirPath)
                }
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            api(projects.shared)
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

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            implementation(libs.napier)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "at.techbee.spectacled.journals.MainKt"

        // jpackage (used to build the native .dmg/.msi/.deb installers) is not shipped with
        // every JDK — notably Android Studio's bundled JBR omits it, which makes `packageDmg`
        // fail in `checkRuntime`. When a desktop packaging task is requested, point it at a
        // full Temurin JDK provisioned via Gradle's Java toolchain support (auto-downloaded by
        // the Foojay resolver). The vendor is pinned to one that ships jpackage so the JBR is
        // never selected, and it's guarded by task name so Android/Web/run builds don't have
        // to provision a JDK they don't need.
        val needsPackagingJdk = gradle.startParameter.taskNames.any { taskName ->
            listOf("package", "distributable", "checkRuntime", "notarize").any {
                taskName.contains(it, ignoreCase = true)
            }
        }
        if (needsPackagingJdk) {
            javaHome = javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.ADOPTIUM)
            }.get().metadata.installationPath.asFile.absolutePath
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)

            // jpackage trims the bundled runtime with jlink and can't see reflectively
            // loaded modules, so it drops these and the packaged app crashes at runtime:
            //   - java.sql: required by SQLDelight's JDBC SQLite driver (NoClassDefFoundError:
            //     java/sql/DriverManager) — without it every database call fails.
            //   - jdk.unsupported: sun.misc.Unsafe, needed by KSafe for the DataStore backend
            //     and OS-backed key custody; without it KSafe silently falls back to a
            //     plain-JSON store.
            modules("java.sql", "jdk.unsupported")

            packageName = "at.techbee.spectacled.journals"
            packageVersion = libs.versions.appVersionString.get()

            linux { iconFile.set(project.file("src/commonMain/composeResources/drawable/icon_journals_png.png")) }
            windows { iconFile.set(project.file("src/commonMain/composeResources/drawable/icon_journals_ico.ico")) }
            macOS { iconFile.set(project.file("src/commonMain/composeResources/drawable/icon_journals_icns.icns")) }
        }
    }
}
