package cn.lux.music.feature.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lux.music.designsystem.LuxButton
import cn.lux.music.designsystem.LuxButtonConfig
import cn.lux.music.designsystem.LuxButtonStyle
import cn.lux.music.designsystem.LuxCollage
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxGroup
import cn.lux.music.designsystem.LuxGroupDivider
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconButton
import cn.lux.music.designsystem.LuxType
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.model.ResolvedSongList
import cn.lux.music.feature.state.LuxSubPage
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun ListDetailScreen(
    listKey: String,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val resolved = state.resolveList(listKey)
    val title =
        when (listKey) {
            "daily" -> "今日推荐"
            "liked", "collected" -> "我的收藏"
            else -> resolved.name
        }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(modifier = Modifier.fillMaxSize()) {
        ListDetailTopBar(listKey = listKey, title = title, onEvent = onEvent)
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
            ListDetailHero(listKey = listKey, title = title, resolved = resolved)
            ListDetailActions(listKey = listKey, onEvent = onEvent)
            ListDetailSongs(listKey = listKey, resolved = resolved, state = state, onEvent = onEvent)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ListDetailTopBar(
    listKey: String,
    title: String,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val playlistId = listKey.removePrefix("pl:")
    val right: (@Composable () -> Unit)? =
        if (listKey.startsWith("pl:")) {
            {
                Row {
                    LuxIconButton(
                        LuxIcon.Pencil,
                        "重命名",
                        { onEvent(LuxUiEvent.PushPage(LuxSubPage.RenamePlaylist(playlistId))) },
                    )
                    LuxIconButton(
                        LuxIcon.Trash,
                        "删除",
                        { onEvent(LuxUiEvent.PushPage(LuxSubPage.DeletePlaylist(playlistId))) },
                    )
                }
            }
        } else {
            null
        }
    LuxPageTopBar(title = title, onBack = { onEvent(LuxUiEvent.PopPage) }, right = right)
}

@Composable
private fun ListDetailHero(
    listKey: String,
    title: String,
    resolved: ResolvedSongList,
) {
    val useCollage = listKey == "daily" || listKey == "weekly" || listKey == "radio"
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp),
    ) {
        if (useCollage) {
            val covers = resolved.songIds.take(4).mapNotNull { LuxMockCatalog.songById[it]?.cover }
            LuxCollage(coverKeys = covers, size = 132.dp, cornerRadius = LuxDimens.RadiusBlock)
        } else {
            LuxCover(coverKey = resolved.cover, size = 132.dp, cornerRadius = LuxDimens.RadiusBlock)
        }
        Column(modifier = Modifier.weight(1f)) {
            BasicText(text = resolved.kind, style = LuxType.Kicker)
            BasicText(text = title, style = LuxType.DetailTitle, modifier = Modifier.padding(top = 2.dp, bottom = 4.dp))
            BasicText(text = resolved.meta, style = LuxType.RowSub)
        }
    }
}

@Composable
private fun ListDetailActions(
    listKey: String,
    onEvent: (LuxUiEvent) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        LuxButton(
            text = "播放全部",
            onClick = { onEvent(LuxUiEvent.PlayAll(listKey)) },
            config = LuxButtonConfig(style = LuxButtonStyle.Primary, icon = LuxIcon.Play),
            modifier = Modifier.weight(1f),
        )
        LuxButton(
            text = "随机",
            onClick = { onEvent(LuxUiEvent.ShuffleAll(listKey)) },
            config = LuxButtonConfig(style = LuxButtonStyle.Tonal, icon = LuxIcon.Shuffle),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ListDetailSongs(
    listKey: String,
    resolved: ResolvedSongList,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    LuxGroup {
        if (resolved.songIds.isEmpty()) {
            BasicText(
                text = "还没有歌曲",
                style = LuxType.RowSub.copy(color = LuxColors.Muted),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            )
        } else {
            resolved.songIds.forEachIndexed { idx, sid ->
                val song = LuxMockCatalog.songById[sid] ?: return@forEachIndexed
                if (idx > 0) LuxGroupDivider()
                val isCur = state.currentSong.id == sid && state.fromName == resolved.name
                LuxSongRow(
                    spec =
                        SongRowSpec(
                            song = song,
                            isCurrent = isCur,
                            isPlaying = state.playing,
                            rank = if (resolved.kind == "排行榜") idx + 1 else null,
                            showSources = true,
                        ),
                    onClick = { onEvent(LuxUiEvent.PlayFromList(listKey, idx)) },
                )
            }
        }
    }
}
