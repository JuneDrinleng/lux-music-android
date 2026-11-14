package cn.lux.music.sources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

internal class ScriptAdmissionTest {
    @Test
    fun confirmationCannotSurviveAContentChangeOrAbiChange() {
        val original = "// 音源\nconst token = 'private';"
        val confirmed = ScriptAdmission.fingerprint(original)
        ScriptAdmission.approve(original, "2.0.0", confirmed)
        assertThrows(IllegalArgumentException::class.java) {
            ScriptAdmission.approve("$original\n", "2.0.0", confirmed)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ScriptAdmission.approve(original, "3.0.0", confirmed)
        }
        assertEquals("ApprovedScript([redacted])", ScriptAdmission.approve(original, "2.0.0", confirmed).toString())
    }

    @Test
    fun budgetCountsUtf8BytesAndSha256IsStable() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ScriptAdmission.fingerprint("abc"),
        )
        val oversized = "中".repeat(ScriptAdmission.MAX_SCRIPT_BYTES / 3 + 1)
        assertThrows(IllegalArgumentException::class.java) {
            ScriptAdmission.approve(oversized, "2.0.0", ScriptAdmission.fingerprint(oversized))
        }
    }
}
