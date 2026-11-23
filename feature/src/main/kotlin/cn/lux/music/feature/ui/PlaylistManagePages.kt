package cn.lux.music.feature.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.lux.music.designsystem.LuxButton
import cn.lux.music.designsystem.LuxButtonConfig
import cn.lux.music.designsystem.LuxButtonStyle
import cn.lux.music.designsystem.LuxChip
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxGroup
import cn.lux.music.designsystem.LuxGroupDivider
import cn.lux.music.designsystem.LuxSearchField
import cn.lux.music.designsystem.LuxSegmentedControl
import cn.lux.music.designsystem.LuxType
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun SmartPlaylistScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val isAuto = state.playlistMode == "auto"
    SubPageScaffold(title = "新建歌单", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        BasicText("创建专属歌单，也可以按聆听记录自动生成。", style = LuxType.RowSub, modifier = Modifier.padding(bottom = 14.dp))
        LuxSegmentedControl(
            items = listOf("空白歌单" to null, "自动生成 ✦" to null),
            selectedIndex = if (isAuto) 1 else 0,
            onSelect = { idx -> onEvent(LuxUiEvent.SetPlaylistMode(if (idx == 1) "auto" else "manual")) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )
        if (!isAuto) {
            BasicText("歌单名称", style = LuxType.Kicker, modifier = Modifier.padding(bottom = 8.dp))
            LuxSearchField(state.playlistNameDraft, {
                onEvent(LuxUiEvent.UpdatePlaylistNameDraft(it))
            }, "例如：深夜清单", showSearchIcon = false)
        } else {
            SmartPlaylistRulesForm(state = state, onEvent = onEvent)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
        ) {
            LuxButton("取消", { onEvent(LuxUiEvent.PopPage) }, Modifier.weight(1f), LuxButtonConfig(LuxButtonStyle.Plain))
            LuxButton(
                text = "创建歌单",
                onClick = { onEvent(if (isAuto) LuxUiEvent.CreateSmartPlaylist else LuxUiEvent.CreatePlaylist) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartPlaylistRulesForm(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    BasicText("选择歌曲来源", style = LuxType.Kicker, modifier = Modifier.padding(bottom = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val rules = listOf("liked" to "收藏的歌曲", "history" to "最近播放", "local" to "本地", "daily" to "今日推荐")
        rules.forEach { (k, label) ->
            LuxChip(label, { onEvent(LuxUiEvent.SetSmartRule(k)) }, selected = state.smartRule == k)
        }
    }
    BasicText("歌单名称", style = LuxType.Kicker, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("智能收藏", "最近常听").forEach { name ->
            LuxChip(name, { onEvent(LuxUiEvent.SetSmartName(name)) }, selected = state.smartName == name)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NewPlaylistScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    SubPageScaffold(title = "新建歌单", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        BasicText("给歌单起个名字", style = LuxType.Kicker, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            listOf("深夜清单", "跑步", "雨天").forEach { name ->
                LuxChip(
                    name,
                    { onEvent(LuxUiEvent.UpdatePlaylistNameDraft(name)) },
                    selected =
                        state.playlistNameDraft == name,
                )
            }
        }
        LuxSearchField(state.playlistNameDraft, {
            onEvent(LuxUiEvent.UpdatePlaylistNameDraft(it))
        }, "或输入名称", showSearchIcon = false)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            LuxButton("取消", { onEvent(LuxUiEvent.PopPage) }, Modifier.weight(1f), LuxButtonConfig(LuxButtonStyle.Plain))
            LuxButton("创建", { onEvent(LuxUiEvent.CreatePlaylist) }, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun RenamePlaylistScreen(
    playlistId: String,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    SubPageScaffold(title = "重命名", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        LuxSearchField(state.playlistNameDraft, {
            onEvent(LuxUiEvent.UpdatePlaylistNameDraft(it))
        }, "新的名称", showSearchIcon = false)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            LuxButton("取消", { onEvent(LuxUiEvent.PopPage) }, Modifier.weight(1f), LuxButtonConfig(LuxButtonStyle.Plain))
            LuxButton("保存", { onEvent(LuxUiEvent.RenamePlaylist(playlistId)) }, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun DeletePlaylistScreen(
    playlistId: String,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val name =
        state.playlists
            .firstOrNull { it.id == playlistId }
            ?.name
            .orEmpty()
    SubPageScaffold(title = "删除歌单", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        BasicText(
            "确定删除「$name」吗？",
            style = LuxType.RowSub,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LuxButton("取消", { onEvent(LuxUiEvent.PopPage) }, Modifier.weight(1f), LuxButtonConfig(LuxButtonStyle.Plain))
            LuxButton("删除", { onEvent(LuxUiEvent.DeletePlaylist(playlistId)) }, Modifier.weight(1f))
        }
    }
}

@Composable
internal fun AddToPlaylistScreen(
    songId: String,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    SubPageScaffold(title = "添加到歌单", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        LuxGroup {
            state.myPlaylists.forEachIndexed { idx, pl ->
                if (idx > 0) LuxGroupDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 68.dp)
                            .clickable { onEvent(LuxUiEvent.AddSongToPlaylist(songId, pl.id)) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    LuxCover(state.playlistCover(pl), size = 56.dp, cornerRadius = LuxDimens.RadiusSm)
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(pl.name, style = LuxType.RowTitle)
                        BasicText("${pl.songs.size} 首", style = LuxType.RowSub)
                    }
                }
            }
        }
    }
}
