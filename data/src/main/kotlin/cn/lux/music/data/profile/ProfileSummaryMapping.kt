package cn.lux.music.data.profile

import cn.lux.music.model.ProfileAuthentication
import cn.lux.music.model.ProfileId
import cn.lux.music.model.ProfileKind
import cn.lux.music.model.ProfileSummary

/** Public projection deliberately omits binding IDs, device IDs and encrypted credentials. */
internal fun ProfileRecord.summary(): ProfileSummary {
    val authentication =
        when {
            endpoint == null -> ProfileAuthentication.NOT_REQUIRED
            credentials == null || authorizationRequired -> ProfileAuthentication.NEEDS_AUTHORIZATION
            else -> ProfileAuthentication.AVAILABLE
        }
    return ProfileSummary(
        ProfileId(id),
        name,
        if (endpoint == null) ProfileKind.LOCAL else ProfileKind.LX,
        endpoint,
        authentication,
    )
}
