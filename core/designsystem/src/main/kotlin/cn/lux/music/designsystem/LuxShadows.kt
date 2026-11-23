package cn.lux.music.designsystem

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private data class InnerCastSpec(
    val color: Color,
    val dx: Float,
    val dy: Float,
    val blurPx: Float,
    val radiusPx: Float,
    val pad: Float,
)

/** Draws a CSS-like blurred outer drop shadow behind a rounded rectangle without clipping it. */
public fun Modifier.luxDropShadow(spec: LuxShadowSpec): Modifier =
    drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint()
            val frameworkPaint = paint.asFrameworkPaint()
            frameworkPaint.isAntiAlias = true
            frameworkPaint.color = spec.color.toArgb()
            val blurPx = spec.blurRadius.toPx()
            if (blurPx > 0.5f) {
                frameworkPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            }
            val spreadPx = spec.spread.toPx()
            val left = spec.offsetX.toPx() - spreadPx
            val top = spec.offsetY.toPx() - spreadPx
            val right = size.width + spec.offsetX.toPx() + spreadPx
            val bottom = size.height + spec.offsetY.toPx() + spreadPx
            val radiusPx = (spec.cornerRadius.toPx() + spreadPx).coerceAtLeast(0f)
            canvas.drawRoundRect(left, top, right, bottom, radiusPx, radiusPx, paint)
        }
    }

/** Draws a CSS-like inner shadow clipped inside a rounded rectangle for inset neumorphic surfaces. */
public fun Modifier.luxInnerShadow(
    darkColor: Color = LuxColors.ShadowDark,
    lightColor: Color = LuxColors.ShadowLight,
    blurRadius: Dp = 7.dp,
    offset: Dp = 3.dp,
    cornerRadius: Dp = LuxDimens.RadiusFull,
): Modifier =
    drawWithContent {
        drawContent()
        val r = cornerRadius.toPx().coerceAtMost(size.minDimension / 2f)
        val clip = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(r, r))) }
        val blurPx = blurRadius.toPx()
        val offsetPx = offset.toPx()
        val pad = blurPx * 2f + offsetPx + 8f
        clipPath(clip) {
            drawInnerCast(InnerCastSpec(darkColor, offsetPx, offsetPx, blurPx, r, pad))
            drawInnerCast(InnerCastSpec(lightColor, -offsetPx, -offsetPx, blurPx, r, pad))
        }
    }

private fun DrawScope.drawInnerCast(spec: InnerCastSpec) {
    val outer = Rect(-spec.pad, -spec.pad, size.width + spec.pad, size.height + spec.pad)
    val hole = Rect(spec.dx, spec.dy, size.width + spec.dx, size.height + spec.dy)
    val ring =
        Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(outer)
            addRoundRect(RoundRect(hole, CornerRadius(spec.radiusPx, spec.radiusPx)))
        }
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fw = paint.asFrameworkPaint()
        fw.isAntiAlias = true
        fw.color = spec.color.toArgb()
        if (spec.blurPx > 0.5f) {
            fw.maskFilter = BlurMaskFilter(spec.blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawPath(ring, paint)
    }
}

/** Matches `--soft-card-shadow` from `lux-earth-palette`. */
public fun Modifier.luxSoftCardShadow(cornerRadius: Dp): Modifier =
    this
        .luxDropShadow(
            LuxShadowSpec(LuxColors.SoftCardShadowPrimary, 15.dp, offsetY = 6.dp, cornerRadius = cornerRadius),
        ).luxDropShadow(
            LuxShadowSpec(LuxColors.SoftCardShadowSecondary, 3.dp, offsetY = 1.dp, cornerRadius = cornerRadius),
        )

/** Matches `--neu-up-sm` raised small button shadow. */
public fun Modifier.luxNeuUpSmall(cornerRadius: Dp): Modifier =
    this
        .luxDropShadow(LuxShadowSpec(LuxColors.ShadowLight, 7.dp, (-3).dp, (-3).dp, cornerRadius))
        .luxDropShadow(LuxShadowSpec(LuxColors.ShadowDark, 7.dp, 3.dp, 4.dp, cornerRadius))

/** Matches `--neu-up` larger raised surface shadow. */
public fun Modifier.luxNeuUp(cornerRadius: Dp): Modifier =
    this
        .luxDropShadow(LuxShadowSpec(LuxColors.ShadowLight.copy(alpha = 0.85f), 13.dp, (-5).dp, (-5).dp, cornerRadius))
        .luxDropShadow(LuxShadowSpec(LuxColors.ShadowDark, 14.dp, 5.dp, 6.dp, cornerRadius))

/** Matches final borderless cover shadow from `lux-seam-and-cover-polish`. */
public fun Modifier.luxCoverShadow(cornerRadius: Dp): Modifier =
    this
        .luxDropShadow(LuxShadowSpec(LuxColors.CoverShadow, 9.dp, offsetY = 3.dp, cornerRadius = cornerRadius))
        .luxDropShadow(LuxShadowSpec(LuxColors.CoverRing, 0.dp, cornerRadius = cornerRadius, spread = 1.dp))
