package cn.lux.music.feature.state

internal object LuxPlayerMetaReducer {
    fun reduce(
        state: LuxUiState,
        event: LuxUiEvent,
    ): LuxUiState? =
        when (event) {
            is LuxUiEvent.ToggleLike -> toggleLike(state, event.songId)
            LuxUiEvent.ToggleTranslation -> state.copy(showTranslation = !state.showTranslation)
            LuxUiEvent.ToggleRomanization -> state.copy(showRomanization = !state.showRomanization)
            is LuxUiEvent.ToggleCommentLike -> toggleCommentLike(state, event.key)
            is LuxUiEvent.SetCommentSort -> state.copy(commentSort = event.sort)
            LuxUiEvent.ToggleMoreSheet -> state.copy(moreSheetOpen = !state.moreSheetOpen)
            is LuxUiEvent.SetSleepTimer -> setSleepTimer(state, event.timer)
            else -> null
        }

    private fun toggleLike(
        state: LuxUiState,
        songId: String?,
    ): LuxUiState {
        val id = songId ?: state.currentSong.id
        val nowLiked = id !in state.likedIds
        val nextSet = if (nowLiked) state.likedIds + id else state.likedIds - id
        return state.copy(likedIds = nextSet).withToast(if (nowLiked) "已添加到「我喜欢的音乐」" else "已取消喜欢")
    }

    private fun toggleCommentLike(
        state: LuxUiState,
        key: String,
    ): LuxUiState {
        val next = if (key in state.commentLikedKeys) state.commentLikedKeys - key else state.commentLikedKeys + key
        return state.copy(commentLikedKeys = next)
    }

    private fun setSleepTimer(
        state: LuxUiState,
        timer: String,
    ): LuxUiState {
        val msg = if (timer.isEmpty()) "已关闭定时" else "将在 $timer 后停止（预览）"
        return state.copy(sleepTimer = timer, moreSheetOpen = false).withToast(msg)
    }
}
