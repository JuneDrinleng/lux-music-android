package cn.lux.music.api

import cn.lux.music.model.ProfileFailure

/** No raw HTTP/JS/crypto exception, connection code or secret URL in public errors. */
public class ProfileException(
    public val failure: ProfileFailure,
) : Exception(failure.name)
