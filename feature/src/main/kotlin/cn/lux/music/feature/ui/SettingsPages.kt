package cn.lux.music.feature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import cn.lux.music.designsystem.LuxChip
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxGroup
import cn.lux.music.designsystem.LuxGroupDivider
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxSearchField
import cn.lux.music.designsystem.LuxSegmentedControl
import cn.lux.music.designsystem.LuxSwitch
import cn.lux.music.designsystem.LuxType
import cn.lux.music.designsystem.luxNeuUpSmall
import cn.lux.music.feature.state.LuxSubPage
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

private data class SettingRowSpec(
    val icon: LuxIcon,
    val name: String,
    val optionId: String? = null,
    val targetPage: LuxSubPage? = null,
    val staticValue: String = "",
)

private data class SettingGroupSpec(
    val title: String,
    val en: String,
    val rows: List<SettingRowSpec>,
)

private val SettingGroups: List<SettingGroupSpec> =
    listOf(
        SettingGroupSpec(
            title = "外观",
            en = "APPEARANCE",
            rows =
                listOf(
                    SettingRowSpec(LuxIcon.Translate, "语言", optionId = "lang"),
                    SettingRowSpec(LuxIcon.Palette, "主题", optionId = "theme"),
                    SettingRowSpec(LuxIcon.Shape, "统计页样式", optionId = "statsStyle"),
                ),
        ),
        SettingGroupSpec(
            title = "搜索与播放",
            en = "SEARCH & PLAY",
            rows =
                listOf(
                    SettingRowSpec(LuxIcon.TextSearch, "搜索来源", optionId = "src"),
                    SettingRowSpec(LuxIcon.Equalizer, "音效", targetPage = LuxSubPage.Fx, staticValue = "响度与均衡（预览）"),
                    SettingRowSpec(LuxIcon.Creation, "自定义源", targetPage = LuxSubPage.CustomSrc, staticValue = "未导入"),
                    SettingRowSpec(LuxIcon.Headphones, "一起听房间布局", targetPage = LuxSubPage.Room),
                ),
        ),
        SettingGroupSpec(
            title = "数据管理与同步",
            en = "DATA & SYNC",
            rows =
                listOf(
                    SettingRowSpec(LuxIcon.Sync, "同步地址与格式", targetPage = LuxSubPage.Sync),
                    SettingRowSpec(LuxIcon.Broom, "清除已记住的同步冲突策略", targetPage = LuxSubPage.Conflict),
                    SettingRowSpec(LuxIcon.Download, "缓存管理", targetPage = LuxSubPage.Cache, staticValue = "128.4 MB"),
                    SettingRowSpec(LuxIcon.Export, "导出数据", targetPage = LuxSubPage.Export, staticValue = "歌单与设置"),
                    SettingRowSpec(LuxIcon.PlAdd, "导入数据", targetPage = LuxSubPage.Import, staticValue = "从备份恢复"),
                ),
        ),
        SettingGroupSpec(
            title = "关于",
            en = "ABOUT",
            rows =
                listOf(
                    SettingRowSpec(LuxIcon.Info, "当前版本", targetPage = LuxSubPage.Version, staticValue = "v0.5.3 预览"),
                    SettingRowSpec(LuxIcon.Update, "检查更新", targetPage = LuxSubPage.Version, staticValue = "已是最新版本"),
                    SettingRowSpec(
                        LuxIcon.Share,
                        "GitHub Releases",
                        targetPage = LuxSubPage.Github,
                        staticValue = "预览中不打开外链",
                    ),
                ),
        ),
    )

@Composable
internal fun SubPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(modifier = Modifier.fillMaxSize()) {
        LuxPageTopBar(title = title, onBack = onBack)
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
            content = content,
        )
    }
}

@Composable
internal fun SettingsScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    SubPageScaffold(title = "设置", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        LuxSearchField(state.settingsQuery, { onEvent(LuxUiEvent.UpdateSettingsQuery(it)) }, "搜索设置选项")
        SettingsAccountHero(state = state, onEvent = onEvent)
        LuxGroup(modifier = Modifier.padding(top = 14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(
                            minHeight = 52.dp,
                        ).padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                IconCircleBadge(LuxIcon.Equalizer)
                BasicText("无缝播放", style = LuxType.RowTitle, modifier = Modifier.weight(1f))
                LuxSwitch(state.swGapless, { onEvent(LuxUiEvent.ToggleSwitch("gapless")) }, "无缝播放")
            }
        }
        val q = state.settingsQuery.trim()
        val matched =
            SettingGroups.mapNotNull { group ->
                val rows =
                    group.rows.filter { r ->
                        q.isEmpty() ||
                            (group.title + group.en + r.name + resolveRowValue(state, r)).contains(q)
                    }
                if (rows.isEmpty()) null else group to rows
            }
        if (matched.isEmpty()) {
            BasicText(
                "没有匹配的设置项",
                style = LuxType.RowSub,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            )
        } else {
            matched.forEach { (group, rows) -> SettingsGroupSection(group, rows, state, onEvent) }
        }
    }
}

