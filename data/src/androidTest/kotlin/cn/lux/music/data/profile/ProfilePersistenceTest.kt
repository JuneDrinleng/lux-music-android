package cn.lux.music.data.profile

import android.content.Context
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.lux.music.api.ProfileException
import cn.lux.music.data.sync.lx.LxDeviceCredentials
import cn.lux.music.data.sync.lx.LxJson
import cn.lux.music.data.sync.lx.LxSnapshot
import cn.lux.music.model.ProfileAuthentication
import cn.lux.music.model.ProfileFailure
import cn.lux.music.model.ProfileId
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
internal class ProfilePersistenceTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var store: ProfileStore

    @Before
    fun prepare() =
        runBlocking {
            check(context.packageName.endsWith(".test"))
            context.databaseList().forEach { context.deleteDatabase(it) }
            store = ProfileStore(context)
            store.initialize()
        }

    @After
    fun close() = runBlocking { store.shutdown() }

    @Test
    fun bootstrapIsPersistentAndProfilesNeverShareDatabase() =
        runBlocking {
            val local = store.state.value.activeId
            store.initialize()
            assertEquals(1, store.state.value.profiles.size)
            val first = seedProfile(context)
            val second = seedProfile(context)
            store.activate(first)
            store.mutate(store.currentLease(), addSong())
            assertEquals(
                1,
                store
                    .currentLease()
                    .database
                    .library()
                    .pending()
                    .size,
            )
            store.activate(second)
            assertEquals(
                EMPTY_LIBRARY,
                store
                    .currentLease()
                    .database
                    .readSnapshot()
                    .canonical(),
            )
            store.activate(first)
            assertTrue(
                store
                    .currentLease()
                    .database
                    .readSnapshot()
                    .canonical()
                    .contains("public-song"),
            )
            store.activate(local!!)
            assertEquals(
                EMPTY_LIBRARY,
                store
                    .currentLease()
                    .database
                    .readSnapshot()
                    .canonical(),
            )
        }

    @Test
    fun lateWritesAfterAtoBtoCandDisconnectAreRejected() =
        runBlocking {
            val first = seedProfile(context)
            val second = seedProfile(context)
            store.activate(first)
            val old = store.currentLease()
            val remote = ProfileSyncStorage(store, old)
            store.activate(second)
            val error = runCatching { remote.apply(addSong()) }.exceptionOrNull() as ProfileException
            assertEquals(ProfileFailure.STALE_OPERATION, error.failure)
            assertEquals(
                EMPTY_LIBRARY,
                store
                    .currentLease()
                    .database
                    .readSnapshot()
                    .canonical(),
            )
            store.activate(first)
            val beforeDisconnect = store.currentLease()
            store.disconnect()
            assertEquals(
                ProfileFailure.STALE_OPERATION,
                (
                    runCatching {
                        store.mutate(
                            beforeDisconnect,
                            addSong(),
                        )
                    }.exceptionOrNull() as ProfileException
                ).failure,
            )
        }

    @Test
    fun roomRollsBackSnapshotChunksAndOutboxTogether() =
        runBlocking {
            val id = seedProfile(context)
            store.activate(id)
            val database = store.currentLease().database
            val revision = database.library().snapshot()!!.revision
            try {
                database.withTransaction {
                    database.writeSnapshot(
                        """{"defaultList":[{"id":"uncommitted"}],"loveList":[],"userList":[]}""",
                        """{"action":"public"}""",
                    )
                    throw IOException("injected transaction failure")
                }
            } catch (_: IOException) {
            }
            assertEquals(EMPTY_LIBRARY, database.readSnapshot().canonical())
            assertEquals(revision, database.library().snapshot()!!.revision)
            assertTrue(database.library().pending().isEmpty())
        }

    @Test
    fun largeUnicodeSnapshotsSurviveSqliteCursorLimits() =
        runBlocking {
            val database = store.currentLease().database
            val original =
                LxSnapshot.from(
                    JsonObject(
                        LxSnapshot.parse(EMPTY_LIBRARY).value +
                            ("futureMetadata" to JsonPrimitive("中🎵".repeat(350_000))),
                    ),
                )
            database.writeSnapshot(original.canonical(), localAction = null)
            assertEquals(original.canonical(), database.readSnapshot().canonical())
            assertTrue(database.library().chunks("snapshot").size > 20)
        }

    @Test
    fun remoteMutationDoesNotEchoAndSignoutPreservesLibrary() =
        runBlocking {
            val id = seedProfile(context, credentials = true)
            seedProfile(context)
            store.activate(id)
            val lease = store.currentLease()
            ProfileSyncStorage(store, lease).apply(addSong())
            assertTrue(
                lease.database
                    .library()
                    .pending()
                    .isEmpty(),
            )
            val order =
                store.state.value.profiles
                    .map { it.id }
            store.signOut(id)
            assertEquals(
                order,
                store.state.value.profiles
                    .map { it.id },
            )
            assertEquals(
                ProfileAuthentication.NEEDS_AUTHORIZATION,
                store.state.value.profiles
                    .first { it.id == id }
                    .authentication,
            )
            assertTrue(
                store
                    .currentLease()
                    .database
                    .readSnapshot()
                    .canonical()
                    .contains("public-song"),
            )
            store.delete(id)
            assertFalse(
                store.state.value.profiles
                    .any { it.id == id },
            )
            assertFalse(context.getDatabasePath("profile-${id.value}.db").exists())
        }

    @Test
    fun openingFailedWorkspaceLeavesOldProfileActive() =
        runBlocking {
            val original = store.state.value.activeId
            val target = seedProfile(context)
            check(context.getDatabasePath("profile-${target.value}.db").mkdirs())
            assertEquals(
                ProfileFailure.STORAGE,
                (runCatching { store.activate(target) }.exceptionOrNull() as ProfileException).failure,
            )
            assertEquals(original, store.state.value.activeId)
            assertEquals(
                EMPTY_LIBRARY,
                store
                    .currentLease()
                    .database
                    .readSnapshot()
                    .canonical(),
            )
        }

    @Test
    fun lostKeystoreKeyRequiresAuthorizationButKeepsSongs() =
        runBlocking {
            val id = seedProfile(context, credentials = true)
            store.activate(id)
            store.mutate(store.currentLease(), addSong())
            java.security.KeyStore
                .getInstance(
                    "AndroidKeyStore",
                ).apply { load(null) }
                .deleteEntry("lux.profile.credentials.v1")
            store.reconnect()
            assertEquals(
                ProfileAuthentication.NEEDS_AUTHORIZATION,
                store.state.value.profiles
                    .first { it.id == id }
                    .authentication,
            )
            assertEquals(ProfileFailure.CREDENTIALS_UNAVAILABLE, store.state.value.failure)
            assertTrue(
                store
                    .currentLease()
                    .database
                    .readSnapshot()
                    .canonical()
                    .contains("public-song"),
            )
        }

    @Test
    fun keystoreNonceAndProfileAadPreventCiphertextSubstitution() {
        val vault = ProfileVault()
        val first = ProfileId(UUID.randomUUID().toString())
        val second = ProfileId(UUID.randomUUID().toString())
        val credentials = publicCredentials()
        val ciphertext = vault.encrypt(first, credentials)
        assertEquals(credentials.clientId, vault.decrypt(first, ciphertext).clientId)
        assertFalse(ciphertext.contentEquals(vault.encrypt(first, credentials)))
        assertTrue(runCatching { vault.decrypt(second, ciphertext) }.isFailure)
        val damaged = ciphertext.copyOf().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
        assertTrue(runCatching { vault.decrypt(first, damaged) }.isFailure)
    }
}

