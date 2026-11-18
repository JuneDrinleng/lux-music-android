package cn.lux.music.data.profile

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import cn.lux.music.model.ProfileId
import java.io.File
import java.util.UUID

internal class ProfileFiles(
    private val context: Context,
) {
    val catalog: ProfileCatalogDatabase =
        Room
            .databaseBuilder(
                context,
                ProfileCatalogDatabase::class.java,
                "profiles.db",
            ).build()

    suspend fun bootstrap(): ProfileId {
        val dao = catalog.profiles()
        catalog.withTransaction {
            if (dao.all().none { it.endpoint == null && !it.deleting }) {
                val local = ProfileRecord(UUID.randomUUID().toString(), "本机资料")
                dao.save(local)
                dao.activate(ActiveProfileRecord(profileId = local.id))
            }
        }
        for (record in dao.all().filter { it.deleting }) {
            try {
                delete(ProfileId(record.id))
                dao.delete(record.id)
            } catch (
                _: Exception,
            ) {
                // Durable deletion marker is retried on next startup.
            }
        }
        val records = dao.all().filterNot { it.deleting }
        val id =
            dao.activeId()?.takeIf { key -> records.any { it.id == key } }
                ?: records.first { it.endpoint == null }.id
        return ProfileId(id)
    }

    private fun safeId(id: ProfileId): String {
        val normalized = UUID.fromString(id.value).toString()
        require(normalized == id.value) { "Non-canonical profile identity" }
        return normalized
    }

    // Close on cancellation, SQL, schema and platform failures before propagating unchanged.
    @Suppress("TooGenericExceptionCaught")
    suspend fun open(id: ProfileId): ProfileLibraryDatabase {
        val key = safeId(id)
        val database =
            Room
                .databaseBuilder(
                    context,
                    ProfileLibraryDatabase::class.java,
                    "profile-$key.db",
                ).build()
        try {
            database.withTransaction {
                if (database.library().snapshot() == null) database.writeSnapshot(EMPTY_LIBRARY, localAction = null)
            }
            return database
        } catch (error: Exception) {
            database.close()
            throw error
        }
    }

    fun delete(id: ProfileId) {
        val key = safeId(id)
        check(context.deleteDatabase("profile-$key.db") || !context.getDatabasePath("profile-$key.db").exists())
        val root = File(context.filesDir, "profiles/$key")
        check(!root.exists() || root.deleteRecursively())
    }
}
