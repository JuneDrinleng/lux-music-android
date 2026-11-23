package cn.lux.music.feature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxAvatar
import cn.lux.music.designsystem.LuxButton
import cn.lux.music.designsystem.LuxButtonConfig
import cn.lux.music.designsystem.LuxButtonStyle
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxSegmentedControl
import cn.lux.music.designsystem.LuxType
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.model.LuxMockLyrics
import cn.lux.music.feature.model.PreviewComment
import cn.lux.music.feature.model.PreviewLyricLine
import cn.lux.music.feature.state.LuxPlayerView
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun ColumnScope.LyricsPlayerBody(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val lines = state.currentLyrics
    val activeIdx = LuxMockLyrics.lineIndexAt(lines, state.positionSec)
    val activeTime = lines.getOrNull(activeIdx)?.let { LuxMockCatalog.formatTime(it.timeSec) } ?: "0:00"
    PlayerTopBar(state = state, backToVinyl = true, onEvent = onEvent)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 6.dp),
    ) {
        LyricTogglePair(state = state, onEvent = onEvent)
        LuxButton(
            text = "黑胶",
            onClick = { onEvent(LuxUiEvent.SetPlayerView(LuxPlayerView.Vinyl)) },
            config = LuxButtonConfig(style = LuxButtonStyle.Plain, small = true, icon = LuxIcon.Album),
        )
    }
    if (state.showRomanization && !state.hasRomanization) {
        BasicText(
            "本曲暂无音译",
            style = LuxType.RowSub.copy(fontSize = 12.sp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
    }
    LyricsListSection(lines = lines, activeIdx = activeIdx, activeTime = activeTime, state = state, onEvent = onEvent)
    PlayerFooterControls(state = state, onEvent = onEvent)
}

@Composable
private fun ColumnScope.LyricsListSection(
    lines: List<PreviewLyricLine>,
    activeIdx: Int,
    activeTime: String,
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.currentSong.id, activeIdx) {
        if (lines.isNotEmpty() && activeIdx in lines.indices) {
            listState.animateScrollToItem(index = (activeIdx - 2).coerceAtLeast(0))
        }
    }
    if (lines.isEmpty()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
            BasicText("暂无歌词", style = LuxType.Body.copy(color = LuxColors.Muted))
        }
        return
    }
    LazyColumn(
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 28.dp),
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        itemsIndexed(lines) { index, line ->
            LyricLineItem(
                line = line,
                index = index,
                activeIdx = activeIdx,
                state = state,
                onClick = { onEvent(LuxUiEvent.SeekLyricLine(index)) },
            )
        }
        item {
            BasicText(
                text = "从 $activeTime 播放",
                style =
                    LuxType.RowTitle.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxColors.AccentInk,
                        textAlign = TextAlign.Center,
                    ),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun LyricLineItem(
    line: PreviewLyricLine,
    index: Int,
    activeIdx: Int,
    state: LuxUiState,
    onClick: () -> Unit,
) {
    val style =
        when {
            index == activeIdx -> LuxType.LyricLineActive
            index < activeIdx -> LuxType.LyricLine.copy(color = LuxColors.Faint)
            else -> LuxType.LyricLine
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
    ) {
        BasicText(line.text, style = style.copy(textAlign = TextAlign.Center))
        if (state.showTranslation && !line.translation.isNullOrEmpty()) {
            BasicText(line.translation, style = LuxType.LyricExtra.copy(textAlign = TextAlign.Center))
        }
        if (state.showRomanization && !line.romanization.isNullOrEmpty()) {
            BasicText(line.romanization, style = LuxType.LyricExtra.copy(textAlign = TextAlign.Center))
        }
    }
}

@Composable
internal fun ColumnScope.CommentsPlayerBody(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val comments =
        if (state.commentSort == "new") {
            LuxMockCatalog.newComments + LuxMockCatalog.hotComments
        } else {
            LuxMockCatalog.hotComments.sortedByDescending { it.baseLikes }
        }
    val totalCount = LuxMockCatalog.hotComments.size + 2380
    PlayerTopBar(state = state, backToVinyl = true, onEvent = onEvent)
    LuxSegmentedControl(
        items = listOf("热门" to null, "最新" to null),
        selectedIndex = if (state.commentSort == "new") 1 else 0,
        onSelect = { idx -> onEvent(LuxUiEvent.SetCommentSort(if (idx == 1) "new" else "hot")) },
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 6.dp),
    )
    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 22.dp)) {
        item {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
            ) {
                BasicText(if (state.commentSort == "new") "最新评论" else "热门评论", style = LuxType.SectionTitle)
                BasicText("$totalCount 条 · ${state.currentSong.title}", style = LuxType.RowSub)
            }
        }
        itemsIndexed(comments) { _, comment ->
            CommentRow(comment, comment.key in state.commentLikedKeys) {
                onEvent(LuxUiEvent.ToggleCommentLike(comment.key))
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CommentRow(
    comment: PreviewComment,
    liked: Boolean,
    onToggleLike: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            LuxAvatar(initial = comment.author.take(1), size = 32.dp)
            Column(modifier = Modifier.weight(1f)) {
                BasicText(comment.author, style = LuxType.RowTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                BasicText(comment.timeLabel, style = LuxType.TabularCaption)
                BasicText(
                    comment.body,
                    style = LuxType.Body.copy(lineHeight = 22.sp),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.defaultMinSize(minHeight = 32.dp).clickable(onClick = onToggleLike),
            ) {
                val color = if (liked) LuxColors.AccentInk else LuxColors.Faint
                LuxIconView(if (liked) LuxIcon.ThumbOn else LuxIcon.Thumb, color, size = 16.dp)
                BasicText(
                    (comment.baseLikes + if (liked) 1 else 0).toString(),
                    style = LuxType.TabularCaption.copy(color = color),
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(LuxColors.Sep))
    }
}
