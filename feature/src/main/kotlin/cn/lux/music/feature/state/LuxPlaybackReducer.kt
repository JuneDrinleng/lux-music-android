package cn.lux.music.feature.state

internal object LuxPlaybackReducer {
    fun reduce(
        state: LuxUiState,
        event: LuxUiEvent,
        randomInt: (Int) -> Int,
    ): LuxUiState? =
        reduceOverlayAndQueue(state, event)
            ?: reduceTransport(state, event, randomInt)
            ?: LuxPlayerMetaReducer.reduce(state, event)

    private fun reduceOverlayAndQueue(
        state: LuxUiState,
        event: LuxUiEvent,
    ): LuxUiState? =
        when (event) {
            LuxUiEvent.OpenPlayer ->
                state.copy(playerOpen = true, playerView = LuxPlayerView.Vinyl, moreSheetOpen = false)
            LuxUiEvent.ClosePlayer -> state.copy(playerOpen = false, moreSheetOpen = false)
            is LuxUiEvent.SetPlayerView ->
                state.copy(playerOpen = true, playerView = event.view, moreSheetOpen = false)
            LuxUiEvent.OpenQueue -> state.copy(queueOpen = true)
            LuxUiEvent.CloseQueue -> state.copy(queueOpen = false)
            LuxUiEvent.ClearQueue -> clearQueue(state)
            is LuxUiEvent.PlayQueueIndex ->
                if (event.index in state.queue.indices) {
                    state.copy(queueIndex = event.index, positionSec = 0.0, playing = true)
                } else {
                    state
                }
            else -> null
        }

    private fun reduceTransport(
        state: LuxUiState,
        event: LuxUiEvent,
        randomInt: (Int) -> Int,
    ): LuxUiState? =
        when (event) {
            is LuxUiEvent.PlayFromList -> playFromList(state, event.listKey, event.index)
            is LuxUiEvent.PlayAll -> playAll(state, event.listKey)
            is LuxUiEvent.ShuffleAll -> shuffleAll(state, event.listKey, randomInt)
            LuxUiEvent.TogglePlay -> state.copy(playing = !state.playing)
            LuxUiEvent.NextTrack -> advanceTrack(state, auto = false, randomInt = randomInt)
            LuxUiEvent.PrevTrack -> prevTrack(state)
            LuxUiEvent.CyclePlayMode -> {
                val modes = LuxPlayMode.entries
                val next = modes[(state.playMode.ordinal + 1) % modes.size]
                state.copy(playMode = next).withToast(next.label)
            }
            is LuxUiEvent.SeekFraction ->
                state.copy(positionSec = event.fraction.coerceIn(0f, 1f) * state.currentSong.durationSec.toDouble())
            is LuxUiEvent.SeekLyricLine -> {
                val line = state.currentLyrics.getOrNull(event.index)
                if (line == null) state else state.copy(positionSec = line.timeSec + 0.01)
            }
            is LuxUiEvent.Tick -> tick(state, event.deltaSec, randomInt)
            else -> null
        }

    private fun clearQueue(state: LuxUiState): LuxUiState {
        val currentId = state.queue.getOrNull(state.queueIndex)
        return if (currentId == null) {
            state
        } else {
            state.copy(queue = listOf(currentId), queueIndex = 0).withToast("已清空，仅保留当前歌曲")
        }
    }

    private fun playFromList(
        state: LuxUiState,
        listKey: String,
        index: Int,
    ): LuxUiState {
        val list = state.resolveList(listKey)
        return if (list.songIds.isEmpty()) {
            state
        } else {
            state.copy(
                queue = list.songIds,
                queueIndex = index.coerceIn(0, list.songIds.lastIndex),
                fromName = list.name,
                positionSec = 0.0,
                playing = true,
            )
        }
    }

    private fun playAll(
        state: LuxUiState,
        listKey: String,
    ): LuxUiState {
        val list = state.resolveList(listKey)
        val curId = state.queue.getOrNull(state.queueIndex)
        return when {
            list.songIds.isEmpty() -> state.withToast("当前列表还没有歌曲")
            state.fromName == list.name && curId != null && curId in list.songIds ->
                state.copy(playing = !state.playing)
            else ->
                state.copy(
                    queue = list.songIds,
                    queueIndex = 0,
                    fromName = list.name,
                    positionSec = 0.0,
                    playing = true,
                )
        }
    }

    private fun shuffleAll(
        state: LuxUiState,
        listKey: String,
        randomInt: (Int) -> Int,
    ): LuxUiState {
        val list = state.resolveList(listKey)
        return if (list.songIds.isEmpty()) {
            state.withToast("当前列表还没有歌曲")
        } else {
            val idx = randomInt(list.songIds.size).coerceIn(0, list.songIds.lastIndex)
            state
                .copy(
                    playMode = LuxPlayMode.Shuffle,
                    queue = list.songIds,
                    queueIndex = idx,
                    fromName = list.name,
                    positionSec = 0.0,
                    playing = true,
                ).withToast("随机播放")
        }
    }

    private fun tick(
        state: LuxUiState,
        deltaSec: Double,
        randomInt: (Int) -> Int,
    ): LuxUiState =
        if (!state.playing) {
            state
        } else {
            val nextPos = state.positionSec + deltaSec.coerceIn(0.0, 1.0)
            if (nextPos >= state.currentSong.durationSec) {
                advanceTrack(state, auto = true, randomInt = randomInt)
            } else {
                state.copy(positionSec = nextPos)
            }
        }

    private fun advanceTrack(
        state: LuxUiState,
        auto: Boolean,
        randomInt: (Int) -> Int,
    ): LuxUiState =
        when {
            state.queue.isEmpty() -> state
            state.playMode == LuxPlayMode.SingleLoop && auto -> state.copy(positionSec = 0.0)
            state.playMode == LuxPlayMode.Shuffle -> {
                val step = if (state.queue.size <= 1) 0 else 1 + randomInt(state.queue.size - 1)
                state.copy(queueIndex = (state.queueIndex + step) % state.queue.size, positionSec = 0.0)
            }
            state.playMode == LuxPlayMode.Order && auto && state.queueIndex == state.queue.lastIndex ->
                state.copy(playing = false, positionSec = 0.0)
            else -> state.copy(queueIndex = (state.queueIndex + 1) % state.queue.size, positionSec = 0.0)
        }

    private fun prevTrack(state: LuxUiState): LuxUiState =
        when {
            state.queue.isEmpty() -> state
            state.positionSec > 3.0 -> state.copy(positionSec = 0.0)
            else ->
                state.copy(
                    queueIndex = (state.queueIndex - 1 + state.queue.size) % state.queue.size,
                    positionSec = 0.0,
                )
        }
}
