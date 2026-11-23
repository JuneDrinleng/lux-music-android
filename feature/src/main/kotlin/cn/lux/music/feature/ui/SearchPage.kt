package cn.lux.music.feature.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxChip
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxGroup
import cn.lux.music.designsystem.LuxGroupDivider
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconButton
import cn.lux.music.designsystem.LuxIconButtonStyle
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxRemovableChip
import cn.lux.music.designsystem.LuxSearchField
import cn.lux.music.designsystem.LuxSegmentedControl
import cn.lux.music.designsystem.LuxType
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.state.LuxSubPage
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun SearchScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val srcLabel = LuxMockCatalog.searchSources.firstOrNull { it.first == state.searchSource }?.second ?: "全部"
    Column(modifier = Modifier.fillMaxSize()) {
        LuxPageTopBar(title = "搜索", onBack = { onEvent(LuxUiEvent.PopPage) })
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = LuxDimens.Gutter,
                        end = LuxDimens.Gutter,
                        top = 4.dp,
                        bottom = LuxDimens.DockSpace + navBottom,
                    ),
        ) {
            LuxSearchField(
                value = state.searchQuery,
                onValueChange = { onEvent(LuxUiEvent.UpdateSearchQuery(it)) },
                placeholder = "搜索歌曲、歌手、歌单...",
            )
            SearchFilterBar(state = state, srcLabel = srcLabel, onEvent = onEvent)
            when {
                state.searchQuery.trim().isEmpty() -> SearchIdleBody(state = state, onEvent = onEvent)
                state.searchTab == "list" -> SearchPlaylistResultsBody(state = state, onEvent = onEvent)
                else -> SearchSongResultsBody(state = state, srcLabel = srcLabel, onEvent = onEvent)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilterBar(
    state: LuxUiState,
    srcLabel: String,
    onEvent: (LuxUiEvent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
    ) {
        LuxSegmentedControl(
            items = listOf("歌曲" to null, "歌单" to null),
            selectedIndex = if (state.searchTab == "list") 1 else 0,
            onSelect = { idx -> onEvent(LuxUiEvent.SetSearchTab(if (idx == 1) "list" else "song")) },
            compact = true,
            modifier = Modifier.width(132.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .defaultMinSize(
                        minHeight = 32.dp,
                    ).clickable { onEvent(LuxUiEvent.ToggleSearchSourceMenu) },
        ) {
            BasicText("来源：$srcLabel ▾", style = LuxType.Kicker.copy(color = LuxColors.AccentInk))
        }
    }
    if (state.searchSourceMenuOpen) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 10.dp),
        ) {
            LuxMockCatalog.searchSources.forEach { (code, label) ->
                LuxChip(label, { onEvent(LuxUiEvent.SetSearchSource(code)) }, selected = state.searchSource == code)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchIdleBody(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    LuxSectionHeader("最近搜索", "清空全部", onAction = { onEvent(LuxUiEvent.ClearSearchHistory) })
    if (state.searchHistory.isEmpty()) {
        BasicText("还没有搜索记录", style = LuxType.RowSub, modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp))
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.searchHistory.forEach { word ->
                LuxRemovableChip(
                    text = word,
                    leadingIcon = LuxIcon.History,
                    onClick = { onEvent(LuxUiEvent.PickSearchWord(word)) },
                    trailing = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(28.dp).clickable { onEvent(LuxUiEvent.DeleteSearchWord(word)) },
                        ) {
                            LuxIconView(LuxIcon.Close, LuxColors.Faint, size = 16.dp)
                        }
                    },
                )
            }
        }
    }
    SearchHotWordsGroup(onEvent = onEvent)
}