@Composable
private fun SettingsAccountHero(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    LuxCard(
        cornerRadius = LuxDimens.RadiusBlock,
        onClick = { onEvent(LuxUiEvent.PushPage(LuxSubPage.Account)) },
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            LuxAvatar(initial = state.nick.take(1).ifEmpty { "满" }, size = 56.dp)
            Column(modifier = Modifier.weight(1f)) {
                BasicText(state.nick, style = LuxType.RowTitle)
                BasicText(state.sign, style = LuxType.RowSub)
            }
            val pillShape = RoundedCornerShape(LuxDimens.RadiusFull)
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .height(
                            24.dp,
                        ).luxNeuUpSmall(12.dp)
                        .clip(pillShape)
                        .background(LuxBrushes.RaisedFill)
                        .padding(horizontal = 10.dp),
            ) {
                BasicText(
                    text = if (state.syncConnected) "已连接" else "未连接",
                    style = LuxType.Kicker.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                )
            }
            LuxIconView(LuxIcon.ChevRight, LuxColors.Faint, size = 18.dp)
        }
    }
}

@Composable
private fun SettingsGroupSection(
    group: SettingGroupSpec,
    rows: List<SettingRowSpec>,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 8.dp),
    ) {
        BasicText(group.title, style = LuxType.Kicker)
        BasicText(group.en, style = LuxType.Kicker.copy(color = LuxColors.Faint, letterSpacing = 0.5.sp))
    }
    LuxGroup {
        rows.forEachIndexed { idx, row ->
            if (idx > 0) LuxGroupDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .clickable {
                            val page = row.optionId?.let { LuxSubPage.Option(it) } ?: row.targetPage
                            if (page != null) onEvent(LuxUiEvent.PushPage(page))
                        }.padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                IconCircleBadge(row.icon)
                BasicText(
                    row.name,
                    style = LuxType.RowTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val value = resolveRowValue(state, row)
                if (value.isNotEmpty()) {
                    BasicText(
                        value,
                        style = LuxType.Body.copy(color = LuxColors.Muted),
                        maxLines = 1,
                    )
                }
                LuxIconView(LuxIcon.ChevRight, LuxColors.Faint, size = 18.dp)
            }
        }
    }
}

@Composable
internal fun IconCircleBadge(icon: LuxIcon) {
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
        LuxIconView(icon, LuxColors.Muted, size = 18.dp)
    }
}

private fun resolveRowValue(
    state: LuxUiState,
    row: SettingRowSpec,
): String =
    when {
        row.optionId != null -> optionValue(state, row.optionId)
        row.targetPage == LuxSubPage.Sync -> if (state.syncConnected) "已连接 · Lux" else "未连接"
        row.targetPage == LuxSubPage.Room -> state.roomLayout
        else -> row.staticValue
    }

private fun optionValue(
    state: LuxUiState,
    optionId: String,
): String =
    when (optionId) {
        "lang" -> state.lang
        "theme" -> state.theme
        "src" -> state.defaultSearchSource
        "statsStyle" -> state.statsStyle
        else -> ""
    }

@Composable
internal fun OptionScreen(
    optionId: String,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val (title, items) =
        when (optionId) {
            "lang" -> "语言" to listOf("简体中文", "繁體中文", "English")
            "theme" -> "主题" to listOf("跟随系统", "浅色", "深色")
            "src" -> "搜索来源" to listOf("全部来源", "酷我", "网易", "QQ")
            "statsStyle" -> "统计页样式" to listOf("杂志风", "年度回顾风")
            else -> "选项" to emptyList()
        }
    val current = optionValue(state, optionId)
    SubPageScaffold(title = title, onBack = { onEvent(LuxUiEvent.PopPage) }) {
        LuxGroup {
            items.forEachIndexed { idx, value ->
                if (idx > 0) LuxGroupDivider()
                val on = value == current
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .clickable { onEvent(LuxUiEvent.PickOption(optionId, value)) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    BasicText(
                        value,
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
internal fun KeyValueRow(
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(
                    minHeight = 52.dp,
                ).padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        BasicText(label, style = LuxType.RowTitle, modifier = Modifier.weight(1f))
        BasicText(value, style = LuxType.Body.copy(color = LuxColors.Muted))
    }
}
