package cn.lux.music.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Styling bundle for [LuxIconButton] keeping call sites clean and parameter counts compact. */
public data class LuxIconButtonStyle(
    val size: Dp = LuxDimens.IconButtonSize,
    val iconSize: Dp = 24.dp,
    val tint: Color = LuxColors.Muted,
    val raised: Boolean = true,
)

/** Circular raised icon button matching `.iconbtn` (44dp default, press translateY 1dp). */
@Composable
public fun LuxIconButton(
    icon: LuxIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: LuxIconButtonStyle = LuxIconButtonStyle(),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val radius = style.size / 2
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(style.size)
                .graphicsLayer { translationY = if (pressed) 1.dp.toPx() else 0f }
                .iconButtonSurface(style.raised, pressed, radius)
                .semantics { this.contentDescription = contentDescription }
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ),
    ) {
        LuxIconView(icon = icon, tint = style.tint, size = style.iconSize)
    }
}

private fun Modifier.iconButtonSurface(
    raised: Boolean,
    pressed: Boolean,
    radius: Dp,
): Modifier =
    when {
        !raised -> this
        pressed ->
            this
                .clip(CircleShape)
                .background(LuxColors.Page)
                .luxInnerShadow(cornerRadius = radius)
                .border(1.dp, LuxColors.SubtleWhiteBorder, CircleShape)
        else ->
            this
                .luxNeuUpSmall(radius)
                .clip(CircleShape)
                .background(LuxBrushes.RaisedFill)
                .border(1.dp, LuxColors.SubtleWhiteBorder, CircleShape)
    }

public enum class LuxButtonStyle {
    Primary,
    Tonal,
    Plain,
}

public data class LuxButtonConfig(
    val style: LuxButtonStyle = LuxButtonStyle.Primary,
    val small: Boolean = false,
    val icon: LuxIcon? = null,
)

/** Capsule button matching `.btn.primary`, `.btn.tonal`, `.btn.plain` in regular and small sizes. */
@Composable
public fun LuxButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: LuxButtonConfig = LuxButtonConfig(),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val height = if (config.small) LuxDimens.SmallButtonHeight else LuxDimens.ButtonHeight
    val shape = RoundedCornerShape(LuxDimens.RadiusFull)
    val fg = buttonForeground(config.style)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        modifier =
            modifier
                .height(height)
                .graphicsLayer { translationY = if (pressed) 1.dp.toPx() else 0f }
                .buttonSurface(config.style, pressed, shape, height / 2)
                .border(1.dp, LuxColors.SubtleWhiteBorder.copy(alpha = 0.45f), shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = if (config.small) 12.dp else 18.dp),
    ) {
        if (config.icon != null) {
            LuxIconView(icon = config.icon, tint = fg, size = if (config.small) 16.dp else 20.dp)
        }
        BasicText(
            text = text,
            style = LuxType.RowTitle.copy(fontSize = if (config.small) 13.sp else 15.sp, color = fg),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun buttonForeground(style: LuxButtonStyle): Color =
    when (style) {
        LuxButtonStyle.Primary -> LuxColors.OnAccent
        LuxButtonStyle.Tonal -> LuxColors.OnPc
        LuxButtonStyle.Plain -> LuxColors.Muted
    }

private fun Modifier.buttonSurface(
    style: LuxButtonStyle,
    pressed: Boolean,
    shape: RoundedCornerShape,
    radius: Dp,
): Modifier {
    val brush: Brush =
        when (style) {
            LuxButtonStyle.Primary -> LuxBrushes.PrimaryFill
            LuxButtonStyle.Tonal -> LuxBrushes.TonalFill
            LuxButtonStyle.Plain -> LuxBrushes.RaisedFill
        }
    return if (pressed && style != LuxButtonStyle.Primary) {
        this.clip(shape).background(LuxColors.Page).luxInnerShadow(cornerRadius = radius)
    } else {
        this.luxNeuUpSmall(radius).clip(shape).background(brush)
    }
}

private data class ChipVisualSpec(
    val text: String,
    val selected: Boolean,
    val leadingIcon: LuxIcon?,
)

/** Pill chip matching `.chip` / `.chip.on`. */
@Composable
public fun LuxChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: LuxIcon? = null,
) {
    LuxChipContainer(ChipVisualSpec(text, selected, leadingIcon), onClick, modifier, null)
}

/** Pill chip with a custom trailing slot (e.g. search history remove button). */
@Composable
public fun LuxRemovableChip(
    text: String,
    onClick: () -> Unit,
    leadingIcon: LuxIcon,
    trailing: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    LuxChipContainer(ChipVisualSpec(text, false, leadingIcon), onClick, modifier, trailing)
}

@Composable
private fun LuxChipContainer(
    spec: ChipVisualSpec,
    onClick: () -> Unit,
    modifier: Modifier,
    trailing: (@Composable RowScope.() -> Unit)?,
) {
    val shape = RoundedCornerShape(LuxDimens.RadiusFull)
    val fg = if (spec.selected) LuxColors.OnAccent else LuxColors.Muted
    val bg = if (spec.selected) LuxBrushes.PrimaryFill else LuxBrushes.RaisedFill
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            modifier
                .height(32.dp)
                .luxNeuUpSmall(16.dp)
                .clip(shape)
                .background(bg)
                .border(1.dp, LuxColors.SubtleWhiteBorder.copy(alpha = 0.5f), shape)
                .clickable(onClick = onClick)
                .padding(start = 14.dp, end = if (trailing != null) 4.dp else 14.dp),
    ) {
        if (spec.leadingIcon != null) {
            LuxIconView(icon = spec.leadingIcon, tint = if (spec.selected) fg else LuxColors.Faint, size = 16.dp)
        }
        BasicText(
            text = spec.text,
            style = LuxType.RowTitle.copy(fontSize = 13.sp, color = fg),
            maxLines = 1,
        )
        trailing?.invoke(this)
    }
}

/** Inset search/text input matching `.sfield` with deep inset + green focus ring. */
@Composable
public fun LuxSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    showSearchIcon: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(LuxDimens.RadiusFull)
    val ring = if (focused) LuxColors.FocusRing else LuxColors.SubtleWhiteBorder.copy(alpha = 0.4f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(shape)
                .background(LuxColors.Page)
                .luxInnerShadow(
                    blurRadius = if (focused) 10.dp else 7.dp,
                    offset = if (focused) 5.dp else 3.dp,
                    cornerRadius = 22.dp,
                ).border(if (focused) 2.dp else 1.dp, ring, shape)
                .padding(start = 14.dp, end = 6.dp),
    ) {
        if (showSearchIcon) {
            LuxIconView(icon = LuxIcon.Magnify, tint = LuxColors.Muted, size = 20.dp)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                BasicText(
                    text = placeholder,
                    style = LuxType.Body.copy(fontSize = 15.sp, color = LuxColors.Faint),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LuxType.Body.copy(fontSize = 15.sp, color = LuxColors.Ink),
                cursorBrush = SolidColor(LuxColors.AccentInk),
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            )
        }
        if (value.isNotEmpty() && showSearchIcon) {
            LuxIconButton(
                icon = LuxIcon.Close,
                contentDescription = "清除",
                onClick = { onValueChange("") },
                style = LuxIconButtonStyle(size = 32.dp, iconSize = 18.dp, raised = false),
            )
        }
    }
}
