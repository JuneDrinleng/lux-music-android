package cn.lux.music.feature.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxAvatar
import cn.lux.music.designsystem.LuxBrushes
import cn.lux.music.designsystem.LuxButton
import cn.lux.music.designsystem.LuxButtonConfig
import cn.lux.music.designsystem.LuxButtonStyle
import cn.lux.music.designsystem.LuxCard
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxGroup
import cn.lux.music.designsystem.LuxGroupDivider
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxSegmentedControl
import cn.lux.music.designsystem.LuxType
import cn.lux.music.designsystem.luxNeuUpSmall
import cn.lux.music.feature.state.LuxSubPage
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun MineTabScreen(
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
        ProfileHeader(state = state, onEvent = onEvent)
        MineQuickEntries(onEvent = onEvent)
        RecentHistoryCard(state = state, onEvent = onEvent)
        LikedSummaryCard(state = state, onEvent = onEvent)
        MyPlaylistsHeader(onEvent = onEvent)
        PlaylistToolsRow(state = state, onEvent = onEvent)
        if (state.playlistSortMenuOpen) {
            SortMenuCard(selected = state.playlistSort, onEvent = onEvent)
        }
        UserPlaylistsContent(state = state, onEvent = onEvent)
    }
}

@Composable
private fun ProfileHeader(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val status = if (state.syncConnected) "已连接示例账号" else "本地资料 · 未连接"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clickable { onEvent(LuxUiEvent.PushPage(LuxSubPage.Account)) },
    ) {
        LuxAvatar(initial = state.nick.take(1).ifEmpty { "满" }, size = 72.dp)
        BasicText(text = state.nick, style = LuxType.Title2.copy(textAlign = TextAlign.Center))
        BasicText(
            text = "${state.sign} · $status",
            style = LuxType.RowSub.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
        )
    }
}

@Composable
private fun MineQuickEntries(onEvent: (LuxUiEvent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 4.dp),
    ) {
        LuxQuickCircleEntry(LuxIcon.Heart, "我的收藏") {
            onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("liked")))
        }
        LuxQuickCircleEntry(LuxIcon.Download, "本地") {
            onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("local")))
        }
        LuxQuickCircleEntry(LuxIcon.ChartLine, "统计") {
            onEvent(LuxUiEvent.PushPage(LuxSubPage.Stats))
        }
        LuxQuickCircleEntry(LuxIcon.Headphones, "一起听") {
            onEvent(LuxUiEvent.PushPage(LuxSubPage.Together))
        }
    }
}

@Composable
private fun RecentHistoryCard(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val count = state.resolveList("history").songIds.size
    LuxCard(
        cornerRadius = LuxDimens.RadiusBlock,
        onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("history"))) },
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(32.dp)
                        .luxNeuUpSmall(16.dp)
                        .clip(CircleShape)
                        .background(LuxBrushes.RaisedFill)
                        .border(1.dp, LuxColors.SubtleWhiteBorder, CircleShape),
            ) {
                LuxIconView(icon = LuxIcon.History, tint = LuxColors.Muted, size = 18.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                BasicText(text = "最近播放", style = LuxType.RowTitle)
                BasicText(text = "$count 首", style = LuxType.RowSub)
            }
            LuxIconView(icon = LuxIcon.ChevRight, tint = LuxColors.Faint, size = 18.dp)
        }
    }
}

@Composable
private fun LikedSummaryCard(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val likedCount = state.orderedLikedSongIds.size
    LuxCard(
        cornerRadius = LuxDimens.RadiusBlock,
        onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("liked"))) },
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            val coverShape = RoundedCornerShape(LuxDimens.RadiusSm)
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(56.dp)
                        .luxNeuUpSmall(LuxDimens.RadiusSm)
                        .clip(coverShape)
                        .background(LuxBrushes.LikeCoverFill),
            ) {
                LuxIconView(icon = LuxIcon.Heart, tint = LuxColors.Clay, size = 24.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                BasicText(text = "我的收藏", style = LuxType.RowTitle)
                BasicText(text = "$likedCount 首", style = LuxType.RowSub)
            }
            LuxButton(
                text = "播放全部",
                onClick = { onEvent(LuxUiEvent.PlayAll("liked")) },
                config = LuxButtonConfig(style = LuxButtonStyle.Tonal, small = true, icon = LuxIcon.Play),
            )
        }
    }
}

@Composable
private fun MyPlaylistsHeader(onEvent: (LuxUiEvent) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 14.dp),
    ) {
        BasicText(text = "我的歌单", style = LuxType.SectionTitle)
        LuxButton(
            text = "新建歌单",
            onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.SmartPlaylist)) },
            config = LuxButtonConfig(style = LuxButtonStyle.Tonal, small = true, icon = LuxIcon.Creation),
            modifier = Modifier.height(38.dp),
        )
    }
}

@Composable
private fun PlaylistToolsRow(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
    ) {
        LuxSegmentedControl(
            items = listOf("列表" to LuxIcon.List, "宫格" to LuxIcon.Grid),
            selectedIndex = if (state.playlistView == "grid") 1 else 0,
            onSelect = { idx -> onEvent(LuxUiEvent.SetPlaylistView(if (idx == 1) "grid" else "list")) },
            compact = true,
            modifier = Modifier.width(140.dp),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .defaultMinSize(minHeight = 32.dp)
                    .clickable { onEvent(LuxUiEvent.TogglePlaylistSortMenu) },
        ) {
            BasicText(
                text = "排序：${state.playlistSort} ▾",
                style = LuxType.Kicker,
            )
        }
    }
}

@Composable
private fun SortMenuCard(
    selected: String,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val options = listOf("最近更新", "名称", "歌曲数")
    LuxGroup(modifier = Modifier.padding(bottom = 12.dp)) {
        options.forEachIndexed { idx, opt ->
            if (idx > 0) LuxGroupDivider()
            val on = opt == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable { onEvent(LuxUiEvent.PickPlaylistSort(opt)) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                BasicText(
                    text = opt,
                    style = LuxType.RowTitle.copy(color = if (on) LuxColors.AccentInk else LuxColors.Ink),
                    modifier = Modifier.weight(1f),
                )
                if (on) {
                    LuxIconView(icon = LuxIcon.Check, tint = LuxColors.AccentInk, size = 20.dp)
                }
            }
        }
    }
}

@Composable
private fun UserPlaylistsContent(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val playlists = state.sortedMyPlaylists
    if (state.playlistView == "grid") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            playlists.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { pl ->
                        LuxPlaylistCard(
                            spec =
                                PlaylistCardSpec(
                                    pl.name,
                                    state.playlistCover(pl),
                                    subtitle = "${pl.songs.size} 首",
                                    fixedWidth = null,
                                ),
                            onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("pl:${pl.id}"))) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    } else {
        LuxGroup {
            playlists.forEachIndexed { index, pl ->
                if (index > 0) LuxGroupDivider()
                UserPlaylistRow(
                    name = pl.name,
                    coverKey = state.playlistCover(pl),
                    meta = "${pl.songs.size} 首 · ${state.nick}",
                    onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("pl:${pl.id}"))) },
                )
            }
        }
    }
}

@Composable
private fun UserPlaylistRow(
    name: String,
    coverKey: String,
    meta: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 68.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        LuxCover(coverKey = coverKey, size = 56.dp, cornerRadius = LuxDimens.RadiusSm)
        Column(modifier = Modifier.weight(1f)) {
            BasicText(name, style = LuxType.RowTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(meta, style = LuxType.RowSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LuxIconView(icon = LuxIcon.ChevRight, tint = LuxColors.Faint, size = 18.dp)
    }
}
