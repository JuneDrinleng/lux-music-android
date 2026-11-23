package cn.lux.music.feature.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LuxUiReducerTest {
    @Test
    fun initialStateMatchesHtmlBaseline() {
        val state = LuxUiState()
        assertEquals(LuxTab.Home, state.tab)
        assertEquals("小满", state.nick)
        assertEquals("晚风里慢慢走", state.sign)
        assertEquals("通勤路上", state.fromName)
        assertEquals(31.0, state.positionSec, 0.001)
        assertFalse(state.playing)
        assertEquals(LuxPlayMode.ListLoop, state.playMode)
        assertTrue(state.showTranslation)
        assertFalse(state.showRomanization)
        assertEquals("月", state.statsPeriod)
        assertEquals("line", state.statsChartKind)
        assertEquals(5, state.orderedLikedSongIds.size)
    }

    @Test
    fun navigationStackTabSwitchAndReselectScrollToTop() {
        var state = LuxUiState()
        state = LuxUiReducer.reduce(state, LuxUiEvent.PushPage(LuxSubPage.Settings))
        assertEquals(1, state.stack.size)

        state = LuxUiReducer.reduce(state, LuxUiEvent.SelectTab(LuxTab.Discover))
        assertEquals(LuxTab.Discover, state.tab)
        assertTrue(state.stack.isEmpty())

        state = LuxUiReducer.reduce(state, LuxUiEvent.SelectTab(LuxTab.Discover))
        assertEquals(1, state.scrollToTopEpoch[LuxTab.Discover])
    }

    @Test
    fun playbackPrevThreeSecondRuleAndFourModes() {
        var state = LuxUiState(playing = true, positionSec = 5.0)
        state = LuxUiReducer.reduce(state, LuxUiEvent.PrevTrack)
        assertEquals(0.0, state.positionSec, 0.001)
        assertEquals(0, state.queueIndex)

        state = LuxUiReducer.reduce(state, LuxUiEvent.PrevTrack)
        assertEquals(state.queue.lastIndex, state.queueIndex)

        state =
            state.copy(
                playMode = LuxPlayMode.Order,
                queueIndex = state.queue.lastIndex,
                playing = true,
                positionSec = state.currentSong.durationSec - 0.1,
            )
        state = LuxUiReducer.reduce(state, LuxUiEvent.Tick(0.5))
        assertFalse(state.playing)

        state =
            state.copy(
                playMode = LuxPlayMode.SingleLoop,
                playing = true,
                positionSec =
                    state.currentSong.durationSec - 0.1,
            )
        val sameIdx = state.queueIndex
        state = LuxUiReducer.reduce(state, LuxUiEvent.Tick(0.5))
        assertEquals(sameIdx, state.queueIndex)
        assertEquals(0.0, state.positionSec, 0.001)

        state = LuxUiReducer.reduce(state, LuxUiEvent.NextTrack)
        assertEquals((sameIdx + 1) % state.queue.size, state.queueIndex)
    }

    @Test
    fun clearQueueKeepsOnlyCurrentSongAndResetsIndex() {
        var state = LuxUiState(queueIndex = 2)
        val currentId = state.queue[2]
        state = LuxUiReducer.reduce(state, LuxUiEvent.ClearQueue)
        assertEquals(listOf(currentId), state.queue)
        assertEquals(0, state.queueIndex)
        assertEquals("已清空，仅保留当前歌曲", state.toastMessage)
    }

    @Test
    fun searchHistoryDeduplicatesCapsAtSixAndFiltersSongsBySource() {
        var state = LuxUiState()
        for (w in listOf("夜航", "一万次日落", "新词A")) {
            state = LuxUiReducer.reduce(state, LuxUiEvent.PickSearchWord(w))
        }
        assertEquals(6, state.searchHistory.size)
        assertEquals("新词A", state.searchHistory.first())

        state = LuxUiReducer.reduce(state, LuxUiEvent.UpdateSearchQuery("晚风"))
        assertTrue(state.searchSongIds().isNotEmpty())
        state = LuxUiReducer.reduce(state, LuxUiEvent.SetSearchSource("qq"))
        val qqResults = state.searchSongIds()
        assertTrue(
            qqResults.all { id ->
                "qq" in
                    cn.lux.music.feature.model.LuxMockCatalog.songById
                        .getValue(id)
                        .sources
            },
        )
    }

    @Test
    fun playlistLifecycleCreateSmartRenameAddAndDelete() {
        var state = LuxUiState()
        val initialCount = state.myPlaylists.size
        state = LuxUiReducer.reduce(state, LuxUiEvent.PushPage(LuxSubPage.SmartPlaylist))
        state = LuxUiReducer.reduce(state, LuxUiEvent.UpdatePlaylistNameDraft("  雨夜清单  "))
        state = LuxUiReducer.reduce(state, LuxUiEvent.CreatePlaylist, nowMillis = { 100L })
        assertEquals(initialCount + 1, state.myPlaylists.size)
        val created = state.myPlaylists.last()
        assertEquals("雨夜清单", created.name)

        state = LuxUiReducer.reduce(state, LuxUiEvent.PushPage(LuxSubPage.AddToPlaylist("s1")))
        state = LuxUiReducer.reduce(state, LuxUiEvent.AddSongToPlaylist("s1", created.id))
        state = LuxUiReducer.reduce(state, LuxUiEvent.AddSongToPlaylist("s1", created.id))
        assertEquals(listOf("s1"), state.playlists.first { it.id == created.id }.songs)

        state = LuxUiReducer.reduce(state, LuxUiEvent.PushPage(LuxSubPage.RenamePlaylist(created.id)))
        state = LuxUiReducer.reduce(state, LuxUiEvent.UpdatePlaylistNameDraft("晴夜清单"))
        state = LuxUiReducer.reduce(state, LuxUiEvent.RenamePlaylist(created.id))
        assertEquals("晴夜清单", state.playlists.first { it.id == created.id }.name)

        state = LuxUiReducer.reduce(state, LuxUiEvent.PushPage(LuxSubPage.ListDetail("pl:${created.id}")))
        state = LuxUiReducer.reduce(state, LuxUiEvent.DeletePlaylist(created.id))
        assertTrue(state.stack.isEmpty())
        assertEquals(initialCount, state.myPlaylists.size)
    }

    @Test
    fun systemBackPriorityOrderQueueModalPlayerSubpage() {
        var state =
            LuxUiState(
                stack = listOf(LuxSubPage.Settings),
                playerOpen = true,
                playerView = LuxPlayerView.Lyrics,
                queueOpen = true,
            )
        state = LuxUiReducer.reduce(state, LuxUiEvent.SystemBack)
        assertFalse(state.queueOpen)
        assertTrue(state.playerOpen)

        state = LuxUiReducer.reduce(state, LuxUiEvent.SystemBack)
        assertEquals(LuxPlayerView.Vinyl, state.playerView)
        assertTrue(state.playerOpen)

        state = LuxUiReducer.reduce(state, LuxUiEvent.SystemBack)
        assertFalse(state.playerOpen)
        assertEquals(1, state.stack.size)

        state = LuxUiReducer.reduce(state, LuxUiEvent.SystemBack)
        assertTrue(state.stack.isEmpty())
    }
}
