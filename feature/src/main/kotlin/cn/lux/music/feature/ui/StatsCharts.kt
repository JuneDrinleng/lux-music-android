package cn.lux.music.feature.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxBrushes
import cn.lux.music.designsystem.LuxCard
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxType
import cn.lux.music.designsystem.luxInnerShadow
import cn.lux.music.designsystem.luxNeuUpSmall

@Composable
internal fun StoryHeroChartCard(
    dataset: StatsDataset,
    period: String,
    chartKind: String,
    onChangeKind: (String) -> Unit,
) {
    LuxCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, top = 18.dp, bottom = 17.dp)) {
            BasicText(
                dataset.subtitle,
                style = LuxType.RowTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                BasicText(dataset.total, style = LuxType.StoryHeroNumber)
                BasicText(
                    dataset.unit,
                    style = LuxType.PageTitle.copy(fontSize = 16.sp, color = LuxColors.AccentInk),
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
                )
            }
            BasicText(
                "↗ 较上一周期增长 12.8%",
                style = LuxType.RowSub.copy(fontSize = 11.sp, color = LuxColors.AccentInk),
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
            ) {
                BasicText("聆听趋势", style = LuxType.RowTitle.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold))
                ChartKindToggle(kind = chartKind, onChange = onChangeKind)
            }
            if (chartKind == "bar") {
                ModernBarChart(values = dataset.values)
            } else {
                val peak = dataset.values.indices.maxByOrNull { dataset.values[it] } ?: 0
                SmoothLineChart(values = dataset.values, focusIndex = if (period == "月") 3 else peak)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 2.dp),
            ) {
                dataset.labels.forEach { label -> BasicText(label, style = LuxType.RowSub.copy(fontSize = 11.sp)) }
            }
        }
    }
}

@Composable
private fun ChartKindToggle(
    kind: String,
    onChange: (String) -> Unit,
) {
    val wrapShape = RoundedCornerShape(13.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier =
            Modifier
                .clip(
                    wrapShape,
                ).background(LuxColors.Page)
                .luxInnerShadow(cornerRadius = 13.dp)
                .padding(3.dp),
    ) {
        ChartToggleChip("折线", LuxIcon.ChartLine, kind == "line") { onChange("line") }
        ChartToggleChip("▥ 柱状", null, kind == "bar") { onChange("bar") }
    }
}

@Composable
private fun ChartToggleChip(
    label: String,
    icon: LuxIcon?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val fg = if (selected) LuxColors.AccentInk else LuxColors.Muted
    val mod =
        if (selected) {
            Modifier
                .luxNeuUpSmall(
                    10.dp,
                ).clip(shape)
                .background(LuxBrushes.SelectedFill)
        } else {
            Modifier.clip(shape)
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = mod.clickable(onClick = onClick).padding(horizontal = 7.dp, vertical = 5.dp),
    ) {
        if (icon != null) LuxIconView(icon, fg, size = 13.dp)
        BasicText(label, style = LuxType.RowSub.copy(fontSize = 11.sp, color = fg))
    }
}

@Composable
private fun SmoothLineChart(
    values: List<Int>,
    focusIndex: Int,
) {
    val safeFocus = focusIndex.coerceIn(0, values.lastIndex)
    Box(modifier = Modifier.fillMaxWidth().height(155.dp)) {
        SmoothLineCanvas(values = values, focusIndex = safeFocus)
        val fracX = safeFocus.toFloat() / (values.size - 1).coerceAtLeast(1)
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (fracX * 220f).dp.coerceIn(4.dp, 220.dp), y = 12.dp)
                    .luxNeuUpSmall(11.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(LuxColors.SurfaceHigh)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            BasicText(
                "↗ ${values[safeFocus]} 首",
                style =
                    LuxType.TabularCaption.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxColors.AccentInk,
                    ),
            )
        }
    }
}

@Composable
private fun SmoothLineCanvas(
    values: List<Int>,
    focusIndex: Int,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val left = 12.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 20.dp.toPx()
        val bottom = size.height - 23.dp.toPx()
        val points =
            values.mapIndexed { i, v ->
                val x = left + (right - left) * i / (values.size - 1).coerceAtLeast(1)
                val y = bottom - (v / 100f) * (bottom - top)
                Offset(x, y)
            }
        val linePath = buildSmoothPath(points)
        val areaPath =
            Path().apply {
                addPath(linePath)
                lineTo(right, bottom)
                lineTo(left, bottom)
                close()
            }
        drawPath(
            areaPath,
            Brush.verticalGradient(listOf(LuxColors.Accent.copy(alpha = 0.29f), Color.Transparent), top, bottom),
        )
        drawPath(
            linePath,
            Brush.horizontalGradient(listOf(LuxColors.Sage, LuxColors.AccentInk)),
            style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
        )
        val focus = points[focusIndex]
        drawCircle(LuxColors.Page, 7.dp.toPx(), focus)
        drawCircle(LuxColors.AccentInk, 7.dp.toPx(), focus, style = Stroke(3.dp.toPx()))
    }
}

private fun buildSmoothPath(points: List<Offset>): Path =
    Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]
            val mid = (p0.x + p1.x) / 2f
            cubicTo(mid, p0.y, mid, p1.y, p1.x, p1.y)
        }
    }

@Composable
private fun ModernBarChart(values: List<Int>) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.fillMaxWidth().height(155.dp).padding(horizontal = 7.dp, vertical = 6.dp),
    ) {
        values.forEach { v ->
            val trackShape = RoundedCornerShape(12.dp)
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier =
                    Modifier
                        .width(
                            23.dp,
                        ).fillMaxHeight()
                        .clip(trackShape)
                        .background(LuxColors.Inset)
                        .luxInnerShadow(cornerRadius = 12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(
                                (v / 100f).coerceIn(0.05f, 1f),
                            ).clip(trackShape)
                            .background(LuxBrushes.BarChartFill),
                )
            }
        }
    }
}

@Composable
internal fun TasteDonutCard() {
    val slices =
        listOf(
            Triple("流行", "42%", LuxColors.Data1 to 0.42f),
            Triple("独立", "28%", LuxColors.Data2 to 0.28f),
            Triple("电子", "18%", LuxColors.Data3 to 0.18f),
            Triple("其他", "12%", LuxColors.Data4 to 0.12f),
        )
    LuxCard(cornerRadius = 24.dp, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)) {
            StatsCardHeader("音乐口味地图", "按聆听时长估算", "♫")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                TasteDonutRing(slices = slices.map { it.third })
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    slices.forEach { (name, pct, pair) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(pair.first))
                            BasicText(
                                name,
                                style = LuxType.RowSub.copy(fontSize = 12.sp),
                                modifier = Modifier.weight(1f),
                            )
                            BasicText(pct, style = LuxType.RowTitle.copy(fontSize = 12.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasteDonutRing(slices: List<Pair<Color, Float>>) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(142.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var start = -90f
            val stroke = 19.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            slices.forEach { (color, frac) ->
                val sweep = frac * 360f
                drawArc(color, start, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .size(
                        104.dp,
                    ).clip(CircleShape)
                    .background(LuxColors.Page)
                    .luxInnerShadow(cornerRadius = 52.dp),
        ) {
            BasicText("318", style = LuxType.DetailTitle.copy(fontSize = 27.sp))
            BasicText("分钟", style = LuxType.RowSub.copy(fontSize = 11.sp))
        }
    }
}
