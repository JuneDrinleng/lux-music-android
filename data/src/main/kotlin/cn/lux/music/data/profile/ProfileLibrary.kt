package cn.lux.music.data.profile

import androidx.room.withTransaction
import cn.lux.music.data.sync.lx.LxSnapshot

internal const val EMPTY_LIBRARY = "{\"defaultList\":[],\"loveList\":[],\"userList\":[]}"

/** Smaller than CursorWindow's row budget; preserve UTF-16 pairs at chunk boundaries. */
internal fun wireChunks(
    owner: String,
    wire: String,
): List<WireChunk> {
    require(wire.toByteArray(Charsets.UTF_8).size <= 32 * 1024 * 1024)
    val result = mutableListOf<WireChunk>()
    var start = 0
    while (start < wire.length) {
        var end = minOf(start + 16_384, wire.length)
        if (end < wire.length && wire[end - 1].isHighSurrogate() && wire[end].isLowSurrogate()) end--
        result += WireChunk(owner, result.size, wire.substring(start, end))
        start = end
    }
    return result
}

internal suspend fun ProfileLibraryDatabase.readSnapshot(): LxSnapshot =
    withTransaction {
        val text = library().chunks("snapshot").joinToString("") { it.text }
        check(library().snapshot() != null && text.isNotEmpty()) { "Missing library snapshot" }
        LxSnapshot.parse(text)
    }

/** Caller owns the profile-generation barrier. Mutation and pending intent share this transaction. */
internal suspend fun ProfileLibraryDatabase.writeSnapshot(
    wire: String,
    localAction: String?,
): Long =
    withTransaction {
        val snapshot = LxSnapshot.parse(wire).canonical()
        val revision = (library().snapshot()?.revision ?: 0) + 1
        library().clearChunks("snapshot")
        library().insertChunks(wireChunks("snapshot", snapshot))
        library().save(LibrarySnapshotRecord(revision = revision))
        if (localAction != null) {
            val id = library().enqueue(OutboxRecord(revision = revision))
            library().insertChunks(wireChunks("outbox:$id", localAction))
        }
        revision
    }

internal suspend fun ProfileLibraryDatabase.clearOutboxThrough(revision: Long) =
    withTransaction {
        library().pending().filter { it.revision <= revision }.forEach { library().clearChunks("outbox:${it.id}") }
        library().acknowledgeThrough(revision)
    }
