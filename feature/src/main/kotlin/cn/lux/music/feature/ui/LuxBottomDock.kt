package cn.lux.music.feature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.lux.music.designsystem.LuxBrushes
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconButton
import cn.lux.music.designsystem.LuxIconButtonStyle
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxPlayButton
import cn.lux.music.designsystem.LuxShadowSpec
import cn.lux.music.designsystem.LuxType
import cn.lux.music.designsystem.luxDropShadow
import cn.lux.music.designsystem.luxInnerShadow
import cn.lux.music.feature.state.LuxTab
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

@Composable
internal fun BoxScope.LuxBottomDock(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dockBottomPad = if (navBottom > 12.dp) navBottom else 12.dp
    Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
        if (state.tab != LuxTab.Roam) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(190.dp + navBottom)
                        .background(LuxBrushes.DockFade),
            )
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = dockBottomPad)) {
            if (state.tab != LuxTab.Roam) {
                MiniPlayerBar(state = state, onEvent = onEvent)
            }
            BottomNavRow(activeTab = state.tab, onSelect = { onEvent(LuxUiEvent.SelectTab(it)) })
        }
    }
}

@Composable
private fun MiniPlayerBar(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val song = state.currentSong
    val frac = (state.positionSec / song.durationSec.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
    val shape = RoundedCornerShape(LuxDimens.MiniRadius)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 9.dp)
                .height(LuxDimens.MiniHeight)
                .luxDropShadow(
                    LuxShadowSpec(
                        LuxColors.ShadowLight.copy(alpha = 0.6f),
                        9.dp,
                        (-3).dp,
                        (-3).dp,
                        LuxDimens.MiniRadius,
                    ),
                ).luxDropShadow(LuxShadowSpec(Color(0x2E767462), 11.dp, 4.dp, 5.dp, LuxDimens.MiniRadius))
                .clip(shape)
                .background(LuxBrushes.RaisedFill)
                .border(1.dp, Color(0x59FFFFFA), shape),
    ) {
        Box(modifier = Modifier.fillMaxWidth(frac).fillMaxHeight().background(LuxBrushes.MiniProgressFill))
        MiniPlayerRow(state = state, onEvent = onEvent)
    }
}

@Composable
private fun MiniPlayerRow(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    val song = state.currentSong
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 9.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxHeight().clickable { onEvent(LuxUiEvent.OpenPlayer) },
        ) {
            LuxCover(coverKey = song.cover, size = 40.dp, cornerRadius = 20.dp)
            Column(modifier = Modifier.weight(1f)) {
                BasicText(song.title, style = LuxType.MiniTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(song.artist, style = LuxType.MiniSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        LuxPlayButton(state.playing, { onEvent(LuxUiEvent.TogglePlay) }, size = 38.dp, iconSize = 20.dp)
        LuxIconButton(
            icon = LuxIcon.Queue,
            contentDescription = "播放队列",
            onClick = { onEvent(LuxUiEvent.OpenQueue) },
            style = LuxIconButtonStyle(size = 38.dp, iconSize = 20.dp),
        )
    }
}

@Composable
private fun BottomNavRow(
    activeTab: LuxTab,
    onSelect: (LuxTab) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(LuxDimens.TabsHeight)
                .padding(start = 7.dp, end = 7.dp, top = 12.dp),
    ) {
        LuxTab.entries.forEach { tab ->
            BottomNavKey(
                tab = tab,
                selected = tab == activeTab,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BottomNavKey(
    tab: LuxTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val indShape = RoundedCornerShape(13.dp)
    val indMod =
        if (selected) {
            Modifier
                .size(33.dp)
                .clip(indShape)
                .background(LuxBrushes.SelectedFill)
                .luxInnerShadow(LuxColors.NavSelectedInDark, LuxColors.NavSelectedInLight, 5.dp, 2.dp, 13.dp)
        } else {
            Modifier
                .size(33.dp)
                .luxDropShadow(LuxShadowSpec(LuxColors.ShadowLight.copy(alpha = 0.8f), 5.dp, (-2).dp, (-2).dp, 13.dp))
                .luxDropShadow(LuxShadowSpec(LuxColors.ShadowDark, 5.dp, 2.dp, 3.dp, 13.dp))
                .clip(indShape)
                .background(LuxBrushes.RaisedFill)
        }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = indMod) {
            LuxIconView(
                icon = if (selected) tab.iconOn else tab.icon,
                tint = if (selected) LuxColors.AccentInk else LuxColors.Muted,
                size = 20.dp,
            )
        }
        BasicText(text = tab.label, style = if (selected) LuxType.NavLabelSelected else LuxType.NavLabel)
    }
}
