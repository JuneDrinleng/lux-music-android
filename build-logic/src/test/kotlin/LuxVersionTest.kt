import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LuxVersionTest {
    @Test
    fun matchesPublishedPolicyExamples() {
        val version = LuxVersion.parse("0.1.0")
        assertEquals(100001, version.code(1))
        assertEquals(100999, version.code(999))
        assertEquals(101001, LuxVersion.parse("0.1.1").code(1))
    }

    @Test
    fun channelsAndTargetTransitionsAlwaysIncrease() {
        for (major in listOf(0, 1, 208, 209)) {
            for (minor in listOf(0, 1, 98, 99)) {
                for (patch in listOf(0, 1, 98, 99)) {
                    assertTransition(LuxVersion(major, minor, patch))
                }
            }
        }
        assertEquals(2099999999, LuxVersion(209, 99, 99).code(999))
    }

    private fun assertTransition(version: LuxVersion) {
        assertTrue(version.code(1) < version.code(998))
        assertTrue(version.code(998) < version.code(999))
        val next =
            when {
                version.patch < 99 -> LuxVersion(version.major, version.minor, version.patch + 1)
                version.minor < 99 -> LuxVersion(version.major, version.minor + 1, 0)
                version.major < 209 -> LuxVersion(version.major + 1, 0, 0)
                else -> null
            }
        next?.let { assertTrue(version.code(999) < it.code(1)) }
    }

    @Test
    fun rejectsAmbiguousVersionsAndOutOfRangeCodes() {
        for (version in listOf("01.1.0", "0.1", "0.1.0-dev.1", "210.0.0", "0.100.0", "-1.0.0")) {
            assertTrue(runCatching { LuxVersion.parse(version) }.isFailure)
        }
        assertTrue(runCatching { LuxVersion(0, 1, 0).code(0) }.isFailure)
        assertTrue(runCatching { LuxVersion(0, 1, 0).code(1000) }.isFailure)
    }
}
