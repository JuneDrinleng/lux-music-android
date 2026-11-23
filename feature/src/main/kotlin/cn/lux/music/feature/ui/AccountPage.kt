package cn.lux.music.feature.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.lux.music.designsystem.LuxAvatar
import cn.lux.music.designsystem.LuxButton
import cn.lux.music.designsystem.LuxButtonConfig
import cn.lux.music.designsystem.LuxButtonStyle
import cn.lux.music.designsystem.LuxCard
import cn.lux.music.designsystem.LuxChip
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxGroup
import cn.lux.music.designsystem.LuxGroupDivider
import cn.lux.music.designsystem.LuxSegmentedControl
import cn.lux.music.designsystem.LuxType
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun AccountScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val modes = listOf("local" to "本地资料", "lux" to "连接 Lux 服务端", "lx" to "连接原版 LX")
    val selectedIdx = modes.indexOfFirst { it.first == state.accountMode }.coerceAtLeast(0)
    SubPageScaffold(title = "账号", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        BasicText("账号流程预览 · 不会真正登录，也没有密码框", style = LuxType.RowSub, modifier = Modifier.padding(bottom = 14.dp))
        AccountProfileCard(state = state, onEvent = onEvent)
        LuxSegmentedControl(
            items = modes.map { it.second to null },
            selectedIndex = selectedIdx,
            onSelect = { idx -> onEvent(LuxUiEvent.SetAccountMode(modes[idx].first)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )
        AccountModePanel(state = state, onEvent = onEvent)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountProfileCard(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    LuxCard(cornerRadius = LuxDimens.RadiusCard, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
        ) {
            LuxAvatar(initial = state.nick.take(1).ifEmpty { "满" }, size = 72.dp)
            BasicText(state.nick, style = LuxType.Title2, modifier = Modifier.padding(top = 4.dp))
            BasicText("${state.sign} · 本地资料", style = LuxType.RowSub.copy(textAlign = TextAlign.Center))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                BasicText("可选昵称：", style = LuxType.RowSub, modifier = Modifier.align(Alignment.CenterVertically))
                listOf("小满", "June", "晚风").forEach { name ->
                    LuxChip(name, { onEvent(LuxUiEvent.SetNick(name)) }, selected = state.nick == name)
                }
            }
        }
    }
}

@Composable
private fun AccountModePanel(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    when (state.accountMode) {
        "local" ->
            LuxGroup {
                KeyValueRow("当前空间", "本机独立资料")
                LuxGroupDivider()
                KeyValueRow("同步状态", "未连接")
            }
        "lux" -> {
            LuxGroup {
                KeyValueRow("服务器类型", "Lux")
                LuxGroupDivider()
                KeyValueRow("同步地址", "lux.local · 示例")
            }
            Spacer(Modifier.height(10.dp))
            LuxButton(
                text = if (state.syncConnected) "已登录（示例）" else "登录",
                onClick = { onEvent(LuxUiEvent.DemoLogin) },
                modifier = Modifier.fillMaxWidth(),
                config = LuxButtonConfig(style = LuxButtonStyle.Primary),
            )
        }
        else -> {
            LuxGroup {
                KeyValueRow("服务器类型", "LX")
                LuxGroupDivider()
                KeyValueRow("同步地址", "lx-sync.local · 示例")
                LuxGroupDivider()
                KeyValueRow("连接码", "A7K2 · 示例")
            }
            Spacer(Modifier.height(10.dp))
            LuxButton(
                text = if (state.syncConnected) "已连接（示例）" else "连接",
                onClick = { onEvent(LuxUiEvent.DemoLogin) },
                modifier = Modifier.fillMaxWidth(),
                config = LuxButtonConfig(style = LuxButtonStyle.Primary),
            )
        }
    }
}
