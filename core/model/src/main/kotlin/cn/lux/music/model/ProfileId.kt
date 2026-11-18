package cn.lux.music.model

/** Stable local profile identity; never a connection code, endpoint or device key. */
@JvmInline
public value class ProfileId(
    public val value: String,
) {
    init {
        require(value.isNotBlank()) { "Profile ID must not be blank" }
    }
}
