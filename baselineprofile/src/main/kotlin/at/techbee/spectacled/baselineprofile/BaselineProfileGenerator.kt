package at.techbee.spectacled.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the Baseline Profile and the startup profile of one Spectacled app.
 *
 * Run it through the app's Gradle task, e.g.
 * `./gradlew :androidJournalsApp:generateReleaseBaselineProfile`, with an emulator or
 * device on API 33+ (or a rooted one on API 28+) connected. The profiles are written to
 * `android<Variant>App/src/release/generated/baselineProfiles/` and are meant to be
 * committed.
 *
 * The app under test is passed in as the `targetAppId` instrumentation argument (see
 * each module's build.gradle.kts), so all three generator modules share this class.
 *
 * A fresh install has no CalDAV account, so the journey covers the cold start up to the
 * first frame: app and Koin initialisation, the database, Compose, navigation and the
 * account list.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        val targetAppId = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: throw IllegalStateException("targetAppId not passed as instrumentation runner argument")

        rule.collect(
            packageName = targetAppId,
            // Also emits a startup profile, which R8 uses to lay out the startup code
            // in the primary DEX file (dexLayoutOptimization in the app modules).
            includeInStartupProfile = true
        ) {
            pressHome()
            startActivityAndWait()
            device.waitForIdle()
        }
    }
}
