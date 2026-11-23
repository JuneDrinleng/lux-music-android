package cn.lux.music.feature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import cn.lux.music.designsystem.LuxSwitch
import cn.lux.music.designsystem.LuxType
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun TogetherScreen(onEvent: (LuxUiEvent) -> Unit) {
    val sessions =
        listOf(
            Triple("晚风小房间", "小满 · 进行中 · 3 人", "s1"),
            Triple("通勤电台", "昨天 · 已结束", "s6"),
            Triple("周末慢歌会", "3 天前 · 已结束", "s8"),
        )
    SubPageScaffold(title = "一起听", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        BasicText("一起听原型 · 下列为示例房间，不能真正连线", style = LuxType.RowSub, modifier = Modifier.padding(bottom = 14.dp))
        LuxGroup {
            sessions.forEachIndexed { idx, (name, meta, sid) ->
                if (idx > 0) LuxGroupDivider()
                val cover = LuxMockCatalog.songById[sid]?.cover ?: "c1"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 68.dp)
                            .clickable { onEvent(LuxUiEvent.ShowToast("一起听房间「$name」为预览")) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    LuxCover(cover, size = 56.dp, cornerRadius = LuxDimens.RadiusSm)
                    Column(modifier = Modifier.weight(1f)) {
                        BasicText(name, style = LuxType.RowTitle)
                        BasicText(meta, style = LuxType.RowSub)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LuxButton("创建房间", { onEvent(LuxUiEvent.ShowToast("创建房间（预览中不联网）")) }, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun CacheScreen(onEvent: (LuxUiEvent) -> Unit) {
    SubPageScaffold(title = "缓存管理", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        LuxGroup {
            KeyValueRow("音频缓存", "96.2 MB")
            LuxGroupDivider()
            KeyValueRow("图片缓存", "24.1 MB")
            LuxGroupDivider()
            KeyValueRow("其它", "8.1 MB")
        }
        Spacer(Modifier.height(10.dp))
        LuxButton(
            text = "清理音频缓存",
            onClick = { onEvent(LuxUiEvent.ShowToast("已清理音频缓存（预览）")) },
            modifier = Modifier.fillMaxWidth(),
            config = LuxButtonConfig(style = LuxButtonStyle.Tonal),
        )
        Spacer(Modifier.height(10.dp))
        LuxButton(
            text = "清理图片缓存",
            onClick = { onEvent(LuxUiEvent.ShowToast("已清理图片缓存（预览）")) },
            modifier = Modifier.fillMaxWidth(),
            config = LuxButtonConfig(style = LuxButtonStyle.Tonal),
        )
    }
}

@Composable
internal fun SyncScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val isLx = state.accountMode == "lx"
    SubPageScaffold(title = "同步地址与格式", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        BasicText("示例地址，不会发起网络请求", style = LuxType.RowSub, modifier = Modifier.padding(bottom = 14.dp))
        LuxSegmentedControl(
            items = listOf("Lux" to null, "LX" to null),
            selectedIndex = if (isLx) 1 else 0,
            onSelect = { idx -> onEvent(LuxUiEvent.SetAccountMode(if (idx == 1) "lx" else "lux")) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )
        LuxGroup {
            KeyValueRow("服务器类型", if (isLx) "LX" else "Lux")
            LuxGroupDivider()
            KeyValueRow("同步地址", if (isLx) "lx-sync.local · 示例" else "lux.local · 示例")
        }
        Spacer(Modifier.height(10.dp))
        LuxButton(
            text = if (state.syncConnected) "已连接（示例）" else "保存并连接（示例）",
            onClick = { onEvent(LuxUiEvent.DemoLogin) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun VersionScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        SubPageScaffold(title = "当前版本", onBack = { onEvent(LuxUiEvent.PopPage) }) {
            LuxCard(cornerRadius = LuxDimens.RadiusCard, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                ) {
                    BasicText("Lux Music", style = LuxType.Title2)
                    BasicText("v0.5.3 预览", style = LuxType.PageTitle, modifier = Modifier.padding(vertical = 6.dp))
                    BasicText("布局预览 · 不会检查在线更新", style = LuxType.RowSub)
                }
            }
            LuxButton("检查更新", { onEvent(LuxUiEvent.SetVersionModal(true)) }, Modifier.fillMaxWidth())
        }
        if (state.versionModalOpen) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().background(LuxColors.Scrim).padding(24.dp),
            ) {
                LuxCard(cornerRadius = LuxDimens.RadiusCard, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp),
                    ) {
                        BasicText("已是最新版本", style = LuxType.PageTitle)
                        BasicText(
                            text = "当前 v0.5.3 预览，没有可安装的更新。",
                            style = LuxType.RowSub.copy(textAlign = TextAlign.Center),
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                        )
                        LuxButton(
                            text = "好",
                            onClick = { onEvent(LuxUiEvent.SetVersionModal(false)) },
                            config = LuxButtonConfig(style = LuxButtonStyle.Tonal),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FxScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    SubPageScaffold(title = "音效", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        BasicText("音效原型 · 仅保存在此预览里", style = LuxType.RowSub, modifier = Modifier.padding(bottom = 14.dp))
        LuxGroup {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(
                            minHeight = 52.dp,
                        ).padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                BasicText("响度均衡", style = LuxType.RowTitle, modifier = Modifier.weight(1f))
                LuxSwitch(state.swHq, { onEvent(LuxUiEvent.ToggleSwitch("hq")) }, "响度均衡")
            }
        }
    }
}

@Composable
internal fun CustomSrcScreen(onEvent: (LuxUiEvent) -> Unit) {
    SubPageScaffold(title = "自定义源", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        BasicText("以下为示例源，不会加载外部脚本", style = LuxType.RowSub, modifier = Modifier.padding(bottom = 14.dp))
        LuxGroup { KeyValueRow("示例源 A", "未启用") }
        Spacer(Modifier.height(10.dp))
        LuxButton(
            text = "导入源",
            onClick = { onEvent(LuxUiEvent.ShowToast("导入源文件（预览中不读取文件）")) },
            modifier = Modifier.fillMaxWidth(),
            config = LuxButtonConfig(style = LuxButtonStyle.Tonal),
        )
    }
}

@Composable
internal fun RoomLayoutScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    SubPageScaffold(title = "一起听房间布局", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        LuxGroup {
            listOf("列表", "卡片").forEachIndexed { idx, opt ->
                if (idx > 0) LuxGroupDivider()
                val on = state.roomLayout == opt
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .clickable { onEvent(LuxUiEvent.SetRoomLayout(opt)) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    BasicText(
                        opt,
                        style = LuxType.RowTitle.copy(color = if (on) LuxColors.AccentInk else LuxColors.Ink),
                        modifier = Modifier.weight(1f),
                    )
                    if (on) LuxIconView(LuxIcon.Check, LuxColors.AccentInk, size = 20.dp)
                }
            }
        }
    }
}

@Composable
internal fun SimpleActionSubScreen(
    spec: SimpleSubSpec,
    onEvent: (LuxUiEvent) -> Unit,
) {
    SubPageScaffold(title = spec.title, onBack = { onEvent(LuxUiEvent.PopPage) }) {
        BasicText(
            spec.bodyText,
            style = LuxType.RowSub,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
        )
        if (spec.buttonText != null) {
            LuxButton(
                text = spec.buttonText,
                onClick = { onEvent(LuxUiEvent.ShowToast(spec.toastMessage)) },
                modifier = Modifier.fillMaxWidth(),
                config = LuxButtonConfig(style = spec.buttonStyle),
            )
        }
    }
}
