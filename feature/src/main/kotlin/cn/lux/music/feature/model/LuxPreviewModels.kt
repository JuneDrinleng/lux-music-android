package cn.lux.music.feature.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class PreviewSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val cover: String,
    val lang: String,
    val sources: List<String>,
)

@Immutable
internal data class PreviewPlaylist(
    val id: String,
    val name: String,
    val cover: String?,
    val liked: Boolean = false,
    val songs: List<String>,
)

@Immutable
internal data class PreviewHotPlaylist(
    val id: String,
    val name: String,
    val source: String,
    val cover: String,
    val songs: List<String>,
)

@Immutable
internal data class PreviewBoard(
    val id: String,
    val name: String,
    val seed: Int,
)

@Immutable
internal data class PreviewLyricLine(
    val timeSec: Double,
    val text: String,
    val translation: String? = null,
    val romanization: String? = null,
)

@Immutable
internal data class PreviewComment(
    val key: String,
    val author: String,
    val timeLabel: String,
    val body: String,
    val baseLikes: Int,
)

@Immutable
internal data class ResolvedSongList(
    val key: String,
    val name: String,
    val songIds: List<String>,
    val cover: String,
    val meta: String,
    val kind: String,
)