@Composable
private fun SearchHotWordsGroup(onEvent: (LuxUiEvent) -> Unit) {
    BasicText(
        "搜索建议 · 热门",
        style = LuxType.Kicker,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
    )
    LuxGroup {
        LuxMockCatalog.hotWords.forEachIndexed { idx, word ->
            if (idx > 0) LuxGroupDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable { onEvent(LuxUiEvent.PickSearchWord(word)) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                BasicText(
                    "${idx + 1}",
                    style = LuxType.TabularRank.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.width(22.dp),
                )
                BasicText(word, style = LuxType.RowTitle, modifier = Modifier.weight(1f))
                LuxIconView(LuxIcon.ArrowTR, LuxColors.Faint, size = 18.dp)
            }
        }
    }
}

@Composable
private fun SearchPlaylistResultsBody(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val results = state.searchPlaylists()
    BasicText(
        "歌单搜索结果 · ${results.size}",
        style = LuxType.Kicker,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
    )
    LuxGroup {
        if (results.isEmpty()) {
            BasicText(
                "没有找到歌单",
                style = LuxType.RowSub,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            )
        } else {
            results.forEachIndexed { idx, hit ->
                if (idx > 0) LuxGroupDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 68.dp)
                            .clickable { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("${hit.kind}:${hit.id}"))) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    LuxCover(hit.cover, size = 56.dp, cornerRadius = LuxDimens.RadiusSm)
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(hit.name, style = LuxType.RowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val prefix = if (hit.source.isNotEmpty()) "${LuxMockCatalog.sourceName(hit.source)} · " else ""
                        BasicText("$prefix${hit.songCount} 首", style = LuxType.RowSub)
                    }
                    LuxIconView(LuxIcon.ChevRight, LuxColors.Faint, size = 18.dp)
                }
            }
        }
    }
}

@Composable
private fun SearchSongResultsBody(
    state: LuxUiState,
    srcLabel: String,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val ids = state.searchSongIds()
    BasicText(
        "歌曲搜索结果 · 约 ${ids.size} 首",
        style = LuxType.Kicker,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 8.dp),
    )
    LuxGroup {
        if (ids.isEmpty()) {
            BasicText(
                text = "在「$srcLabel」里没有找到结果，换个来源试试。",
                style = LuxType.RowSub,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            )
        } else {
            ids.forEachIndexed { idx, sid ->
                if (idx > 0) LuxGroupDivider()
                SearchSongHitRow(songId = sid, index = idx, state = state, onEvent = onEvent)
            }
            LuxGroupDivider()
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable { onEvent(LuxUiEvent.ShowToast("没有更多结果了（预览数据有限）")) },
            ) {
                BasicText("加载更多", style = LuxType.RowTitle.copy(fontSize = 14.sp, color = LuxColors.Muted))
            }
        }
    }
}

@Composable
private fun SearchSongHitRow(
    songId: String,
    index: Int,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val song = LuxMockCatalog.songById[songId] ?: return
    val isCur = state.currentSong.id == songId && state.fromName == "搜索：${state.searchQuery}"
    val liked = songId in state.likedIds
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        LuxSongRow(
            spec = SongRowSpec(song = song, isCurrent = isCur, isPlaying = state.playing, showSources = true),
            onClick = { onEvent(LuxUiEvent.PlayFromList("search", index)) },
            modifier = Modifier.weight(1f),
        )
        LuxIconButton(
            icon = if (liked) LuxIcon.Heart else LuxIcon.HeartO,
            contentDescription = if (liked) "取消喜欢" else "喜欢",
            onClick = { onEvent(LuxUiEvent.ToggleLike(songId)) },
            style =
                LuxIconButtonStyle(
                    size = 38.dp,
                    iconSize = 20.dp,
                    tint = if (liked) LuxColors.Accent else LuxColors.Ink,
                    raised = false,
                ),
        )
        LuxIconButton(
            icon = LuxIcon.PlAdd,
            contentDescription = "添加到歌单",
            onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.AddToPlaylist(songId))) },
            style = LuxIconButtonStyle(size = 38.dp, iconSize = 20.dp, raised = false),
        )
    }
}
