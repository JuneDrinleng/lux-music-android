package cn.lux.music.data.sync.lx

import cn.lux.music.model.ProfileFailure
import cn.lux.music.model.ProfileSyncState

/** Bound to a captured profile generation and database, never a global currentDatabase lookup. */
internal interface LxSyncStorage {
    suspend fun beginReconciliation()

    suspend fun snapshot(): LxSnapshot

    suspend fun replace(snapshot: LxSnapshot)

    suspend fun apply(action: kotlinx.serialization.json.JsonElement)

    suspend fun reconciled()

    suspend fun pending(): List<LxPendingAction>

    suspend fun acknowledge(id: Long)

    suspend fun status(
        value: ProfileSyncState,
        failure: ProfileFailure? = null,
    )
}

internal data class LxPendingAction(
    val id: Long,
    val wire: String,
)
