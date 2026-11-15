package cn.lux.music.model

public enum class ProfileKind { LOCAL, LX }

public enum class ProfileAuthentication { NOT_REQUIRED, AVAILABLE, NEEDS_AUTHORIZATION }

public enum class ProfileSyncState { OFFLINE, CONNECTING, NEGOTIATING, AWAITING_CHOICE, SYNCING, READY, FAILED }

public enum class ProfileFailure {
    INVALID_INPUT,
    INVALID_ADDRESS,
    CLEARTEXT_CONFIRMATION_REQUIRED,
    NETWORK,
    TLS,
    INCOMPATIBLE_SERVER,
    AUTHORIZATION_REJECTED,
    RATE_LIMITED,
    SERVER,
    STORAGE,
    CREDENTIALS_UNAVAILABLE,
    STALE_OPERATION,
    CHOICE_EXPIRED,
}

public enum class InitialSyncChoice {
    MERGE_DEVICE_FIRST,
    MERGE_SERVER_FIRST,
    REPLACE_DEVICE,
    REPLACE_SERVER,
    CANCEL,
}

public data class ProfileSummary(
    public val id: ProfileId,
    public val name: String,
    public val kind: ProfileKind,
    public val endpoint: String?,
    public val authentication: ProfileAuthentication,
)

public data class ProfileState(
    public val profiles: List<ProfileSummary> = emptyList(),
    public val activeId: ProfileId? = null,
    public val initializing: Boolean = true,
    public val switching: Boolean = false,
    public val sync: ProfileSyncState = ProfileSyncState.OFFLINE,
    public val failure: ProfileFailure? = null,
)

public data class LxServer(
    public val endpoint: String,
    public val serverId: String,
    public val cleartextConfirmed: Boolean,
)
