package cn.lux.music.data.profile

import android.content.Context
import androidx.room.withTransaction
import cn.lux.music.api.ProfileException
import cn.lux.music.api.ProfileSession
import cn.lux.music.data.sync.lx.LxHttp
import cn.lux.music.data.sync.lx.LxJson
import cn.lux.music.data.sync.lx.LxListReducer
import cn.lux.music.data.sync.lx.LxSnapshot
import cn.lux.music.data.sync.lx.LxSyncSession
import cn.lux.music.model.InitialSyncChoice
import cn.lux.music.model.LxServer
import cn.lux.music.model.ProfileFailure
import cn.lux.music.model.ProfileId
import cn.lux.music.model.ProfileState
import cn.lux.music.model.ProfileSyncState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// One owner implements the complete ProfileSession command surface; do not split its state.
@Suppress("TooManyFunctions")
@Singleton
internal class ProfileStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : ProfileSession {
        private val files = ProfileFiles(context)
        private val vault = ProfileVault()
        private val http = LxHttp()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val barrier = Mutex()
        private val mutableState = MutableStateFlow(ProfileState())
        override val state = mutableState.asStateFlow()
        private var active: Lease? = null
        private var generation = 0L
        private var session: LxSyncSession? = null
        private val catalog get() = files.catalog.profiles()

        internal data class Lease(
            val id: ProfileId,
            val generation: Long,
            val database: ProfileLibraryDatabase,
        )

        override suspend fun initialize() =
            onIo {
                barrier.withLock {
                    if (active != null) {
                        publish()
                        return@withLock
                    }
                    switchLocked(files.bootstrap())
                }
            }

        override suspend fun checkServer(
            address: String,
            cleartextConfirmed: Boolean,
        ): LxServer = withContext(Dispatchers.IO) { http.inspect(address, cleartextConfirmed) }

        override suspend fun addConnection(
            server: LxServer,
            connectionCode: CharArray,
            deviceName: String,
        ): ProfileId =
            onIo {
                try {
                    initialize()
                    val expected = barrier.withLock { generation }
                    val credentials = http.authenticate(server, connectionCode, deviceName)
                    barrier.withLock {
                        if (expected != generation) throw ProfileException(ProfileFailure.STALE_OPERATION)
                        val id = ProfileId(UUID.randomUUID().toString())
                        val occupied = catalog.all().map { it.name }.toSet()
                        val baseName = credentials.serverName.ifBlank { "LX 同步资料" }
                        var name = baseName
                        var ordinal = 2
                        while (name in occupied) name = "$baseName · ${ordinal++}"
                        val record =
                            ProfileRecord(
                                id.value,
                                name,
                                server.endpoint,
                                server.serverId,
                                UUID.randomUUID().toString(),
                                deviceName,
                                server.cleartextConfirmed,
                                vault.encrypt(id, credentials),
                            )
                        catalog.save(record)
                        publish()
                        id
                    }
                } finally {
                    connectionCode.fill('\u0000')
                }
            }

        override suspend fun activate(id: ProfileId) =
            onIo {
                initialize()
                barrier.withLock { if (active?.id != id) switchLocked(id) }
            }

        // This resource/transaction boundary must close the prepared DB and rethrow any failure.
        @Suppress("TooGenericExceptionCaught")
        private suspend fun switchLocked(id: ProfileId) {
            val target =
                catalog.find(id.value)?.takeUnless { it.deleting }
                    ?: throw ProfileException(ProfileFailure.STALE_OPERATION)
            val database = files.open(id)
            withContext(NonCancellable) {
                val old = active
                session?.close()
                session = null
                generation++
                mutableState.value = state.value.copy(switching = true, sync = ProfileSyncState.OFFLINE)
                try {
                    files.catalog.withTransaction { catalog.activate(ActiveProfileRecord(profileId = id.value)) }
                } catch (error: Exception) {
                    database.close()
                    active = old?.copy(generation = generation)
                    publish(ProfileFailure.STORAGE)
                    throw error
                }
                active = Lease(id, generation, database)
                mutableState.value = state.value.copy(activeId = id, initializing = false, switching = false)
                old?.database?.close()
                publish()
                startLocked(target)
            }
        }

        override suspend fun reconnect() =
            onIo {
                initialize()
                barrier.withLock {
                    session?.close()
                    session = null
                    val lease = active ?: throw ProfileException(ProfileFailure.STALE_OPERATION)
                    generation++
                    active =
                        lease.copy(
                            generation = generation,
                        )
                    val record = catalog.find(lease.id.value) ?: throw ProfileException(ProfileFailure.STALE_OPERATION)
                    if (record.authorizationRequired) throw ProfileException(ProfileFailure.AUTHORIZATION_REJECTED)
                    catalog.save(record.copy(syncEnabled = true))
                    startLocked(record.copy(syncEnabled = true))
                }
            }

        private suspend fun startLocked(record: ProfileRecord) {
            val lease = active ?: throw ProfileException(ProfileFailure.STALE_OPERATION)
            val encryptedCredentials = record.credentials
            val mayConnect = record.syncEnabled && !record.authorizationRequired
            if (record.endpoint == null || encryptedCredentials == null || !mayConnect) {
                publish()
                return
            }
            val credentials =
                try {
                    vault.decrypt(lease.id, encryptedCredentials)
                } catch (_: Exception) {
                    catalog.save(record.copy(authorizationRequired = true))
                    publish(ProfileFailure.CREDENTIALS_UNAVAILABLE)
                    return
                }
            mutableState.value = state.value.copy(sync = ProfileSyncState.CONNECTING, failure = null)
            session =
                LxSyncSession(
                    scope,
                    http,
                    LxServer(record.endpoint, record.serverId.orEmpty(), record.cleartextConfirmed),
                    credentials,
                    record.deviceName,
                    ProfileSyncStorage(this, lease),
                )
        }