internal fun publicCredentials(): LxDeviceCredentials =
    LxDeviceCredentials("public-fixture-device", "MDEyMzQ1Njc4OWFiY2RlZg==", "Fixture LX")

internal suspend fun seedProfile(
    context: Context,
    credentials: Boolean = false,
    endpoint: String = "http://127.0.0.1:9",
    serverId: String = "public-server",
): ProfileId {
    val id = ProfileId(UUID.randomUUID().toString())
    val catalog = ProfileFiles(context).catalog
    try {
        catalog.profiles().save(
            ProfileRecord(
                id.value,
                "Fixture LX",
                endpoint = endpoint,
                serverId = serverId,
                bindingId = UUID.randomUUID().toString(),
                cleartextConfirmed = true,
                credentials = if (credentials) ProfileVault().encrypt(id, publicCredentials()) else null,
                syncEnabled = false,
            ),
        )
    } finally {
        catalog.close()
    }
    return id
}

internal fun addSong() =
    LxJson.parse(
        """
        {"action":"list_music_add","data":{"id":"default","musicInfos":[{
          "id":"public-song","name":"Fixture","singer":"Fixture","source":"kw","interval":null,
          "meta":{"songId":"1","albumName":"","future":true}
        }],"addMusicLocationType":"bottom"}}
        """.trimIndent(),
    )
