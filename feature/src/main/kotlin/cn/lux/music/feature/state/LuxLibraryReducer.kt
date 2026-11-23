package cn.lux.music.feature.state

import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.model.PreviewPlaylist

internal object LuxLibraryReducer {
    fun reduce(
        state: LuxUiState,
        event: LuxUiEvent,
        nowMillis: () -> Long,
    ): LuxUiState? =
        reduceSearch(state, event)
            ?: reducePlaylistUi(state, event)
            ?: reducePlaylistMutations(state, event, nowMillis)

    private fun reduceSearch(
        state: LuxUiState,
        event: LuxUiEvent,
    ): LuxUiState? =
        when (event) {
            is LuxUiEvent.UpdateSearchQuery -> state.copy(searchQuery = event.query)
            LuxUiEvent.ClearSearch -> state.copy(searchQuery = "")
            is LuxUiEvent.PickSearchWord -> pickWord(state, event.word)
            is LuxUiEvent.DeleteSearchWord ->
                state.copy(searchHistory = state.searchHistory.filterNot { it == event.word })
            LuxUiEvent.ClearSearchHistory -> state.copy(searchHistory = emptyList())
            is LuxUiEvent.SetSearchTab -> state.copy(searchTab = event.tab)
            LuxUiEvent.ToggleSearchSourceMenu ->
                state.copy(searchSourceMenuOpen = !state.searchSourceMenuOpen)
            is LuxUiEvent.SetSearchSource ->
                state.copy(searchSource = event.source, searchSourceMenuOpen = false)
            else -> null
        }

    private fun pickWord(
        state: LuxUiState,
        word: String,
    ): LuxUiState {
        val hist =
            if (word in state.searchHistory) {
                state.searchHistory
            } else {
                (listOf(word) + state.searchHistory).take(6)
            }
        return state.copy(searchQuery = word, searchHistory = hist)
    }

    private fun reducePlaylistUi(
        state: LuxUiState,
        event: LuxUiEvent,
    ): LuxUiState? =
        when (event) {
            is LuxUiEvent.SetPlaylistView -> state.copy(playlistView = event.view)
            LuxUiEvent.TogglePlaylistSortMenu ->
                state.copy(playlistSortMenuOpen = !state.playlistSortMenuOpen)
            is LuxUiEvent.PickPlaylistSort ->
                state.copy(playlistSort = event.sort, playlistSortMenuOpen = false)
            is LuxUiEvent.SetPlaylistMode -> state.copy(playlistMode = event.mode)
            is LuxUiEvent.UpdatePlaylistNameDraft -> state.copy(playlistNameDraft = event.name)
            is LuxUiEvent.SetSmartRule -> state.copy(smartRule = event.rule)
            is LuxUiEvent.SetSmartName -> state.copy(smartName = event.name)
            else -> null
        }

    private fun reducePlaylistMutations(
        state: LuxUiState,
        event: LuxUiEvent,
        nowMillis: () -> Long,
    ): LuxUiState? =
        when (event) {
            LuxUiEvent.CreatePlaylist -> createPlaylist(state, nowMillis)
            LuxUiEvent.CreateSmartPlaylist -> createSmartPlaylist(state, nowMillis)
            is LuxUiEvent.RenamePlaylist -> renamePlaylist(state, event.playlistId)
            is LuxUiEvent.DeletePlaylist ->
                state
                    .copy(playlists = state.playlists.filterNot { it.id == event.playlistId }, stack = emptyList())
                    .withToast("已删除歌单（预览）")
            is LuxUiEvent.AddSongToPlaylist -> addSong(state, event.songId, event.playlistId)
            else -> null
        }

    private fun createPlaylist(
        state: LuxUiState,
        nowMillis: () -> Long,
    ): LuxUiState {
        val name = state.playlistNameDraft.trim().ifEmpty { "新建歌单" }
        val pl = PreviewPlaylist("p_new_${nowMillis()}", name, "c2", false, emptyList())
        return state
            .copy(playlists = state.playlists + pl, playlistNameDraft = "", stack = state.stack.dropLast(1))
            .withToast("已创建「$name」")
    }

    private fun createSmartPlaylist(
        state: LuxUiState,
        nowMillis: () -> Long,
    ): LuxUiState {
        val name = state.smartName.ifEmpty { "智能收藏" }
        val ids =
            when (state.smartRule) {
                "liked" -> state.orderedLikedSongIds
                "history" -> LuxMockCatalog.recentIds
                "local" -> LuxMockCatalog.localIds
                else -> LuxMockCatalog.dailyIds
            }.take(8)
        val pl = PreviewPlaylist("p_smart_${nowMillis()}", name, "c8", false, ids)
        return state
            .copy(playlists = state.playlists + pl, stack = state.stack.dropLast(1))
            .withToast("已创建智能歌单「$name」")
    }

    private fun renamePlaylist(
        state: LuxUiState,
        playlistId: String,
    ): LuxUiState {
        val target = state.playlists.firstOrNull { it.id == playlistId }
        val newName = state.playlistNameDraft.trim().ifEmpty { target?.name.orEmpty() }
        val next = state.playlists.map { if (it.id == playlistId) it.copy(name = newName) else it }
        return state
            .copy(playlists = next, playlistNameDraft = "", stack = state.stack.dropLast(1))
            .withToast("已重命名为「$newName」")
    }

    private fun addSong(
        state: LuxUiState,
        songId: String,
        playlistId: String,
    ): LuxUiState {
        val target = state.playlists.firstOrNull { it.id == playlistId }
        val next =
            state.playlists.map { pl ->
                if (pl.id == playlistId && songId !in pl.songs) pl.copy(songs = pl.songs + songId) else pl
            }
        return state
            .copy(playlists = next, stack = state.stack.dropLast(1))
            .withToast("已加入「${target?.name.orEmpty()}」（预览）")
    }
}
