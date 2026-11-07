package at.techbee.spectacled.screens.core

import at.techbee.spectacled.shared.BuildKonfig

/**
 * The channel this build was released on. It is set at build time from the release tag
 * (`v1.2.3-internal|-alpha|-beta`, or `v1.2.3` for production); see `releaseChannel` in
 * `shared/build.gradle.kts`. Local and branch builds are [DEVELOPMENT].
 *
 * Note: a build promoted to another track in the Play Console or App Store Connect keeps
 * the channel it was built with.
 */
enum class ReleaseChannel(val buildValue: String) {
    INTERNAL("internal"),
    ALPHA("alpha"),
    BETA("beta"),
    PRODUCTION("production"),
    DEVELOPMENT("development");

    val isProduction: Boolean get() = this == PRODUCTION

    /** internal, alpha or beta: a release handed to testers, not to everyone. */
    val isPreRelease: Boolean get() = this == INTERNAL || this == ALPHA || this == BETA

    companion object {
        /** Unknown values fall back to [DEVELOPMENT], so a bad value never claims production. */
        fun fromBuildValue(value: String): ReleaseChannel =
            entries.firstOrNull { it.buildValue.equals(value.trim(), ignoreCase = true) } ?: DEVELOPMENT

        /** The channel of the running build. */
        val current: ReleaseChannel by lazy { fromBuildValue(BuildKonfig.APP_RELEASE_CHANNEL) }
    }
}

/** True only for builds released from a production tag (`v1.2.3`, no suffix). */
fun isProductionBuild(): Boolean = ReleaseChannel.current.isProduction
