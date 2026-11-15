package cn.lux.music.data.profile

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "profiles")
internal data class ProfileRecord(
    @PrimaryKey val id: String,
    val name: String,
    val endpoint: String? = null,
    val serverId: String? = null,
    val bindingId: String? = null,
    val deviceName: String = "",
    val cleartextConfirmed: Boolean = false,
    val credentials: ByteArray? = null,
    val deleting: Boolean = false,
    val syncEnabled: Boolean = true,
    val authorizationRequired: Boolean = false,
)

@Entity(tableName = "active_profile")
internal data class ActiveProfileRecord(
    @PrimaryKey val singleton: Int = 0,
    val profileId: String,
)

@Dao
internal interface ProfileCatalogDao {
    @Query("SELECT * FROM profiles ORDER BY rowid")
    suspend fun all(): List<ProfileRecord>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun find(id: String): ProfileRecord?

    @Upsert
    suspend fun save(record: ProfileRecord)

    @Query("SELECT profileId FROM active_profile WHERE singleton = 0")
    suspend fun activeId(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun activate(record: ActiveProfileRecord)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [ProfileRecord::class, ActiveProfileRecord::class], version = 1, exportSchema = true)
internal abstract class ProfileCatalogDatabase : RoomDatabase() {
    abstract fun profiles(): ProfileCatalogDao
}

/** One canonical LX snapshot, not a duplicate of domain tables or a second UI state store. */
@Entity(tableName = "library_snapshot")
internal data class LibrarySnapshotRecord(
    @PrimaryKey val singleton: Int = 0,
    val revision: Long = 0,
)

@Entity(tableName = "sync_outbox")
internal data class OutboxRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val revision: Long,
)

@Entity(tableName = "wire_chunks", primaryKeys = ["owner", "position"])
internal data class WireChunk(
    val owner: String,
    val position: Int,
    val text: String,
)

@Dao
internal interface ProfileLibraryDao {
    @Query("SELECT * FROM library_snapshot WHERE singleton = 0")
    suspend fun snapshot(): LibrarySnapshotRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(record: LibrarySnapshotRecord)

    @Insert
    suspend fun enqueue(record: OutboxRecord): Long

    @Query("SELECT * FROM wire_chunks WHERE owner = :owner ORDER BY position")
    suspend fun chunks(owner: String): List<WireChunk>

    @Query("DELETE FROM wire_chunks WHERE owner = :owner")
    suspend fun clearChunks(owner: String)

    @Insert
    suspend fun insertChunks(chunks: List<WireChunk>)

    @Query("SELECT * FROM sync_outbox ORDER BY id")
    suspend fun pending(): List<OutboxRecord>

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun acknowledge(id: Long)

    @Query("DELETE FROM sync_outbox WHERE revision <= :revision")
    suspend fun acknowledgeThrough(revision: Long)
}

@Database(
    entities = [LibrarySnapshotRecord::class, OutboxRecord::class, WireChunk::class],
    version = 1,
    exportSchema = true,
)
internal abstract class ProfileLibraryDatabase : RoomDatabase() {
    abstract fun library(): ProfileLibraryDao
}
