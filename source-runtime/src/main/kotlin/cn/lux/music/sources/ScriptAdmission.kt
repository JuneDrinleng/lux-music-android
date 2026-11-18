package cn.lux.music.sources

import java.security.MessageDigest

/** Trust binds to bytes, never the script name or its self-reported version. */
internal object ScriptAdmission {
    const val ABI = "2.0.0"
    const val MAX_SCRIPT_BYTES = 262_144

    fun fingerprint(script: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(script.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    fun approve(
        script: String,
        abi: String,
        confirmedSha256: String,
    ): ApprovedScript {
        require(abi == ABI) { "Unsupported script ABI" }
        require(script.toByteArray(Charsets.UTF_8).size <= MAX_SCRIPT_BYTES) { "Script exceeds byte budget" }
        require(fingerprint(script) == confirmedSha256) { "Script content requires confirmation" }
        return ApprovedScript(script)
    }

    class ApprovedScript internal constructor(
        internal val code: String,
    ) {
        override fun toString(): String = "ApprovedScript([redacted])"
    }
}
