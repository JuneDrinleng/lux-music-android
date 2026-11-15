package cn.lux.music.data.profile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cn.lux.music.model.ProfileAuthentication
import cn.lux.music.model.ProfileSyncState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Host runs prepare, force-stops this test package, then runs recover in a new process. */
@RunWith(AndroidJUnit4::class)
internal class ProfileRestartTest {
    @Test
    fun prepare() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            check(context.packageName.endsWith(".test"))
            context.databaseList().forEach { context.deleteDatabase(it) }
            val store = ProfileStore(context)
            store.initialize()
            val id = seedProfile(context, credentials = true)
            store.activate(id)
            store.mutate(store.currentLease(), addSong())
            assertEquals(ProfileSyncState.OFFLINE, store.state.value.sync)
            // Intentionally leave the Room handles open; the host kills the application process.
        }

    @Test
    fun recover() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val store = ProfileStore(context)
            try {
                store.initialize()
                val lease = store.currentLease()
                assertEquals(ProfileSyncState.OFFLINE, store.state.value.sync)
                assertTrue(
                    lease.database
                        .readSnapshot()
                        .canonical()
                        .contains("public-song"),
                )
                assertEquals(
                    1,
                    lease.database
                        .library()
                        .pending()
                        .size,
                )
                val catalog = ProfileFiles(context).catalog
                try {
                    val record = catalog.profiles().find(lease.id.value)!!
                    assertEquals(
                        "public-fixture-device",
                        ProfileVault().decrypt(lease.id, record.credentials!!).clientId,
                    )
                    assertEquals(
                        ProfileAuthentication.AVAILABLE,
                        store.state.value.profiles
                            .first { it.id == lease.id }
                            .authentication,
                    )
                } finally {
                    catalog.close()
                }
            } finally {
                store.shutdown()
            }
        }
}
