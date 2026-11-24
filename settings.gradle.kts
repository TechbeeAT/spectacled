rootProject.name = "spectacled"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://jitpack.io") // dav4jvm
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":androidNotesApp")
include(":composeNotesApp")
include(":androidJournalsApp")
include(":composeJournalsApp")
include(":androidTasksApp")
include(":composeTasksApp")
include(":screenshots")
include(":server")
include(":shared")

// One Baseline Profile generator per Android app (a com.android.test module can only
// target one app). They share the generator source in baselineprofile/src/.
include(":baselineprofileJournals")
project(":baselineprofileJournals").projectDir = file("baselineprofile/journals")
include(":baselineprofileNotes")
project(":baselineprofileNotes").projectDir = file("baselineprofile/notes")
include(":baselineprofileTasks")
project(":baselineprofileTasks").projectDir = file("baselineprofile/tasks")

// NOTE: The iOS apps (iosJournalsApp, iosNotesApp, iosTasksApp) are standalone
// Xcode projects, not Gradle modules. They consume the shared framework via the
// `embedAndSignAppleFrameworkForXcode` build phase in their .xcodeproj, so they
// must NOT be declared with include(...) here. Doing so gives Gradle empty,
// build-script-less subprojects and breaks Android Studio's module tree.
