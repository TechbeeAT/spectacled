// Renders store screenshots of the real screens off-screen on the JVM, no emulator or simulator.
// Deliberately a separate module with its own tasks instead of a test, so `:shared:allTests`
// (and CI) never pays for it. See screenshots/README.md.

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.androidx.lifecycle.viewmodelCompose)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.koin.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.mock)   // the generator runs offline, see Main.kt
}

val outputDir = layout.buildDirectory.dir("screenshots")
val sampleDataFile = rootProject.layout.projectDirectory.file("sample-data/screenshot_entries.sql")

// Comma-separated filters, e.g. -Pscreenshots.locales=en,de -Pscreenshots.devices=ios-phone
// (a device matches by prefix of its folder name, see Device.kt). Empty means "all".
val locales = providers.gradleProperty("screenshots.locales").orElse("en")
val themes = providers.gradleProperty("screenshots.themes").orElse("light,dark")
val devices = providers.gradleProperty("screenshots.devices").orElse("")
val scenes = providers.gradleProperty("screenshots.scenes").orElse("")

// One JVM per app: the database, Koin graph and preferences are process-wide singletons in
// :shared, so the variants can't share a process.
val variantTasks = listOf("Journals", "Notes", "Tasks").map { variant ->
    tasks.register<JavaExec>("generateScreenshots$variant") {
        group = "screenshots"
        description = "Renders the store screenshots of spectacled $variant into build/screenshots."

        // Locals, so the lambdas below don't capture the build script (configuration cache).
        val homeDir = layout.buildDirectory.dir("screenshots-home/$variant").get().asFile
        val outDir = outputDir.get().asFile
        val sampleData = sampleDataFile.asFile
        val localesArg = locales
        val themesArg = themes
        val devicesArg = devices
        val scenesArg = scenes

        doFirst { homeDir.deleteRecursively() }   // fresh database and preferences on every run

        mainClass.set("at.techbee.spectacled.screenshots.MainKt")
        classpath = sourceSets["main"].runtimeClasspath
        jvmArgs("-Djava.awt.headless=true")
        argumentProviders.add(CommandLineArgumentProvider {
            listOf(
                "variant=${variant.uppercase()}",
                "home=${homeDir.absolutePath}",
                "out=${outDir.absolutePath}",
                "sampleData=${sampleData.absolutePath}",
                "locales=${localesArg.get()}",
                "themes=${themesArg.get()}",
                "devices=${devicesArg.get()}",
                "scenes=${scenesArg.get()}",
            )
        })
    }
}

tasks.register("generateScreenshots") {
    group = "screenshots"
    description = "Renders the store screenshots of all three apps into build/screenshots."
    dependsOn(variantTasks)
}
