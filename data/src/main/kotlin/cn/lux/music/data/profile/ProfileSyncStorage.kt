package cn.lux.music.data.profile

import androidx.room.withTransaction
import cn.lux.music.data.sync.lx.LxListReducer
import cn.lux.music.data.sync.lx.LxPendingAction
import cn.lux.music.data.sync.lx.LxSnapshot
import cn.lux.music.data.sync.lx.LxSyncStorage
import cn.lux.music.model.ProfileFailure
import cn.lux.music.model.ProfileSyncState
import kotlinx.serialization.json.JsonElement

internal class ProfileSyncStorage(
    private val store: ProfileStore,
    private val lease: ProfileStore.Lease,
) : LxSyncStorage {
    private var reconciliationRevision = 0L

    override suspend fun beginReconciliation() {
        store.withLease(lease) {
            reconciliationRevision =
                lease.database
                    .library()
                    .snapshot()!!
                    .revision
        }
    }

    override suspend fun snapshot(): LxSnapshot = store.withLease(lease) { lease.database.readSnapshot() }

    override suspend fun replace(snapshot: LxSnapshot) {
        store.withLease(lease) { lease.database.writeSnapshot(snapshot.canonical(), localAction = null) }
    }

    override suspend fun apply(action: JsonElement) {
        store.withLease(lease) {
            lease.database.writeSnapshot(
                LxListReducer(lease.database.readSnapshot()).apply(action).canonical(),
                localAction = null,
            )
        }
    }

    override suspend fun reconciled() {
        store.withLease(lease) { lease.database.clearOutboxThrough(reconciliationRevision) }
    }

    override suspend fun pending(): List<LxPendingAction> =
        store.withLease(lease) {
            lease.database.library().pending().map { pending ->
                LxPendingAction(
                    pending.id,
                    lease.database
                        .library()
                        .chunks("outbox:${pending.id}")
                        .joinToString("") { it.text },
                )
            }
        }

    override suspend fun acknowledge(id: Long) {
        store.withLease(lease) {
            lease.database.withTransaction {
                lease.database.library().clearChunks("outbox:$id")
                lease.database.library().acknowledge(id)
            }
        }
    }

    override suspend fun status(
        value: ProfileSyncState,
        failure: ProfileFailure?,
    ) {
        store.syncStatus(lease, value, failure)
    }
}
