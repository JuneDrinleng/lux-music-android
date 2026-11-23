package cn.lux.music.feature.state

import androidx.compose.runtime.Immutable
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.model.LuxMockLyrics
import cn.lux.music.feature.model.PreviewLyricLine
import cn.lux.music.feature.model.PreviewPlaylist
import cn.lux.music.feature.model.PreviewSong
import cn.lux.music.feature.model.ResolvedSongList

internal enum class LuxTab(
    val id: String,
    val label: String,
    val icon: LuxIcon,
    val iconOn: LuxIcon,
) {
    Home("home", "推荐", LuxIcon.Home, LuxIcon.HomeOn),
    Discover("discover", "发现", LuxIcon.Compass, LuxIcon.CompassOn),
    Roam("roam", "电台", LuxIcon.Radio, LuxIcon.RadioOn),
    Mine("library", "我的", LuxIcon.Account, LuxIcon.AccountOn),
}

internal enum class LuxPlayMode(
    val icon: LuxIcon,
    val label: String,
) {
    ListLoop(LuxIcon.Repeat, "列表循环"),
    Shuffle(LuxIcon.Shuffle, "随机播放"),
    Order(LuxIcon.PlaylistPlay, "顺序播放"),
    SingleLoop(LuxIcon.RepeatOnce, "单曲循环"),
}

internal enum class LuxPlayerView {
    Vinyl,
    Lyrics,
    Comments,
}

@Immutable
internal sealed interface LuxSubPage {
    data class ListDetail(
        val key: String,
    ) : LuxSubPage

    data object Search : LuxSubPage

    data object Settings : LuxSubPage

    data class Option(
        val id: String,
    ) : LuxSubPage

    data object Account : LuxSubPage

    data object Stats : LuxSubPage

    data object Together : LuxSubPage

    data object Cache : LuxSubPage

    data object Sync : LuxSubPage

    data object Version : LuxSubPage

    data object Fx : LuxSubPage

    data object CustomSrc : LuxSubPage

    data object Room : LuxSubPage

    data object Conflict : LuxSubPage

    data object Export : LuxSubPage

    data object Import : LuxSubPage

    data object Github : LuxSubPage

    data object NewPlaylist : LuxSubPage

    data object SmartPlaylist : LuxSubPage

    data class RenamePlaylist(
        val playlistId: String,
    ) : LuxSubPage

    data class DeletePlaylist(
        val playlistId: String,
    ) : LuxSubPage

    data class AddToPlaylist(
        val songId: String,
    ) : LuxSubPage
}

@Immutable
internal data class SearchPlaylistHit(
    val kind: String,
    val id: String,
    val name: String,
    val cover: String,
    val songCount: Int,
    val source: String,
)

