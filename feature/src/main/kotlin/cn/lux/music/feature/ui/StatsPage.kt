package cn.lux.music.feature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxBrushes
import cn.lux.music.designsystem.LuxCard
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxType
import cn.lux.music.designsystem.luxInnerShadow
import cn.lux.music.designsystem.luxNeuUpSmall
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

private val StatsDatasets: Map<String, StatsDataset> =
    mapOf(
        "日" to
            StatsDataset(
                listOf("06", "08", "10", "12", "14", "16", "18"),
                listOf(22, 31, 20, 58, 42, 70, 48),
                "7",
                "首",
                "2026年11月14日",
                "今日听歌首数",
            ),
        "周" to
            StatsDataset(
                listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日"),
                listOf(24, 42, 34, 68, 49, 82, 58),
                "34",
                "首",
                "2026年第46周",
                "本周听歌首数",
            ),
        "月" to
            StatsDataset(
                listOf("6月", "7月", "8月", "9月", "10月", "11月"),
                listOf(34, 28, 88, 46, 40, 32),
                "46",
                "首",
                "2026年11月",
                "本月听歌首数",
            ),
        "年" to
            StatsDataset(
                listOf("1月", "3月", "5月", "7月", "9月", "11月"),
                listOf(25, 43, 37, 68, 94, 76),
                "386",
                "首",
                "2026年度",
                "年度听歌首数",
            ),
    )

@Composable
internal fun StatsScreen(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val dataset = StatsDatasets[state.statsPeriod] ?: StatsDatasets.getValue("月")
    SubPageScaffold(title = "聆听统计", onBack = { onEvent(LuxUiEvent.PopPage) }) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 17.dp, start = 2.dp, end = 2.dp),
        ) {
            BasicText(
                text = "LISTENING REPORT / 音乐报告",
                style =
                    LuxType.Kicker.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp,
                        color = LuxColors.AccentInk,
                    ),
            )
            BasicText(
                "✦ INSIGHTS",
                style =
                    LuxType.Kicker.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LuxColors.AccentInk,
                    ),
            )
        }
        PeriodSelector(selected = state.statsPeriod, onSelect = { onEvent(LuxUiEvent.SetStatsPeriod(it)) })
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp),
        ) {
            BasicText(
                "${dataset.date} ",
                style = LuxType.PageTitle.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold),
            )
            BasicText("⌄", style = LuxType.PageTitle.copy(fontSize = 16.sp, color = LuxColors.AccentInk))
        }
        StoryHeroChartCard(
            dataset,
            state.statsPeriod,
            state.statsChartKind,
        ) { onEvent(LuxUiEvent.SetStatsChartKind(it)) }
        StoryQuoteCard()
        HighlightsSection()
        HabitsCard()
        TasteDonutCard()
        TopRankingSection()
        BasicText(
            text = "统计数据为离线界面演示；切换周期与图表会更新示例趋势。",
            style = LuxType.RowSub.copy(fontSize = 11.sp, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun PeriodSelector(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val shape = RoundedCornerShape(25.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 18.dp)
                .clip(shape)
                .background(LuxColors.Page)
                .luxInnerShadow(cornerRadius = 25.dp)
                .padding(4.dp),
    ) {
        listOf("日", "周", "月", "年").forEach { item ->
            val on = item == selected
            val pill = RoundedCornerShape(22.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .then(
                            if (on) {
                                Modifier
                                    .luxNeuUpSmall(
                                        22.dp,
                                    ).clip(pill)
                                    .background(LuxBrushes.PrimaryFill)
                            } else {
                                Modifier.clip(pill)
                            },
                        ).clickable { onSelect(item) }
                        .padding(vertical = 10.dp),
            ) {
                BasicText(
                    item,
                    style =
                        LuxType.RowTitle.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (on) LuxColors.OnAccent else LuxColors.Muted,
                        ),
                )
            }
        }
    }
}

