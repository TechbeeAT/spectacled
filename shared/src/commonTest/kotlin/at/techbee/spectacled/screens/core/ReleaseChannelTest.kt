package at.techbee.spectacled.screens.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseChannelTest {

    @Test
    fun parsesEveryBuildValue() {
        ReleaseChannel.entries.forEach {
            assertEquals(it, ReleaseChannel.fromBuildValue(it.buildValue))
        }
        assertEquals(ReleaseChannel.BETA, ReleaseChannel.fromBuildValue(" Beta "))
    }

    @Test
    fun unknownValuesAreNeverProduction() {
        listOf("", "rc", "prod", "release").forEach {
            assertEquals(ReleaseChannel.DEVELOPMENT, ReleaseChannel.fromBuildValue(it))
        }
    }

    @Test
    fun onlyProductionIsProduction() {
        assertTrue(ReleaseChannel.PRODUCTION.isProduction)
        assertFalse(ReleaseChannel.PRODUCTION.isPreRelease)
        listOf(ReleaseChannel.INTERNAL, ReleaseChannel.ALPHA, ReleaseChannel.BETA).forEach {
            assertFalse(it.isProduction)
            assertTrue(it.isPreRelease)
        }
        assertFalse(ReleaseChannel.DEVELOPMENT.isProduction)
        assertFalse(ReleaseChannel.DEVELOPMENT.isPreRelease)
    }

    @Test
    fun currentMatchesBuildConfig() {
        // Tests run untagged locally; on CI they may run on a tag. Either way it must parse.
        assertEquals(ReleaseChannel.current.isProduction, isProductionBuild())
    }
}
