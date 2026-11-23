package cn.lux.music.feature.state

import cn.lux.music.feature.model.LuxMockCatalog
import kotlin.random.Random

internal fun LuxUiState.withToast(message: String): LuxUiState = copy(toastMessage = message, toastSeq = toastSeq + 1)

/** Pure deterministic state reducer matching `LuxCore` + `netease-lux` in `ui/index.html`. */
internal object LuxUiReducer {
    fun reduce(
        state: LuxUiState,
        event: LuxUiEvent,
        randomInt: (Int) -> Int = { bound -> if (bound <= 1) 0 else Random.nextInt(bound) },
        nowMillis: () -> Long = { System.currentTimeMillis() },
    ): LuxUiState =
        reduceNavigation(state, event)
            ?: LuxPlaybackReducer.reduce(state, event, randomInt)
            ?: LuxLibraryReducer.reduce(state, event, nowMillis)
            ?: LuxSettingsReducer.reduce(state, event)
            ?: state

    private fun reduceNavigation(
        state: LuxUiState,
        event: LuxUiEvent,
    ): LuxUiState? =
        when (event) {
            is LuxUiEvent.SelectTab -> selectTab(state, event.tab)
            LuxUiEvent.GoDiscover -> state.copy(tab = LuxTab.Discover, stack = emptyList())
            LuxUiEvent.GoRadio -> goRadio(state)
            LuxUiEvent.StartRadio -> startRadio(state)
            is LuxUiEvent.PushPage -> pushPage(state, event.page)
            LuxUiEvent.PopPage -> state.copy(stack = state.stack.dropLast(1), versionModalOpen = false)
            LuxUiEvent.SystemBack -> reduceSystemBack(state)
            else -> null
        }

    private fun selectTab(
        state: LuxUiState,
        tab: LuxTab,
    ): LuxUiState =
        if (tab == state.tab && state.stack.isEmpty()) {
            val nextEpoch = (state.scrollToTopEpoch[tab] ?: 0) + 1
            state.copy(scrollToTopEpoch = state.scrollToTopEpoch + (tab to nextEpoch))
        } else {
            state.copy(tab = tab, stack = emptyList(), playlistSortMenuOpen = false)
        }

    private fun goRadio(state: LuxUiState): LuxUiState {
        val base = state.copy(tab = LuxTab.Roam, stack = emptyList())
        return if (base.fromName == "无尽电台") base else startRadio(base)
    }

    private fun pushPage(
        state: LuxUiState,
        page: LuxSubPage,
    ): LuxUiState {
        val draft =
            when (page) {
                is LuxSubPage.RenamePlaylist ->
                    state.playlists
                        .firstOrNull { it.id == page.playlistId }
                        ?.name
                        .orEmpty()
                LuxSubPage.SmartPlaylist, LuxSubPage.NewPlaylist -> ""
                else -> state.playlistNameDraft
            }
        return state.copy(
            stack = state.stack + page,
            playlistNameDraft = draft,
            versionModalOpen = false,
        )
    }

    private fun reduceSystemBack(state: LuxUiState): LuxUiState =
        when {
            state.queueOpen -> state.copy(queueOpen = false)
            state.versionModalOpen -> state.copy(versionModalOpen = false)
            state.playerOpen && state.playerView != LuxPlayerView.Vinyl ->
                state.copy(playerView = LuxPlayerView.Vinyl)
            state.playerOpen -> state.copy(playerOpen = false, moreSheetOpen = false)
            state.stack.isNotEmpty() -> state.copy(stack = state.stack.dropLast(1))
            else -> state
        }

    private fun startRadio(state: LuxUiState): LuxUiState =
        state.copy(
            queue = LuxMockCatalog.radioIds,
            queueIndex = 0,
            fromName = "无尽电台",
            positionSec = 0.0,
            playing = true,
        )
}