@Composable
private fun StoryQuoteCard() {
    LuxCard(cornerRadius = LuxDimens.RadiusBlock, modifier = Modifier.fillMaxWidth().padding(bottom = 27.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 17.dp),
        ) {
            BasicText("✧", style = LuxType.DetailTitle.copy(color = LuxColors.Clay))
            BasicText(
                text = "最近听的歌曲里，「从前」出现得格外频繁。那些旋律像一封寄给旧时光的信，陪你走过安静的夜晚。愿未来的每次播放，都能收藏新的故事。",
                style = LuxType.Body.copy(fontSize = 12.sp, lineHeight = 22.sp),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HighlightsSection() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp)) {
        BasicText("本期高光时刻", style = LuxType.SectionTitle)
        BasicText(
            "闪亮的音乐记忆，值得被 Highlight",
            style = LuxType.RowSub.copy(fontSize = 12.sp),
            modifier = Modifier.padding(top = 3.dp, bottom = 14.dp),
        )
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 25.dp),
    ) {
        HighlightTile("最长连续聆听", "00:32 AM", "◷", "深夜仍有音乐相伴", Modifier.weight(1f))
        HighlightTile("与你相伴", "5 位歌手", "♫", "在旋律里遇见不同的世界", Modifier.weight(1f))
    }
}

@Composable
private fun HighlightTile(
    label: String,
    value: String,
    glyph: String,
    note: String,
    modifier: Modifier,
) {
    LuxCard(cornerRadius = 21.dp, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 15.dp)) {
            BasicText(label, style = LuxType.RowSub.copy(fontSize = 11.sp))
            BasicText(value, style = LuxType.Title2.copy(fontSize = 22.sp), maxLines = 1)
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.height(64.dp)) {
                BasicText(glyph, style = LuxType.LargeTitle.copy(fontSize = 40.sp, color = LuxColors.AccentInk))
            }
            BasicText(note, style = LuxType.Body.copy(fontSize = 11.sp, lineHeight = 16.sp))
        }
    }
}

@Composable
private fun HabitsCard() {
    LuxCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)) {
            StatsCardHeader("聆听习惯", "什么时候最爱听音乐？", "◷")
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                listOf("清晨" to "12%", "午后" to "24%", "傍晚" to "26%", "深夜" to "38%").forEach { (k, v) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BasicText(k, style = LuxType.RowSub.copy(fontSize = 10.sp))
                        BasicText(v, style = LuxType.RowTitle.copy(fontSize = 12.sp))
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 12.dp,
                        ).height(14.dp)
                        .clip(RoundedCornerShape(15.dp)),
            ) {
                Box(Modifier.weight(0.12f).fillMaxHeight().background(LuxColors.Data4))
                Box(Modifier.weight(0.24f).fillMaxHeight().background(LuxColors.Data3))
                Box(Modifier.weight(0.26f).fillMaxHeight().background(LuxColors.Data2))
                Box(Modifier.weight(0.38f).fillMaxHeight().background(LuxColors.Data1))
            }
            BasicText("你的音乐黄金时段是 21:00 – 01:00", style = LuxType.RowSub.copy(fontSize = 11.sp))
        }
    }
}

@Composable
internal fun StatsCardHeader(
    title: String,
    subtitle: String,
    glyph: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        Column {
            BasicText(title, style = LuxType.PageTitle.copy(fontSize = 16.sp))
            BasicText(subtitle, style = LuxType.RowSub.copy(fontSize = 11.sp), modifier = Modifier.padding(top = 4.dp))
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(33.dp)
                    .luxNeuUpSmall(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LuxColors.Page)
                    .border(1.dp, Color.White, RoundedCornerShape(12.dp)),
        ) {
            BasicText(glyph, style = LuxType.PageTitle.copy(fontSize = 15.sp, color = LuxColors.AccentInk))
        }
    }
}

@Composable
private fun TopRankingSection() {
    val topIds = LuxMockCatalog.pick(3, 7)
    val counts = listOf(28, 21, 16, 12, 9, 7, 5)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 7.dp)) {
        StatsCardHeader("本期听歌排行 ▶", "你的私人热播榜 · Top 7", "♬")
        topIds.forEachIndexed { idx, sid ->
            val song = LuxMockCatalog.songById[sid] ?: return@forEachIndexed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            ) {
                BasicText(
                    text = "${idx + 1}",
                    style = LuxType.TabularRank.copy(color = if (idx < 4) LuxColors.AccentInk else LuxColors.Muted),
                    modifier = Modifier.width(20.dp),
                )
                LuxCover(song.cover, size = 52.dp, cornerRadius = 11.dp)
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        song.title,
                        style = LuxType.RowTitle.copy(fontSize = 14.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    BasicText(
                        text = "${song.artist} · ${if (idx < 3) "热播" else "常听"}",
                        style = LuxType.RowSub.copy(fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicText(
                    "${counts[idx]} 次",
                    style = LuxType.TabularCaption.copy(fontSize = 11.sp, color = LuxColors.Muted),
                )
            }
            if (idx <
                topIds.lastIndex
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(LuxColors.Sep.copy(alpha = 0.6f)))
            }
        }
    }
}
