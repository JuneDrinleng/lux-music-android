package cn.lux.music.feature.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxBrushes
import cn.lux.music.designsystem.LuxCard
import cn.lux.music.designsystem.LuxCollage
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxGroup
import cn.lux.music.designsystem.LuxGroupDivider
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxType
import cn.lux.music.designsystem.luxNeuUpSmall
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.state.LuxSubPage
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun HomeTabScreen(
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
                .padding(top = topInset + 8.dp, bottom = LuxDimens.DockSpace + bottomInset),
    ) {
        Column(modifier = Modifier.padding(horizontal = LuxDimens.Gutter)) {
            LuxTopSearchBar(
                onOpenSettings = { onEvent(LuxUiEvent.PushPage(LuxSubPage.Settings)) },
                onOpenSearch = { onEvent(LuxUiEvent.PushPage(LuxSubPage.Search)) },
            )
            DailyHeroCard(state = state, onEvent = onEvent)
            HomeQuickEntries(onEvent = onEvent)
            LuxSectionHeader(
                title = "热门日推歌单",
                actionText = "更多",
                actionIcon = LuxIcon.ChevRight,
                onAction = { onEvent(LuxUiEvent.GoDiscover) },
            )
        }
        HotPlaylistsCarousel(onEvent = onEvent)
        Column(modifier = Modifier.padding(horizontal = LuxDimens.Gutter)) {
            LuxSectionHeader(
                title = "本周发现",
                actionText = "播放",
                actionIcon = LuxIcon.Play,
                onAction = { onEvent(LuxUiEvent.PlayAll("weekly")) },
            )
            WeeklyDiscoveryGroup(state = state, onEvent = onEvent)
            LuxSectionHeader(title = "排行榜")
        }
        BoardsCarousel(state = state, onEvent = onEvent)
    }
}

@Composable
private fun DailyHeroCard(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val daily = state.resolveList("daily")
    val collageCovers = daily.songIds.take(4).mapNotNull { LuxMockCatalog.songById[it]?.cover }
    LuxCard(
        cornerRadius = LuxDimens.RadiusBlock,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("daily"))) },
            ) {
                CalendarTile()
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(text = "今日推荐", style = LuxType.SectionTitle)
                    BasicText(
                        text = "根据你的口味生成 · ${daily.songIds.size} 首 · 每天 6:00 更新",
                        style = LuxType.RowSub.copy(fontSize = 12.sp, lineHeight = 17.sp),
                    )
                }
                LuxCollage(coverKeys = collageCovers, size = 56.dp, cornerRadius = LuxDimens.RadiusSm)
            }
            val pillShape = RoundedCornerShape(LuxDimens.RadiusFull)
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .height(28.dp)
                        .luxNeuUpSmall(14.dp)
                        .clip(pillShape)
                        .background(LuxBrushes.TonalFill)
                        .border(1.dp, LuxColors.SubtleWhiteBorder.copy(alpha = 0.55f), pillShape)
                        .clickable { onEvent(LuxUiEvent.GoRadio) }
                        .padding(horizontal = 12.dp),
            ) {
                BasicText(
                    text = "播放无尽电台",
                    style = LuxType.RowTitle.copy(fontSize = 12.sp, color = LuxColors.OnPc),
                )
            }
        }
    }
}

@Composable
private fun CalendarTile() {
    val shape = RoundedCornerShape(LuxDimens.RadiusMd)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .width(56.dp)
                .height(60.dp)
                .luxNeuUpSmall(LuxDimens.RadiusMd)
                .clip(shape)
                .background(LuxBrushes.CalendarFill)
                .border(1.dp, LuxColors.SubtleWhiteBorder.copy(alpha = 0.55f), shape),
    ) {
        BasicText(
            text = "十一月",
            style = LuxType.Kicker.copy(fontSize = 11.sp, lineHeight = 14.sp, color = LuxColors.Ochre),
        )
        BasicText(
            text = "13",
            style =
                LuxType.DetailTitle.copy(
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = LuxColors.Ochre,
                ),
        )
    }
}

@Composable
private fun HomeQuickEntries(onEvent: (LuxUiEvent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 4.dp),
    ) {
        LuxQuickCircleEntry(LuxIcon.Calendar, "今日推荐") {
            onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("daily")))
        }
        LuxQuickCircleEntry(LuxIcon.ListNum, "排行榜") {
            onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("board:hot")))
        }
        LuxQuickCircleEntry(LuxIcon.MusicBox, "歌单") { onEvent(LuxUiEvent.GoDiscover) }
        LuxQuickCircleEntry(LuxIcon.Radio, "电台") { onEvent(LuxUiEvent.GoRadio) }
    }
}

@Composable
private fun HotPlaylistsCarousel(onEvent: (LuxUiEvent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = LuxDimens.Gutter, vertical = 8.dp),
    ) {
        LuxMockCatalog.hotPlaylists.forEachIndexed { index, hot ->
            LuxPlaylistCard(
                spec = PlaylistCardSpec(hot.name, hot.cover, playCount = LuxMockCatalog.playCountLabel(index)),
                onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("hot:${hot.id}"))) },
            )
        }
    }
}

@Composable
private fun WeeklyDiscoveryGroup(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val weekly = state.resolveList("weekly")
    LuxGroup {
        weekly.songIds.take(3).forEachIndexed { index, id ->
            val song = LuxMockCatalog.songById[id] ?: return@forEachIndexed
            if (index > 0) LuxGroupDivider()
            val isCur = state.currentSong.id == id && state.fromName == weekly.name
            LuxSongRow(
                spec = SongRowSpec(song = song, isCurrent = isCur, isPlaying = state.playing),
                onClick = { onEvent(LuxUiEvent.PlayFromList("weekly", index)) },
            )
        }
    }
}

@Composable
private fun BoardsCarousel(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = LuxDimens.Gutter, end = LuxDimens.Gutter, top = 6.dp, bottom = 14.dp),
    ) {
        LuxMockCatalog.boards.take(3).forEach { board ->
            val resolved = state.resolveList("board:${board.id}")
            LuxCard(
                cornerRadius = LuxDimens.RadiusBlock,
                onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.ListDetail("board:${board.id}"))) },
                modifier = Modifier.width(260.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicText(text = board.name, style = LuxType.PageTitle)
                        LuxIconView(icon = LuxIcon.ChevRight, tint = LuxColors.Faint, size = 18.dp)
                    }
                    resolved.songIds.take(3).forEachIndexed { idx, id ->
                        val song = LuxMockCatalog.songById[id] ?: return@forEachIndexed
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            LuxCover(coverKey = song.cover, size = 36.dp, cornerRadius = LuxDimens.RadiusSm)
                            BasicText(
                                text = "${idx + 1}",
                                style = LuxType.RowTitle.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.width(12.dp),
                            )
                            BasicText(
                                text = song.title,
                                style = LuxType.Body.copy(fontSize = 14.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(4.dp))
}
