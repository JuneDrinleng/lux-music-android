package cn.lux.music.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Soft elevated card matching `.card` / `.grp` with `--soft-card-shadow`. */
@Composable
public fun LuxCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = LuxDimens.RadiusBlock,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val base =
        modifier
            .luxSoftCardShadow(cornerRadius)
            .clip(shape)
            .background(LuxColors.Card)
            .border(1.dp, LuxColors.CardBorder, shape)
    val interactive = if (onClick != null) base.clickable(onClick = onClick) else base
    Box(modifier = interactive, content = content)
}

/** Grouped card container with `22.dp` radius matching `.grp`. */
@Composable
public fun LuxGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(LuxDimens.RadiusBlock)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .luxSoftCardShadow(LuxDimens.RadiusBlock)
                .clip(shape)
                .background(LuxColors.Card)
                .border(1.dp, LuxColors.CardBorder, shape),
        content = content,
    )
}

/** 1px inner light divider matching `.grp > * + *`. */
@Composable
public fun LuxGroupDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LuxColors.DividerHighlight),
    )
}

/** Inset segmented selector matching `.seg` (44dp) and `.seg.sm` (36dp). */
@Composable
public fun LuxSegmentedControl(
    items: List<Pair<String, LuxIcon?>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val height = if (compact) 36.dp else 44.dp
    val pad = if (compact) 3.dp else 4.dp
    val fontSize = if (compact) 12.sp else 13.sp
    val shape = RoundedCornerShape(LuxDimens.RadiusFull)
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .height(height)
                .clip(shape)
                .background(LuxColors.Page)
                .luxInnerShadow(cornerRadius = height / 2)
                .border(1.dp, LuxColors.SubtleWhiteBorder.copy(alpha = 0.4f), shape)
                .padding(pad),
    ) {
        items.forEachIndexed { index, (label, icon) ->
            val on = index == selectedIndex
            val itemShape = RoundedCornerShape(LuxDimens.RadiusFull)
            val fg = if (on) LuxColors.OnAccent else LuxColors.Muted
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(itemShape)
                        .then(if (on) Modifier.background(LuxBrushes.PrimaryFill) else Modifier)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 8.dp),
            ) {
                if (icon != null) LuxIconView(icon = icon, tint = fg, size = 16.dp)
                BasicText(
                    text = label,
                    style = LuxType.RowTitle.copy(fontSize = fontSize, color = fg),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Neumorphic switch matching `.switch` (52x32, 24x24 thumb, 300ms spring-like cubic). */
@Composable
public fun LuxSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val trackColor by
        animateColorAsState(
            targetValue = if (checked) LuxColors.SwitchOnTrack else LuxColors.Page,
            animationSpec = tween(300),
            label = "switchTrack",
        )
    val thumbOffset by
        animateDpAsState(
            targetValue = if (checked) 24.dp else 4.dp,
            animationSpec = tween(300, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
            label = "switchThumb",
        )
    val trackShape = RoundedCornerShape(16.dp)
    Box(
        modifier =
            modifier
                .width(52.dp)
                .height(32.dp)
                .clip(trackShape)
                .background(trackColor)
                .luxInnerShadow(cornerRadius = 16.dp)
                .border(1.dp, LuxColors.SubtleWhiteBorder.copy(alpha = 0.5f), trackShape)
                .semantics { contentDescription = label }
                .clickable(role = Role.Switch) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier =
                Modifier
                    .offset {
                        androidx.compose.ui.unit
                            .IntOffset(thumbOffset.roundToPx(), 4.dp.roundToPx())
                    }.size(24.dp)
                    .luxNeuUpSmall(12.dp)
                    .clip(CircleShape)
                    .background(if (checked) LuxBrushes.PrimaryFill else LuxBrushes.RaisedFill),
        )
    }
}

/** Lyric translation / romanization toggle pill matching `.tgl`. */
@Composable
public fun LuxTogglePill(
    text: String,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
) {
    val shape = RoundedCornerShape(LuxDimens.RadiusFull)
    val fg = if (checked) LuxColors.OnAccent else LuxColors.Muted
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .height(30.dp)
                .defaultMinSize(minWidth = 44.dp)
                .alpha(if (dimmed) 0.5f else 1f)
                .luxNeuUpSmall(15.dp)
                .clip(shape)
                .background(if (checked) LuxBrushes.PrimaryFill else LuxBrushes.RaisedFill)
                .border(1.dp, LuxColors.SubtleWhiteBorder.copy(alpha = 0.45f), shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
    ) {
        BasicText(
            text = text,
            style = LuxType.RowTitle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg),
            maxLines = 1,
        )
    }
}

/** Circular raised avatar matching `.avatar` (36dp) and `.avatar.l` (72dp). */
@Composable
public fun LuxAvatar(
    initial: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val fontSize =
        when {
            size >= 64.dp -> 28.sp
            size <= 32.dp -> 13.sp
            else -> 15.sp
        }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .luxNeuUpSmall(size / 2)
                .clip(CircleShape)
                .background(LuxBrushes.RaisedFill)
                .border(1.dp, LuxColors.SubtleWhiteBorder.copy(alpha = 0.6f), CircleShape),
    ) {
        BasicText(
            text = initial,
            style = LuxType.RowTitle.copy(fontSize = fontSize, fontWeight = FontWeight.Bold, color = LuxColors.Muted),
        )
    }
}

/** Centered capsule toast matching `.toast` at 150.dp above bottom. */
@Composable
public fun LuxToast(
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (message == null) return
    val shape = RoundedCornerShape(LuxDimens.RadiusFull)
    Box(
        modifier =
            modifier
                .luxNeuUpSmall(20.dp)
                .clip(shape)
                .background(LuxColors.Inverse)
                .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        BasicText(
            text = message,
            style = LuxType.RowSub.copy(fontSize = 13.sp, color = LuxColors.OnInverse),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
