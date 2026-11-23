package cn.lux.music.feature.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lux.music.designsystem.LuxBrushes
import cn.lux.music.designsystem.LuxColors
import cn.lux.music.designsystem.LuxCover
import cn.lux.music.designsystem.LuxDimens
import cn.lux.music.designsystem.LuxIcon
import cn.lux.music.designsystem.LuxIconButton
import cn.lux.music.designsystem.LuxIconView
import cn.lux.music.designsystem.LuxShadowSpec
import cn.lux.music.designsystem.LuxTogglePill
import cn.lux.music.designsystem.LuxType
import cn.lux.music.designsystem.luxDropShadow
import cn.lux.music.designsystem.luxInnerShadow
import cn.lux.music.feature.model.LuxMockCatalog
import cn.lux.music.feature.model.LuxMockLyrics
import cn.lux.music.feature.model.PreviewSong
import cn.lux.music.feature.state.LuxPlayerView
import cn.lux.music.feature.state.LuxUiEvent
import cn.lux.music.feature.state.LuxUiState

internal data class PlaylistCardSpec(
    val name: String,
    val coverKey: String,
    val playCount: String? = null,
    val subtitle: String? = null,
    val fixedWidth: Dp? = 116.dp,
)

internal data class SongRowSpec(
    val song: PreviewSong,
    val isCurrent: Boolean,
    val isPlaying: Boolean,
    val rank: Int? = null,
    val showSources: Boolean = false,
)

