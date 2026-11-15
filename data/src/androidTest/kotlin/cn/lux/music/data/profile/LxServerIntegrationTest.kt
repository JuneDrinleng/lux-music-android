package cn.lux.music.data.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.lux.music.api.ProfileException
import cn.lux.music.data.sync.lx.LxJson
import cn.lux.music.model.InitialSyncChoice
import cn.lux.music.model.ProfileAuthentication
import cn.lux.music.model.ProfileFailure
import cn.lux.music.model.ProfileId
import cn.lux.music.model.ProfileSyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Full original LX deployment, Android Room/Keystore client and independent Node client. */
@RunWith(AndroidJUnit4::class)
internal class LxServerIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val endpoint =
        InstrumentationRegistry.getArguments().getString("lxEndpoint")
            ?: error("Start the pinned LX deployment and pass lxEndpoint")
    private val control = LxFixtureControl()

    @Test
    fun rejectedDeviceAuthorizationSurvivesRestartWithoutDeletingSongs() =
        runBlocking {
            val context = instrumentation.targetContext
            check(context.packageName.endsWith(".test"))
            context.databaseList().forEach { context.deleteDatabase(it) }
            var store = ProfileStore(context)
            try {
                store.initialize()
                val server = store.checkServer(endpoint, true)
                val id = seedProfile(context, true, server.endpoint, server.serverId)
                store.activate(id)
                store.mutate(store.currentLease(), addSong())
                store.reconnect()
                withTimeout(20_000) {
                    while (store.state.value.failure != ProfileFailure.AUTHORIZATION_REJECTED) delay(50)
                }
                store.disconnect()
                store.shutdown()
                store = ProfileStore(context)
                store.initialize()
                assertEquals(
                    ProfileAuthentication.NEEDS_AUTHORIZATION,
                    store.state.value.profiles
                        .first { it.id == id }
                        .authentication,
                )
                assertTrue(ids(store).contains("public-song"))
                assertEquals(ProfileSyncState.OFFLINE, store.state.value.sync)
            } finally {
                store.shutdown()
            }
        }

    @Test
    fun nativeClientSynchronizesThroughProxyAndRecoversWithoutReplayingUncertainWrite() =
        runBlocking {
            val context = instrumentation.targetContext
            check(context.packageName.endsWith(".test"))
            context.databaseList().forEach { context.deleteDatabase(it) }
            val store = ProfileStore(context)
            try {
                store.initialize()
                val server = store.checkServer(endpoint, cleartextConfirmed = true)
                val first = store.addConnection(server, "public-fixture-a".toCharArray(), "Android first")
                val second = store.addConnection(server, "public-fixture-a".toCharArray(), "Android second")
                val anotherUser = store.addConnection(server, "public-fixture-b".toCharArray(), "Android isolated user")
                assertNotEquals(first, second)
                store.activate(first)
                ready(store)
                mutate(store, "android-initial")
                drained(store)
                store.activate(second)
                ready(store)
                assertTrue(ids(store).contains("android-initial"))
                control.post("/connect", "{}")
                control.post("/add", """{"id":"node-live"}""")
                awaitSong(store, "node-live")
                mutate(store, "android-live")
                drained(store)
                control.awaitIds(listOf("android-live"))
                uncertainWrite(store)
                offlineMerge(store)
                isolateOtherAccount(store, second, anotherUser)
                val failure = runCatching { store.checkServer("https://10.0.2.2:19530", false) }.exceptionOrNull()
                assertEquals(ProfileFailure.TLS, (failure as ProfileException).failure)
            } finally {
                store.shutdown()
            }
        }

    private suspend fun uncertainWrite(store: ProfileStore) {
        val before = control.counter("clientActions")
        control.post("/drop-reply", "{}")
        mutate(store, "uncertain-once")
        withTimeout(20_000) { while (control.counter("droppedReplies") < 1) delay(100) }
        ready(store)
        drained(store)
        assertEquals(
            "Must reconcile instead of replaying the committed action",
            before + 1,
            control.counter("clientActions"),
        )
        assertEquals(1, ids(store).count { it == "uncertain-once" })
        control.awaitIds(listOf("uncertain-once"))
        assertEquals(1, control.ids().count { it == "uncertain-once" })
    }

    private suspend fun offlineMerge(store: ProfileStore) {
        store.disconnect()
        control.post("/add", """{"id":"node-while-offline"}""")
        mutate(store, "android-while-offline")
        assertTrue(
            store
                .currentLease()
                .database
                .library()
                .pending()
                .isNotEmpty(),
        )
        store.reconnect()
        ready(store)
        drained(store)
        assertTrue(ids(store).containsAll(listOf("node-while-offline", "android-while-offline")))
        control.awaitIds(listOf("node-while-offline", "android-while-offline"))
    }

    private suspend fun isolateOtherAccount(
        store: ProfileStore,
        first: ProfileId,
        other: ProfileId,
    ) {
        val stale = ProfileSyncStorage(store, store.currentLease())
        store.activate(other)
        ready(store)
        assertTrue(ids(store).isEmpty())
        assertEquals(
            ProfileFailure.STALE_OPERATION,
            (
                runCatching {
                    stale.apply(addSong())
                }.exceptionOrNull() as ProfileException
            ).failure,
        )
        store.activate(first)
        ready(store)
        assertTrue(ids(store).contains("android-live"))
    }

    private suspend fun ready(store: ProfileStore) {
        withTimeout(45_000) {
            while (store.state.value.sync != ProfileSyncState.READY) {
                if (store.state.value.sync == ProfileSyncState.AWAITING_CHOICE) {
                    store.chooseInitialSync(InitialSyncChoice.MERGE_DEVICE_FIRST)
                }
                if (store.state.value.sync == ProfileSyncState.FAILED &&
                    store.state.value.failure != ProfileFailure.NETWORK
                ) {
                    error("Sync failed: ${store.state.value.failure}")
                }
                delay(50)
            }
        }
    }

    private suspend fun drained(store: ProfileStore) {
        withTimeout(20_000) {
            while (store
                    .currentLease()
                    .database
                    .library()
                    .pending()
                    .isNotEmpty()
            ) {
                delay(50)
            }
        }
    }

    private suspend fun ids(store: ProfileStore): List<String> =
        store.currentLease().database.readSnapshot().value.getValue("defaultList").jsonArray.map {
            it.jsonObject
                .getValue("id")
                .jsonPrimitive.content
        }

    private suspend fun awaitSong(
        store: ProfileStore,
        id: String,
    ) {
        withTimeout(20_000) { while (id !in ids(store)) delay(50) }
    }

    private suspend fun mutate(
        store: ProfileStore,
        id: String,
    ) {
        val action = addSong().jsonObject
        val data = action.getValue("data").jsonObject
        val info =
            data
                .getValue("musicInfos")
                .jsonArray
                .single()
                .jsonObject
        store.mutate(
            store.currentLease(),
            JsonObject(
                action + (
                    "data" to
                        JsonObject(
                            data +
                                ("musicInfos" to JsonArray(listOf(JsonObject(info + ("id" to JsonPrimitive(id)))))),
                        )
                ),
            ),
        )
    }
}
