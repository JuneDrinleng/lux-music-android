package cn.lux.music.data.sync.lx

/** Messages deliberately exclude raw frames, keys, codes and authentication bodies. */
internal class LxProtocolException(
    message: String,
) : Exception(message)

internal class LxConnectionClosed : Exception("LX connection closed")

internal class LxRemoteException(
    val remoteMessage: String,
) : Exception("LX remote call failed")