        override suspend fun chooseInitialSync(choice: InitialSyncChoice) =
            onIo {
                if (choice == InitialSyncChoice.CANCEL) {
                    disconnect()
                } else {
                    barrier.withLock {
                        (
                            session ?: throw ProfileException(
                                ProfileFailure.STALE_OPERATION,
                            )
                        ).choose(choice)
                    }
                }
            }

        override suspend fun disconnect() =
            onIo {
                barrier.withLock {
                    session?.close()
                    session = null
                    generation++
                    active = active?.copy(generation = generation)
                    active?.let { lease ->
                        catalog.find(lease.id.value)?.let { catalog.save(it.copy(syncEnabled = false)) }
                    }
                    publish()
                }
            }

        override suspend fun signOut(id: ProfileId) =
            onIo {
                barrier.withLock {
                    val record = catalog.find(id.value) ?: throw ProfileException(ProfileFailure.STALE_OPERATION)
                    if (record.endpoint == null) return@withLock
                    if (active?.id == id) {
                        session?.close()
                        session = null
                        generation++
                        active = active?.copy(generation = generation)
                    }
                    catalog.save(record.copy(credentials = null, syncEnabled = false))
                    publish()
                }
            }

        override suspend fun delete(id: ProfileId) =
            onIo {
                barrier.withLock {
                    val record = catalog.find(id.value) ?: throw ProfileException(ProfileFailure.STALE_OPERATION)
                    require(record.endpoint != null) { "The local profile cannot be deleted" }
                    if (active?.id == id) {
                        val local = catalog.all().first { it.endpoint == null && !it.deleting }
                        switchLocked(ProfileId(local.id))
                    }
                    catalog.save(record.copy(credentials = null, deleting = true))
                    publish()
                    files.delete(id)
                    catalog.delete(id.value)
                    Unit
                }
            }

        private suspend fun publish(failure: ProfileFailure? = null) {
            val profiles = catalog.all().filterNot { it.deleting }.map { it.summary() }
            mutableState.value =
                ProfileState(
                    profiles.toList(),
                    active?.id,
                    initializing = false,
                    sync = if (session == null) ProfileSyncState.OFFLINE else state.value.sync,
                    failure = failure,
                )
        }

        private fun check(lease: Lease) {
            if (active?.id != lease.id || active?.generation != lease.generation ||
                active?.database !== lease.database
            ) {
                throw ProfileException(ProfileFailure.STALE_OPERATION)
            }
        }

        internal suspend fun currentLease(): Lease =
            barrier.withLock {
                active ?: throw ProfileException(ProfileFailure.STALE_OPERATION)
            }

        internal suspend fun mutate(
            lease: Lease,
            action: JsonElement,
        ) = onIo {
            barrier.withLock {
                check(lease)
                if (state.value.sync !in
                    setOf(ProfileSyncState.OFFLINE, ProfileSyncState.FAILED, ProfileSyncState.READY)
                ) {
                    throw ProfileException(ProfileFailure.STALE_OPERATION)
                }
                val updated = LxListReducer(lease.database.readSnapshot()).apply(action)
                val localAction = if (catalog.find(lease.id.value)?.endpoint != null) LxJson.stringify(action) else null
                lease.database.writeSnapshot(updated.canonical(), localAction)
            }
        }

        internal suspend fun <T> withLease(
            lease: Lease,
            block: suspend () -> T,
        ): T =
            barrier.withLock {
                check(lease)
                try {
                    block()
                } catch (
                    error: CancellationException,
                ) {
                    throw error
                } catch (
                    error: ProfileException,
                ) {
                    throw error
                } catch (_: Exception) {
                    throw ProfileException(ProfileFailure.STORAGE)
                }
            }

        internal suspend fun syncStatus(
            lease: Lease,
            value: ProfileSyncState,
            failure: ProfileFailure?,
        ) {
            barrier.withLock {
                check(lease)
                if (failure == ProfileFailure.AUTHORIZATION_REJECTED) {
                    catalog.find(lease.id.value)?.let { catalog.save(it.copy(authorizationRequired = true)) }
                    publish(failure)
                }
                mutableState.value = state.value.copy(sync = value, failure = failure)
            }
        }

        internal suspend fun shutdown() {
            barrier.withLock {
                scope.cancel()
                session?.close()
                active?.database?.close()
                active = null
                files.catalog.close()
            }
        }

        private suspend fun <T> onIo(block: suspend () -> T): T =
            withContext(Dispatchers.IO) {
                try {
                    block()
                } catch (
                    error: CancellationException,
                ) {
                    throw error
                } catch (error: ProfileException) {
                    throw error
                } catch (_: Exception) {
                    mutableState.value =
                        state.value.copy(
                            initializing = false,
                            switching = false,
                            failure = ProfileFailure.STORAGE,
                        )
                    throw ProfileException(ProfileFailure.STORAGE)
                }
            }
    }
