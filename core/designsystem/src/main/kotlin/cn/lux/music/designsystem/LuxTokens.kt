package cn.lux.music.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Exact color tokens from the final `lux-earth-palette` cascade in `ui/index.html`. */
public object LuxColors {
    public val Page: Color = Color(0xFFF3F0E8)
    public val Card: Color = Color(0xFFF3F0E8)
    public val Inset: Color = Color(0xFFE5E1D7)
    public val Ink: Color = Color(0xFF292D28)
    public val Muted: Color = Color(0xFF65685E)
    public val Faint: Color = Color(0xFF6B6E64)
    public val Line: Color = Color(0xFFD0CFC1)
    public val Sep: Color = Color(0xFFE0DDD2)
    public val Accent: Color = Color(0xFF52654B)
    public val AccentInk: Color = Color(0xFF40543B)
    public val OnAccent: Color = Color(0xFFFFFEF9)
    public val Pc: Color = Color(0xFFE1E7D9)
    public val OnPc: Color = Color(0xFF40543B)
    public val Inverse: Color = Color(0xFF303A2D)
    public val OnInverse: Color = Color(0xFFFFFEF9)

    public val Ok: Color = Color(0xFFDCE8D7)
    public val OnOk: Color = Color(0xFF344E35)
    public val Warn: Color = Color(0xFFEEE0BF)
    public val OnWarn: Color = Color(0xFF71572B)
    public val Err: Color = Color(0xFFF0DED5)
    public val OnErr: Color = Color(0xFF944A37)

    public val Clay: Color = Color(0xFFA65E47)
    public val ClaySoft: Color = Color(0xFFEEE0D6)
    public val Ochre: Color = Color(0xFF8A682E)
    public val OchreSoft: Color = Color(0xFFEDE3CB)
    public val Sage: Color = Color(0xFF9BA98B)
    public val SageSoft: Color = Color(0xFFE1E7D9)

    public val SurfaceHigh: Color = Color(0xFFF8F6F0)
    public val SurfaceLow: Color = Color(0xFFEAE6DD)
    public val SwitchOnTrack: Color = Color(0xFFCBD6C1)

    public val Data1: Color = Color(0xFF607655)
    public val Data2: Color = Color(0xFFA4B092)
    public val Data3: Color = Color(0xFFBB8E70)
    public val Data4: Color = Color(0xFFD6C6A1)

    public val Scrim: Color = Color(0x4D252B21)
    public val PlayBadgeBg: Color = Color(0xB8262D23)
    public val CardBorder: Color = Color(0x14767462)
    public val SubtleWhiteBorder: Color = Color(0x8CFFFFFC)
    public val DividerHighlight: Color = Color(0xBFFFFFFF)
    public val VinylHalo: Color = Color(0xE6E1DFD3)
    public val VinylGroove: Color = Color(0x12FFFFFF)
    public val MiniProgressStart: Color = Color(0x1752654B)
    public val MiniProgressEnd: Color = Color(0x0652654B)
    public val FocusRing: Color = Color(0x3052654B)
    public val ShadowLight: Color = Color(0xEBFFFFFC)
    public val ShadowDark: Color = Color(0x30767462)
    public val SoftCardShadowPrimary: Color = Color(0x1C767462)
    public val SoftCardShadowSecondary: Color = Color(0x0D767462)
    public val CoverShadow: Color = Color(0x2667545B)
    public val CoverRing: Color = Color(0x0E5B4D52)
    public val NavSelectedInDark: Color = Color(0x38576949)
    public val NavSelectedInLight: Color = Color(0xCCFFFFFA)
}

/** Exact gradients from `lux-earth-palette` and bottom dock overrides. */
public object LuxBrushes {
    public val PrimaryFill: Brush =
        Brush.linearGradient(colors = listOf(Color(0xFF607655), Color(0xFF40543B)))
    public val RaisedFill: Brush =
        Brush.linearGradient(colors = listOf(LuxColors.SurfaceHigh, LuxColors.SurfaceLow))
    public val SelectedFill: Brush =
        Brush.linearGradient(colors = listOf(Color(0xFFDCE3D4), Color(0xFFEDF0E5)))
    public val TonalFill: Brush =
        Brush.linearGradient(colors = listOf(Color(0xFFEDF0E5), Color(0xFFDEE5D5)))
    public val SeekFill: Brush =
        Brush.horizontalGradient(colors = listOf(Color(0xFF9AAA87), Color(0xFF52654B)))
    public val MiniProgressFill: Brush =
        Brush.horizontalGradient(colors = listOf(LuxColors.MiniProgressStart, LuxColors.MiniProgressEnd))
    public val LikeCoverFill: Brush =
        Brush.linearGradient(colors = listOf(Color(0xFFF2E7DD), Color(0xFFE7D6C9)))
    public val CalendarFill: Brush =
        Brush.linearGradient(colors = listOf(Color(0xFFEFE7D1), Color(0xFFDFCDA8)))
    public val BarChartFill: Brush =
        Brush.verticalGradient(colors = listOf(Color(0xFFA4B092), Color(0xFF607655)))
    public val DockFade: Brush =
        Brush.verticalGradient(
            0.0f to LuxColors.Page.copy(alpha = 0f),
            0.29f to LuxColors.Page.copy(alpha = 0.76f),
            0.52f to LuxColors.Page.copy(alpha = 0.98f),
            0.72f to LuxColors.Page,
            1.0f to LuxColors.Page,
        )
    public val StatusFrostFade: Brush =
        Brush.verticalGradient(
            0.0f to LuxColors.Page.copy(alpha = 0.78f),
            0.55f to LuxColors.Page.copy(alpha = 0.34f),
            1.0f to LuxColors.Page.copy(alpha = 0f),
        )
}

/** Exact radii and layout spacing tokens matching `ui/index.html`. */
public object LuxDimens {
    public val RadiusCard: Dp = 34.dp
    public val RadiusBlock: Dp = 22.dp
    public val RadiusMd: Dp = 16.dp
    public val RadiusSm: Dp = 8.dp
    public val RadiusXs: Dp = 4.dp
    public val RadiusSheet: Dp = 28.dp
    public val RadiusFull: Dp = 1000.dp

    public val Gutter: Dp = 16.dp
    public val DockSpace: Dp = 166.dp
    public val PageTopHeight: Dp = 52.dp
    public val IconButtonSize: Dp = 44.dp
    public val ButtonHeight: Dp = 44.dp
    public val SmallButtonHeight: Dp = 32.dp
    public val MiniHeight: Dp = 60.dp
    public val MiniRadius: Dp = 30.dp
    public val TabsHeight: Dp = 66.dp
}

/** Parameter bundle for CSS-like drop shadow rendering. */
public data class LuxShadowSpec(
    val color: Color,
    val blurRadius: Dp,
    val offsetX: Dp = 0.dp,
    val offsetY: Dp = 0.dp,
    val cornerRadius: Dp = 0.dp,
    val spread: Dp = 0.dp,
)
