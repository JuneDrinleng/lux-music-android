package cn.lux.music.feature.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxCard
import cn.lux.music.designsystem.LuxChip
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconButton
import cn.lux.music.designsystem.LuxIconButtonStyle
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxPlayButton
import cn.lux.music.designsystem.LuxSeekBar
import cn.lux.music.designsystem.LuxShadowSpec
import cn.lux.music.designsystem.LuxType
import cn.lux.music.designsystem.luxDropShadow
import cn.lux.music.designsystem.luxNeuUpSmall
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.state.LuxPlayerView
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun PlayerOverlay(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    AnimatedVisibility(
        visible = state.playerOpen,
        enter =
            slideInVertically(
                animationSpec =
                    tween(
                        420,
                        easing = CubicBezierEasing(0.25f, 1.1f, 0.4f, 1f),
                    ),
                initialOffsetY = {
                    it
                },
            ),
        exit =
            slideOutVertically(
                animationSpec =
                    tween(
                        300,
                        easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f),
                    ),
                targetOffsetY = {
                    it
                },
            ),
    ) {
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier.fillMaxSize().background(LuxColors.Page).padding(top = topInset, bottom = bottomInset),
        ) {
            when (state.playerView) {
                LuxPlayerView.Vinyl -> VinylPlayerBody(state = state, onEvent = onEvent)
                LuxPlayerView.Lyrics -> LyricsPlayerBody(state = state, onEvent = onEvent)
                LuxPlayerView.Comments -> CommentsPlayerBody(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
internal fun PlayerTopBar(
    state: LuxUiState,
    backToVinyl: Boolean,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val song = state.currentSong
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(horizontal = 10.dp),
    ) {
        LuxIconButton(
            icon = if (backToVinyl) LuxIcon.ChevLeft else LuxIcon.ChevDown,
            contentDescription = if (backToVinyl) "返回" else "收起",
            onClick = {
                onEvent(if (backToVinyl) LuxUiEvent.SetPlayerView(LuxPlayerView.Vinyl) else LuxUiEvent.ClosePlayer)
            },
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            BasicText(
                text = song.title,
                style = LuxType.PageTitle.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = "正在播放 · 来自「${state.fromName}」",
                style = LuxType.RowSub.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LuxIconButton(LuxIcon.Share, "分享", { onEvent(LuxUiEvent.ShowToast("分享（预览中不执行）")) })
    }
}

@Composable
private fun ColumnScope.VinylPlayerBody(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    PlayerTopBar(state = state, backToVinyl = false, onEvent = onEvent)
    if (state.moreSheetOpen) InlineMoreCard(state = state, onEvent = onEvent)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        VinylTurntable(
            coverKey = state.currentSong.cover,
            playing = state.playing,
            onOpenLyrics = { onEvent(LuxUiEvent.SetPlayerView(LuxPlayerView.Lyrics)) },
        )
        Spacer(Modifier.height(26.dp))
        LuxLyricPreview(state = state, onEvent = onEvent)
        Spacer(Modifier.height(8.dp))
    }
    PlayerActionStrip(state = state, onEvent = onEvent)
    PlayerFooterControls(state = state, onEvent = onEvent)
}

@Composable
private fun VinylTurntable(
    coverKey: String,
    playing: Boolean,
    onOpenLyrics: () -> Unit,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        while (playing) {
            val current = rotation.value % 360f
            rotation.snapTo(current)
            val durationMs = (((360f - current) / 360f) * 20_000f).toInt().coerceAtLeast(16)
            rotation.animateTo(360f, tween(durationMs, easing = LinearEasing))
            rotation.snapTo(0f)
        }
    }
    val armAngle by animateFloatAsState(if (playing) -2f else -28f, tween(500), label = "tonearm")
    Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth()) {
        VinylDisc(coverKey = coverKey, rotationDeg = rotation.value, onClick = onOpenLyrics)
        TonearmOverlay(angleDeg = armAngle)
    }
}

@Composable
private fun VinylDisc(
    coverKey: String,
    rotationDeg: Float,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .luxDropShadow(LuxShadowSpec(LuxColors.ShadowLight.copy(alpha = 0.8f), 20.dp, (-8).dp, (-8).dp, 150.dp))
                .luxDropShadow(LuxShadowSpec(LuxColors.ShadowDark, 26.dp, 10.dp, 14.dp, 150.dp))
                .border(10.dp, LuxColors.VinylHalo, CircleShape)
                .padding(10.dp)
                .graphicsLayer { rotationZ = rotationDeg }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        0.0f to LuxColors.Inverse,
                        0.30f to LuxColors.Inverse,
                        0.31f to LuxColors.Ink,
                        1.0f to LuxColors.Ink,
                    ),
                ).clickable(onClick = onClick),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f
            for (factor in listOf(0.94f, 0.84f, 0.74f)) {
                drawCircle(LuxColors.VinylGroove, radius * factor, center, style = Stroke(1.dp.toPx()))
            }
        }
        LuxCover(coverKey = coverKey, cornerRadius = 120.dp, modifier = Modifier.fillMaxSize(0.64f))
    }
}

