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
        namespace = "at.techbee.spectacled.tasks.lib"  // namespace must be different from in androidJournalsApp, otherwise it will conflict
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
            baseName = "ComposeTasksApp"
            //isStatic = true
            export(projects.shared)
            linkerOpts.add("-lsqlite3")
            freeCompilerArgs += listOf("-Xbinary=bundleId=at.techbee.spectacled.tasks.ios")
        }
    }

    jvm()
    
    js {
        outputModuleName = "composeTasksApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeTasksApp.js"
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
        outputModuleName = "composeTasksApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeTasksApp.js"
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
        mainClass = "at.techbee.spectacled.tasks.MainKt"

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

            // Display name: passed to jpackage as --name, which becomes the "Name=" of the
            // Linux .desktop entry, the macOS .app bundle name and the Windows install
            // directory. Deliberately separate from the Debian package name below, which
            // has to stay a lowercase identifier.
            packageName = "spectacled Tasks"
            packageVersion = libs.versions.appVersionString.get()
            description = "Track to-dos with due dates and priorities over CalDAV"
            vendor = "Techbee e.U."
            // Deliberately ASCII: jpackage runs as a separate process and decodes its
            // arguments with the platform locale, so a "©" turns into mojibake in the
            // packages whenever the builder's locale is not UTF-8.
            copyright = "Copyright (c) 2026 Techbee e.U."

            linux {
                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon_tasks_png.png"))

                // Debian package identity: a conventional short lowercase name (Debian
                // Policy 5.6.1), which is also the /opt/<name> installation path. The
                // reverse-DNS id lives on in bundleID / applicationId, not here.
                packageName = "spectacled-tasks"
                debMaintainer = "spectacled@techbee.at"   // -> "Techbee e.U. <spectacled@techbee.at>"
                appCategory = "utils"                     // "Section:" of the deb control file
                menuGroup = "Office;ProjectManagement;"   // "Categories=" of the .desktop entry

                // jpackage writes no .desktop file at all unless this is set, so without
                // it the package installs without a launcher and the display name and
                // categories above have nothing to apply to.
                shortcut = true
            }
            windows {
                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon_tasks_ico.ico"))

                // Without these the MSI installs the app with no Start menu entry and no
                // desktop icon at all. jpackage always creates the desktop icon; the DSL
                // has no equivalent of its --win-shortcut-prompt opt-out checkbox.
                menu = true
                menuGroup = "spectacled"
                shortcut = true

                // Identifies the product across versions. jpackage generates a random one
                // when it is missing, which makes every new version install alongside the
                // old one instead of upgrading it, so this must stay fixed forever.
                upgradeUuid = "8956CA6E-6CFF-4CD3-AB94-010B07136B0B"
            }
            macOS {
                dockName = "spectacled Tasks"
                iconFile.set(project.file("src/commonMain/composeResources/drawable/icon_tasks_icns.icns"))
                bundleID = "at.techbee.spectacled.tasks"

                // LSApplicationCategoryType and LSMinimumSystemVersion of the bundle:
                // where the app is filed, and which macOS versions refuse to run it.
                appCategory = "public.app-category.productivity"
                minimumSystemVersion = "11.0"

                // Sign the .app with the "Developer ID Application" certificate only when a
                // signing identity is provided (CI release builds on macOS). Local and
                // Linux/Windows builds keep working unsigned with no Apple setup required.
                // Notarization + stapling of the resulting .dmg is done in the release
                // workflow (create-release.yml) via `xcrun notarytool` / `stapler`.
                System.getenv("MACOS_SIGN_IDENTITY")?.takeIf { it.isNotBlank() }?.let { signIdentity ->
                    signing {
                        sign.set(true)
                        identity.set(signIdentity)
                    }
                }
            }

        }
    }
}
