package at.techbee.spectacled.screenshots

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.doInitKoin
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_NO_CRITERIA
import at.techbee.spectacled.screens.core.data.LIST_COLLAPSED_GROUP_TRASHBIN
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.theme.ThemeOption
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.koin.core.Koin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Renders the [scenesFor] of one app for every locale x theme x [Device] into
 * `<out>/<app>/<locale>/<device>/<scene>-<theme>.png`. Run through the Gradle tasks of this module,
 * which pass the arguments as key=value pairs (see build.gradle.kts).
 */
fun main(args: Array<String>) {
    val options = args.associate { it.substringBefore('=') to it.substringAfter('=', "") }
    fun option(key: String) = options[key]?.takeIf { it.isNotBlank() }
    fun list(key: String) = option(key)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    // Isolation first, before anything reads user.home: the app keeps its database and preferences
    // there, and the sample data must never end up in the real desktop app's database. The
    // software key vault keeps the fake credentials out of the OS keychain.
    val home = File(option("home") ?: error("Missing home=<empty directory>"))
    require(!home.exists() || home.list().isNullOrEmpty()) { "home=$home must be empty, the sample data is inserted on every run" }
    home.mkdirs()
    System.setProperty("user.home", home.absolutePath)
    System.setProperty("ksafe.jvm.keyVault", "software")
    System.setProperty("java.awt.headless", "true")

    val variant = SpectacledVariant.valueOf(option("variant") ?: error("Missing variant=JOURNALS|NOTES|TASKS"))
    val outDir = File(option("out") ?: error("Missing out=<directory>"))
    val sampleDataFile = File(option("sampleData") ?: error("Missing sampleData=<sql file>"))
    val locales = list("locales").ifEmpty { listOf("en") }
    val themes = list("themes").ifEmpty { listOf("light", "dark") }.map { ThemeOption.valueOf(it.uppercase()) }
    val devices = list("devices").let { filters ->
        Device.entries.filter { device -> filters.isEmpty() || filters.any { device.folder.startsWith(it) } }
    }
    val scenes = list("scenes").let { filters ->
        scenesFor(variant).filter { scene -> filters.isEmpty() || filters.any { scene.name.startsWith(it) } }
    }

    val exitCode = try {
        // Main is the Swing EDT here, which is also where the app's ViewModels run (viewModelScope)
        // and where every ImageComposeScene is created, rendered and closed.
        runBlocking(Dispatchers.Main) {
            doInitKoin(variant)
            val koin = KoinPlatform.getKoin()
            // Offline: the fake account must never reach the network. Syncs the app requests
            // (e.g. when a details screen closes) fail at once and are undone by resetSyncStatus.
            koin.loadModules(
                listOf(module { single { HttpClient(MockEngine { throw IOException("Screenshots are rendered offline") }) } }),
                allowOverride = true
            )
            val calendarRepository = koin.get<CalendarRepository>()
            val sampleData = SampleData.seed(variant, sampleDataFile, calendarRepository, koin.get())
            val preferences = koin.get<PlatformUserAppPreferencesStore>()

            for (locale in locales) {
                Locale.setDefault(Locale.forLanguageTag(locale))
                for (device in devices) {
                    for (theme in themes) {
                        for (scene in scenes) {
                            resetPreferences(preferences, variant, theme)
                            SampleData.resetSyncStatus(calendarRepository)
                            val png = render(koin, variant, device, sampleData, scene)
                            val file = File(outDir, "${variant.name.lowercase()}/$locale/${device.folder}/${scene.name}-${theme.name.lowercase()}.png")
                            file.parentFile.mkdirs()
                            file.writeBytes(png)
                            println("Wrote $file")
                        }
                    }
                }
            }
        }
        0
    } catch (e: Throwable) {
        e.printStackTrace()
        1
    }
    // The Swing EDT and the app's background scopes would keep the JVM alive.
    exitProcess(exitCode)
}

/** Scenes may change preferences through their actions (sort order, filters...): start each one clean. */
private fun resetPreferences(preferences: PlatformUserAppPreferencesStore, variant: SpectacledVariant, theme: ThemeOption) {
    preferences.themeOption = theme
    preferences.themeFont = variant.defaultThemeFont
    preferences.listSortedBy = variant.defaultListSortedBy
    preferences.listSortedByAscending = false
    preferences.listLayout = variant.defaultListLayout
    preferences.listCollapsedGroups = setOf(LIST_COLLAPSED_GROUP_NO_CRITERIA, LIST_COLLAPSED_GROUP_TRASHBIN)
    preferences.listFilterCriteria = emptyMap()
}

private suspend fun render(
    koin: Koin,
    variant: SpectacledVariant,
    device: Device,
    sampleData: SeededSampleData,
    scene: Scene
): ByteArray {
    val sceneScope = SceneScope(koin, variant, device, sampleData)
    val composeScene = ImageComposeScene(
        width = device.widthPx,
        height = device.heightPx,
        density = Density(device.density),
        coroutineContext = Dispatchers.Main
    ) {
        sceneScope.Content()
    }

    try {
        val start = System.nanoTime()
        fun frame(): Image {
            Snapshot.sendApplyNotifications()
            return composeScene.render(System.nanoTime() - start)
        }

        coroutineScope {
            // Keep frames coming while the scene loads and acts, so effects and animations run as in the app.
            val frames = launch {
                while (isActive) {
                    frame()
                    delay(16.milliseconds)
                }
            }
            withTimeout(60.seconds) { scene.setup(sceneScope) }
            delay(SETTLE_TIME)
            frames.cancelAndJoin()
        }

        return frame().encodeToData(EncodedImageFormat.PNG)?.bytes
            ?: error("Could not encode ${scene.name} as PNG")
    } finally {
        composeScene.close()
        sceneScope.clear()
    }
}

/** Long enough for bottom sheets to slide in, lists to settle and images to load. */
private val SETTLE_TIME = 1500.milliseconds
