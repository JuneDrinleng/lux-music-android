package cn.lux.music.feature.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxButton
import cn.lux.music.designsystem.LuxButtonConfig
import cn.lux.music.designsystem.LuxButtonStyle
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconButton
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxShadowSpec
import cn.lux.music.designsystem.LuxType
import cn.lux.music.designsystem.luxDropShadow
import cn.lux.music.designsystem.luxInnerShadow
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.model.PreviewSong
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun BoxScope.QueueOverlay(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    AnimatedVisibility(visible = state.queueOpen, enter = fadeIn(tween(300)), exit = fadeOut(tween(300))) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(LuxColors.Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onEvent(LuxUiEvent.CloseQueue) },
                    ),
        )
    }
    AnimatedVisibility(
        visible = state.queueOpen,
        enter =
            slideInVertically(
                animationSpec =
                    tween(
                        320,
                        easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f),
                    ),
                initialOffsetY = {
                    it
                },
            ),
        exit = slideOutVertically(animationSpec = tween(260), targetOffsetY = { it }),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        QueueSheetContent(state = state, onEvent = onEvent)
    }
}

@Composable
private fun QueueSheetContent(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val sheetShape = RoundedCornerShape(topStart = LuxDimens.RadiusSheet, topEnd = LuxDimens.RadiusSheet)
    val sheetShadow =
        LuxShadowSpec(LuxColors.ShadowDark, 30.dp, offsetY = (-10).dp, cornerRadius = LuxDimens.RadiusSheet)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .luxDropShadow(sheetShadow)
                .clip(sheetShape)
                .background(LuxColors.Page)
                .padding(bottom = navBottom),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(LuxColors.Line),
        )
        QueueHeader(state = state, onEvent = onEvent)
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            itemsIndexed(state.queue) { index, songId ->
                val song = LuxMockCatalog.songById[songId] ?: return@itemsIndexed
                QueueRow(song = song, isCurrent = index == state.queueIndex) {
                    onEvent(LuxUiEvent.PlayQueueIndex(index))
                }
            }
        }
    }
}

@Composable
private fun QueueHeader(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = "播放队列 · 正在播放 ${state.queueIndex + 1}",
                style = LuxType.SectionTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = "${state.fromName} · ${state.queue.size} 首",
                style = LuxType.RowSub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LuxButton(
            text = "清空",
            onClick = { onEvent(LuxUiEvent.ClearQueue) },
            config = LuxButtonConfig(style = LuxButtonStyle.Tonal, small = true, icon = LuxIcon.Broom),
        )
        LuxIconButton(LuxIcon.Close, "关闭", { onEvent(LuxUiEvent.CloseQueue) })
    }
}

@Composable
private fun QueueRow(
    song: PreviewSong,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val rowShape = RoundedCornerShape(LuxDimens.RadiusMd)
    val rowBg =
        if (isCurrent) {
            Modifier.clip(rowShape).background(LuxColors.Page).luxInnerShadow(cornerRadius = LuxDimens.RadiusMd)
        } else {
            Modifier.clip(rowShape)
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .then(rowBg)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        LuxCover(coverKey = song.cover, size = 36.dp, cornerRadius = LuxDimens.RadiusSm)
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = song.title,
                style = LuxType.RowTitle.copy(color = if (isCurrent) LuxColors.OnPc else LuxColors.Ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                text = song.artist,
                style = LuxType.RowSub.copy(color = if (isCurrent) LuxColors.OnPc else LuxColors.Muted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isCurrent) {
            LuxIconView(icon = LuxIcon.Equalizer, tint = LuxColors.AccentInk, size = 20.dp)
        } else {
            BasicText(
                text = LuxMockCatalog.formatTime(song.durationSec.toDouble()),
                style = LuxType.TabularCaption.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
            )
        }
    }
}