@Immutable
internal data class LuxUiState(
    val tab: LuxTab = LuxTab.Home,
    val scrollToTopEpoch: Map<LuxTab, Int> = emptyMap(),
    val stack: List<LuxSubPage> = emptyList(),
    val searchQuery: String = "",
    val searchSource: String = "all",
    val searchTab: String = "song",
    val searchSourceMenuOpen: Boolean = false,
    val searchHistory: List<String> = LuxMockCatalog.initialHistory,
    val nick: String = "小满",
    val sign: String = "晚风里慢慢走",
    val lang: String = "简体中文",
    val theme: String = "跟随系统",
    val defaultSearchSource: String = "全部来源",
    val statsStyle: String = "杂志风",
    val swDefaultTr: Boolean = true,
    val swGapless: Boolean = true,
    val swHq: Boolean = false,
    val swCellular: Boolean = false,
    val playlists: List<PreviewPlaylist> = LuxMockCatalog.initialPlaylists,
    val likedIds: Set<String> =
        LuxMockCatalog.initialPlaylists
            .filter { it.liked }
            .flatMap { it.songs }
            .toSet(),
    val queue: List<String> = initialCommutePlaylist().songs,
    val queueIndex: Int = 0,
    val fromName: String = initialCommutePlaylist().name,
    val positionSec: Double = 31.0,
    val playing: Boolean = false,
    val playMode: LuxPlayMode = LuxPlayMode.ListLoop,
    val playerOpen: Boolean = false,
    val playerView: LuxPlayerView = LuxPlayerView.Vinyl,
    val showTranslation: Boolean = true,
    val showRomanization: Boolean = false,
    val queueOpen: Boolean = false,
    val commentSort: String = "hot",
    val commentLikedKeys: Set<String> = emptySet(),
    val moreSheetOpen: Boolean = false,
    val sleepTimer: String = "",
    val playlistView: String = "list",
    val playlistSort: String = "最近更新",
    val playlistSortMenuOpen: Boolean = false,
    val settingsQuery: String = "",
    val accountMode: String = "local",
    val syncConnected: Boolean = false,
    val roomLayout: String = "列表",
    val versionModalOpen: Boolean = false,
    val statsPeriod: String = "月",
    val statsChartKind: String = "line",
    val playlistMode: String = "manual",
    val playlistNameDraft: String = "",
    val smartRule: String = "liked",
    val smartName: String = "智能收藏",
    val toastMessage: String? = null,
    val toastSeq: Int = 0,
) {
    val currentSong: PreviewSong
        get() =
            LuxMockCatalog.songById[queue.getOrNull(queueIndex)]
                ?: LuxMockCatalog.songs.first()

    val currentLyrics: List<PreviewLyricLine>
        get() = LuxMockLyrics.forSong(currentSong.id)

    val hasTranslation: Boolean
        get() = currentLyrics.any { it.translation != null }

    val hasRomanization: Boolean
        get() = currentLyrics.any { it.romanization != null }

    val orderedLikedSongIds: List<String>
        get() = LuxMockCatalog.songs.filter { it.id in likedIds }.map { it.id }

    val myPlaylists: List<PreviewPlaylist>
        get() = playlists.filterNot { it.liked }

    val sortedMyPlaylists: List<PreviewPlaylist>
        get() =
            when (playlistSort) {
                "名称" -> myPlaylists.sortedBy { it.name }
                "歌曲数" -> myPlaylists.sortedByDescending { it.songs.size }
                else -> myPlaylists
            }

    fun searchSongIds(): List<String> {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return LuxMockCatalog.songs
            .filter { song ->
                val haystack = (song.title + song.artist + song.album).lowercase()
                haystack.contains(q) && (searchSource == "all" || searchSource in song.sources)
            }.map { it.id }
    }

    fun searchPlaylists(): List<SearchPlaylistHit> {
        val q = searchQuery.trim().lowercase()
        val hotHits =
            LuxMockCatalog.hotPlaylists
                .filter { q.isEmpty() || it.name.lowercase().contains(q) }
                .map { SearchPlaylistHit("hot", it.id, it.name, it.cover, it.songs.size, it.source) }
        val userHits =
            myPlaylists
                .filter { q.isEmpty() || it.name.lowercase().contains(q) }
                .map {
                    SearchPlaylistHit(
                        kind = "pl",
                        id = it.id,
                        name = it.name,
                        cover = playlistCover(it),
                        songCount = it.songs.size,
                        source = "",
                    )
                }
        return hotHits + userHits
    }

    fun playlistCover(pl: PreviewPlaylist): String =
        pl.cover ?: LuxMockCatalog.songById[pl.songs.firstOrNull()]?.cover ?: "c1"

    fun resolveList(key: String): ResolvedSongList {
        val parts = key.split(":")
        val prefix = parts.first()
        val id = parts.getOrNull(1).orEmpty()
        return when (prefix) {
            "pl" -> resolveUserPlaylist(key, id)
            "hot" -> resolveHotPlaylist(key, id)
            "board" -> resolveBoard(key, id, parts.getOrNull(2))
            else -> resolveBuiltInList(key, prefix)
        }
    }

    private fun resolveBuiltInList(
        key: String,
        prefix: String,
    ): ResolvedSongList =
        when (prefix) {
            "daily" -> makeList(key, "每日推荐", LuxMockCatalog.dailyIds, "11月13日 · 根据你的口味生成", "每日推荐")
            "liked" -> makeList(key, "我喜欢的音乐", orderedLikedSongIds, "${orderedLikedSongIds.size} 首", "收藏")
            "collected" -> makeList(key, "我的收藏", orderedLikedSongIds, "${orderedLikedSongIds.size} 首", "收藏")
            "local" -> makeList(key, "本地音乐", LuxMockCatalog.localIds, "${LuxMockCatalog.localIds.size} 首 · 已下载", "本地")
            "history" ->
                makeList(
                    key,
                    "最近播放",
                    LuxMockCatalog.recentIds,
                    "${LuxMockCatalog.recentIds.size} 首 · 本周",
                    "历史",
                )
            "search" -> {
                val ids = searchSongIds()
                makeList(key, "搜索：$searchQuery", ids, "${ids.size} 首", "搜索")
            }
            "weekly" ->
                makeList(
                    key,
                    "本周发现",
                    LuxMockCatalog.weeklyIds,
                    "每周一更新 · ${LuxMockCatalog.weeklyIds.size} 首",
                    "本周发现",
                )
            "radio" -> makeList(key, "无尽电台", LuxMockCatalog.radioIds, "根据今日推荐连续播放", "无尽电台")
            else -> makeList(key, "歌单", emptyList(), "0 首", "歌单")
        }

    private fun makeList(
        key: String,
        name: String,
        ids: List<String>,
        meta: String,
        kind: String,
    ): ResolvedSongList = ResolvedSongList(key, name, ids, coverOf(ids), meta, kind)

    private fun resolveUserPlaylist(
        key: String,
        id: String,
    ): ResolvedSongList {
        val pl = playlists.firstOrNull { it.id == id }
        val ids = pl?.songs.orEmpty()
        return ResolvedSongList(
            key = key,
            name = pl?.name ?: "我的歌单",
            songIds = ids,
            cover = pl?.let(::playlistCover) ?: "c1",
            meta = "$nick · ${ids.size} 首",
            kind = "我的歌单",
        )
    }

    private fun resolveHotPlaylist(
        key: String,
        id: String,
    ): ResolvedSongList {
        val hot = LuxMockCatalog.hotPlaylists.firstOrNull { it.id == id } ?: LuxMockCatalog.hotPlaylists.first()
        return ResolvedSongList(
            key = key,
            name = hot.name,
            songIds = hot.songs,
            cover = hot.cover,
            meta = "${LuxMockCatalog.sourceName(hot.source)} · ${hot.songs.size} 首",
            kind = "推荐歌单",
        )
    }

    private fun resolveBoard(
        key: String,
        id: String,
        sourceFilter: String?,
    ): ResolvedSongList {
        val board = LuxMockCatalog.boards.firstOrNull { it.id == id } ?: LuxMockCatalog.boards.first()
        if (!sourceFilter.isNullOrEmpty()) {
            val pool = LuxMockCatalog.pick(board.seed, 20)
            val filtered =
                pool
                    .filter { sid -> sourceFilter in (LuxMockCatalog.songById[sid]?.sources.orEmpty()) }
                    .toMutableList()
            for (sid in pool) {
                if (filtered.size >= 10) break
                if (sid !in filtered) filtered.add(sid)
            }
            val ids = filtered.take(10)
            val label = LuxMockCatalog.sourceName(sourceFilter)
            return ResolvedSongList(
                key,
                "${board.name} · $label",
                ids,
                coverOf(ids),
                "$label · 今日更新 · 前 ${ids.size}",
                "排行榜",
            )
        }
        val ids = LuxMockCatalog.pick(board.seed, 10)
        return ResolvedSongList(key, board.name, ids, coverOf(ids), "每日更新 · 前 10", "排行榜")
    }

    private fun coverOf(ids: List<String>): String = LuxMockCatalog.songById[ids.firstOrNull()]?.cover ?: "c1"
}

private fun initialCommutePlaylist(): PreviewPlaylist =
    LuxMockCatalog.initialPlaylists.firstOrNull { it.id == "p2" }
        ?: LuxMockCatalog.initialPlaylists.first()