@Composable
internal fun LuxTopSearchBar(
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 12.dp),
    ) {
        LuxIconButton(icon = LuxIcon.Cog, contentDescription = "设置", onClick = onOpenSettings)
        val barShape = RoundedCornerShape(LuxDimens.RadiusFull)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(barShape)
                    .background(LuxColors.Page)
                    .luxInnerShadow(cornerRadius = 20.dp)
                    .border(1.dp, LuxColors.SubtleWhiteBorder.copy(alpha = 0.4f), barShape)
                    .clickable(onClick = onOpenSearch)
                    .padding(horizontal = 14.dp),
        ) {
            LuxIconView(icon = LuxIcon.Magnify, tint = LuxColors.Muted, size = 20.dp)
            BasicText(
                text = "搜索歌曲、歌手、歌单...",
                style = LuxType.Body.copy(fontSize = 14.sp, color = LuxColors.Muted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun LuxPageTopBar(
    title: String,
    onBack: () -> Unit,
    right: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().height(LuxDimens.PageTopHeight).padding(horizontal = 8.dp),
    ) {
        LuxIconButton(icon = LuxIcon.ChevLeft, contentDescription = "返回", onClick = onBack)
        BasicText(
            text = title,
            style = LuxType.PageTitle.copy(textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (right != null) right() else Spacer(Modifier.width(44.dp))
    }
}

@Composable
internal fun LuxSectionHeader(
    title: String,
    actionText: String? = null,
    actionIcon: LuxIcon? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 10.dp),
    ) {
        BasicText(text = title, style = LuxType.SectionTitle)
        if (actionText != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.defaultMinSize(minHeight = 32.dp).clickable(onClick = onAction),
            ) {
                BasicText(text = actionText, style = LuxType.RowTitle.copy(color = LuxColors.AccentInk))
                if (actionIcon != null) LuxIconView(icon = actionIcon, tint = LuxColors.AccentInk, size = 18.dp)
            }
        }
    }
}

@Composable
internal fun LuxPlaylistCard(
    spec: PlaylistCardSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rootMod = if (spec.fixedWidth != null) modifier.width(spec.fixedWidth) else modifier
    val boxMod =
        if (spec.fixedWidth !=
            null
        ) {
            Modifier.size(spec.fixedWidth)
        } else {
            Modifier.fillMaxWidth().aspectRatio(1f)
        }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = rootMod.clickable(onClick = onClick)) {
        Box(modifier = boxMod) {
            LuxCover(coverKey = spec.coverKey, cornerRadius = LuxDimens.RadiusMd, modifier = Modifier.matchParentSize())
            if (spec.playCount != null) {
                PlayCountBadge(spec.playCount, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
        BasicText(
            text = spec.name,
            style = LuxType.RowTitle.copy(fontSize = 13.sp, lineHeight = 18.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (spec.subtitle != null) {
            BasicText(
                text = spec.subtitle,
                style = LuxType.RowSub.copy(fontSize = 12.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayCountBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        modifier =
            modifier
                .height(20.dp)
                .clip(RoundedCornerShape(LuxDimens.RadiusFull))
                .background(LuxColors.PlayBadgeBg)
                .padding(start = 2.dp, end = 6.dp),
    ) {
        LuxIconView(icon = LuxIcon.Play, tint = LuxColors.OnAccent, size = 14.dp)
        BasicText(text = text, style = LuxType.TabularCaption.copy(fontSize = 11.sp, color = LuxColors.OnAccent))
    }
}

@Composable
internal fun LuxSongRow(
    spec: SongRowSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = spec.song
    val subText =
        if (spec.showSources && song.sources.isNotEmpty()) {
            "${song.artist} · ${song.sources.joinToString("/") { LuxMockCatalog.sourceName(it) }}"
        } else {
            song.artist
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 60.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        if (spec.rank != null) {
            BasicText(
                text = spec.rank.toString(),
                style = LuxType.TabularRank.copy(textAlign = TextAlign.Center),
                modifier = Modifier.width(22.dp),
            )
        }
        LuxCover(coverKey = song.cover, size = 44.dp, cornerRadius = LuxDimens.RadiusSm)
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = song.title,
                style = LuxType.RowTitle.copy(color = if (spec.isCurrent) LuxColors.AccentInk else LuxColors.Ink),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(text = subText, style = LuxType.RowSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (spec.isCurrent && spec.isPlaying) {
            LuxIconView(icon = LuxIcon.Equalizer, tint = LuxColors.Accent, size = 20.dp)
        }
        BasicText(
            text = LuxMockCatalog.formatTime(song.durationSec.toDouble()),
            style = LuxType.TabularCaption.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
        )
    }
}

@Composable
internal fun LuxLyricPreview(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lines = state.currentLyrics
    val index = LuxMockLyrics.lineIndexAt(lines, state.positionSec)
    val current = lines.getOrNull(index)
    val next = lines.getOrNull(index + 1)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f).clickable { onEvent(LuxUiEvent.SetPlayerView(LuxPlayerView.Lyrics)) },
            ) {
                BasicText(text = current?.text ?: "纯音乐，请欣赏", style = LuxType.LyricPreviewCurrent)
                if (state.showTranslation && !current?.translation.isNullOrEmpty()) {
                    BasicText(text = current.translation, style = LuxType.LyricExtra)
                }
                if (state.showRomanization && !current?.romanization.isNullOrEmpty()) {
                    BasicText(text = current.romanization, style = LuxType.LyricExtra)
                }
                if (next != null) {
                    BasicText(
                        text = next.text,
                        style = LuxType.LyricExtra.copy(color = LuxColors.Faint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            LyricTogglePair(state = state, onEvent = onEvent)
        }
        if (state.showRomanization && !state.hasRomanization) {
            BasicText("本曲暂无音译", style = LuxType.RowSub.copy(fontSize = 12.sp), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
internal fun LyricTogglePair(
    state: LuxUiState,
    onEvent: (LuxUiEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        LuxTogglePill(
            "译",
            state.showTranslation,
            { onEvent(LuxUiEvent.ToggleTranslation) },
            dimmed = !state.hasTranslation,
        )
        LuxTogglePill(
            "音",
            state.showRomanization,
            { onEvent(LuxUiEvent.ToggleRomanization) },
            dimmed = !state.hasRomanization,
        )
    }
}

@Composable
internal fun LuxQuickCircleEntry(
    icon: LuxIcon,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.defaultMinSize(minWidth = 64.dp).clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(46.dp)
                    .luxDropShadow(LuxShadowSpec(LuxColors.ShadowLight, 7.dp, (-3).dp, (-3).dp, 23.dp))
                    .luxDropShadow(LuxShadowSpec(LuxColors.ShadowDark, 7.dp, 3.dp, 4.dp, 23.dp))
                    .clip(CircleShape)
                    .background(LuxBrushes.RaisedFill)
                    .border(1.dp, LuxColors.SubtleWhiteBorder, CircleShape),
        ) {
            LuxIconView(icon = icon, tint = LuxColors.Muted, size = 24.dp)
        }
        BasicText(text = label, style = LuxType.RowTitle.copy(fontSize = 13.sp, lineHeight = 18.sp))
    }
}
