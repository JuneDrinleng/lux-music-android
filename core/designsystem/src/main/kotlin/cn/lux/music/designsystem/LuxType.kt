package cn.lux.music.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Exact typographic scale mapped from `design.md` Section 3 and `ui/index.html`. */
public object LuxType {
    private val Family: FontFamily = FontFamily.SansSerif

    public val Body: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            color = LuxColors.Ink,
        )

    public val LargeTitle: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 34.sp,
            lineHeight = 41.sp,
            fontWeight = FontWeight.Bold,
            color = LuxColors.Ink,
        )

    public val Title2: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = LuxColors.Ink,
        )

    public val SectionTitle: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 20.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            color = LuxColors.Ink,
        )

    public val PageTitle: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = LuxColors.Ink,
        )

    public val DetailTitle: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            color = LuxColors.Ink,
        )

    public val RowTitle: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = LuxColors.Ink,
        )

    public val RowSub: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Normal,
            color = LuxColors.Muted,
        )

    public val Kicker: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = LuxColors.Muted,
        )

    public val MiniTitle: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = LuxColors.Ink,
        )

    public val MiniSub: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Normal,
            color = LuxColors.Muted,
        )

    public val NavLabel: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            letterSpacing = 0.3.sp,
            fontWeight = FontWeight.SemiBold,
            color = LuxColors.Muted,
        )

    public val NavLabelSelected: TextStyle =
        NavLabel.copy(
            fontWeight = FontWeight.Bold,
            color = LuxColors.AccentInk,
        )

    public val LyricPreviewCurrent: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = LuxColors.Ink,
        )

    public val LyricLine: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = LuxColors.Muted,
        )

    public val LyricLineActive: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            color = LuxColors.Ink,
        )

    public val LyricExtra: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
            color = LuxColors.Muted,
        )

    public val TabularCaption: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum",
            color = LuxColors.Faint,
        )

    public val TabularRank: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFeatureSettings = "tnum",
            color = LuxColors.Ink,
        )

    public val StoryHeroNumber: TextStyle =
        TextStyle(
            fontFamily = Family,
            fontSize = 46.sp,
            lineHeight = 57.sp,
            letterSpacing = (-2).sp,
            fontWeight = FontWeight.ExtraBold,
            fontFeatureSettings = "tnum",
            color = LuxColors.AccentInk,
        )
}
