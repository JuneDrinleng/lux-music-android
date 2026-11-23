package cn.lux.music.feature.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lux.music.designsystem.LuxCollage
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxGroup
import cn.lux.music.designsystem.LuxGroupDivider
import cn.lux.music.designsystem.LuxType
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.state.LuxSubPage
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun DiscoverTabScreen(
    state: LuxUiState,
    scrollState: ScrollState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = LuxDimens.Gutter,
                    end = LuxDimens.Gutter,
                    top = topInset + 8.dp,
                    bottom = LuxDimens.DockSpace + bottomInset,
                ),
    ) {
        LuxTopSearchBar(
            onOpenSettings = { onEvent(LuxUiEvent.PushPage(LuxSubPage.Settings)) },
            onOpenSearch = { onEvent(LuxUiEvent.PushPage(LuxSubPage.Search)) },
        )
        BasicText(
            text = "歌单广场",
            style = LuxType.Title2,
            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
        )
        PlaylistSquareGrid(onEvent = onEvent)
        BasicText(
            text = "官方榜",
            style = LuxType.Title2,
            modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
        )
        OfficialBoardsGroup(state = state, onEvent = onEvent)
    }
}

@Composable
private fun PlaylistSquareGrid(onEvent: (LuxUiEvent) -> Unit) {
    val rows = LuxMockCatalog.hotPlaylists.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEachIndexed { rowIdx, items ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items.forEachIndexed { colIdx, hot ->
                    val globalIndex = rowIdx * 3 + colIdx
                    LuxPlaylistCard(
                        spec =
                            PlaylistCardSpec(
                                name = hot.name,
                                coverKey = hot.cover,
                                playCount = LuxMockCatalog.playCountLabel(globalIndex + 2),
                                fixedWidth = null,
                            ),
                        onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("hot:${hot.id}"))) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - items.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OfficialBoardsGroup(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    LuxGroup {
        LuxMockCatalog.boards.forEachIndexed { index, board ->
            if (index > 0) LuxGroupDivider()
            val resolved = state.resolveList("board:${board.id}")
            val covers = resolved.songIds.take(4).mapNotNull { LuxMockCatalog.songById[it]?.cover }
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("board:${board.id}"))) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                LuxCollage(coverKeys = covers, size = 72.dp, cornerRadius = LuxDimens.RadiusSm)
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(text = board.name, style = LuxType.RowTitle)
                    resolved.songIds.take(3).forEachIndexed { idx, sid ->
                        val song = LuxMockCatalog.songById[sid] ?: return@forEachIndexed
                        BasicText(
                            text = "${idx + 1} ${song.title} - ${song.artist}",
                            style = LuxType.RowSub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