@Composable
private fun TonearmOverlay(angleDeg: Float) {
    Box(
        modifier =
            Modifier
                .offset(x = 7.dp, y = (-30).dp)
                .width(22.dp)
                .height(128.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 11f / 128f)
                    rotationZ = angleDeg
                },
    ) {
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.TopCenter,
                    ).padding(top = 11.dp)
                    .width(4.dp)
                    .height(106.dp)
                    .background(LuxColors.SurfaceHigh),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .size(22.dp)
                    .luxNeuUpSmall(11.dp)
                    .clip(CircleShape)
                    .background(LuxColors.SurfaceHigh)
                    .border(6.dp, LuxColors.Line, CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(16.dp)
                    .height(24.dp)
                    .luxNeuUpSmall(4.dp)
                    .clip(RoundedCornerShape(LuxDimens.RadiusXs))
                    .background(LuxColors.SurfaceHigh),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineMoreCard(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val timers = listOf("", "15 分", "30 分", "45 分", "60 分")
    LuxCard(
        cornerRadius = LuxDimens.RadiusBlock,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            BasicText("定时关闭", style = LuxType.Kicker, modifier = Modifier.padding(bottom = 8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                timers.forEach { timer ->
                    LuxChip(
                        text = timer.ifEmpty { "关闭" },
                        onClick = { onEvent(LuxUiEvent.SetSleepTimer(timer)) },
                        selected = state.sleepTimer == timer,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            MoreActionRow(LuxIcon.Share, "分享") { onEvent(LuxUiEvent.ShowToast("分享（预览中不执行）")) }
            MoreActionRow(LuxIcon.Download, "下载") { onEvent(LuxUiEvent.ShowToast("下载（预览中不执行）")) }
        }
    }
}

@Composable
private fun MoreActionRow(
    icon: LuxIcon,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(
                    minHeight = 46.dp,
                ).clickable(onClick = onClick)
                .padding(horizontal = 4.dp),
    ) {
        LuxIconView(icon = icon, tint = LuxColors.Muted, size = 20.dp)
        BasicText(text = label, style = LuxType.RowTitle)
    }
}

@Composable
private fun PlayerActionStrip(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val song = state.currentSong
    val liked = song.id in state.likedIds
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
    ) {
        LuxIconButton(
            icon = if (liked) LuxIcon.Heart else LuxIcon.HeartO,
            contentDescription = if (liked) "取消喜欢" else "喜欢",
            onClick = { onEvent(LuxUiEvent.ToggleLike(song.id)) },
            style = LuxIconButtonStyle(tint = if (liked) LuxColors.Accent else LuxColors.Ink, raised = false),
        )
        Box {
            LuxIconButton(LuxIcon.Comment, "评论 2385", { onEvent(LuxUiEvent.SetPlayerView(LuxPlayerView.Comments)) })
            BasicText(
                text = "2385",
                style =
                    LuxType.TabularCaption.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxColors.Ink,
                    ),
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(LuxColors.Page)
                        .padding(horizontal = 2.dp),
            )
        }
        LuxIconButton(LuxIcon.Dots, "更多", { onEvent(LuxUiEvent.ToggleMoreSheet) })
    }
}

@Composable
internal fun PlayerFooterControls(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val song = state.currentSong
    val frac = (state.positionSec / song.durationSec.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 22.dp)) {
        LuxSeekBar(fraction = frac, onSeek = { onEvent(LuxUiEvent.SeekFraction(it)) })
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            BasicText(LuxMockCatalog.formatTime(state.positionSec), style = LuxType.TabularCaption)
            BasicText(LuxMockCatalog.formatTime(song.durationSec.toDouble()), style = LuxType.TabularCaption)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            LuxIconButton(state.playMode.icon, state.playMode.label, { onEvent(LuxUiEvent.CyclePlayMode) })
            LuxIconButton(
                LuxIcon.Prev,
                "上一首",
                { onEvent(LuxUiEvent.PrevTrack) },
                style = LuxIconButtonStyle(iconSize = 28.dp),
            )
            LuxPlayButton(state.playing, { onEvent(LuxUiEvent.TogglePlay) }, size = 68.dp, iconSize = 34.dp)
            LuxIconButton(
                LuxIcon.Next,
                "下一首",
                { onEvent(LuxUiEvent.NextTrack) },
                style = LuxIconButtonStyle(iconSize = 28.dp),
            )
            LuxIconButton(LuxIcon.Queue, "播放队列", { onEvent(LuxUiEvent.OpenQueue) })
        }
    }
}
