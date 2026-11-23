package cn.lux.music.feature.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxButton
import cn.lux.music.designsystem.LuxButtonConfig
import cn.lux.music.designsystem.LuxButtonStyle
import cn.lux.music.designsystem.LuxCard
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconButton
import cn.lux.music.designsystem.LuxIconButtonStyle
import cn.lux.music.designsystem.LuxPlayButton
import cn.lux.music.designsystem.LuxType
import cn.lux.music.feature.state.LuxSubPage
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun RoamTabScreen(
    state: LuxUiState,
    scrollState: ScrollState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val song = state.currentSong
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = LuxDimens.Gutter,
                    end = LuxDimens.Gutter,
                    top = topInset + 8.dp,
                    bottom = 110.dp + bottomInset,
                ),
    ) {
        LuxTopSearchBar(
            onOpenSettings = { onEvent(LuxUiEvent.PushPage(LuxSubPage.Settings)) },
            onOpenSearch = { onEvent(LuxUiEvent.PushPage(LuxSubPage.Search)) },
        )
        Spacer(Modifier.height(4.dp))
        LuxCover(
            coverKey = song.cover,
            cornerRadius = LuxDimens.RadiusCard,
            modifier =
                Modifier
                    .widthIn(max = 270.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onEvent(LuxUiEvent.OpenPlayer) },
        )
        Spacer(Modifier.height(18.dp))
        BasicText(song.title, style = LuxType.DetailTitle.copy(textAlign = TextAlign.Center))
        BasicText(song.artist, style = LuxType.Body.copy(color = LuxColors.Muted, textAlign = TextAlign.Center))
        Spacer(Modifier.height(14.dp))
        LuxCard(cornerRadius = LuxDimens.RadiusBlock, modifier = Modifier.fillMaxWidth()) {
            LuxLyricPreview(state, onEvent, Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
        }
        RoamTransportBlock(state = state, onEvent = onEvent)
    }
}

@Composable
private fun RoamTransportBlock(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val song = state.currentSong
    val isLiked = song.id in state.likedIds
    Spacer(Modifier.height(16.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        LuxIconButton(
            icon = if (isLiked) LuxIcon.Heart else LuxIcon.HeartO,
            contentDescription = if (isLiked) "取消喜欢" else "喜欢",
            onClick = { onEvent(LuxUiEvent.ToggleLike(song.id)) },
            style = LuxIconButtonStyle(size = 52.dp, tint = if (isLiked) LuxColors.Accent else LuxColors.Ink),
        )
        LuxPlayButton(state.playing, { onEvent(LuxUiEvent.TogglePlay) }, size = 68.dp, iconSize = 34.dp)
        LuxIconButton(
            icon = LuxIcon.Next,
            contentDescription = "下一首",
            onClick = { onEvent(LuxUiEvent.NextTrack) },
            style = LuxIconButtonStyle(size = 52.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    BasicText(
        text = "无尽电台 · 根据今日推荐连续播放",
        style = LuxType.RowSub.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
    )
    if (state.fromName != "无尽电台") {
        Spacer(Modifier.height(14.dp))
        LuxButton(
            text = "开始无尽电台",
            onClick = { onEvent(LuxUiEvent.StartRadio) },
            config = LuxButtonConfig(style = LuxButtonStyle.Tonal, small = true),
        )
    }
}
