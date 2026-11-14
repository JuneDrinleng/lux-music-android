/** Release encoding shared by build configuration and policy tests. */
data class LuxVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
    init {
        require(major in 0..209 && minor in 0..99 && patch in 0..99)
    }

    fun code(slot: Int): Int {
        require(slot in 1..999)
        return ((major * 100 + minor) * 100 + patch) * 1000 + slot
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        fun parse(value: String): LuxVersion {
            require(value.matches(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)"))) {
                "Expected an unambiguous X.Y.Z target version"
            }
            val parts = value.split('.').map(String::toInt)
            return LuxVersion(parts[0], parts[1], parts[2])
        }
    }
}
