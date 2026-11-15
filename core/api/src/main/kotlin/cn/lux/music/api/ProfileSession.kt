package cn.lux.music.api

import cn.lux.music.model.InitialSyncChoice
import cn.lux.music.model.LxServer
import cn.lux.music.model.ProfileId
import cn.lux.music.model.ProfileState
import kotlinx.coroutines.flow.StateFlow

/** The sole public profile state. Credentials and database handles never escape data. */
public interface ProfileSession {
    public val state: StateFlow<ProfileState>

    public suspend fun initialize(): Unit

    public suspend fun checkServer(
        address: String,
        cleartextConfirmed: Boolean = false,
    ): LxServer

    /** The implementation consumes and clears connectionCode even when authentication fails. */
    public suspend fun addConnection(
        server: LxServer,
        connectionCode: CharArray,
        deviceName: String,
    ): ProfileId

    public suspend fun activate(id: ProfileId): Unit

    public suspend fun reconnect(): Unit

    public suspend fun chooseInitialSync(choice: InitialSyncChoice): Unit

    public suspend fun disconnect(): Unit

    public suspend fun signOut(id: ProfileId): Unit

    public suspend fun delete(id: ProfileId): Unit
}
